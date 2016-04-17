package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot

import java.io.ByteArrayInputStream
import java.util.UUID
import javax.imageio.ImageIO

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao.{BallotDAO, DAO}
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.report.AnswerParser
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.snippet.SnippetHTMLValidator
import org.joda.time.DateTime

import scala.xml._

/**
  * Created by mattia on 06.07.15.
  */
@HCompPortal(builder = classOf[BallotPortalBuilder], autoInit = true)
class BallotPortalAdapter(val decorated: HCompPortalAdapter with AnswerRejection, val dao: DAO = new BallotDAO(),
						  val baseURL: String) extends HCompPortalAdapter {

	private var maxRetriesAfterRejectedAnswers = 10
	private var questionIdToQuery = collection.mutable.HashMap.empty[Long, HCompQuery]

	override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
		val (actualProperties, batchIdFromDB) =
			this.synchronized {
				val actualProperties: BallotProperties = properties match {
					case p: BallotProperties => p
					case _ =>
						new BallotProperties(Batch(0, UUID.randomUUID()), List(Asset(Array.empty[Byte], "application/pdf", "")), 1)
				}

				val batchIdFromDB: Long =
					dao.getBatchIdByUUID(actualProperties.batch.uuid).getOrElse(
						dao.createBatch(actualProperties.batch.allowedAnswersPerTurker, actualProperties.batch.uuid))

				(actualProperties, batchIdFromDB)
			}

		val html = query match {
			case q: HTMLQuery => q.html
			case _ => scala.xml.PCData(query.toString)
		}

		val htmlToDisplayOnBallotPage: NodeSeq = SnippetHTMLValidator(baseURL).fixFormAttributes(html)

		if ((htmlToDisplayOnBallotPage \\ "form").nonEmpty) {
			val notValid = (htmlToDisplayOnBallotPage \\ "form").exists(form => SnippetHTMLValidator(baseURL).hasInvalidInputElements(form))
			if (notValid) {
				logger.error("Form's content is not valid.")
				None
			} else {
				val permutation = dao.getPermutationById(actualProperties.permutationId)
				val methodHeight = if (permutation.get.methodOnTop) {
					permutation.get.relativeHeightTop
				} else {
					permutation.get.relativeHeightBottom
				}
				val prerequisiteHeight = if (permutation.get.methodOnTop) {
					permutation.get.relativeHeightBottom
				} else {
					permutation.get.relativeHeightTop
				}

				val snippetHeight = try {
					val inputImage = new ByteArrayInputStream(actualProperties.assets.find(_.contentType.equalsIgnoreCase("image/png")).get.binary)
					val reader = ImageIO.read(inputImage)
					reader.getHeight
				} catch {
					case e: Exception => 300
				}

				val (questionId: Long, link: String) = createQuestion(actualProperties, batchIdFromDB, htmlToDisplayOnBallotPage, methodHeight, prerequisiteHeight, snippetHeight)

				val externalQuery: ExternalQuery = ExternalQuery(link, query.title, "code", "target")

				addQueryToLog(questionId, externalQuery)
				val answer = decorated.sendQueryAndAwaitResult(
					externalQuery, actualProperties.propertiesForDecoratedPortal)
						.get.is[FreetextAnswer]

				val answerId = dao.getAnswerIdByOutputCode(answer.answer.trim)

				if (answerId.isDefined) {
					decorated.approveAndBonusAnswer(answer)
					dao.updateAnswer(answerId.get, accepted = true)
					val ans = dao.getAnswerById(answerId.get)
					logger.info(s"approving answer $answer of worker ${answer.responsibleWorkers.mkString(",")} to question ${ans.get.questionId}")
					extractSingleAnswerFromDatabase(ans.get.answerJson, htmlToDisplayOnBallotPage)
				}
				else {
					decorated.rejectAnswer(answer, "Invalid code")
					logger.info(s"rejecting answer $answer of worker ${answer.responsibleWorkers.mkString(",")} to question $questionId")
					if (maxRetriesAfterRejectedAnswers > 0) {
						maxRetriesAfterRejectedAnswers -= 1
						processQuery(query, actualProperties)
					} else {
						logger.error("Query reached the maximum number of retry attempts.")
						None
					}
				}
			}
		} else {
			logger.error("There exists no Form tag in the html page.")
			None
		}
	}

	def createQuestion(actualProperties: BallotProperties, batchIdFromDB: Long, htmlToDisplayOnBallotPage: NodeSeq, methodHeight: Double, prerequisiteHeight: Double, snippetHeight: Double): (Long, String) = {
		this.synchronized {
			val assetsId: Map[String, Long] =
				actualProperties.assets.map(asset => {
					asset.url -> dao.createAsset(asset.binary, asset.contentType, asset.filename)
				}).toMap

			val imageHeight = if (snippetHeight < 300) {
				300
			} else if (snippetHeight > 900) {
				900
			} else {
				snippetHeight
			}

			val newJsPlaceholder: String = "jsPlaceholder\"> var relativeHeightMethod = " + methodHeight + ";\n var relativeHeightPrerequisite = " + prerequisiteHeight + ";\n var snippetHeight = \"" + imageHeight + "px\";" +
					"\n\\$(function() { \\$(\"form\") })"

			var htmlWithValidLinks: String = htmlToDisplayOnBallotPage.toString().replaceAll("jsPlaceholder\">", newJsPlaceholder)

			val secret = Utils.generateSecret()

			assetsId.foreach(asset => {
				htmlWithValidLinks = htmlWithValidLinks.replaceAll(asset._1, "asset://" + asset._2 + "/" + secret)
			})

			val questionUUID = UUID.randomUUID()
			val questionId = dao.createQuestion(htmlWithValidLinks, batchIdFromDB, questionUUID, permutationId = actualProperties.permutationId, secret = secret)
			val link = s"$baseURL/showMTQuestion?q=$questionUUID&amp;s=$secret"
			assetsId.foreach(assetId => dao.mapQuestionToAssets(questionId, assetId._2))
			(questionId, link)
		}
	}

	def extractSingleAnswerFromDatabase(answerJson: String, html: NodeSeq): Option[HCompAnswer] = {
		val answerMap = AnswerParser.buildAnswerMap(answerJson)
		Some(HTMLQueryAnswer(answerMap, HTMLQuery(html)))
	}

	override def getDefaultPortalKey: String = BallotPortalAdapter.PORTAL_KEY

	override def cancelQuery(query: HCompQuery): Unit = decorated.cancelQuery(query)

	def addQueryToLog(questionId: Long, query: HCompQuery): Unit = {
		dao.synchronized {
			questionIdToQuery += questionId -> query
		}
	}

	def forcePoll(questionId: Long): Unit = {
		val q = questionIdToQuery(questionId)
		decorated match {
			case portal: ForcedQueryPolling => portal.poll(q)
		}
	}

	new Thread() {
		setDaemon(true)

		override def run(): Unit = {
			while (true) {
				val lastCheck = DateTime.now()
				Thread.sleep(1000)
				try {
					dao.getQuestionIDsAnsweredSince(lastCheck).foreach(q => forcePoll(q))
				} catch {
					case e: Exception => logger.error("exception when polling", e)
				}
			}
		}
	}.start()
}

object BallotPortalAdapter {
	val CONFIG_ACCESS_ID_KEY = "decoratedPortalKey"
	val CONFIG_BASE_URL = "baseURL"
	val PORTAL_KEY = "ballot"
}

class BallotPortalBuilder extends HCompPortalBuilder {

	val DECORATED_PORTAL_KEY_BALLOT = "decoratedPortalKey"
	val BASE_URL = "BaseURL"

	override def build: HCompPortalAdapter = new BallotPortalAdapter(
		HComp(params(DECORATED_PORTAL_KEY_BALLOT))
			.asInstanceOf[HCompPortalAdapter with AnswerRejection],
		baseURL = params(BASE_URL))

	override def expectedParameters: List[String] = List(DECORATED_PORTAL_KEY_BALLOT, BASE_URL)

	override def parameterToConfigPath: Map[String, String] = Map(
		DECORATED_PORTAL_KEY_BALLOT -> BallotPortalAdapter.CONFIG_ACCESS_ID_KEY,
		BASE_URL -> BallotPortalAdapter.CONFIG_BASE_URL
	)

	override def key: String = BallotPortalAdapter.PORTAL_KEY
}
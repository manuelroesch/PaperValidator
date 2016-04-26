package controllers


import java.security.SecureRandom
import javax.inject.Inject

import helper.QuestionHTMLFormatter
import models._
import org.joda.time.DateTime
import play.Configuration
import play.api.Logger
import play.api.mvc._

import scala.util.parsing.json.JSONObject

object Application  {
	val TEMPLATE_ID = 1L
	val TURKER_ID_KEY: String = "TurkerId"
}

class Application @Inject() (configuration: Configuration, questionService: QuestionService, answerService: AnswerService, assetService: AssetService, userService: UserService, batchService: BatchService, log: Log) extends Controller {

	def index = Action { request =>
			Ok(views.html.index())
	}

	def showAsset(id: Long, secret: String) = Action { request =>
		val parentQuestions = questionService.findByAssetId(id).filter(_.secret == secret)
		val turkerId: Option[String] = request.session.get(Application.TURKER_ID_KEY)

		val isAssetOfTemplate: Boolean = parentQuestions.exists(_.id.get == Application.TEMPLATE_ID)
		if (!isAssetOfTemplate && logAccessAndCheckIfExceedsAccessCount(request, turkerId.orNull)) {
			Unauthorized("We received too many requests by your IP address")
		} else {

			if (turkerId.isDefined || isAssetOfTemplate) {
				val asset = assetService.findById(id)
				val hasUnansweredQuestions: Boolean = !parentQuestions.forall(q => answerService.existsAcceptedAnswerForQuestionId(q.id.get))

				if (asset.isDefined && parentQuestions.nonEmpty && hasUnansweredQuestions) {
					val contentType = asset.get.contentType
					if (contentType.equalsIgnoreCase("application/pdf")) {
						Ok(asset.get.byteArray).as(contentType)
					} else {
						Ok(asset.get.byteArray)
					}
				} else {
					UnprocessableEntity("There exists no asset with id: " + id)
				}
			} else {
				Ok(views.html.login())
			}
		}
	}

	def sessionUser(request: Request[AnyContent]) = request.session.get(Application.TURKER_ID_KEY).filterNot(_.isEmpty)

	def showMTQuestion(uuid: String, secret: String, assignmentId: String, hitId: String, turkSubmitTo: String, workerId: String, target: String) = Action { request =>
		if (workerId.length > 5 && userService.findByTurkerId(workerId).isEmpty) {
			userService.create(workerId, new DateTime())
		}

		if (assignmentId == "ASSIGNMENT_ID_NOT_AVAILABLE") {
			def showAlreadyUsedMessage: Boolean = {
				if (sessionUser(request).isDefined) {
					val userFound = userService.findByTurkerId(sessionUser(request).get)
					if (userFound.isDefined) {
						val question = questionService.findById(questionService.findIdByUUID(uuid))
						if (question.isDefined) !checkUserDidntExceedMaxAnswersPerBatch(userFound.get.id.get, question.get) else false
					} else false
				} else false
			}

			if (showAlreadyUsedMessage) Unauthorized(views.html.tooManyAnswersInBatch(true)) else Ok(views.html.question(workerId, questionService.findById(Application.TEMPLATE_ID).map(q => new QuestionHTMLFormatter(q.html).format).getOrElse("No Example page defined")))

		} else {
			assert(!workerId.isEmpty)
			//val newSession = request.session + ("TurkerID" -> workerId) + ("assignmentId" -> assignmentId) + ("target" -> target)
			val newSession = request.session + (Application.TURKER_ID_KEY -> workerId) + ("assignmentId" -> assignmentId) + ("target" -> target)

			showQuestionAction(uuid, secret, request, Some(workerId), Some(newSession))
		}
	}

	/**
	  * Show a question.
	  * The method below counts the number of answers already sent by the turker and loads the question
	  * from the database.
	  *
	  * If this number is equal to the maximal number of allowed answer per turker per batch, then the turker will be
	  * redirected to a warning page.
	  *
	  * @param uuid
	  * @return
	  */
	def showQuestion(uuid: String, secret: String = "") = Action { request =>
		showQuestionAction(uuid, secret, request, request.session.get(Application.TURKER_ID_KEY))
	}

	def showQuestionAction(uuid: String, secret: String, request: Request[AnyContent], turkerId: Option[String], _replaceSession: Option[Session] = None) = {
		//dont allow session to be reset turker ID field
		val replaceSession = _replaceSession.map(s => {
			val t = s.get(Application.TURKER_ID_KEY)
			if (t.isDefined && t.get.length < 5) {
				println("nasty removal")
				s - Application.TURKER_ID_KEY
			} else s
		})
		if (!logAccessAndCheckIfExceedsAccessCount(request, turkerId.orNull)) {
			val questionId = questionService.findIdByUUID(uuid)
			turkerId.map { user =>
				// get the answers of the turker in the batch group
				val userFound = userService.findByTurkerId(user)
				if (userFound.isDefined && isUserAllowedToAnswer(questionId, userFound.get.id.get, secret)) {
					val question = questionService.findById(questionId).get
					val formattedHTML: String = new QuestionHTMLFormatter(question.html).format
					Ok(views.html.question(user, formattedHTML, questionId, secret)).withSession(replaceSession.getOrElse(request.session))
				} else if (userFound.isDefined) {
					if (checkUserDidntExceedMaxAnswersPerBatch(userFound.get.id.get, questionService.findById(questionId).get))
						Unauthorized("This HIT has already been answered / you don't have permission to answer this HIT. If this error persists, please write pdeboer@mit.edu ")
					else
						Unauthorized(views.html.tooManyAnswersInBatch()).withSession(replaceSession.getOrElse(request.session))
				} else {
					Ok(views.html.login()).withSession("redirect" -> (configuration.getString("assetPrefix") + "/showQuestion?q=" + uuid + "&s=" + secret))
				}
			}.getOrElse {
				Ok(views.html.login()).withSession("redirect" -> (configuration.getString("assetPrefix") + "/showQuestion?q=" + uuid + "&s=" + secret))
			}
		} else Unauthorized("We have received too many requests from your IP address")
	}


	def insertSnippetInHTMLPage(html: String, snippet: String): String = {
		html.replace("<img id=\"snippet\" src=\"\" width=\"100%\"></img>", "<img id=\"snippet\" src=\"data:image/gif;base64," + snippet + "\" width=\"100%\"></img>")
	}

	def removeCDATA(html: String): String = {
		html.replaceAll("\\Q<![CDATA[\\E", "").replaceAll("\\Q]]>\\E", "")
	}

	def isUserAllowedToAnswer(questionId: Long, userId: Long, providedSecret: String = ""): Boolean = {
		val question = questionService.findById(questionId)
		// The question exists and there is no answer yet accepted in the DB
		if (question.isDefined && !answerService.existsAcceptedAnswerForQuestionId(questionId) && question.get.secret == providedSecret) {
			checkUserDidntExceedMaxAnswersPerBatch(userId, question.get)
		} else {
			false
		}
	}

	def checkUserDidntExceedMaxAnswersPerBatch(userId: Long, question: Question): Boolean = {
		val batch = batchService.findById(question.batchId)
		if (batch.get.allowedAnswersPerTurker == 0) {
			true
		} else {
			if (batch.get.allowedAnswersPerTurker > answerService.countUserAnswersForBatch(userId, question.batchId)) {
				true
			} else {
				false
			}
		}
	}

	/**
	  * Store an answer in the database.
	  * This method extract the answer of a question and stores it in the database. After storing the answer the user will
	  * be redirected to a conclusion page where a code is displayed in order to get the reward.
 *
	  * @return
	  */
	def storeAnswer = Action { request =>
		request.session.get(Application.TURKER_ID_KEY).map { user =>
			try {

				val questionId = request.getQueryString("questionId").mkString.toLong
				val isRelated = request.getQueryString("isRelated").mkString == ("yes")
				val isCheckedBefore = request.getQueryString("isCheckedBefore").mkString == ("yes")
				val confidence = request.getQueryString("confidence").mkString.toInt
				val extraAnswer = request.getQueryString("answer").mkString
				val secret = request.getQueryString("secret").mkString
				val userId: Long = userService.findByTurkerId(user).get.id.get

				if (isUserAllowedToAnswer(questionId, userId, secret)) {
					val outputCode = Math.abs(new SecureRandom().nextLong())

					val answer: JSONObject = JSONObject.apply(request.queryString.map(m => {
						(m._1, m._2.mkString(","))
					}))

					answerService.create(questionId, userId, new DateTime, isRelated, isCheckedBefore, extraAnswer, confidence, answer.toString(), outputCode)

					if (request.session.get("assignmentId").isDefined) {
						val newSessionInclUser: Session = Session() + (Application.TURKER_ID_KEY -> request.session.get(Application.TURKER_ID_KEY).get)
						Ok(views.html.postToTurk(request.session.get("target").get, request.session.get("assignmentId").get, outputCode)).withSession(newSessionInclUser)
					} else
						Ok(views.html.code(user, outputCode)).withSession(request.session)
				} else {
					Unauthorized("This question has already been answered.")
				}
			} catch {
				case e: Exception => {
					e.printStackTrace()
					Unauthorized("Invalid request format.")
				}
			}
		}.getOrElse {
			Ok(views.html.login())
		}
	}

	def logAccessAndCheckIfExceedsAccessCount(request: Request[AnyContent], username: String = ""): Boolean = {
		val userIdCleaned = userService.findByTurkerId(username).map(_.id.get).getOrElse(-1L)

		log.createEntry(request.uri, request.remoteAddress, userIdCleaned)

		val requestsPerSnippetAnswer = 3
		val maxSnippetsPerCrowdWorker: Int = 200

		val numberOfEntries = log.ipLogEntriesSince(request.remoteAddress, DateTime.now().minusWeeks(4))
		if(numberOfEntries.isLeft) {
			Logger.debug("logAccessAndCheckIfExceedsAccessCount error")
			true
		} else {
			numberOfEntries.right.get > (requestsPerSnippetAnswer * maxSnippetsPerCrowdWorker)
		}
	}

}
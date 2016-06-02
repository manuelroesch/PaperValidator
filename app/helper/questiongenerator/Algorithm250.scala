package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot

import java.io.{File, FileInputStream, InputStream}
import javax.activation.MimetypesFileTypeMap

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao.DAO
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.integrationtest.console.Constants
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.Permutation
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.report.{AnswerParser, ParsedAnswer}
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.snippet.{SnippetHTMLQueryBuilder, SnippetHTMLTemplate}
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompPortalAdapter, HCompQueryProperties, HTMLQueryAnswer}
import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
import ch.uzh.ifi.pdeboer.pplib.process.entities.IndexedPatch
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithBeatByKVotingProcess
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithBeatByKVotingProcess._
import models.Method2AssumptionService

import scala.xml.NodeSeq


/**
  * Created by mattia on 01.09.15.
  */
case class Algorithm250(dao: DAO, ballotPortalAdapter: HCompPortalAdapter, method2AssumptionService: Method2AssumptionService) {

	def executePermutation(conferenceId: Int, p: Permutation) = {
		val answers: List[ParsedAnswer] = buildAndExecuteQuestion(conferenceId, p)
		if (isFinalAnswerYesYes(answers)) {
			dao.updateStateOfPermutationId(p.id, p.id)
			dao.getAllOpenByGroupName(p.groupName).foreach(g => {
				dao.updateStateOfPermutationId(g.id, p.id, 1)
			})
			val groupName = p.groupName.split("/")
			val secondExclusionMatches = groupName.slice(0, 2).mkString("/")
			dao.getAllOpenGroupsStartingWith(secondExclusionMatches).filter(_.methodIndex.equalsIgnoreCase(p.methodIndex)).foreach(g => {
				dao.updateStateOfPermutationId(g.id, p.id, 2)
			})
		} else {
			dao.updateStateOfPermutationId(p.id, -1)
		}
	}


	def buildAndExecuteQuestion(conferenceId: Int, permutation: Permutation): List[ParsedAnswer] = {
		val (properties: BallotProperties, ballotHtmlPage: NodeSeq) = buildQuestion(conferenceId, permutation)
		val description: String = "Can you grasp some of the main concepts in the field of statistics without necessary prior knowledge in the field - just by basic text understanding?"
		val title: String = s"Are these two words related? {Batch ${properties.permutationId}}"
		val process = new ContestWithBeatByKVotingProcess(Map(
			K.key -> 4,
			PORTAL_PARAMETER.key -> ballotPortalAdapter,
			MAX_ITERATIONS.key -> 30,
			MEMOIZER_NAME.key -> Some("bbk_mem_" + properties.permutationId),
			QUESTION_PRICE.key -> properties,
			QUERY_BUILDER_KEY -> new SnippetHTMLQueryBuilder(ballotHtmlPage, description, title)
		))

		process.process(IndexedPatch.from(List(SnippetHTMLQueryBuilder.POSITIVE, SnippetHTMLQueryBuilder.NEGATIVE)))

		process.portal.queries.map(_.answer.get.is[HTMLQueryAnswer]).map(a => {
			ParsedAnswer(a.answers.get("isRelated"), a.answers.get("isCheckedBefore"), a.answers.get("confidence").get.toInt, a.answers.get("descriptionIsRelated").get)
		})
	}

	def buildQuestion(conferenceId: Int, permutation: Permutation, isTemplate: Boolean = false): (BallotProperties, NodeSeq) = {
		val snippetFile: File = new File(permutation.snippetFilename)
		val snippetInputStream: InputStream = new FileInputStream(snippetFile)
		val snippetByteArray = Stream.continually(snippetInputStream.read()).takeWhile(-1 !=).map(_.toByte).toArray
		snippetInputStream.close()

		val javascriptByteArray = if (permutation.methodOnTop) {
			SnippetHTMLTemplate.generateJavascript.toString.map(_.toByte).toArray
		} else {
			SnippetHTMLTemplate.generateJavascript.toString.map(_.toByte).toArray
		}

		val snippetContentType = new MimetypesFileTypeMap().getContentType(snippetFile.getName)
		val javascriptContentType = "application/javascript"

		val snippetAsset = Asset(snippetByteArray, snippetContentType, snippetFile.getName)
		val jsAsset = Asset(javascriptByteArray, javascriptContentType, "script.js")

		val properties = new BallotProperties(Batch(allowedAnswersPerTurker = 1), List(
			snippetAsset, jsAsset), permutation.id, propertiesForDecoratedPortal = new HCompQueryProperties(50, qualifications = Nil)) //TODO put in qualifications

		val method = permutation.methodIndex.substring(0,permutation.methodIndex.indexOf("_"))
		val assumption = permutation.groupName.substring(permutation.groupName.indexOf("/")+1,permutation.groupName.lastIndexOf("/"))
		val m2a = method2AssumptionService.findByMethodAndAssumptionName(method,assumption,conferenceId).get
		val ballotHtmlPage: NodeSeq =
			SnippetHTMLTemplate.generateHTMLPage(snippetAsset.url, jsAsset.url, m2a.question, isTemplate)
		(properties, ballotHtmlPage)
	}

	def isFinalAnswerYesYes(answers: List[ParsedAnswer]): Boolean = {
		val cleanedAnswers = answers.filter(_.likert >= Constants.LIKERT_VALUE_CLEANED_ANSWERS)
		val yesYes = cleanedAnswers.count(ans => AnswerParser.evaluateAnswer(ans.q1).contains(true) && AnswerParser.evaluateAnswer(ans.q2).contains(true))
		val no = cleanedAnswers.size - yesYes
		yesYes > no
	}
}

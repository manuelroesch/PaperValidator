package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.report

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.Answer
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.snippet.SnippetHTMLQueryBuilder
import play.api.libs.json.{JsObject, Json}

/**
 * Created by mattia on 31.08.15.
 */
object AnswerParser {
	def evaluateAnswer(toCheck: Option[String]): Option[Boolean] = {
		toCheck match {
			case Some(x) => Option(x.equalsIgnoreCase(SnippetHTMLQueryBuilder.POSITIVE))
			case _ => None
		}
	}

	def parseJSONAnswers(answers: List[Answer]): List[ParsedAnswer] = {
		val ans = answers.map(answer => buildAnswerMap(answer.answerJson))
		createParsedAnswers(ans)
	}

	private def createParsedAnswers(answers: List[Map[String, String]]): List[ParsedAnswer] = {
		answers.map(ans => {
			val isRelated = ans.get("isRelated")
			val isCheckedBefore = ans.get("isCheckedBefore")
			val likert = ans.get("confidence")
			val descriptionIsRelated = ans.get("descriptionIsRelated")

			ParsedAnswer(isRelated, isCheckedBefore, likert.get.toInt, descriptionIsRelated.get)
		})
	}

	def buildAnswerMap(answerJson: String): Map[String, String] = {
		val result = Json.parse(answerJson).asInstanceOf[JsObject]
		result.fieldSet.map(field => field._1 -> field._2.toString().replaceAll("\"", "")).toMap
	}
}

case class ParsedAnswer(q1: Option[String], q2: Option[String], likert: Int, feedback: String)


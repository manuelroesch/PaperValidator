package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.report

/**
 * Created by mattia on 01.09.15.
 */
case class SummarizedAnswersFormat(yesQ1: Int, noQ1: Int, yesQ2: Int, noQ2: Int)

object SummarizedAnswersFormat {
	def summarizeAnswers(answers: List[ParsedAnswer]): SummarizedAnswersFormat = {
		val yesQ1 = answers.count(ans => AnswerParser.evaluateAnswer(ans.q1).contains(true))
		val yesQ2 = answers.count(ans => AnswerParser.evaluateAnswer(ans.q2).contains(true))

		val noQ1 = answers.count(ans => AnswerParser.evaluateAnswer(ans.q1).contains(false))
		val noQ2 = answers.count(ans => AnswerParser.evaluateAnswer(ans.q2).contains(false))
		SummarizedAnswersFormat(yesQ1, noQ1, yesQ2, noQ2)
	}
}
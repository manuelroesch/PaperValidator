package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.report

import java.io.File

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao.DAO
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.integrationtest.console.Constants
import com.github.tototoshi.csv.CSVWriter
import com.typesafe.config.ConfigFactory

/**
 * Created by mattia on 31.08.15.
 */
object Report {

  def writeCSVReport(dao: DAO) = {

    val config = ConfigFactory.load()
    val RESULT_CSV_FILENAME = config.getString("resultFilename")

    val basedir = System.getProperty("user.dir")

    val writer = CSVWriter.open(new File(basedir, RESULT_CSV_FILENAME))
    writer.writeRow(Seq("snippet", "yes answers", "no answers", "cleaned yes", "cleaned no", "yes answers", "no answers", "cleaned yes", "cleaned no", "feedback", "first type disabled snippets", "second type disabled snippets"))

    dao.allAnswers().groupBy(g => {
			dao.getAssetPDFFileNameByQuestionId(g.questionId).get
		}).foreach(answersForSnippet => {

			val permutationId = dao.getPermutationIdByQuestionId(answersForSnippet._2.head.questionId).get
			val allPermutationsDisabledByActualAnswer = dao.getAllPermutationsWithStateEquals(permutationId).filterNot(_.excluded_step == 0)
			val snippetName = answersForSnippet._1

      val allAnswersParsed: List[ParsedAnswer] = AnswerParser.parseJSONAnswers(answersForSnippet._2)

			val overallSummary = SummarizedAnswersFormat.summarizeAnswers(allAnswersParsed)

			val cleanedAnswers = allAnswersParsed.filter(_.likert >= Constants.LIKERT_VALUE_CLEANED_ANSWERS)
			val cleanedSummary = SummarizedAnswersFormat.summarizeAnswers(cleanedAnswers)

			val feedback = allAnswersParsed.map(_.feedback).mkString(";")

			val firstTypeDisabledSnippets = allPermutationsDisabledByActualAnswer.filter(_.excluded_step == 1).map(_.snippetFilename).mkString(";")
			val secondTypeDisabledSnippets = allPermutationsDisabledByActualAnswer.filter(_.excluded_step == 2).map(_.snippetFilename).mkString(";")

			writer.writeRow(Seq[Any](snippetName, overallSummary.yesQ1, overallSummary.noQ1, cleanedSummary.yesQ1, cleanedSummary.noQ1,
        overallSummary.yesQ2, overallSummary.noQ2, cleanedSummary.yesQ2, cleanedSummary.noQ2,
        feedback, firstTypeDisabledSnippets, secondTypeDisabledSnippets))
		})

		writer.close()
	}

  def writeCSVReportAllAnswers(dao: DAO) = {
    val config = ConfigFactory.load()
    val RESULT_CSV_FILENAME = config.getString("resultFilename")
    val LIKERT_CLEANED_ANSWERS = config.getInt("likertCleanedAnswers")
    val basedir = System.getProperty("user.dir")
    val writer = CSVWriter.open(new File(basedir, RESULT_CSV_FILENAME.substring(0, RESULT_CSV_FILENAME.length-4)+"_DETAILS.csv"))

    writer.writeRow(Seq("Manuscript", "id", "Yes 1", "No 1", "Yes 2", "No 2", "likert"))

    dao.allAnswers().groupBy(g => {
      dao.getAssetPDFFileNameByQuestionId(g.questionId).get
    }).foreach(answersForSnippet => {
      val parsedAnswers = AnswerParser.parseJSONAnswers(answersForSnippet._2.filter(_.accepted).sortWith(_.time.getMillis < _.time.getMillis))

      val cleanedParsedAnswers = parsedAnswers.filter(_.likert >= LIKERT_CLEANED_ANSWERS)
      cleanedParsedAnswers.zipWithIndex.foreach(anss => {
        val q1 = if(AnswerParser.evaluateAnswer(anss._1.q1).getOrElse(false)) 1 else 0
        val q2 = if(AnswerParser.evaluateAnswer(anss._1.q2).getOrElse(false)) 1 else 0
        writer.writeRow(Seq(answersForSnippet._1, anss._2, q1, Math.abs(1-q1), q2, Math.abs(1-q2), anss._1.likert))
      })
    })

    writer.close()
  }
}

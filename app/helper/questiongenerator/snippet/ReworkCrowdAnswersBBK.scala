package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.snippet

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao.BallotDAO
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.DBSettings
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.report.AnswerParser
import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
import ch.uzh.ifi.pdeboer.pplib.process.entities.IndexedPatch
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithBeatByKVotingProcess
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithBeatByKVotingProcess._

private[snippet] class TestPortal extends HCompPortalAdapter {
	val answerIDsToGive = List(281, 278, 277, 279, 280, 282, 283, 284, 285, 286, 288, 287)
	var it: Int = 0
	DBSettings.initialize()
	val dao = new BallotDAO

	override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
		answerIDsToGive.synchronized {
			val targetId = answerIDsToGive(it)
			val answer = dao.getAnswerById(targetId)

			it += 1

			logger.info(s"returning $targetId ${answer.get.answerJson})")
			Some(HTMLQueryAnswer(AnswerParser.buildAnswerMap(answer.get.answerJson), query, Nil))
		}
	}

	override def getDefaultPortalKey: String = "mytest"

	override def cancelQuery(query: HCompQuery): Unit = {}
}

private[snippet] object SnippetHTMLTest extends App {
	val process = new ContestWithBeatByKVotingProcess(Map(
		K.key -> 4,
		PORTAL_PARAMETER.key -> new TestPortal(),
		MAX_ITERATIONS.key -> 30,
		QUESTION_PRICE.key -> new HCompQueryProperties(),
		QUERY_BUILDER_KEY -> new SnippetHTMLQueryBuilder(<div>This is a test</div>, "testquestion")
	))

	process.process(IndexedPatch.from(List(SnippetHTMLQueryBuilder.POSITIVE, SnippetHTMLQueryBuilder.NEGATIVE)))
}
package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.integrationtest.console

import ch.uzh.ifi.pdeboer.pplib.hcomp._

/**
 * Created by pdeboer on 22/07/15.
 */
object ConsolePortalAdapter {
	val PORTAL_KEY = "consolePortal"
	val CONFIG_PARAM = "active"
}

@HCompPortal(builder = classOf[ConsolePortalBuilder], autoInit = true)
class ConsolePortalAdapter(param: String = "") extends HCompPortalAdapter with AnswerRejection {
	override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {

		val freeTextQuery = query match {
			case p: FreetextQuery => p
			case _ => FreetextQuery(query.question)
		}

		println(freeTextQuery.question)


		Some(FreetextAnswer(freeTextQuery, scala.io.StdIn.readLine("> ").trim))

	}

	override def getDefaultPortalKey: String = ConsolePortalAdapter.PORTAL_KEY

	override def cancelQuery(query: HCompQuery): Unit = ???

	override def rejectAnswer(ans: HCompAnswer, message: String): Boolean = {
		println(s"rejecting answer $ans with message $message")
		super.rejectAnswer(ans, message)
	}

	override def approveAndBonusAnswer(ans: HCompAnswer, message: String, bonusCents: Int): Boolean = {
		println(s"accepting answer $ans with message $message and bonus $bonusCents")
		super.approveAndBonusAnswer(ans, message, bonusCents)
	}
}

class ConsolePortalBuilder extends HCompPortalBuilder {
	val PARAM_KEY = "active"

	override val parameterToConfigPath = Map(PARAM_KEY -> ConsolePortalAdapter.CONFIG_PARAM)

	override def build: HCompPortalAdapter = new ConsolePortalAdapter(params(PARAM_KEY))

	override def expectedParameters: List[String] = List(PARAM_KEY)

	override def key: String = ConsolePortalAdapter.PORTAL_KEY
}

package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.integrationtest.console

import com.typesafe.config.ConfigFactory

/**
 * Created by pdeboer on 22/09/15.
 */
object Constants {
	val conf = ConfigFactory.load()
	val LIKERT_VALUE_CLEANED_ANSWERS = conf.getInt("likertCleanedAnswers")
}

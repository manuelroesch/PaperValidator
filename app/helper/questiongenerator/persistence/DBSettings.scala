package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence

/**
 * Created by Mattia on 19.01.2015.
 */

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.integrationtest.console.ConsoleIntegrationTest._
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import scalikejdbc._
import scalikejdbc.config.DBs

trait DBSettings {
	DBSettings.initialize()
}

object DBSettings extends LazyLogger {

	private var isInitialized = false

	def initialize(): Unit = this.synchronized {
		if (!isInitialized) {
			DBs.setupAll()

			GlobalSettings.loggingSQLErrors = true
			DBInitializer.run()
			isInitialized = true
			logger.debug("Database initialized")
		}
		logger.debug("Database already initialized")
	}

	def loadPermutations(init: String, path: String): Unit = {
		if (init.equalsIgnoreCase("init")) {
			logger.info("Loading permutations...")
			dao.loadPermutationsCSV(path)
		}
	}

}
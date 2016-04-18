package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.integrationtest.console

import java.io.File
import javax.inject.Inject

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot._
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao.BallotDAO
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.{DBSettings, Permutation}
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.report.Report
import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import helper.pdfpreprocessing.PreprocessPDF
import models.QuestionService

import scala.io.Source

/**
	* Created by mattia on 07.07.15.
	*/
object ConsoleIntegrationTest {
	val DEFAULT_TEMPLATE_ID: Long = 1L
	val dao = new BallotDAO
}

class ConsoleIntegrationTest @Inject()(questionService: QuestionService) extends LazyLogger {

	DBSettings.initialize()
	val dao = new BallotDAO

	def start(): Unit = {

		val ballotPortalAdapter = HComp(BallotPortalAdapter.PORTAL_KEY)

		/**
			* Algorithm 250:
			* The algorithm250 allows to disable redundant questions (or permutations).
			* First, all permutations with the same groupname are disabled since an assumption is assumed to be related only to a single method.
			* Second, all the permutation with the same methodIndex, pdf filename and assumption name are disabled. This second step is performed
			* because it is assumed that a method (or group of methods) is/are related to only one assumption type.
			*/
		val algorithm250 = Algorithm250(dao, ballotPortalAdapter)

		val DEFAULT_TEMPLATE_ID: Long = 1L

		if (questionService.findById(1L).isEmpty) {
			logger.info("init template")
			val template: File = new File("template/perm.csv")
			if (template.exists()) {
				val templatePermutations = Source.fromFile(template).getLines().drop(1).map(l => {
					val perm: Permutation = Permutation.fromCSVLine(l)
					dao.createPermutation(perm)
				})
				Thread.sleep(1000)
				templatePermutations.foreach(permutationId => {
					val q = algorithm250.buildQuestion(dao.getPermutationById(permutationId).get, isTemplate = false)
					logger.info("now")
					//ballotPortalAdapter.sendQuery(HTMLQuery(q._2, 1, "Statistical Methods and Prerequisites", ""), q._1)
					Thread.sleep(1000)
				})
				Thread.sleep(1000)
				assert(!templatePermutations.contains(DEFAULT_TEMPLATE_ID), "Our template didn't get ID 1. Please adapt DB. Current template IDs: " + templatePermutations.mkString(","))
			}
			logger.info("done")
			System.exit(0)
		} else {
			logger.info("Loading new permutations")
			dao.loadPermutationsCSV(PreprocessPDF.PERMUTATIONS_CSV_FILENAME)
			logger.info("Removing state information of previous runs")
			new File("state").listFiles().foreach(f => f.delete())
		}

		val groups = dao.getAllPermutations().filter(_.id != DEFAULT_TEMPLATE_ID).groupBy(gr => {
			gr.groupName.split("/").apply(0)
		}).map(g => (g._1, g._2.sortBy(_.distanceMinIndexMax))).toList

		groups.mpar.foreach(group => {
			group._2.foreach(permutation => {
				if (dao.getPermutationById(permutation.id).map(_.state).getOrElse(-1) == 0) {
					algorithm250.executePermutation(permutation)
				}
			})
		})

		Report.writeCSVReport(dao)
		Report.writeCSVReportAllAnswers(dao)

	}
}
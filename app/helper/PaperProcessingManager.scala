package helper

import java.io.File

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao.BallotDAO
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.integrationtest.console.ConsoleIntegrationTest
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.{DBSettings, Permutation}
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.report.Report
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.{Algorithm250, BallotPortalAdapter}
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HTMLQuery}
import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._
import controllers.routes
import helper.email.{MailTemplates}
import helper.pdfpreprocessing.PreprocessPDF
import helper.questiongenerator.HCompNew
import helper.statcheck.Statchecker
import models._
import org.codehaus.plexus.util.FileUtils
import play.api.{Configuration, Logger}
import play.api.db.Database

import scala.io.Source


/**
  * Created by manuel on 21.04.2016.
  */
object PaperProcessingManager {

  var isRunning = false


  def run(database: Database, configuration: Configuration, papersService: PapersService,
          questionService: QuestionService, method2AssumptionService: Method2AssumptionService,
          paperResultService: PaperResultService): Boolean = {
    if(!isRunning) {
      isRunning = true
      val papersToProcess = papersService.findProcessablePapers()
      if(papersToProcess.nonEmpty) {
        papersToProcess.foreach(paper =>
          processPaper(database, configuration, papersService, questionService, method2AssumptionService,
            paperResultService, paper)
        )
        isRunning = false
        run(database, configuration, papersService, questionService, method2AssumptionService, paperResultService)
      }
      isRunning = false
    }
    true
  }

  def processPaper(database: Database, configuration: Configuration, papersService: PapersService,
                   questionService: QuestionService, method2AssumptionService: Method2AssumptionService,
                   paperResultService: PaperResultService, paper : Papers): Unit = {
    val paperLink = configuration.getString("hcomp.ballot.baseURL").get + routes.Paper.confirmPaper(paper.id.get,paper.secret).url
    if(paper.status == Papers.STATUS_NEW) {
      Commons.generateCoverFile(paper)
      Statchecker.run(paper, paperResultService)
      val permutations = PreprocessPDF.start(database,paper)
      if(permutations>0) {
        papersService.updateStatus(paper.id.get,Papers.STATUS_AWAIT_CONFIRMATION)
        papersService.updatePermutations(paper.id.get,permutations)
      } else {
        papersService.updateStatus(paper.id.get,Papers.STATUS_COMPLETED)
      }
      MailTemplates.sendPaperAnalyzedMail(paper.name,paperLink,permutations,paper.email)
    } else if(paper.status == Papers.STATUS_IN_PPLIB_QUEUE) {
      questionGenerator(questionService, method2AssumptionService, paper)
      papersService.updateStatus(paper.id.get,Papers.STATUS_COMPLETED)
      MailTemplates.sendPaperCompletedMail(paper.name,paperLink,paper.email)
      cleanUpTmpDir(paper)
    }
  }

  def cleanUpTmpDir(paper: Papers): Unit = {
    FileUtils.deleteDirectory(new File(PreprocessPDF.INPUT_DIR + "/" + Commons.getSecretHash(paper.secret)))
    FileUtils.deleteDirectory(new File(PreprocessPDF.OUTPUT_DIR + "/" + Commons.getSecretHash(paper.secret)))
  }

  def questionGenerator(questionService: QuestionService, method2AssumptionService: Method2AssumptionService , paper: Papers): Unit = {

    val DEFAULT_TEMPLATE_ID: Long = 1L

    DBSettings.initialize()
    val dao = new BallotDAO
    val hComp = HCompNew
    hComp.autoloadConfiguredPortals()
    Logger.info(HComp.allDefinedPortals.toString())
    val ballotPortalAdapter = hComp(BallotPortalAdapter.PORTAL_KEY)
    val algorithm250 = Algorithm250(dao, ballotPortalAdapter, method2AssumptionService)

    if (questionService.findById(DEFAULT_TEMPLATE_ID).isEmpty) {
      Logger.info("templateInit")
      val template: File = new File("public/template/perm.csv")
      if (template.exists()) {
        val templatePermutations = Source.fromFile(template).getLines().drop(1).map(l => {
          val perm: Permutation = Permutation.fromCSVLine(l)
          dao.createPermutation(perm,paper.id.get)
        })
        Thread.sleep(1000)
        templatePermutations.foreach(permutationId => {
          val q = algorithm250.buildQuestion(paper.conferenceId, dao.getPermutationById(permutationId).get, isTemplate = true)
          Logger.info("WriteTemplate")
          ballotPortalAdapter.sendQuery(HTMLQuery(q._2, 1, "Statistical Methods and Prerequisites", ""), q._1)
          Thread.sleep(1000)
        })
        Thread.sleep(1000)
        assert(!templatePermutations.contains(DEFAULT_TEMPLATE_ID), "Our template didn't get ID 1. Please adapt DB. Current template IDs: " + templatePermutations.mkString(","))
      }
      Logger.info("templateInit Done")
      questionGenerator(questionService, method2AssumptionService, paper)
    } else {
      Logger.info("Loading new permutations")
      dao.loadPermutationsCSV(PreprocessPDF.OUTPUT_DIR + "/" + Commons.getSecretHash(paper.secret) + "/permutations.csv",
        paper.id.get)
      Logger.info("Removing state information of previous runs")
      new File("state").listFiles().foreach(f => f.delete())
    }

    val groups = dao.getAllPermutations().filter(_.id != ConsoleIntegrationTest.DEFAULT_TEMPLATE_ID).groupBy(gr => {
      gr.groupName.split("/").apply(0)
    }).map(g => (g._1, g._2.sortBy(_.distanceMinIndexMax))).toList

    groups.mpar.foreach(group => {
      group._2.foreach(permutation => {
        if (dao.getPermutationById(permutation.id).map(_.state).getOrElse(-1) == 0) {
          algorithm250.executePermutation(paper.conferenceId,permutation)
        }
      })
    })

    Report.writeCSVReport(dao)
    Report.writeCSVReportAllAnswers(dao)
  }

}
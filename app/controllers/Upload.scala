package controllers

import java.io.{FileWriter, BufferedWriter, File}

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.integrationtest.console.ConsoleIntegrationTest
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.report.Report
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HTMLQuery, HComp}
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.{Algorithm250, BallotPortalAdapter}
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao.BallotDAO
import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.{Permutation, DBSettings}
import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._
import helper.pdfpreprocessing.PreprocessPDF
import helper.pdfpreprocessing.csv.{CSVExporter, Snippet}
import helper.pdfpreprocessing.pdf.PDFLoader
import helper.pdfpreprocessing.png.StatTermLocationsInPNG
import helper.pdfpreprocessing.stats.{StatTermPermuter, PruneTermsWithinOtherTerms, StatTermPruning, StatTermSearcher}
import helper.pdfpreprocessing.util.FileUtils
import models.QuestionDAO
import play.api.Logger
import play.api.mvc.{Action, Controller}
import scalikejdbc.config.DBs

import scala.io.Source

/**
  * Created by manuel on 11.04.2016.
  */
class Upload extends Controller {
  def upload = Action {
    Ok(views.html.upload())
  }

  def uploaded = Action(parse.multipartFormData) { request =>
    createDirs
    request.body.file("paper").map { paper =>
      val filename = paper.filename
      //val contentType = paper.contentType
      paper.ref.moveTo(new File(PreprocessPDF.INPUT_DIR + "/" + filename))
      PreprocessPDF.start()
      permutation2DB(true)
      Logger.info("done")
      Ok("Ok")
    }.getOrElse {
      Ok("Error")
    }
  }

  def permutation2DB(isTemplate: Boolean): Unit = {
    DBs.setupAll()
    DBSettings.initialize()
    val dao = new BallotDAO
    val ballotPortalAdapter = HComp(BallotPortalAdapter.PORTAL_KEY)
    val algorithm250 = Algorithm250(dao, ballotPortalAdapter)
    if (QuestionDAO.findById(1L).isEmpty) {
      Logger.info("init template")
      val template: File = new File("public/template/perm.csv")
      if (template.exists()) {
        val templatePermutations = Source.fromFile(template).getLines().drop(1).map(l => {
          val perm: Permutation = Permutation.fromCSVLine(l)
          dao.createPermutation(perm)
        })
        Thread.sleep(1000)
        templatePermutations.foreach(permutationId => {
          val q = algorithm250.buildQuestion(dao.getPermutationById(permutationId).get, isTemplate = true)
          Logger.info("WriteTemplate")
          ballotPortalAdapter.sendQuery(HTMLQuery(q._2, 1, "Statistical Methods and Prerequisites", ""), q._1)
          Thread.sleep(1000)
        })
        Thread.sleep(1000)
        assert(!templatePermutations.contains(1L), "Our template didn't get ID 1. Please adapt DB. Current template IDs: " + templatePermutations.mkString(","))
      }
    } else {
      Logger.info("Loading new permutations")
      dao.loadPermutationsCSV(PreprocessPDF.PERMUTATIONS_CSV_FILENAME)
      Logger.info("Removing state information of previous runs")
      new File("state").listFiles().foreach(f => f.delete())
      val groups = dao.getAllPermutations().filter(_.id != ConsoleIntegrationTest.DEFAULT_TEMPLATE_ID).groupBy(gr => {
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


  def createDirs: Unit = {
    var tmpDirs: File = new File(PreprocessPDF.TMP_DIR);
    if (!tmpDirs.exists()) tmpDirs.mkdir();
    tmpDirs = new File(PreprocessPDF.PNG_ERROR_OUTPUT_PATH);
    if (!tmpDirs.exists()) tmpDirs.mkdir();
    tmpDirs = new File(PreprocessPDF.OUTPUT_DIR);
    if (!tmpDirs.exists()) tmpDirs.mkdir();
    tmpDirs = new File(PreprocessPDF.INPUT_DIR);
    if (!tmpDirs.exists()) tmpDirs.mkdir();
  }

  def statTest = Action {
    createDirs
    Logger.debug("starting highlighting")


    FileUtils.emptyDir(new File(PreprocessPDF.OUTPUT_DIR))

    val allPapers = new PDFLoader(new File(PreprocessPDF.INPUT_DIR)).papers
    //val snippets = allPapers.par.flatMap(paper => {
    val writer = new BufferedWriter(new FileWriter(new File("tmp/out.csv")))
    for (paper <- allPapers) {
      val searcher = new StatTermSearcher(paper)
      val statTermsInPaper = new StatTermPruning(List(new PruneTermsWithinOtherTerms)).prune(searcher.occurrences)
      val combinationsOfMethodsAndAssumptions = new StatTermPermuter(statTermsInPaper).permutations
      combinationsOfMethodsAndAssumptions.sortBy(_.distanceBetweenMinMaxIndex).zipWithIndex.par.map(p => {
        writer.write(p._1.toString + "\n")
      })

      //val snippets = combinationsOfMethodsAndAssumptions.sortBy(_.distanceBetweenMinMaxIndex).zipWithIndex.par.map(p => {
      //val statTermLocationsInSnippet = Some(StatTermLocationsInPNG(true))
      //statTermLocationsInSnippet.map(s => Snippet(new File("tmp/test.png"), p._1, s))
      //})

      Logger.info(s"finished processing paper $paper")
      //snippets.filter(_.isDefined).map(_.get)
    } //.toList
    Logger.info("done")
    //new CSVExporter("tmp/out.csv", snippets).persist()
    Ok("Ok")
  }
}

package controllers

import java.io._
import java.util.zip.{ZipEntry, ZipInputStream}
import javax.inject.Inject

import helper.pdfpreprocessing.PreprocessPDF
import helper.pdfpreprocessing.pdf.PDFLoader
import helper.pdfpreprocessing.stats.{PruneTermsWithinOtherTerms, StatTermPermuter, StatTermPruning, StatTermSearcher}
import helper.pdfpreprocessing.util.FileUtils
import helper.{Commons, PaperProcessingManager}
import models._
import play.api.{Configuration, Logger}
import play.api.db.Database
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
  * Created by manuel on 11.04.2016.
  */
class Upload @Inject() (database: Database, configuration: Configuration, questionService : QuestionService,
                        papersService: PapersService, conferenceService: ConferenceService,
                        method2AssumptionService: Method2AssumptionService, paperResultService: PaperResultService,
                        paperMethodService: PaperMethodService, permutationsServcie: PermutationsService,
                        answerService: AnswerService
                       ) extends Controller {
  def upload = Action {
    Future  {
      PaperProcessingManager.run(database, configuration, papersService, questionService, method2AssumptionService,
        paperResultService,paperMethodService, permutationsServcie, answerService)
    }
    val conferences = conferenceService.findAll()
    Ok(views.html.upload(conferences))
  }

  def uploaded = Action(parse.multipartFormData) { request =>
    createDirs()
    val email = request.body.dataParts.get("email").get.mkString("")
    val conference = request.body.dataParts.get("conference").get.mkString("").toInt
    request.body.file("paper").map { paper =>
      val filename = paper.filename
      if(filename.toLowerCase().contains(".pdf") || filename.toLowerCase().contains(".zip")) {
        val secret = Commons.generateSecret()
        val tmpDirs: File = new File(PreprocessPDF.INPUT_DIR + "/" + Commons.getSecretHash(secret))
        if (!tmpDirs.exists()) tmpDirs.mkdir()
        val file = paper.ref.moveTo(new File(PreprocessPDF.INPUT_DIR + "/" + Commons.getSecretHash(secret) + "/" + filename))
        if(filename.toLowerCase.contains(".zip")) {
          extractAndProcessZip(file,email,conference)
        } else {
          papersService.create(filename,email,conference,secret)
        }
        Future  {
          PaperProcessingManager.run(database, configuration, papersService, questionService, method2AssumptionService,
            paperResultService,paperMethodService, permutationsServcie, answerService)
        }
        Logger.info("done")
        Ok("Ok")
      } else {
        Ok("Error")
      }
    }.getOrElse {
      Ok("Error")
    }
  }

  def extractAndProcessZip(file : File, email: String, conference: Int) = {
    val zis: ZipInputStream = new ZipInputStream(new FileInputStream(file))
    val buffer = new Array[Byte](1024)
    var ze: ZipEntry = zis.getNextEntry()
    while (ze != null) {
      if(ze.getName().contains(".pdf")) {
        val fileName = ze.getName()
        val secret = Commons.generateSecret()
        val newFile = new File(PreprocessPDF.INPUT_DIR + "/" + Commons.getSecretHash(secret) + "/" + fileName)
        new File(newFile.getParent()).mkdirs()
        val fos = new FileOutputStream(newFile)
        var len: Int = zis.read(buffer)

        while (len > 0) {
          fos.write(buffer, 0, len)
          len = zis.read(buffer)
        }

        fos.close()
        papersService.create(fileName,email,conference,secret)
      }
      ze = zis.getNextEntry()
    }
    zis.closeEntry()
    zis.close()
    file.delete()
  }

  def createDirs(): Unit = {
    var tmpDirs: File = new File("state")
    if (!tmpDirs.exists()) tmpDirs.mkdir()
    tmpDirs = new File(PreprocessPDF.TMP_DIR)
    if (!tmpDirs.exists()) tmpDirs.mkdir()
    tmpDirs = new File(PreprocessPDF.PNG_ERROR_OUTPUT_PATH)
    if (!tmpDirs.exists()) tmpDirs.mkdir()
    tmpDirs = new File(PreprocessPDF.OUTPUT_DIR)
    if (!tmpDirs.exists()) tmpDirs.mkdir()
    tmpDirs = new File(PreprocessPDF.INPUT_DIR)
    if (!tmpDirs.exists()) tmpDirs.mkdir()
  }

  def statTest = Action {
    createDirs()
    Logger.debug("starting highlighting")


    FileUtils.emptyDir(new File(PreprocessPDF.OUTPUT_DIR))

    val allPapers = new PDFLoader(new File(PreprocessPDF.INPUT_DIR)).papers
    //val snippets = allPapers.par.flatMap(paper => {
    val writer = new BufferedWriter(new FileWriter(new File("tmp/out.csv")))
    for (paper <- allPapers) {
      val searcher = new StatTermSearcher(paper, database, null)
      val statTermsInPaper = new StatTermPruning(List(new PruneTermsWithinOtherTerms)).prune(searcher.occurrences)
      val combinationsOfMethodsAndAssumptions = new StatTermPermuter(statTermsInPaper).permutations
      combinationsOfMethodsAndAssumptions.sortBy(_.distanceBetweenMinMaxIndex).zipWithIndex.par.map(p => {
        writer.write(p._1.toString + "\n")
      })
      Logger.info(s"finished processing paper $paper")

    }
    Logger.info("done")
    Ok("Ok")
  }
}

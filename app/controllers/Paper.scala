package controllers

import javax.inject.Inject

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.integrationtest.console.Constants
import play.libs.Json

import scala.collection.mutable.ListBuffer
import util.control.Breaks._
import helper._
import models._
import play.api.Configuration
import play.api.db.Database
import play.api.mvc.{Action, Controller}


/**
  * Created by manuel on 11.04.2016.
  */
class Paper @Inject()(database: Database, configuration: Configuration, papersService: PapersService,
                      questionService: QuestionService, method2AssumptionService: Method2AssumptionService,
                      paperResultService: PaperResultService, answerService: AnswerService,
                      conferenceSettingsService: ConferenceSettingsService, paperMethodService: PaperMethodService,
                      permutationsServcie: PermutationsService, conferenceService: ConferenceService
                     ) extends Controller {

  def show(id:Int, secret:String) = Action {
    val paper = papersService.findByIdAndSecret(id,secret)
    if(paper.isDefined) {
      var results = paperResultService.findByPaperId(id)
      results = M2AResultHelper.addMethodsAndAssumptions(id,results,papersService,answerService,conferenceSettingsService)
      val fileBasePath = configuration.getString("highlighter.pdfSourceDir").get+"/"+Commons.getSecretHash(secret)
      val fileLengh = scala.reflect.io.File(fileBasePath + "/log.txt").length
      var log = ""
      if(fileLengh < 9999999 && fileLengh > 0) {
        val source = scala.io.Source.fromFile(fileBasePath + "/log.txt")
        log = try source.mkString.replace("\n","\n<br>") finally source.close()
      }
      val answers = answerService.findJsonAnswerByPaperId(id)
      val answersEvaluated = evaluateAnswers(answers)
      val annotated = scala.reflect.io.File(fileBasePath + "/annotated-" + paper.get.name).exists
      Ok(views.html.paper.showPaper(paper.get,Commons.getSecretHash(secret),results,log,answers,answersEvaluated, annotated))
    } else {
      Unauthorized(views.html.error.unauthorized())
    }
  }

  def annotatePaper(id:Int, secret:String) = Action {
    val paper = papersService.findByIdAndSecret(id,secret)
    if(paper.isDefined) {
      PaperAnnotator.annotatePaper(configuration,answerService, papersService, conferenceSettingsService,
        paperResultService, paperMethodService,paper.get,false)
      Ok("Ok")
    } else {
      Unauthorized(views.html.error.unauthorized())
    }
  }

  def annotatePaperGlossaryMode(id:Int, secret:String) = Action {
    val paper = papersService.findByIdAndSecret(id,secret)
    if(paper.isDefined) {
      PaperAnnotator.annotatePaper(configuration,answerService,papersService, conferenceSettingsService,
        paperResultService, paperMethodService, paper.get,true)
      Ok("Ok")
    } else {
      Unauthorized(views.html.error.unauthorized())
    }
  }

  def loadSpellCheckerResults(id:Int, secret: String) = Action {
    val paper = papersService.findByIdAndSecret(id,secret)
    if(paper.isDefined) {
      Ok(SpellChecker.check(paper.get)).as("text/html")
    } else {
      Unauthorized(views.html.error.unauthorized())
    }
  }

  def evaluateAnswers(answers: List[AnswerShowPaper]): Map[String,Int] = {
    var countAnswers: Map[String,Int] = Map()
    var confidenceList : ListBuffer[Int] = ListBuffer()
    var snippetFilename : String = null
    answers.foreach(a => {
      if (snippetFilename != null && (snippetFilename != a.snippetFilename || a == answers.last)) {
        val confMean = confidenceList.sum / confidenceList.length.toFloat
        val confVar = Math.sqrt(confidenceList.map(cv => Math.pow(cv-confMean,2)).sum / confidenceList.length)
        countAnswers += ((snippetFilename+"confMean") -> Math.round(confMean*100))
        countAnswers += ((snippetFilename+"confVar") -> Math.round(confVar.toFloat*100))
        confidenceList = ListBuffer()
      }
      if(!a.answerJson.isEmpty) {
        snippetFilename = a.snippetFilename
        val parsedJSON = Json.parse(a.answerJson)
        val confidence = parsedJSON.get("confidence").asInt()
        confidenceList += confidence
        List("isRelated","isCheckedBefore").foreach(q => {
          if(parsedJSON.has(q)) {
            val countKey = a.snippetFilename+q+parsedJSON.get(q).asText()+"-"+(confidence>=Constants.LIKERT_VALUE_CLEANED_ANSWERS)
            if(countAnswers.isDefinedAt(countKey)) {
              countAnswers += (countKey -> (countAnswers(countKey)+1))
            } else {
              countAnswers += (countKey -> 1)
            }
          }
        })
      }
    })
    countAnswers
  }

  def generateMturkResults(conferenceId: Int, secret: String) = Action {
    if(conferenceService.findByIdAndSecret(conferenceId,secret).nonEmpty) {
      val answers = answerService.findAllJsonAnswersByConference(conferenceId)
      val answersEvaluated = evaluateAnswers(answers)
      Ok(views.html.paper.generateMturkAnswers(answers,answersEvaluated))
    } else {
      Unauthorized(views.html.error.unauthorized())
    }
  }

  def confirmPaper(id:Int, secret:String) = Action {
    if(papersService.findByIdAndSecret(id,secret).nonEmpty){
      papersService.updateStatus(id,Papers.STATUS_IN_PPLIB_QUEUE)
      PaperProcessingManager.run(database, configuration, papersService, questionService,
        method2AssumptionService, paperResultService, paperMethodService, permutationsServcie, answerService,
        conferenceSettingsService)
      Ok(views.html.paper.confirmPaper(true))
    } else {
      Unauthorized(views.html.error.unauthorized())
    }
  }

  def skipPaper(id:Int, secret:String) = Action {
    if(papersService.findByIdAndSecret(id,secret).nonEmpty){
      PaperProcessingManager.skipCrowdWork(id,secret, questionService, permutationsServcie, answerService)
      papersService.updateStatus(id,Papers.STATUS_COMPLETED)
      Ok(views.html.paper.confirmPaper(false))
    } else {
      Unauthorized(views.html.error.unauthorized())
    }
  }

  def getFile(basePath:String,path:String) = Action {
    val file = new java.io.File(basePath+"/"+path)
    if(file.exists() && (basePath =="public" || basePath == "tmp")) { //TODO: Security
      Ok.sendFile(
        content = file,
        inline = true
      )
    } else {
      Unauthorized(views.html.error.unauthorized())
    }
  }

}

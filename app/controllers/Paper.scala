package controllers

import javax.inject.Inject

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.integrationtest.console.Constants
import play.libs.Json

import scala.collection.mutable.ListBuffer
import scala.reflect.io.File
import util.control.Breaks._
import helper.{SpellChecker, Commons, PaperProcessingManager}
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
      results = addMethodsAndAssumptions(id,results)
      val filePath = configuration.getString("highlighter.pdfSourceDir").get+"/"+Commons.getSecretHash(secret)+"/log.txt"
      val fileLengh = File(filePath).length
      var log = ""
      if(fileLengh < 9999999 && fileLengh > 0) {
        val source = scala.io.Source.fromFile(filePath)
        log = try source.mkString.replace("\n","\n<br>") finally source.close()
      }
      val answers = answerService.findJsonAnswerByPaperId(id)
      val answersEvaluated = evaluateAnswers(answers)
      Ok(views.html.paper.showPaper(paper.get,Commons.getSecretHash(secret),results,log,answers,answersEvaluated))
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

  def addMethodsAndAssumptions(id:Int, results: List[PaperResult]) : List[PaperResult] = {
    var allResults = results
    val paper = papersService.findById(id).get
    val m2aList = answerService.findByPaperId(paper.id.get)
    val conferenceSettings = conferenceSettingsService.findAllByPaperId(paper.id.get,paper.conferenceId).to[ListBuffer]
    m2aList.foreach(m2a => {
      breakable {
        conferenceSettings.zipWithIndex.foreach{case (confSetting,i) => {
          if(confSetting.flag.get != ConferenceSettings.FLAG_IGNORE) {
            if(m2a.method.toLowerCase() == confSetting.methodName.toLowerCase() &&
              m2a.assumption.toLowerCase() == confSetting.assumptionName.toLowerCase()) {
              val m2aDescr = m2a.method+" <span class='glyphicon glyphicon-arrow-right'></span> "+m2a.assumption
              var m2aResult = "Related: <b>"+ (m2a.isRelated > 0.5) + "</b>, " +
                "Checked before: <b>" + (m2a.isCheckedBefore > 0.5) + "</b>"
              var symbol = PaperResult.SYMBOL_ERROR
              if(m2a.isRelated > 0.5 && m2a.isCheckedBefore > 0.5) {
                symbol = PaperResult.SYMBOL_OK
              } else if(m2a.isRelated > 0.5) {
                symbol = PaperResult.SYMBOL_WARNING
                m2aResult += getM2AFlagText(confSetting)
              } else {
                m2aResult += getM2AFlagText(confSetting)
              }
              allResults = allResults:+ new PaperResult(Some(1L),id,PaperResult.TYPE_M2A,m2aDescr,m2aResult,symbol)
              conferenceSettings.remove(i)
              break
            }
          }
        }}
      }
    })
    conferenceSettings.foreach(confSetting => {
      if(confSetting.flag.get != ConferenceSettings.FLAG_IGNORE){
        val m2aDescr = confSetting.methodName+" <span class='glyphicon glyphicon-arrow-right'></span> "+
          confSetting.assumptionName
        var m2aResult = "Not Found in Paper"
        m2aResult += getM2AFlagText(confSetting)
        var symbol = PaperResult.SYMBOL_ERROR
        if(confSetting.flag.get==ConferenceSettings.FLAG_EXPECT) {
          symbol = PaperResult.SYMBOL_WARNING
        }
        allResults:+ new PaperResult(Some(1L),id,PaperResult.TYPE_M2A,m2aDescr,m2aResult,symbol)
      }
    })
    allResults
  }

  def getM2AFlagText(confSetting: ConferenceSettings): String = {
    confSetting.flag.get match {
      case ConferenceSettings.FLAG_REQUIRE =>
        "<span class='m2aFlag text-danger glyphicon glyphicon-flag'><span>"
      case ConferenceSettings.FLAG_EXPECT =>
        "<span class='m2aFlag text-warning glyphicon glyphicon-flag'><span>"
      case ConferenceSettings.FLAG_IGNORE =>
        "<span class='m2aFlag text-muted glyphicon glyphicon-flag'><span>"
    }
  }

  def confirmPaper(id:Int, secret:String) = Action {
    if(papersService.findByIdAndSecret(id,secret).nonEmpty){
      papersService.updateStatus(id,Papers.STATUS_IN_PPLIB_QUEUE)
      PaperProcessingManager.run(database, configuration, papersService, questionService,
        method2AssumptionService, paperResultService, paperMethodService, permutationsServcie, answerService)
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
    if(file.exists() && (basePath =="public" || basePath == "tmp")) {
      Ok.sendFile(
        content = file,
        inline = true
      )
    } else {
      Unauthorized(views.html.error.unauthorized())
    }
  }

}

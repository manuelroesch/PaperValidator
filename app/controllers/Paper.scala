package controllers

import javax.inject.Inject

import helper.{Commons, PaperProcessingManager}
import models._
import play.api.{Logger, Configuration}
import play.api.db.Database
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
  * Created by manuel on 11.04.2016.
  */
class Paper @Inject()(database: Database, configuration: Configuration, papersService: PapersService,
                      questionService: QuestionService, method2AssumptionService: Method2AssumptionService,
                      paperResultService: PaperResultService, answerService: AnswerService,
                      conferenceSettingsService: ConferenceSettingsService) extends Controller {

  def show(id:Int, secret:String) = Action {
    if(papersService.findByIdAndSecret(id,secret).size > 0) {
      var results = paperResultService.findByPaperId(id)
      results = addMethodsAndAssumptions(id,results)
      Ok(views.html.paper.showPaper(id,secret,Commons.getSecretHash(secret),results))
    } else {
      Unauthorized("Invalid URL!")
    }
  }

  def addMethodsAndAssumptions(id:Int,results: List[PaperResult]) : List[PaperResult] = {
    var allResults = results
    val paper = papersService.findById(id).get
    val m2aList = answerService.findByEmail(paper.email)
    val conferenceSettings = conferenceSettingsService.findAllByConference(paper.conferenceId)
    m2aList.foreach(m2a => {
      conferenceSettings.foreach(confSetting => {
        if(m2a.method.toLowerCase() == confSetting.methodName.toLowerCase() &&
          m2a.assumption.toLowerCase() == confSetting.assumptionName.toLowerCase()) {
          val m2aDescr = m2a.method+" <span class='glyphicon glyphicon-arrow-right'></span> "+m2a.assumption
          val m2aResult = "Related: <b>"+ m2a.isRelated + "</b>, Checked before: <b>" + m2a.isCheckedBefore + "</b>"
          var symbol = PaperResult.SYMBOL_ERROR
          if(m2a.isRelated && m2a.isCheckedBefore && m2a.extraAnswer) symbol = PaperResult.SYMBOL_OK
          if(m2a.isRelated) symbol = PaperResult.SYMBOL_WARNING
          allResults = allResults:+ new PaperResult(Some(1L),id,PaperResult.TYPE_M2A,m2aDescr,m2aResult,symbol)
        }
      })
    })
    allResults
  }

  def confirmPaper(id:Int, secret:String) = Action {
    if(papersService.findByIdAndSecret(id,secret).size > 0){
      papersService.updateStatus(id,Papers.STATUS_IN_PPLIB_QUEUE)
      Future  {
        PaperProcessingManager.run(database, configuration, papersService, questionService,
          method2AssumptionService, paperResultService)
      }
      Ok(views.html.paper.confirmPaper())
    } else {
      Unauthorized("Invalid URL!")
    }
  }

}

package controllers

import javax.inject.Inject

import helper.PaperProcessingManager
import models.{QuestionService, PapersService}
import play.api.Configuration
import play.api.db.Database
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
  * Created by manuel on 11.04.2016.
  */
class Paper @Inject()(database: Database, configuration: Configuration, papersService: PapersService, questionService: QuestionService) extends Controller {

  def show(id:Int, secret:String) = Action {
    val papers = papersService.findAll()
    Ok(views.html.paper.showPaper(papers))
  }

  def confirmPaper(id:Int, secret:String) = Action {
    if(papersService.findByIdAndSecret(id,secret).size > 0){
      papersService.updateStatus(id,PaperProcessingManager.PAPER_STATUS_IN_PPLIB_QUEUE)
      Future  {
        PaperProcessingManager.run(database, configuration, papersService, questionService)
      }
      Ok(views.html.paper.confirmPaper())
    } else {
      Unauthorized("Invalid URL!")
    }
  }

}

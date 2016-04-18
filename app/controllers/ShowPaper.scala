package controllers

import java.io.{BufferedWriter, File, FileWriter}
import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import models.{AnswerService, Answer}
import play.api.mvc.{Action, Controller}

/**
  * Created by manuel on 11.04.2016.
  */
class ShowPaper @Inject() (answerService: AnswerService) extends Controller {
  def show = Action {
    val answer = answerService.getAll()
    Ok(views.html.showPaper(answer))
  }

}

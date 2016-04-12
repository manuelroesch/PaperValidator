package controllers

import java.io.{BufferedWriter, File, FileWriter}

import com.typesafe.config.ConfigFactory
import models.{AnswerDAO, Answer}
import play.api.mvc.{Action, Controller}

/**
  * Created by manuel on 11.04.2016.
  */
class ShowPaper extends Controller {
  def show = Action {
    val answer = AnswerDAO.getAll()
    Ok(views.html.showPaper(answer))
  }

}

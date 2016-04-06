package controllers

import models.Log
import play.api._
import play.api.mvc._

class Application extends Controller {

  def index = Action {
    //Log.createEntry("test", "test2", 123)
    Ok(views.html.index("Your new application is ready."))
  }

}
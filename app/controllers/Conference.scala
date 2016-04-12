package controllers

import java.io.{BufferedWriter, FileWriter, File}

import com.typesafe.config.ConfigFactory
import play.api.mvc.{Action, Controller}

/**
  * Created by manuel on 11.04.2016.
  */
class Conference extends Controller {
  def conference = Action {
    Ok(views.html.conference())
  }

  def writeMet2AssFile = Action(parse.json) { request =>
    val writer = new BufferedWriter(new FileWriter(new File(ConfigFactory.load().getString("highlighter.statFile"))))
    request.body.asOpt[Map[String,Map[String,String]]].map { met2ass =>
      met2ass.foreach{ keyValMeth =>
        keyValMeth._2.foreach { keyValAss =>
          if(keyValAss._2=="danger" || keyValAss._2=="warning")
          writer.write(keyValMeth._1+","+keyValAss._1+"\n")
        }
      }
      writer.close()
      Ok("Ok")
    }.getOrElse {
      Ok("Error")
    }
  }
}

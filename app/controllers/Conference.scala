package controllers

import java.io.{BufferedWriter, FileWriter, File}
import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import helper.Commons
import helper.email.MailTemplates
import models._
import play.Configuration
import play.api.Logger
import play.api.mvc.{Action, Controller}

import scala.io.Source

/**
  * Created by manuel on 11.04.2016.
  */
class Conference @Inject() (configuration: Configuration, conferenceService: ConferenceService,
                            conferenceSettingsService: ConferenceSettingsService, methodService: MethodService,
                            assumptionService: AssumptionService, method2AssumptionService: Method2AssumptionService,
                            emailService: EmailService
                           ) extends Controller {

  def conferenceCreator = Action {
    val templateDir = new File("statterms/templates")
    Ok(views.html.conference.conferenceCreator(templateDir.list().toList))
  }

  def conferenceCreated = Action(parse.urlFormEncoded) { request =>
    val name = request.body.get("name").get.head
    val email = request.body.get("email").get.head
    val template = request.body.get("template").get.head
    val secret = Commons.generateSecret()
    val id = conferenceService.create(name, email, secret)
    readTemplate(id,template)
    MailTemplates.sendAccountMail(email,configuration,emailService)
    val conferenceLink = configuration.getString("hcomp.ballot.baseURL") + routes.Conference.conferenceEditor(id,secret).url
    MailTemplates.sendConferenceMail(name,conferenceLink,email)
    Ok(views.html.conference.conferenceCreated(name))
  }

  def readTemplate(conferenceId: Int, templateName: String) = {
    Source.fromFile("statterms/templates/" + templateName + "/methods.csv", "UTF-8").getLines().foreach(line => {
      val cols = line.split(";")
      if(cols.length > 2) {
        methodService.create(conferenceId, cols(0), cols(1).toInt, cols(2))
      } else {
        methodService.create(conferenceId, cols(0), cols(1).toInt, "")
      }
    })
    Source.fromFile("statterms/templates/" + templateName + "/assumptions.csv", "UTF-8").getLines().foreach(line => {
      val cols = line.split(";")
      if(cols.length > 1) {
        assumptionService.create(conferenceId, cols(0), cols(1))
      } else {
        assumptionService.create(conferenceId, cols(0), "")
      }
    })
    Source.fromFile("statterms/templates/" + templateName + "/met2ass.csv", "UTF-8").getLines().foreach(line => {
      Logger.debug(line)
      val cols = line.split(";")
      val methodId = methodService.findByName(conferenceId,cols(0)).get.id.get
      val assumptionId = assumptionService.findByName(conferenceId,cols(1)).get.id.get
      if(cols.length > 2) {
        method2AssumptionService.create(conferenceId, methodId, assumptionId,cols(2))
      } else {
        method2AssumptionService.create(conferenceId, methodId, assumptionId,"")
      }
    })

  }

  def conferenceEditor(conferenceId: Int, secret: String) = Action {
    val conference = conferenceService.findByIdAndSecret(conferenceId,secret)
    if(conference.isEmpty) {
      Unauthorized(views.html.error.unauthorized())
    } else {
      Ok(views.html.conference.conferenceEditor(conferenceId,secret,conference.get.name))
    }
  }

  def flagEditor(conferenceId: Int, secret: String) = Action {
    val conference = conferenceService.findByIdAndSecret(conferenceId,secret)
    if(conference.isEmpty) {
      Unauthorized(views.html.error.unauthorized())
    } else {
      Ok(views.html.conference.conferenceFlags(conferenceId,secret,conference.get.name,conferenceSettingsService.findAllByConference(conferenceId)))
    }
  }

  def saveFlags = Action(parse.json) { request =>
    request.body.asOpt[Map[String,String]].map { cs =>
      val conference = conferenceService.findByIdAndSecret(cs("conferenceId").toInt,cs("secret"))
      if(conference.isEmpty) Unauthorized(views.html.error.unauthorized())
      if(cs("settingId").toInt < 0) {
        conferenceSettingsService.create(cs("conferenceId").toInt,cs("m2aId").toInt,cs("flag").toInt)
      } else {
        conferenceSettingsService.update(cs("settingId").toInt,cs("conferenceId").toInt,cs("flag").toInt)
      }
      Ok("Ok")
    }.getOrElse {
      Ok("Error")
    }
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

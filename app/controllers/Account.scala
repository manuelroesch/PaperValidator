package controllers

import java.io.{BufferedWriter, File, FileWriter}
import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import helper.{PaperProcessingManager, Commons}
import helper.email.MailTemplates
import models._
import play.Configuration
import play.api.Logger
import play.api.mvc.{Action, Controller}

import scala.io.Source

/**
  * Created by manuel on 11.04.2016.
  */
class Account @Inject()(configuration: Configuration, conferenceService: ConferenceService, papersService: PapersService,
                        emailService: EmailService, paperResultService: PaperResultService, answerService: AnswerService,
                        conferenceSettingsService: ConferenceSettingsService
                           ) extends Controller {

  def account(id: Int, secret: String) = Action {
    val email = emailService.findById(id,secret)
    if(email.isEmpty) {
      Unauthorized(views.html.error.unauthorized())
    } else {
      val papers = papersService.findByEmail(email.get.emailAddress)
      val papersWithStats = PaperStats.getStats(papers, papersService, paperResultService,
        answerService, conferenceSettingsService)
      val conferences = conferenceService.findByEmail(email.get.emailAddress)
      Ok(views.html.account.account(email.get.emailAddress,papersWithStats,conferences))
    }
  }

  def getAccess = Action {
    Ok(views.html.account.getAccess(""))
  }

  def sendAccessEmail = Action(parse.urlFormEncoded) { request =>
      val email = request.body.get("email").get.head
      MailTemplates.sendAccountMail(email,configuration,emailService)
      Ok(views.html.account.getAccess(email))
  }


}

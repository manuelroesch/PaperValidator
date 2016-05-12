package controllers

import java.io.{BufferedWriter, File, FileWriter}
import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import helper.Commons
import helper.email.MailTemplates
import models._
import play.Configuration
import play.api.mvc.{Action, Controller}

import scala.io.Source

/**
  * Created by manuel on 11.04.2016.
  */

class StatTerms @Inject()(configuration: Configuration, methodService: MethodService,
                          assumptionService: AssumptionService, method2AssumptionService: Method2AssumptionService,
                          conferenceService: ConferenceService, conferenceSettingsService: ConferenceSettingsService
                         ) extends Controller {
  def showStatTerms(conferenceId: Int, secret: String) = Action {
    if(conferenceService.findByIdAndSecret(conferenceId,secret).isEmpty) {
      Unauthorized(views.html.error.unauthorized())
    } else {
      Ok(views.html.conference.statterms(conferenceId,secret,methodService.findAll(conferenceId),assumptionService.findAll(conferenceId)))
    }
  }

  def addMethod(conferenceId: Int, secret: String) = Action(parse.urlFormEncoded) { request =>
    if(conferenceService.findByIdAndSecret(conferenceId,secret).isEmpty) {
      Unauthorized(views.html.error.unauthorized())
    } else {
      val name = request.body.get("method-name").get(0)
      var delta = 0
      if (request.body.get("method-delta").get(0) != "") {
        delta = request.body.get("method-delta").get(0).toInt
      }
      val synonyms = request.body.get("method-synonyms").get(0)
      methodService.create(conferenceId, name, delta, synonyms)
      Ok(views.html.conference.statterms(conferenceId,secret,methodService.findAll(conferenceId),assumptionService.findAll(conferenceId)))
    }
  }

  def editMethod(conferenceId: Int, secret: String) = Action(parse.urlFormEncoded) { request =>
    if(conferenceService.findByIdAndSecret(conferenceId,secret).isEmpty) {
      Unauthorized(views.html.error.unauthorized())
    } else {
      if(request.body.get("delete-method-id").isDefined) {
        val id=request.body.get("method-id").get(0).toInt
        conferenceSettingsService.deleteByMethodId(id)
        method2AssumptionService.deleteByMethodId(id)
        methodService.delete(id,conferenceId)
      } else {
        val id=request.body.get("method-id").get(0).toInt
        val name = request.body.get("method-name").get(0)
        var delta = 0
        if(request.body.get("method-delta").get(0) != "") {
          delta = request.body.get("method-delta").get(0).toInt
        }
        val synonyms = request.body.get("method-synonyms").get(0)
        methodService.update(id,conferenceId,name,delta,synonyms)
      }
      Ok(views.html.conference.statterms(conferenceId,secret,methodService.findAll(conferenceId),assumptionService.findAll(conferenceId)))
    }
  }

  def addAssumption(conferenceId: Int, secret: String) = Action(parse.urlFormEncoded) { request =>
    if(conferenceService.findByIdAndSecret(conferenceId,secret).isEmpty) {
      Unauthorized(views.html.error.unauthorized())
    } else {
      val name = request.body.get("assumption-name").get(0)
      val synonyms = request.body.get("assumption-synonyms").get(0)
      assumptionService.create(conferenceId, name, synonyms)
      Ok(views.html.conference.statterms(conferenceId,secret,methodService.findAll(conferenceId),assumptionService.findAll(conferenceId)))
    }
  }


  def editAssumption(conferenceId: Int, secret: String) = Action(parse.urlFormEncoded) { request =>
    if(conferenceService.findByIdAndSecret(conferenceId,secret).isEmpty) {
      Unauthorized(views.html.error.unauthorized())
    } else {
      if(request.body.get("delete-assumption-id").isDefined) {
        val id=request.body.get("assumption-id").get(0).toInt
        conferenceSettingsService.deleteByAssumptionId(id)
        method2AssumptionService.deleteByAssumptionId(id)
        assumptionService.delete(id,conferenceId)
      } else {
        val id=request.body.get("assumption-id").get(0).toInt
        val name = request.body.get("assumption-name").get(0)
        val synonyms = request.body.get("assumption-synonyms").get(0)
        assumptionService.update(id,conferenceId,name,synonyms)
      }
      Ok(views.html.conference.statterms(conferenceId,secret,methodService.findAll(conferenceId),assumptionService.findAll(conferenceId)))
    }
  }

  def method2assumptions(conferenceId : Int, secret: String) = Action {
    if(conferenceService.findByIdAndSecret(conferenceId,secret).isEmpty) {
      Unauthorized(views.html.error.unauthorized())
    } else {
      Ok(views.html.conference.method2assumption(conferenceId,secret,method2AssumptionService.findAll(conferenceId),methodService.findAll(conferenceId),assumptionService.findAll(conferenceId)))
    }
  }

  def addMethod2assumptions(conferenceId : Int, secret: String) = Action(parse.urlFormEncoded) { request =>
    if(conferenceService.findByIdAndSecret(conferenceId,secret).isEmpty) {
      Unauthorized(views.html.error.unauthorized())
    } else {
      val methodId = request.body.get("method2assumption-methodId").get(0).toInt
      val assumptionId = request.body.get("method2assumption-assumptionId").get(0).toInt
      val question = request.body.get("method2assumption-question").get(0).toString
      method2AssumptionService.create(conferenceId, methodId, assumptionId, question)
      Ok(views.html.conference.method2assumption(conferenceId,secret,method2AssumptionService.findAll(conferenceId), methodService.findAll(conferenceId), assumptionService.findAll(conferenceId)))
    }
  }

  def editMethod2assumptions(conferenceId : Int, secret: String) = Action(parse.urlFormEncoded) { request =>
    if(conferenceService.findByIdAndSecret(conferenceId,secret).isEmpty) {
      Unauthorized(views.html.error.unauthorized())
    } else {
      if(request.body.get("delete-method2assumption-id").isDefined) {
        val id=request.body.get("method2assumption-id").get(0).toInt
        conferenceSettingsService.deleteByM2AId(id)
        method2AssumptionService.delete(id,conferenceId)
      } else {
        val id=request.body.get("method2assumption-id").get(0).toInt
        val methodId = request.body.get("method2assumption-methodId").get(0).toInt
        val assumptionId = request.body.get("method2assumption-assumptionId").get(0).toInt
        val question = request.body.get("method2assumption-question").get(0).toString
        method2AssumptionService.update(id, conferenceId, methodId,assumptionId, question)
      }
      Ok(views.html.conference.method2assumption(conferenceId,secret,method2AssumptionService.findAll(conferenceId),methodService.findAll(conferenceId),assumptionService.findAll(conferenceId)))
    }
  }

}

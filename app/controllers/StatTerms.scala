package controllers

import java.io.{BufferedWriter, File, FileWriter}
import javax.inject.Inject

import com.typesafe.config.ConfigFactory
import helper.Commons
import helper.email.MailTemplates
import models.{Method2AssumptionService, AssumptionService, MethodService, ConferenceService}
import play.Configuration
import play.api.mvc.{Action, Controller}

/**
  * Created by manuel on 11.04.2016.
  */
class StatTerms @Inject()(configuration: Configuration, methodService: MethodService, assumptionService: AssumptionService, method2AssumptionService: Method2AssumptionService) extends Controller {
  def showStatTerms = Action {
    Ok(views.html.statterms(methodService.findAll(),assumptionService.findAll()))
  }

  def addMethod = Action(parse.urlFormEncoded) { request =>
    val name = request.body.get("method-name").get(0)
    var delta = 0
    if (request.body.get("method-delta").get(0) != "") {
      delta = request.body.get("method-delta").get(0).toInt
    }
    val synonyms = request.body.get("method-synonyms").get(0)
    methodService.create(name, delta, synonyms)
    Ok(views.html.statterms(methodService.findAll(),assumptionService.findAll()))
  }

  def editMethod = Action(parse.urlFormEncoded) { request =>
    if(request.body.get("delete-method-id").isDefined) {
      val id=request.body.get("method-id").get(0).toInt
      methodService.delete(id)
    } else {
      val id=request.body.get("method-id").get(0).toInt
      val name = request.body.get("method-name").get(0)
      var delta = 0
      if(request.body.get("method-delta").get(0) != "") {
        delta = request.body.get("method-delta").get(0).toInt
      }
      val synonyms = request.body.get("method-synonyms").get(0)
      methodService.update(id,name,delta,synonyms)
    }
    Ok(views.html.statterms(methodService.findAll(),assumptionService.findAll()))
  }

  def addAssumption = Action(parse.urlFormEncoded) { request =>
    val name = request.body.get("assumption-name").get(0)
    val synonyms = request.body.get("assumption-synonyms").get(0)
    assumptionService.create(name, synonyms)
    Ok(views.html.statterms(methodService.findAll(),assumptionService.findAll()))
  }


  def editAssumption = Action(parse.urlFormEncoded) { request =>
    if(request.body.get("delete-assumption-id").isDefined) {
      val id=request.body.get("assumption-id").get(0).toInt
      methodService.delete(id)
    } else {
      val id=request.body.get("assumption-id").get(0).toInt
      val name = request.body.get("assumption-name").get(0)
      val synonyms = request.body.get("assumption-synonyms").get(0)
      assumptionService.update(id,name,synonyms)
    }
    Ok(views.html.statterms(methodService.findAll(),assumptionService.findAll()))
  }

  def method2assumptions = Action {
    Ok(views.html.method2assumption(method2AssumptionService.findAll(),methodService.findAll(),assumptionService.findAll()))
  }

  def addMethod2assumptions = Action(parse.urlFormEncoded) { request =>
    val methodId = request.body.get("method2assumption-methodId").get(0).toInt
    val assumptionId = request.body.get("method2assumption-assumptionId").get(0).toInt
    val question = request.body.get("method2assumption-question").get(0).toString
    val answers = request.body.get("method2assumption-answers").get(0).toString
    method2AssumptionService.create(methodId, assumptionId, question, answers)
    Ok(views.html.method2assumption(method2AssumptionService.findAll(),methodService.findAll(),assumptionService.findAll()))
  }

  def editMethod2assumptions = Action(parse.urlFormEncoded) { request =>
    if(request.body.get("delete-method2assumption-id").isDefined) {
      val id=request.body.get("method2assumption-id").get(0).toInt
      method2AssumptionService.delete(id)
    } else {
      val id=request.body.get("method2assumption-id").get(0).toInt
      val methodId = request.body.get("method2assumption-methodId").get(0).toInt
      val assumptionId = request.body.get("method2assumption-assumptionId").get(0).toInt
      val question = request.body.get("method2assumption-question").get(0).toString
      val answers = request.body.get("method2assumption-answers").get(0).toString
      method2AssumptionService.update(id,methodId,assumptionId, question, answers)
    }
    Ok(views.html.method2assumption(method2AssumptionService.findAll(),methodService.findAll(),assumptionService.findAll()))
  }

}

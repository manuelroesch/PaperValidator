package helper.pdfpreprocessing.stats


import javax.inject.Inject

import helper.pdfpreprocessing.entities.{StatisticalAssumption, StatisticalMethod}
import models.{Papers, AssumptionService, ConferenceSettingsService, MethodService}
import play.api.Logger
import play.api.db.Database

import scala.collection.mutable
import scala.io.Source

/**
 * Created by pdeboer on 16/06/15.
 */

class StatTermloader(database: Database, paper: Papers) {
	val methodService = new MethodService(database)
	val assumptionService = new AssumptionService(database)
	val conferenceSettingsService = new ConferenceSettingsService(database)

	lazy val deltas: Map[String, Int] = methodService.findAll().map(method => {
		(method.name, method.delta)
	}).toMap


	lazy val terms: List[StatisticalMethod] = {

		val assumptionsNamesAndSynonyms = assumptionService.findAll().map(a => {
			var synonyms : List[String] = List()
			if(!a.synonyms.isEmpty) {
				synonyms = a.synonyms.split(",").toList
			}
			StatisticalAssumption(a.name,synonyms)
		})
		val methodNamesAndSynonyms = methodService.findAll().map(m => {
			var synonyms : List[String] = List()
			if(!m.synonyms.isEmpty) {
				synonyms = m.synonyms.split(",").toList
			}
			(m.name,synonyms)
		})

		var methodMap = new mutable.HashMap[String, List[StatisticalAssumption]]()
		conferenceSettingsService.findAllByConference(paper.conferenceId).map(cs => {
			if(cs.flag.isDefined) {
				val assumption = assumptionsNamesAndSynonyms.find(_.name == cs.assumptionName).getOrElse(throw new Exception(cs.assumptionName))
				methodMap += cs.methodName -> (assumption :: methodMap.getOrElse(cs.methodName, Nil))
			}
		})

		val methods = methodMap.map { case (method, assumptions) =>
			val methodAndSynonym = methodNamesAndSynonyms.find(_._1 == method).get
			val methodKey: String = methodAndSynonym._1
			StatisticalMethod(methodKey, methodAndSynonym._2, assumptions, deltas.getOrElse(methodKey, 0))
		}

		methods
	}.toList


	def getMethodAndSynonymsFromMethodName(method: String): Option[StatisticalMethod] = {
		terms.find(m => m.name.equalsIgnoreCase(method))
	}

}
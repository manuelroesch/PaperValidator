package helper.pdfpreprocessing.stats

import com.typesafe.config.ConfigFactory
import helper.pdfpreprocessing.entities.{StatisticalMethod, StatisticalAssumption}

import scala.collection.mutable
import scala.io.Source

/**
 * Created by pdeboer on 16/06/15.
 */
object StatTermloaderCSV {
	lazy val deltas: Map[String, Int] = Source.fromFile("statterms/deltas.csv", "UTF-8").getLines().map(l => {
		val cols = l.split(",")
		(cols(0), cols(1).toInt)
	}).toMap


	lazy val terms: List[StatisticalMethod] = {

		def getTermCSV(filename: String) = Source.fromFile(filename, "UTF-8").getLines().map(l => {
			val cols = l.split(",").map(_.trim)
			(cols(0), cols.drop(1).toList)
		}).toList

		val assumptionsInCSV = getTermCSV("statterms/assumptions.csv").map(a => StatisticalAssumption(a._1, a._2))
		val methodNamesAndSynonyms = getTermCSV("statterms/methods.csv")

		var methodMap = new mutable.HashMap[String, List[StatisticalAssumption]]()
		Source.fromFile(ConfigFactory.load().getString("highlighter.statFile"), "UTF-8").getLines().foreach(l => {
			val cols = l.split(",").map(_.trim)

			val assumption = assumptionsInCSV.find(_.name == cols(1)).getOrElse(throw new Exception(cols(1)))
			methodMap += cols(0) -> (assumption :: methodMap.getOrElse(cols(0), Nil))
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
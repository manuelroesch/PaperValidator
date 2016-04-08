package helper.pdfpreprocessing.entities

import java.io.File

import helper.pdfpreprocessing.pdf.PDFTextExtractor
import helper.pdfpreprocessing.stats.{StatTermSearcher, UniqueSearchStringIdentifier}

/**
  * Created by pdeboer on 16/10/15.
  */
case class Journal(name: String = "journal", basePath: File, year: Int = 2014) extends Serializable {
	def numColumns: Int = numColumnsOption.getOrElse(2)

	def numColumnsOption: Option[Int] = {
		".*_([0-9])+col".r.findFirstMatchIn(name).map(_.group(1).toInt)
	}

	def nameWithoutCol = if (numColumnsOption.isDefined) name.substring(0, name.length - "_1col".length) else name
}


case class Paper(name: String, file: File, journal: Journal) extends Serializable {
	val contents = new PDFTextExtractor(file.getAbsolutePath).pages
	lazy val lowerCaseContents = contents.map(_.toLowerCase)

	override def toString = s"Paper: $name"
}

case class StatTermOccurrence(term: StatisticalTerm, matchedExpression: String, paper: Paper, startIndex: Int, endIndex: Int, page: Int) {
	def actualText = paper.contents(page).substring(startIndex, endIndex)

	def escapedMatchText = StatTermSearcher.addRegexToAllowSpaces(actualText)

	def inclPageOffset(index: Int) = paper.contents.take(Math.max(0, page - 1)).map(_.length).sum + index

	lazy val uniqueSearchStringInPaper = new UniqueSearchStringIdentifier(this).findUniqueTerm()
}

case class StatTermOccurrenceGroup(term: StatisticalTerm, occurrences: List[StatTermOccurrence]) {
	lazy val minIndex = occurrences.map(o => o.inclPageOffset(o.startIndex)).min

	lazy val maxIndex = occurrences.map(o => o.inclPageOffset(o.endIndex)).max
}

sealed trait StatisticalTerm extends Serializable {
	def name: String

	def synonyms: List[String]

	def searchTerm = name.toLowerCase

	def isStatisticalMethod: Boolean
}

case class StatisticalMethod(name: String, synonyms: List[String], assumptions: List[StatisticalAssumption], delta: Int = 0) extends StatisticalTerm {
	override def isStatisticalMethod = true

	override def toString = "Method: " + name
}

case class StatisticalAssumption(name: String, synonyms: List[String]) extends StatisticalTerm {
	override def isStatisticalMethod = false

	override def toString = s"Assumption: " + name
}
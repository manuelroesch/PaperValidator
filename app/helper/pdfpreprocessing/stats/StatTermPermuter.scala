package helper.pdfpreprocessing.stats

import java.awt.Color

import helper.pdfpreprocessing.entities._

/**
 * Created by pdeboer on 20/10/15.
 */
class StatTermPermuter(occurrences: Iterable[StatTermOccurrence]) {
	private var _missingAssumptions = List.empty[(StatTermOccurrence, StatisticalAssumption)]

	def missingAssumptions = _missingAssumptions

	lazy val permutations: List[PDFPermutation] = {
		val methods = occurrences.filter(_.term.isInstanceOf[StatisticalMethod])
		val assumptionsMap: Map[StatisticalAssumption, Iterable[StatTermOccurrence]] = occurrences.filter(_.term.isInstanceOf[StatisticalAssumption])
			.groupBy(_.term).map(a => a._1.asInstanceOf[StatisticalAssumption] -> a._2)

		methods.flatMap(methodOccurrence => {
			val termOfMethod = methodOccurrence.term.asInstanceOf[StatisticalMethod]
			termOfMethod.assumptions.flatMap(a => {
				assumptionsMap.getOrElse(a, {
					_missingAssumptions = (methodOccurrence, a) :: _missingAssumptions
					Nil
				}).map(assumptionOccurrence => {
					PDFPermutation(assumptionOccurrence.paper, List(
						PDFHighlightTerm.fromTermOccurrence(assumptionOccurrence),
						PDFHighlightTerm.fromTermOccurrence(methodOccurrence)
					))
				})
			})
		}).toList
	}
}

case class PDFPermutation(paper: Paper, highlights: List[PDFHighlightTerm]) {
	def method = getTermByType(getMethods = true).asInstanceOf[StatisticalMethod]

	def assumption = getTermByType(getMethods = false).asInstanceOf[StatisticalAssumption]

	def getOccurrencesByType(getMethods: Boolean = true) = {
		highlights.filter(_.occurrence.term.isStatisticalMethod == getMethods).map(_.occurrence)
	}

	def distanceBetweenMinMaxIndex = {
		val indices = highlights.map(o => o.occurrence.inclPageOffset(o.occurrence.startIndex))
		indices.max - indices.min
	}

	private def getTermByType(getMethods: Boolean = true) = {
		getOccurrencesByType(getMethods).head.term
	}

	override def toString: String = s"Permutation($paper, $method, $assumption)"
}

case class PDFHighlightTerm(color: Color, occurrence: StatTermOccurrence)

object PDFHighlightTerm {
	def fromTermOccurrence(o: StatTermOccurrence) = {
		val color = o.term match {
			case m: StatisticalMethod => Color.yellow
			case a: StatisticalAssumption => Color.green
		}

		PDFHighlightTerm(color, o)
	}
}



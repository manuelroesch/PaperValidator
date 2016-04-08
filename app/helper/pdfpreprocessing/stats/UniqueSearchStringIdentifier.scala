package helper.pdfpreprocessing.stats

import helper.pdfpreprocessing.entities.StatTermOccurrence

/**
 * Created by pdeboer on 19/10/15.
 */
class UniqueSearchStringIdentifier(term: StatTermOccurrence) {
	val relevantPage = term.paper.contents(term.page)

	def checkUniqueness(startIndex: Int, endIndex: Int): String = {
		val targetString: String = StatTermSearcher.addRegexToAllowSpaces(relevantPage.substring(startIndex, endIndex))
		val resultCount: Int = targetString.r.findAllIn(relevantPage).length
		assert(resultCount > 0)

		if (resultCount == 1) {
			targetString
		} else {
			checkUniqueness(Math.max(0, startIndex - 1), Math.min(endIndex + 1, relevantPage.length))
		}
	}

	def findUniqueTerm(): String = {
		checkUniqueness(term.startIndex, term.endIndex)
	}
}

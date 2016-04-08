package helper.pdfpreprocessing.stats

import helper.pdfpreprocessing.entities.StatTermOccurrence

/**
 * Created by pdeboer on 20/10/15.
 */
trait TermPruner {
	def prune(occurrences: Iterable[StatTermOccurrence]): Iterable[StatTermOccurrence]
}

class StatTermPruning(pruners: List[TermPruner]) {
	def prune(occurrences: Iterable[StatTermOccurrence]): Iterable[StatTermOccurrence] =
		pruners.foldLeft(occurrences)((l, termpruner) => termpruner.prune(l))
}

class PruneTermsWithinOtherTerms extends TermPruner {
	override def prune(occurrences: Iterable[StatTermOccurrence]): Iterable[StatTermOccurrence] = {
		occurrences.groupBy(_.page).flatMap(p => {
			p._2.map(longerOccurrence =>
				if (p._2.exists(shorterOccurrence =>
					shorterOccurrence.startIndex >= longerOccurrence.startIndex && shorterOccurrence.endIndex <= longerOccurrence.endIndex
						&& shorterOccurrence != longerOccurrence
				)) None
				else Some(longerOccurrence))
		}).filter(_.isDefined).map(_.get)
	}
}

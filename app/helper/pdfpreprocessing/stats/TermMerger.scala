package helper.pdfpreprocessing.stats

import helper.pdfpreprocessing.entities.{StatTermOccurrenceGroup, StatTermOccurrence, StatisticalMethod}

/**
 * Created by pdeboer on 30/10/15.
 */
class TermMerger(terms: Iterable[StatTermOccurrence]) {
	lazy val methodOccurrences = terms.filter(_.term.isStatisticalMethod).groupBy(_.term).asInstanceOf[Map[StatisticalMethod, Iterable[StatTermOccurrence]]]

	def mergeMethod(method: StatisticalMethod, terms: Iterable[StatTermOccurrence]) = {
		var groups = terms.map(t => StatTermOccurrenceGroup(method, List(t))).toList
		var changes: Boolean = true
		while (changes) {
			changes = false


			val mergers = (for (i <- groups.indices; j <- groups.indices; if i != j) yield {
				val (g1, g2) = (groups(i), groups(j))
				if (g1.minIndex < g2.minIndex && g1.minIndex + method.delta > g2.minIndex)
					Some((g1, g2))
				else None
			}).filter(_.isDefined).map(_.get).toList

			val mergeList = mergers.flatMap(m => List(m._1, m._2))
			assert(mergeList.toSet.size == mergeList.size)

			val unmerged = groups.filterNot(g => mergeList.contains(g))

			groups = mergers.map(m => StatTermOccurrenceGroup(method, m._1.occurrences ::: m._2.occurrences)) ::: unmerged
		}
		groups
	}

	def mergeAllMethods = {
		methodOccurrences.map(m => mergeMethod(m._1, m._2))
	}
}

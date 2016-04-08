package helper.pdfpreprocessing.sampling

import java.io.File

import helper.pdfpreprocessing.entities.{StatTermOccurrence, Paper, StatisticalMethod}
import com.github.tototoshi.csv.CSVWriter

/**
  * Created by pdeboer on 30/10/15.
  */
class PaperSelection(val papers: List[PaperMethodMap]) extends MethodDistribution(Map.empty) {
	override lazy val methodOccurrenceMap: Map[StatisticalMethod, Int] = papers.foldLeft(Map.empty[StatisticalMethod, Int])((l, r) => {
		val allMethodKeys = l.keys.toSet ++ r.methodOccurrenceMap.keys.toSet
		allMethodKeys.map(key => key -> (l.getOrElse(key, 0) + r.methodOccurrenceMap.getOrElse(key, 0))).toMap
	})

	def newSelectionWithPaper(p: PaperMethodMap) = new PaperSelection(p :: papers)

	def persist(filename: String): Unit = {
		val w = CSVWriter.open(new File(filename))
		val keysList = methodOccurrenceMap.keys.toList
		w.writeRow("paper" :: keysList.map(_.name))
		papers.foreach(p => w.writeRow(p.paper.name :: keysList.map(k => p.methodOccurrenceMap.getOrElse(k, 0))))
		w.close()
	}

	var distanceToCache: Option[(Map[StatisticalMethod, Int], Int)] = None

	def distanceTo(target: Map[StatisticalMethod, Int]): Int = {
		if (distanceToCache.isDefined && distanceToCache.get._1 == target) {
			distanceToCache.get._2
		} else {
			val keys = target.keys.toSet ++ methodOccurrenceMap.keys.toSet
			val sum = keys.foldLeft(0)((cur, key) => {
				val delta = target.getOrElse(key, 0) - methodOccurrenceMap.getOrElse(key, 0)
				cur + Math.abs(delta)
			})
			distanceToCache = Some(target, sum)
			sum
		}
	}

	def exceedsMaxForAMethod(target: Map[StatisticalMethod, Int]) = {
		val keys = target.keys.toSet ++ methodOccurrenceMap.keys.toSet
		keys.exists(key => target.getOrElse(key, 0) - methodOccurrenceMap.getOrElse(key, 0) < 0)
	}

	def vectorLength = distanceTo(Map.empty)


	override def canEqual(other: Any): Boolean = other.isInstanceOf[PaperSelection]

	override def equals(other: Any): Boolean = other match {
		case that: PaperSelection =>
			super.equals(that) &&
				(that canEqual this) &&
				papers == that.papers
		case _ => false
	}

	override def hashCode(): Int = {
		val state = Seq(super.hashCode(), papers)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}
}

class PaperMethodMap(val paper: Paper, methodOccurrenceMap: Map[StatisticalMethod, Int]) extends MethodDistribution(methodOccurrenceMap) with Serializable {
	override def canEqual(other: Any): Boolean = other.isInstanceOf[PaperMethodMap]

	override def equals(other: Any): Boolean = other match {
		case that: PaperMethodMap =>
			super.equals(that) &&
				(that canEqual this) &&
				paper == that.paper
		case _ => false
	}

	override def hashCode(): Int = {
		val state = Seq(super.hashCode(), paper)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}
}

object PaperMethodMap {
	def fromOccurrenceList(occurrences: List[StatTermOccurrence]) = {
		val methodOccurrences = occurrences.filter(_.term.isStatisticalMethod)
		val map = methodOccurrences.groupBy(_.term).map(g => g._1.asInstanceOf[StatisticalMethod] -> g._2.size)
		new PaperMethodMap(methodOccurrences.head.paper, map)
	}
}

class MethodDistribution(_methodOccurrenceMap: Map[StatisticalMethod, Int]) extends Serializable {
	def methodOccurrenceMap: Map[StatisticalMethod, Int] = _methodOccurrenceMap

	override def toString = methodOccurrenceMap.toString

	def canEqual(other: Any): Boolean = other.isInstanceOf[MethodDistribution]

	override def equals(other: Any): Boolean = other match {
		case that: MethodDistribution =>
			(that canEqual this) &&
				methodOccurrenceMap == that.methodOccurrenceMap
		case _ => false
	}

	override def hashCode(): Int = {
		val state = Seq(methodOccurrenceMap)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}

	def hasMoreOccurrencesForAtLeastOneMethod(other: MethodDistribution) =
		methodOccurrenceMap.exists(e => e._2 > other.methodOccurrenceMap.getOrElse(e._1, 0))
}

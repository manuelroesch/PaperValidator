package helper.pdfpreprocessing.sampling

import java.util.concurrent.atomic.{AtomicLong, AtomicInteger}


import scala.collection.mutable

/**
  * Created by pdeboer on 05/11/15.
  */
class GreedySampler(val targetDistribution: MethodDistribution, val allPaperMethodMaps: List[PaperMethodMap]) {

	case class OrderablePaperSelection(paperSelection: PaperSelection) extends Comparable[OrderablePaperSelection] {
		lazy val f = paperSelection.distanceTo(targetDistribution.methodOccurrenceMap)
		lazy val g = paperSelection.vectorLength

		override def compareTo(o: OrderablePaperSelection): Int = {
			f.compareTo(o.f)
		}

		def unexploredSelections = allPaperMethodMaps.filterNot(m => paperSelection.papers.contains(m))

			.map(p => paperSelection.newSelectionWithPaper(p))
	}

	def run() {
		var closedSet = List.empty[PaperSelection]
		val openSet = new mutable.PriorityQueue[OrderablePaperSelection]()
		openSet += new OrderablePaperSelection(new PaperSelection(Nil))
		var best = new OrderablePaperSelection(new PaperSelection(Nil))

		val t = new Thread {
			val counter = new AtomicInteger(0)
			val discovered = new AtomicLong(0)
			val processed = new AtomicLong(0)
			var closestDistance = new AtomicInteger(Integer.MAX_VALUE)

			this.setDaemon(true)

			override def run(): Unit = {
				while (true) {
					Thread.sleep(1000)
					println(s"tried $counter variations. Discovered $discovered, processed $processed. closest distance $closestDistance")
					counter.set(0)
				}
			}
		}

		t.start()

		while (openSet.nonEmpty) {
			val current = openSet.dequeue()
			t.processed.incrementAndGet()
			t.counter.incrementAndGet()
			if (current.f == 0) {
				println("found working selection!")
				current.paperSelection.persist("target.csv")
				System.exit(0)
			} else {
				closedSet = current.paperSelection :: closedSet
				current.unexploredSelections.par.foreach(s => {
					if (!s.exceedsMaxForAMethod(targetDistribution.methodOccurrenceMap) && !closedSet.contains(s)) {
						val ops = OrderablePaperSelection(s)
						openSet.synchronized {
							if (!openSet.toList.contains(ops)) {
								openSet.enqueue(ops)
								if (ops.f < best.f) {
									openSet.synchronized {
										best = ops
									}
									t.closestDistance.set(best.f)
									println(s"found selection with lower distance: ${best.f}: $best")
									best.paperSelection.persist("tempselection.csv")
								}
								t.discovered.incrementAndGet()
							}
						}
					}
				})
			}
		}
	}
}

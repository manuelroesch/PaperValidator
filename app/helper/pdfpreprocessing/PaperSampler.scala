package helper.pdfpreprocessing

import java.io.File
import javax.inject.Inject

import helper.pdfpreprocessing.entities.Paper
import helper.pdfpreprocessing.pdf.PDFLoader
import helper.pdfpreprocessing.sampling.{RandomSampler, MethodDistribution, PaperMethodMap, PaperSelection}
import helper.pdfpreprocessing.stats.StatTermSearcher
import ch.uzh.ifi.pdeboer.pplib.process.entities.FileProcessMemoizer
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import play.api.db.Database


/**
  * Created by pdeboer on 30/10/15.
  */
class PaperSampler @Inject()(database: Database) extends App with LazyLogging {
	logger.info("starting sampling")
	val mem = new FileProcessMemoizer("everything")

	val conf = ConfigFactory.load()
	val INPUT_DIR = conf.getString("highlighter.pdfSourceDir")
	val PERCENTAGE = conf.getDouble("sampler.targetPercentage")

	case class SerializedPaper(p: Array[Paper]) extends Serializable

	val allPapers: Array[Paper] = new PDFLoader(new File(INPUT_DIR)).papers
	val allPaperMethodMaps: List[PaperMethodMap] = allPapers.map(p => new StatTermSearcher(p, database, null, includeAssumptions = false).occurrences.toList)
		.filter(_.nonEmpty).map(p => PaperMethodMap.fromOccurrenceList(p)).filter(_.methodOccurrenceMap.values.sum > 0).toList

	val targetDistribution: MethodDistribution = new MethodDistribution(
		new PaperSelection(allPaperMethodMaps).methodOccurrenceMap.map(e => e._1 -> (e._2 * PERCENTAGE).toInt))
	logger.info("complete distribution is " + new PaperSelection(allPaperMethodMaps))
	logger.info(s"target distribution is $targetDistribution")

	(1 to Runtime.getRuntime.availableProcessors()).par.exists(i => {
		new RandomSampler(targetDistribution, allPaperMethodMaps, i).run()
		true
	})
	logger.info("found solution. completed")
}


package helper.pdfpreprocessing

import java.io.File

import helper.Commons
import helper.pdfpreprocessing.csv.{CSVExporter, Snippet}
import helper.pdfpreprocessing.pdf.{PDFHighlighter, PDFLoader}
import helper.pdfpreprocessing.png.{PNGProcessor, PDFToPNGConverter}
import helper.pdfpreprocessing.stats._
import helper.pdfpreprocessing.util.FileUtils
import com.typesafe.config.ConfigFactory
import models.{PaperMethodService, Papers}
import play.api.Logger
import play.api.db.Database

/**
	* Created by pdeboer on 16/10/15.
	*/
object PreprocessPDF {

	val conf = ConfigFactory.load()

	val TMP_DIR = conf.getString("highlighter.tmpDir")
	val INPUT_DIR = conf.getString("highlighter.pdfSourceDir")
	val OUTPUT_DIR = conf.getString("highlighter.snippetDir")
	val CONVERT_CMD = conf.getString("highlighter.convertCmd")
	val PERMUTATIONS_CSV_FILENAME = conf.getString("highlighter.permutationFilename")
	val PNG_ERROR_OUTPUT_PATH = conf.getString("highlighter.pngErrorPath")

	def start(database: Database, paperMethodService: PaperMethodService, paper : Papers) : Int = {
		Logger.debug("starting highlighting")


		//FileUtils.emptyDir(new File(OUTPUT_DIR))
		val secretHash = Commons.getSecretHash(paper.secret)
		val allPapers = new PDFLoader(new File(INPUT_DIR + "/" + secretHash)).papers
		val snippets = allPapers.par.flatMap(snip => {
			val searcher = new StatTermSearcher(snip,database,paper)
			searcher.occurrences.foreach(occurence => {
				if(occurence.term.isStatisticalMethod) {
					paperMethodService.create(paper.id.get,occurence.term.name)
				}
			})
			val statTermsInPaper = new StatTermPruning(List(new PruneTermsWithinOtherTerms)).prune(searcher.occurrences)
			val combinationsOfMethodsAndAssumptions = new StatTermPermuter(statTermsInPaper).permutations

			val snippets = combinationsOfMethodsAndAssumptions.sortBy(_.distanceBetweenMinMaxIndex).zipWithIndex.par.map(p => {
				val highlightedPDF = new PDFHighlighter(p._1, OUTPUT_DIR, p._2 + "_").copyAndHighlight()
				val fullPNG = new PDFToPNGConverter(highlightedPDF, p._1, CONVERT_CMD).convert()

				val statTermLocationsInSnippet = new PNGProcessor(fullPNG, p._1, true).process()
				statTermLocationsInSnippet.map(s => Snippet(fullPNG, p._1, s))
			})

			Logger.info(s"finished processing paper $snip")
			snippets.filter(_.isDefined).map(_.get)
		}).toList

		new CSVExporter(OUTPUT_DIR + "/" + secretHash + "/permutations.csv", snippets).persist()
		snippets.length
	}

}
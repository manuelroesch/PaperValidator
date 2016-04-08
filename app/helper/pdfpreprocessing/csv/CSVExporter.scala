package helper.pdfpreprocessing.csv

import java.io.File

import helper.pdfpreprocessing.png.StatTermLocationsInPNG
import helper.pdfpreprocessing.stats.PDFPermutation
import com.github.tototoshi.csv.CSVWriter

/**
  * Created by pdeboer on 21/10/15.
  */
class CSVExporter(file: String, snippets: Iterable[Snippet]) {
	def persist() {
		val writer = CSVWriter.open(new File(file))
		writer.writeRow(Seq("group_name", "method_index", "snippet_filename", "pdf_path", "method_on_top", "relative_height_top", "relative_height_bottom", "distance_min_index_max"))
		snippets.foreach(p => writer.writeRow(Seq(p.groupName, p.methodIdentifier, p.snippetPath, p.permutation.paper.file.getAbsolutePath, p.methodOnTop, p.statTermLocationsInPNG.relativeTop, p.statTermLocationsInPNG.relativeBottom, p.permutation.distanceBetweenMinMaxIndex)))
		writer.close()
	}
}

case class Snippet(snippetPath: File, permutation: PDFPermutation, statTermLocationsInPNG: StatTermLocationsInPNG) {

	def groupName = Seq(journalAndPaper, permutation.assumption.name, assumptionPosition).mkString("/")

	def methodIdentifier = permutation.method.name + "_" + permutation.getOccurrencesByType(getMethods = true).map(m => m.page + ":" + m.startIndex).mkString("_")

	def assumptionPosition: String = {
		val assumption = permutation.getOccurrencesByType(getMethods = false).head
		assumption.page + ":" + assumption.startIndex
	}

	def journalAndPaper: String = {
		permutation.paper.journal.name + "_" + permutation.paper.name
	}

	def methodOnTop = if (statTermLocationsInPNG.methodOnTop) 1 else 0
}
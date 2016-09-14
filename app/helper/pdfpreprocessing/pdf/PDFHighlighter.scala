package helper.pdfpreprocessing.pdf

import java.io._

import helper.pdfpreprocessing.stats.PDFPermutation
import helper.pdfpreprocessing.util.FileUtils
import play.api.Logger
import org.apache.pdfbox.pdmodel.PDDocument

/**
 * Created by pdeboer on 16/10/15.
	* Edited by mroesch on 04/05/16
 */
class PDFHighlighter(permutation: PDFPermutation, outputBaseFolder: String = "output/", filenamePrefix: String = "") {
	def targetFolder = {
		val basefolderWithTrailingSlash = if (outputBaseFolder.endsWith("/")) outputBaseFolder else outputBaseFolder + "/"
		val fullname = basefolderWithTrailingSlash + permutation.paper.journal.name + "/" + targetFilename + "/"
		new File(fullname).mkdirs()
		fullname
	}

	def targetFilename = filenamePrefix + permutation.paper.name.replaceAll("[\\(\\) \\[\\]]", "")

	def highlight(pdfToHighlight: File): Boolean = {
		try {
			val pdDoc: PDDocument = PDDocument.load(pdfToHighlight)

			val pdfHighlight: TextHighlight = new TextHighlight("UTF-8")
			pdfHighlight.setLineSeparator(" ")
			pdfHighlight.initialize(pdDoc)

			permutation.highlights.foreach(i =>
				pdfHighlight.highlight(
					i.occurrence.uniqueSearchStringInPaper.r.pattern,
					i.occurrence.escapedMatchText.r.pattern, i.color, i.occurrence.page,false,""))

			val byteArrayOutputStream = new ByteArrayOutputStream()

			if (pdDoc != null) {
				pdDoc.save(byteArrayOutputStream)
				pdDoc.close()
			}

			Some(new BufferedOutputStream(new FileOutputStream(pdfToHighlight))).foreach(o => {
				o.write(byteArrayOutputStream.toByteArray)
				o.close()
			})
			Logger.info(s"highlighted $permutation")
			true
		}
		catch {
			case e: Throwable => {
				Logger.error("couldn't highlight pdf", e)
				false
			}
		}
	}

	def copyAndHighlight(): File = {
		val pdfToHighlight = FileUtils.copyFileIntoDirectory(permutation.paper.file, targetFolder, Some(targetFilename + ".pdf"))

		highlight(pdfToHighlight)
		pdfToHighlight
	}
}

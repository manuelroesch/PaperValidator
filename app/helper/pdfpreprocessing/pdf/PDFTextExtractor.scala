package helper.pdfpreprocessing.pdf

import java.io.FileInputStream

import com.typesafe.scalalogging.LazyLogging
import org.apache.pdfbox.pdfparser.PDFParser
import org.apache.pdfbox.pdmodel.PDDocument

/**
 * Created by pdeboer on 16/10/15.
 */
class PDFTextExtractor(pdfPath: String) extends LazyLogging {
	lazy val pages: List[String] = {
		try {
			val parser: PDFParser = new PDFParser(new FileInputStream(pdfPath))
			parser.parse()
			val pdDoc: PDDocument = new PDDocument(parser.getDocument)

			val pdfHighlight: TextHighlight = new TextHighlight("UTF-8")
			pdfHighlight.setLineSeparator(" ")
			pdfHighlight.initialize(pdDoc)

			val txt: List[String] = (0 to pdDoc.getNumberOfPages).map(pdfHighlight.textCache.getText(_)).toList
			pdDoc.close()
			txt
		} catch {
			case e1: Throwable => {
				logger.error("An error occurred while extracting text from pdf ", e1)
				Nil
			}
		}
	}
}

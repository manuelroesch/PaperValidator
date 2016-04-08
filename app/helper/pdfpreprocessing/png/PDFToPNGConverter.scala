package helper.pdfpreprocessing.png

import java.awt.image.BufferedImage
import java.io.File


import org.apache.pdfbox.pdmodel.{PDPage, PDDocument}
import org.apache.pdfbox.util.ImageIOUtil
import scala.collection.JavaConversions._

import scala.sys.process._
import helper.pdfpreprocessing.stats.PDFPermutation
import helper.pdfpreprocessing.util.FileUtils
import play.api.Logger

/**
 * Created by pdeboer on 20/10/15.
 */
class PDFToPNGConverter(pdfFile: File, perm: PDFPermutation, conversionCommand: String) {
	def convert(): File = {
		val document = PDDocument.loadNonSeq(pdfFile, null)
		val pdPages  = document.getDocumentCatalog().getAllPages()
		/*for (i <- 0 until pdPages.size()) yield {
			val page = pdPages.get(i)
			val image = page.asInstanceOf[PDPage].convertToImage(BufferedImage.TYPE_BYTE_GRAY,200)
			ImageIOUtil.writeImage(image, destinationPath, 200)
		}*/
		var pr = ""
		if(pageRange.contains(",")) {
			pr = pageRange.substring(pageRange.indexOf(",")+1).replace("[","").replace("]","")
		} else {
			pr = pageRange.replace("[","").replace("]","")
		}
		val page = pdPages.get(pr.toInt)
		val image = page.asInstanceOf[PDPage].convertToImage(BufferedImage.TYPE_INT_RGB,200)
		ImageIOUtil.writeImage(image, destinationPath, 100)
		document.close()
		new File(destinationPath)
			/*if (conversionCommandWithParameters.! != 0) {
			FileUtils.copyFileIntoDirectory(pdfFile, "errors_convertPDFtoPNG/")
			Logger.error(s"couldn't convert file using $conversionCommandWithParameters")
			null
		} else {
			Logger.debug(s"Permutation successfully converted to PNG: " + perm)
			new File(destinationPath)
		}*/
	}

	def destinationPath = pdfFile.getParentFile.getAbsolutePath + "/" + FileUtils.filenameWithoutExtension(pdfFile) + ".png"

	def conversionCommandWithParameters = {
		Seq("cmd", "/C", s"$conversionCommand -density 200 -append ${pdfFile.getPath + pageRange} $destinationPath")
	}

	private def pageRange: String = {
		val pageIndices = perm.highlights.map(_.occurrence.page - 1)
		val (minPage, maxPage) = (pageIndices.min, pageIndices.max)
		if (minPage == maxPage) s"[$minPage]" else s"[$minPage,$maxPage]"
	}

}

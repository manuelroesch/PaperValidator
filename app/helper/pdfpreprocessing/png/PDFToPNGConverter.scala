package helper.pdfpreprocessing.png

import java.awt.Color
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
			val pdPages = document.getDocumentCatalog.getAllPages
			/*
			var pr = ""
			if(pageRange.contains(",")) {
				pr = pageRange.substring(pageRange.indexOf(",")+1).replace("[","").replace("]","")
			} else {
				pr = pageRange.replace("[","").replace("]","")
			}
			val page = pdPages.get(pr.toInt)
			val image = page.asInstanceOf[PDPage].convertToImage(BufferedImage.TYPE_INT_RGB,200)*/
			////////////////////////////////
			var image: BufferedImage = null
			if (pageRange.contains(",")) {
				val pageNumberStart = pageRange.substring(0, pageRange.indexOf(",")).replace("[", "").replace("]", "").toInt
				val pageNumberStop = pageRange.substring(pageRange.indexOf(",") + 1).replace("[", "").replace("]", "").toInt
				image = pdPages.get(pageNumberStart).asInstanceOf[PDPage].convertToImage(BufferedImage.TYPE_INT_RGB, 150)
				image = joinBufferedImage(image, pdPages.get(pageNumberStop).asInstanceOf[PDPage].convertToImage(BufferedImage.TYPE_INT_RGB, 150))
			} else {
				val pageNumber = pageRange.replace("[", "").replace("]", "").toInt
				image = pdPages.get(pageNumber).asInstanceOf[PDPage].convertToImage(BufferedImage.TYPE_INT_RGB, 150)
			}
			ImageIOUtil.writeImage(image, destinationPath, 100)
			document.close()
			new File(destinationPath)
		/*
			if (conversionCommandWithParameters.! != 0) {
			FileUtils.copyFileIntoDirectory(pdfFile, "errors_convertPDFtoPNG/")
			Logger.error(s"couldn't convert file using $conversionCommandWithParameters")
			null
		} else {
			Logger.debug(s"Permutation successfully converted to PNG: " + perm)
			new File(destinationPath)
		}*/
	}

	def joinBufferedImage(img1 : BufferedImage, img2 : BufferedImage) : BufferedImage = {
		//do some calculate first
		val wid = Math.max(img1.getWidth(),img2.getWidth())
		val height = img1.getHeight()+img2.getHeight()
		//create a new buffer and draw two image into the new image
		val newImage = new BufferedImage(wid,height, BufferedImage.TYPE_INT_RGB)
		val g2 = newImage.createGraphics()
		val oldColor = g2.getColor
		//fill background
		g2.setPaint(Color.WHITE)
		g2.fillRect(0, 0, wid, height)
		//draw image
		g2.setColor(oldColor)
		g2.drawImage(img1, null, 0, 0)
		g2.drawImage(img2, null, 0, img1.getHeight())
		g2.dispose()
		newImage
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

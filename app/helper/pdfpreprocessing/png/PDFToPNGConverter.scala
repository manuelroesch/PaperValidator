package helper.pdfpreprocessing.png

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO


import org.apache.pdfbox.pdmodel.{PDPage, PDDocument}

import helper.pdfpreprocessing.stats.PDFPermutation
import helper.pdfpreprocessing.util.FileUtils
import org.apache.pdfbox.rendering.{ImageType, PDFRenderer}
import org.apache.pdfbox.tools.imageio.ImageIOUtil

/**
 * Created by pdeboer on 20/10/15.
 */
class PDFToPNGConverter(pdfFile: File, perm: PDFPermutation, conversionCommand: String) {
	def convert(): File = {
		//System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider")
		val pdfDoc = PDDocument.load(pdfFile)
		val pdfRenderer = new PDFRenderer(pdfDoc)

		var image: BufferedImage = null
		var image2: BufferedImage = null

		if (pageRange.contains(",")) {
			val pageNumberStart = pageRange.substring(0, pageRange.indexOf(",")).replace("[", "").replace("]", "").toInt
			val pageNumberStop = pageRange.substring(pageRange.indexOf(",") + 1).replace("[", "").replace("]", "").toInt
			image = pdfRenderer.renderImageWithDPI(pageNumberStart, 150, ImageType.RGB)
			image2 = pdfRenderer.renderImageWithDPI(pageNumberStop, 150, ImageType.RGB)
			image = joinBufferedImage(image, image2)
		} else {
			val pageNumber = pageRange.replace("[", "").replace("]", "").toInt
				try {
					image = pdfRenderer.renderImageWithDPI(pageNumber, 150, ImageType.RGB)
				} catch  {
					case error : Error => error.printStackTrace()
					case exception : Exception => exception.printStackTrace()
					case throwable : Throwable => throwable.printStackTrace()
				}
		}
		if(image!=null)	ImageIOUtil.writeImage(image, destinationPath, 150)
		pdfDoc.close()
		new File(destinationPath)

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
		val pageBreakImg = ImageIO.read(new File("public/images/page-break.png"))
		g2.drawImage(pageBreakImg, null, 0, img1.getHeight()-62)
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

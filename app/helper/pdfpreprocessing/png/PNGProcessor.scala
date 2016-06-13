package helper.pdfpreprocessing.png

import java.awt.Color
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

import helper.pdfpreprocessing.stats.PDFPermutation
import helper.pdfpreprocessing.{PreprocessPDF, util}
import play.api.Logger

/**
  * Created by Mattia
  * With some modifications by pdeboer
  */
class PNGProcessor(pngImage: File, pdfPermutation: PDFPermutation, val enableCropping: Boolean = true) {
	assert(pngImage != null)

	//tolerated color range of highlighted Methods (yellow) and assumptions (green)
	val YELLOW_RANGES = List[(Int, Int)]((190, 255), (190, 255), (100, 160))
	val GREEN_RANGES = List[(Int, Int)]((100, 160), (190, 255), (100, 160))

	val PADDING_SNIPPET = 200
	val MINIMAL_SNIPPET_HEIGHT = 300

	lazy val inputImage = try {
		Some(ImageIO.read(pngImage))
	}
	catch {
		case e: Throwable => Logger.error("couldn't load image", e); None
	}

	private lazy val inputImageHeight: Int = inputImage.get.getHeight

	lazy val (yellowCoords: List[Point2D], greenCoords: List[Point2D]) = coordinatesOfHighlights

	def process() = {
		val managerForTargetPNG = if (enableCropping) {
			if (pdfPermutation.paper.journal.numColumns < 3) cropPNG()
			new PNGProcessor(pngImage, pdfPermutation, enableCropping = false)
		} else this

		if (!managerForTargetPNG.enableCropping) {
			if (managerForTargetPNG.yellowCoords.isEmpty || managerForTargetPNG.greenCoords.isEmpty) {
				copyToCroppingErrorFolder(1)
				None
			} else {
				val minGreen = managerForTargetPNG.greenCoords.map(_.getY).min / inputImageHeight
				val closestYellow = managerForTargetPNG.yellowCoords.minBy(v => Math.abs((v.getY / inputImageHeight) - minGreen)).getY / inputImageHeight

				val boundaryMin = Math.min(minGreen, closestYellow) * 100
				val boundaryMax = Math.max(minGreen, closestYellow) * 100

				Some(StatTermLocationsInPNG(managerForTargetPNG.isMethodOnTop, boundaryMin, boundaryMax))
			}
		} else Some(StatTermLocationsInPNG(managerForTargetPNG.isMethodOnTop))
	}

	private def copyToCroppingErrorFolder(nr : Int): Unit = {
		util.FileUtils.copyFileIntoDirectory(pngImage, PreprocessPDF.PNG_ERROR_OUTPUT_PATH)
		Logger.error(s"Couldn't find highlightings in $pngImage" + nr)
	}

	def cropPNG() {
		extractAndGenerateImage()
	}

	def coordinatesOfHighlights: (List[Point2D], List[Point2D]) = {
		val width = inputImage.get.getWidth

		var yellowCoords = List.empty[Point2D]
		var greenCoords = List.empty[Point2D]

		for (x <- 0 until width) {
			for (y <- 0 until inputImageHeight) {
				val color = new Color(inputImage.get.getRGB(x, y))
				if (isSameColor(color, YELLOW_RANGES)) {
					yellowCoords ::= new Point2D.Double(x, y)
				} else if (isSameColor(color, GREEN_RANGES)) {
					greenCoords ::= new Point2D.Double(x, y)
				}
			}
		}
		(yellowCoords, greenCoords)
	}

	def isMethodOnTop: Boolean = {
		try {
			val greenMin: Point2D = greenCoords match {
				case Nil => new Point2D.Double(0.0, 0.0)
				case greenList => greenList.minBy(_.getY)
			}
			val closestYellow: Point2D = yellowCoords match {
				case Nil => new Point2D.Double(0.0, 0.0)
				case yellowList => yellowList.minBy(v => math.abs(v.getY - greenMin.getY))
			}
			if (Math.abs(closestYellow.getY - greenMin.getY) < 5) {
				closestYellow.getX < greenMin.getX
			} else {
				closestYellow.getY < greenMin.getY
			}

		} catch {
			case e: Exception => {
				copyToCroppingErrorFolder(2)
				Logger.debug("Cannot find highlight to establish if method or prerequisite is on top.")
				true
			}
		}
	}

	def extractAndGenerateImage() {
		if (greenCoords.nonEmpty && yellowCoords.nonEmpty) {
			val (startY: Int, endY: Int) = extractImageBoundaries()

			val snippetImage: BufferedImage = createImage(inputImage.get, startY, endY)
			ImageIO.write(snippetImage, "png", pngImage)

			Logger.debug(s"Snippet successfully written: $pngImage")
		} else {
			copyToCroppingErrorFolder(3)
		}
	}

	def createImage(inputImage: BufferedImage, startY: Int, endY: Int): BufferedImage = {
		val snippetHeight = endY - startY

		/*val bothHighlightsOnLeftSide = yellowCoords.map(_.getX).max < inputImage.getWidth / 2 && greenCoords.map(_.getX).max < inputImage.getWidth / 2
		val bothHighlightsOnRightSide = yellowCoords.map(_.getX).min > inputImage.getWidth / 2 && greenCoords.map(_.getX).min > inputImage.getWidth / 2
		val isTwoColumnPaper: Boolean = pdfPermutation.paper.journal.numColumns == 2
		val isOnSamePage: Boolean = pdfPermutation.highlights.forall(_.occurrence.page == pdfPermutation.highlights.head.occurrence.page)
		val offsetLeft = if (isOnSamePage && isTwoColumnPaper && (bothHighlightsOnLeftSide || bothHighlightsOnRightSide)) inputImage.getWidth / 2
		else 0*/
		val bothHighlightsOnRightSide = false
		val offsetLeft = 0

		val snippetWidth = inputImage.getWidth - offsetLeft


		val snippetImage = new BufferedImage(snippetWidth, snippetHeight, BufferedImage.TYPE_INT_RGB)
		for (x <- 0 until snippetWidth) {
			for (y <- 0 until snippetHeight) {
				val targetXCoordinate: Int = if (bothHighlightsOnRightSide) x + offsetLeft else x
				snippetImage.setRGB(x, y, new Color(inputImage.getRGB(targetXCoordinate, startY + y)).getRGB)
			}
		}
		snippetImage
	}

	def extractImageBoundaries(): (Int, Int) = {
		val maxHeight = inputImageHeight
		val minGreen = greenCoords.minBy(_.getY)
		val minYellow = yellowCoords.map(y => (Math.abs(minGreen.getY - y.getY), y)).minBy(_._1)._2
		val startY = Math.max(0, Math.min(minYellow.getY, minGreen.getY) - PADDING_SNIPPET)
		val endY = Math.min(Math.max(minYellow.getY, minGreen.getY) + PADDING_SNIPPET, maxHeight)

		checkMinimalBoundaries(startY.toInt, endY.toInt, maxHeight)
	}

	def checkMinimalBoundaries(startY: Int, endY: Int, maxImageHeight: Int): (Int, Int) = {
		var minY = startY
		var maxY = endY
		val originalHeight = maxY - minY
		if (originalHeight < MINIMAL_SNIPPET_HEIGHT) {
			val deltaHeight = (MINIMAL_SNIPPET_HEIGHT - originalHeight) / 2
			if (minY - deltaHeight > 0) {
				minY = minY - deltaHeight
			} else {
				minY = 0
			}
			if (maxY + deltaHeight < maxImageHeight) {
				maxY = maxY + deltaHeight
			} else {
				maxY = maxImageHeight
			}
		}
		(minY, maxY)
	}

	def delta(x: Int, y: Int): Int = {
		Math.abs(x - y)
	}

	def isSameColor(color1: Color, color2: List[(Int, Int)]): Boolean = {
		color1.getRed <= color2.head._2 &&
			color1.getRed >= color2.head._1 &&
			color1.getGreen <= color2(1)._2 &&
			color1.getGreen >= color2(1)._1 &&
			color1.getBlue <= color2.last._2 &&
			color1.getBlue >= color2.last._1

	}
}

case class StatTermLocationsInPNG(methodOnTop: Boolean, relativeTop: Double = 0, relativeBottom: Double = 0)

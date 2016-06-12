package helper

import java.awt.Color
import java.io.File

import helper.pdfpreprocessing.PreprocessPDF
import models.{PaperResult, PaperResultService, Papers}
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.{ImageType, PDFRenderer}
import play.api.Logger

/**
  * Created by manuel on 29.05.2016.
  */
object LayoutChecker {

  val BORDER_SIZE = 30
  val COLOR_SENSITIVITY = 5

  def check(paper: Papers, paperResultService: PaperResultService) = {
    val paperDir = PreprocessPDF.INPUT_DIR + "/" + Commons.getSecretHash(paper.secret) + "/" + paper.name
    val pdfDoc = PDDocument.load(new File(paperDir))
    val pdfRenderer = new PDFRenderer(pdfDoc)
    var i = 0
    var checkBorderOk = true
    var checkColorsOk = true
    while(i < pdfDoc.getNumberOfPages && (checkBorderOk || checkColorsOk)) {
      val image = pdfRenderer.renderImageWithDPI(i, 150, ImageType.RGB)
      for(w <- 0 until image.getWidth) {
        for(h <- 0 until image.getHeight) {
          val rgb = new Color(image.getRGB(w,h))
          if(checkBorderOk && (h < BORDER_SIZE || w < BORDER_SIZE || h > image.getHeight-BORDER_SIZE || w > image.getWidth-BORDER_SIZE)) {
            if(rgb.getRed < 250 || rgb.getBlue < 250 || rgb.getGreen < 250) {
              checkBorderOk = false
            }
          }
          if(checkColorsOk && (Math.abs(rgb.getRed - rgb.getBlue) > COLOR_SENSITIVITY ||
            Math.abs(rgb.getRed - rgb.getGreen) > COLOR_SENSITIVITY ||
            Math.abs(rgb.getBlue - rgb.getGreen) > COLOR_SENSITIVITY)) {
            checkColorsOk = false
          }
        }
      }
      i=i+1
    }

    if(checkBorderOk)
      paperResultService.create(paper.id.get,PaperResult.TYPE_LAYOUT_BORDER,"Printable Borders",
        "Borders are printable",PaperResult.SYMBOL_OK)
    else
      paperResultService.create(paper.id.get,PaperResult.TYPE_LAYOUT_BORDER,"Printable Borders",
        "Borders are too small",PaperResult.SYMBOL_WARNING)
    if(checkColorsOk)
      paperResultService.create(paper.id.get,PaperResult.TYPE_LAYOUT_COLORS,"Printable Colors",
        "Paper is b/w",PaperResult.SYMBOL_OK)
    else
      paperResultService.create(paper.id.get,PaperResult.TYPE_LAYOUT_COLORS,"Printable Colors",
        "Paper may contain unprintable colors",PaperResult.SYMBOL_WARNING)
  }

}

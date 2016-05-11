package helper

import java.io.File
import java.security.{MessageDigest, SecureRandom}

import helper.pdfpreprocessing.PreprocessPDF
import models.Papers
import org.apache.commons.codec.binary.Base64
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.{ImageType, PDFRenderer}
import org.apache.pdfbox.tools.imageio.ImageIOUtil

/**
  * Created by manuel on 19.04.2016.
  */
object Commons {

  def generateSecret(size: Int = 32): String = {
    val b = new Array[Byte](size)
    new SecureRandom().nextBytes(b)
    Base64.encodeBase64URLSafeString(b)
  }

  def getSecretHash(secret:String): String = {
    val secretHash = MessageDigest.getInstance("MD5").digest(secret.getBytes)
    Base64.encodeBase64URLSafeString(secretHash)
  }

  def generateCoverFile(paper: Papers) = {
    val paperDir = PreprocessPDF.INPUT_DIR + "/" + Commons.getSecretHash(paper.secret) + "/" + paper.name
    val pdfDoc = PDDocument.load(new File(paperDir))
    val pdfRenderer = new PDFRenderer(pdfDoc)
    val image = pdfRenderer.renderImageWithDPI(0, 33, ImageType.RGB)
    val newDir = new File("public/papers/"+getSecretHash(paper.secret))
    if(!newDir.exists()) newDir.mkdir()
    ImageIOUtil.writeImage(image, newDir.getPath+"/cover.jpg", 33)
  }

}

package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot

import java.io.{File, FileInputStream}
import java.security.SecureRandom

import org.apache.commons.codec.binary.Base64

/**
 * Created by mattia on 28.08.15.
 */
object Utils {

	def getBase64String(image: File) = {
		val imageInFile: FileInputStream = new FileInputStream(image)
		val imageData = new Array[Byte](image.length().asInstanceOf[Int])
		imageInFile.read(imageData)
		"data:image/png;base64," + Base64.encodeBase64String(imageData)
	}

	def generateSecret(size: Int = 32): String = {
		val b = new Array[Byte](size)
		new SecureRandom().nextBytes(b)
		Base64.encodeBase64URLSafeString(b)
	}

}

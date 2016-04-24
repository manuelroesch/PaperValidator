package helper

import java.security.{MessageDigest, SecureRandom}

import org.apache.commons.codec.binary.Base64

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

}

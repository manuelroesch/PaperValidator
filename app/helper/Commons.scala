package helper

import java.security.SecureRandom

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

}

package helper.statcheck

import java.io._

import breeze.stats.distributions.FDistribution
import helper.Commons
import helper.pdfpreprocessing.PreprocessPDF
import helper.pdfpreprocessing.pdf.PDFTextExtractor
import models.Papers
import org.ddahl.jvmr.RInScala
import play.api.Logger

import scala.util.matching.Regex

/**
  * Created by manuel on 25.04.2016.
  */
object Statchecker {
  //### A vector of strings.
  var stat=List("t","F","cor","chisq","Z")
  //### "t" to extract t-values, "F" to extract F-values, "cor" to extract correlations, "chisq"to extract chi-square values, and "Z" to extract Z-values.
  var OneTailedTests=false
  //### Logical. Do we assume that all reported tests are one tailed (TRUE) or two tailed (FALSE, default)?
  var alpha=.05
  //### Assumed level of significance in the scanned texts. Defaults to .05.
  var pEqualAlphaSig=true
  //### Logical. If TRUE, statcheck counts p <= alpha as significant (default), if FALSE, statcheck counts p < alpha as significant
  var OneTailedTxt=false
  //### Logical. If TRUE, statcheck searches the text for "one-sided", "one-tailed", and "directional" to identify the possible use of one-sided tests. If one or more of these strings is found in the text AND the result would have been correct if it was a one-sided test, the result is assumed to be indeed one-sided and is counted as correct.
  var AllPValues=false
  //### Logical. If TRUE, the output will consist of a dataframe with all detected p values, also the ones that were not part of the full results in APA format

  def run(paper: Papers): String = {
    val paperLink = PreprocessPDF.INPUT_DIR + "/" + Commons.getSecretHash(paper.secret) + "/" + paper.name
    val contents = new PDFTextExtractor(paperLink).pages//map(_.toLowerCase)
    val text = contents.mkString(" ")
    val pw = new PrintWriter(new File(paperLink+".txt"))
    pw.write(text)
    pw.close
    "<b>Statcheck Results:</b><br>\n"+extractFValues(text).mkString("<br>\n") + "<br><br>"
  }

  val REGEX_EXTRACT_P_RAW = new Regex("([^a-z]ns)|(p\\s?[<>=]\\s?\\d?\\.\\d+e?-?\\d*)")
  val REGEX_EXTRACT_P_NUMBER_COUNT = new Regex("([^a-z]ns)|(p\\s?[<>=]\\s?\\d?\\.\\d+e?-?\\d*)")
  val REGEX_EXTRACT_P = new Regex("(\\d?\\.\\d+e?-?\\d*)")

  def extractPValues(text: String): List[Double] = {
    val rawPValsString = REGEX_EXTRACT_P_RAW.findAllIn(text).toList
    val pValsNumberCount = rawPValsString.map(rawPVal => REGEX_EXTRACT_P_NUMBER_COUNT.findFirstIn(rawPVal).get.toInt)
    val pVals = rawPValsString.map(rawPValString => REGEX_EXTRACT_P_NUMBER_COUNT.findFirstIn(rawPValString).get.toDouble).filter(_ < 1)
    pVals
  }

  def scalaRtest: Unit = {
    val fd = 1 - new FDistribution(6.0,34.0).cdf(3.48)
    // pull the fitted coefficents back into scala
    val R = RInScala() // initialise an R interpreter
    R.eval("mod <- pf(1,2,3,lower.tail=FALSE)") // fit the model in R
    val beta = R.toVector[Double]("mod")
    beta.toList
  }

  val REGEX_ONE_SIDED = new Regex("one.?sided|one.?tailed|directional")

  def extractIsOneSided(text: String): Boolean = {
    val isOneSided = REGEX_ONE_SIDED.findFirstIn(text)
    if(isOneSided.isDefined) true else false
  }

  val REGEX_EXTRACT_T_RAW = new Regex("t\\s?\\(\\s?\\d*\\.?\\d+\\s?\\)\\s?[<>=]\\s?[^a-z\\d]{0,3}\\s?\\d*,?\\d*\\.?\\d+\\s?,\\s?(([^a-z]ns)|(p\\s?[<>=]\\s?\\d?\\.\\d+e?-?\\d*))")
  val REGEX_T_REMOVE_COMMA = new Regex("(?<=\\d),(?=\\d+)")
  val REGEX_T_REMOVE_SPACES = new Regex("(?<=\\=)\\s+(?=.*\\,)")
  val REGEX_T_MINUS_FIX = new Regex("(?<=\\=)\\s?[^\\d\\.]+(?=.*\\,)")
  val REGEX_T_ADD_SPACES_AGAIN = new Regex("(?<=\\=)(?=(\\.|\\d))")
  val REGEX_T_EXTRACT_T = new Regex("(\\-?\\s?\\d*\\.?\\d+\\s?e?-?\\d*)|ns")

  def extractTValues(text: String): List[Double] = {
    var rawTVals = REGEX_EXTRACT_T_RAW.findAllIn(text).toList
    rawTVals = rawTVals.map(rawTVal => REGEX_T_REMOVE_COMMA.replaceAllIn(rawTVal,""))
    rawTVals = rawTVals.map(rawTVal => REGEX_T_REMOVE_SPACES.replaceAllIn(rawTVal,""))
    rawTVals = rawTVals.map(rawTVal => REGEX_T_MINUS_FIX.replaceAllIn(rawTVal," -"))
    rawTVals = rawTVals.map(rawTVal => REGEX_T_ADD_SPACES_AGAIN.replaceAllIn(rawTVal," "))
    val tVals = rawTVals.map(rawTVal => REGEX_T_EXTRACT_T.findFirstIn(rawTVal).get.toDouble)
    tVals
  }

  val REGEX_EXTRACT_F_RAW = new Regex("F\\s?\\(\\s?(\\d*\\.?(I|l|\\d+))\\s?,\\s?(\\d*\\.?\\d+)\\s?\\)\\s?[<>=]\\s?(\\d*,?\\d*\\.?\\d+)\\s?,\\s?(([^a-z]ns)|(p\\s?([<>=])\\s?(\\d?\\.\\d+e?-?\\d*)))")

  def extractFValues(text: String): List[String] = {
    val rawFVals = REGEX_EXTRACT_F_RAW.findAllIn(text).matchData.map({m =>
      new ExtractedFValues(m.group(1).toDouble,m.group(3).toDouble,m.group(4).toDouble,m.group(8),m.group(9).toDouble)
    }).toList
    rawFVals.map({r =>
      val res = 1 - new FDistribution(r.d1,r.d2).cdf(r.res)
      r.pComp match {
        case ">" => "F("+r.d1+","+r.d2+")="+res+r.pComp+r.p+"(p-Value):"+(res>r.p)
        case "<" => "F("+r.d1+","+r.d2+")="+res+r.pComp+r.p+"(p-Value):"+(res<r.p)
        case "=" => "F("+r.d1+","+r.d2+")="+res+r.pComp+r.p+"(p-Value):"+(res==r.p)
      }
    })
  }


}

class ExtractedFValues(val d1: Double, val d2: Double, val res: Double, val pComp: String, val p: Double)
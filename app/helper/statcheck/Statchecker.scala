package helper.statcheck

import java.io._
import java.text.DecimalFormat
import java.util.regex.Pattern

import breeze.numerics._
import breeze.stats.distributions.FDistribution
import helper.Commons
import helper.pdfpreprocessing.PreprocessPDF
import helper.pdfpreprocessing.pdf.PDFTextExtractor
import models.{PaperResult, PaperResultService, Papers}
import org.apache.commons.math3.distribution.{ChiSquaredDistribution, TDistribution}
import org.apache.commons.math3.stat.inference.{ChiSquareTest, TTest}
import play.api.Logger

import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex

/**
  * Created by manuel on 25.04.2016.
  */
object Statchecker {
  //### Logical. Do we assume that all reported tests are one tailed (TRUE) or two tailed (FALSE, default)?
  var oneTailedTests=false
  //### Assumed level of significance in the scanned texts. Defaults to .05.
  var alpha=.05
  //### Logical. If TRUE, statcheck counts p <= alpha as significant (default), if FALSE, statcheck counts p < alpha as significant
  var pEqualAlphaSig=true
  //### Logical. If TRUE, statcheck searches the text for "one-sided", "one-tailed", and "directional" to identify the possible use of one-sided tests. If one or more of these strings is found in the text AND the result would have been correct if it was a one-sided test, the result is assumed to be indeed one-sided and is counted as correct.
  var oneTailedTxt=false
  //### Logical. If TRUE, the output will consist of a dataframe with all detected p values, also the ones that were not part of the full results in APA format
  var AllPValues=false
  var numberOfpVals = 0

  def run(paper: Papers, paperResultService: PaperResultService) = {
    val text = convertPDFtoText(paper).toLowerCase()
    recalculateStats(paper,text,paperResultService)
  }

  def run(text: String) : String = {
    extractFValues(text).mkString(";")
  }

  def recalculateStats(paper:Papers, text: String, paperResultService: PaperResultService) = {
    oneTailedTxt = extractIsOneSided(text)
    numberOfpVals = extractPValues(text)
    writeResultsToDB(paper, extractChi2Values(text), PaperResult.TYPE_STATCHECK_CHI2, paperResultService)
    writeResultsToDB(paper, extractFValues(text), PaperResult.TYPE_STATCHECK_F, paperResultService)
    writeResultsToDB(paper, extractRValues(text), PaperResult.TYPE_STATCHECK_R, paperResultService)
    writeResultsToDB(paper, extractTValues(text), PaperResult.TYPE_STATCHECK_T, paperResultService)
    writeResultsToDB(paper, extractZValues(text), PaperResult.TYPE_STATCHECK_Z, paperResultService)
  }

  def writeResultsToDB(paper : Papers, extractedStats:List[ExtractedStatValues], resultType: Int,
                       paperResultService: PaperResultService) = {
    extractedStats.foreach({es =>
      val resultDifference = es.pCalculated-es.pExtracted
      if(es.pComp == ">" && es.pCalculated <= es.pExtracted) {
        es.error = true
      } else if(es.pComp == "<" && es.pCalculated >= es.pExtracted) {
        es.error = true
      } else if(es.pComp == "=" && abs(resultDifference) > 0.05) {
        es.error = true
      }
      val formattedpCalc = "%.5f".format(es.pCalculated)
      val resultDescr = es.statName+"-Stats: p calculated ="+formattedpCalc+", p claimed " + es.pComp+es.pExtracted
      if(es.error){
        paperResultService.create(paper.id.get,resultType,resultDescr,"",PaperResult.SYMBOL_ERROR)
      } else {
        paperResultService.create(paper.id.get,resultType,resultDescr,"",PaperResult.SYMBOL_OK)
      }
    })
  }


  def convertPDFtoText(paper: Papers): String = {
    val paperLink = PreprocessPDF.INPUT_DIR + "/" + Commons.getSecretHash(paper.secret) + "/" + paper.name
    val contents = new PDFTextExtractor(paperLink).pages//map(_.toLowerCase)
    val text = contents.mkString(" ")
    val pw = new PrintWriter(new File(paperLink+".txt"))
    pw.write(text)
    pw.close()
    text
  }

  val REGEX_EXTRACT_P = new Regex("([^a-z]ns)|(p\\s?[<>=]\\s?(\\d?\\.\\d+e?-?\\d*))")
  def extractPValues(text: String): Int = {
    val pVals = REGEX_EXTRACT_P.findAllIn(text).matchData.map({m =>
      try {
        parsePValue(m.group(3))
      } catch {
        case _:Throwable => {1.1}
      }
    }).filter(_ < 1)
    pVals.length
  }

  val REGEX_ONE_SIDED = new Regex("one.?sided|one.?tailed|directional")
  def extractIsOneSided(text: String): Boolean = {
    val isOneSided = REGEX_ONE_SIDED.findFirstIn(text)
    if(isOneSided.isDefined) true else false
  }

  val REGEX_EXTRACT_T = new Regex("t\\s?\\(\\s?(\\d*\\.?\\d+)\\s?\\)\\s?([<>=])\\s?[^a-z\\d]{0,3}\\s?(\\d*,?\\d*\\.?\\d+)\\s?,\\s?(([^a-z]ns)|(p\\s?([<>=])\\s?(\\d?\\.\\d+e?-?\\d*)))")
  def extractTValues(text: String): List[ExtractedStatValues] = {
    REGEX_EXTRACT_T.findAllIn(text).matchData.map({ m =>
      val a = m.subgroups.mkString(",")
      val sv = new ExtractedStatValues("t",m.group(1).toDouble,0,m.group(2),m.group(3).toDouble,m.group(7),parsePValue(m.group(4)))
      sv.pCalculated = new TDistribution(sv.input1).cumulativeProbability(-1*Math.abs(sv.output))*2
      sv
    }).toList
  }

  val REGEX_EXTRACT_F = new Regex("f\\s?\\(\\s?(\\d*\\.?(I|l|\\d+))\\s?,\\s?(\\d*\\.?\\d+)\\s?\\)\\s?([<>=])\\s?(\\d*,?\\d*\\.?\\d+)\\s?,\\s?(([^a-z]ns)|(p\\s?([<>=])\\s?(\\d?\\.\\d+e?-?\\d*)))")
  def extractFValues(text: String): List[ExtractedStatValues] = {
    REGEX_EXTRACT_F.findAllIn(text).matchData.map({m =>
      val sv = new ExtractedStatValues("F",m.group(1).replace("I","1").replace("l","1").toDouble,m.group(3).toDouble,m.group(4), m.group(5).toDouble,m.group(9),parsePValue(m.group(10)))
      sv.pCalculated = 1 - new FDistribution(sv.input1,sv.input2).cdf(sv.output)
      sv
    }).toList
  }

  val REGEX_EXTRACT_R = new Regex("r\\s?\\(\\s?(\\d*\\.?\\d+)\\s?\\)\\s?([<>=])\\s?[^a-z\\d]{0,3}\\s?(\\d*\\.?\\d+)\\s?,\\s?(([^a-z]ns)|(p\\s?([<>=])\\s?(\\d?\\.\\d+e?-?\\d*)))")
  def extractRValues(text: String): List[ExtractedStatValues] = {
    REGEX_EXTRACT_R.findAllIn(text).matchData.map({m =>
      val sv = new ExtractedStatValues("r",m.group(1).toDouble,0,m.group(2),m.group(3).toDouble,m.group(7),parsePValue(m.group(8)))
      val r2t = sv.output / sqrt((1 - pow(sv.output, 2)) / sv.input1)
      sv.pCalculated =  Math.min(new TDistribution(sv.input1).cumulativeProbability(-1*abs(r2t))*2,1)
      sv
    }).toList
  }

  val REGEX_EXTRACT_Z = new Regex("[^a-z]z\\s?([<>=])\\s?[^a-z\\d]{0,3}\\s?(\\d*,?\\d*\\.?\\d+)\\s?,\\s?(([^a-z]ns)|(p\\s?([<>=])\\s?(\\d?\\.\\d+e?-?\\d*)))")
  def extractZValues(text: String): List[ExtractedStatValues] = {
    REGEX_EXTRACT_Z.findAllIn(text).matchData.map({m =>
      val sv = new ExtractedStatValues("z",0,0,m.group(1),m.group(2).toDouble,m.group(6),parsePValue(m.group(7)))
      sv.pCalculated = erfc(abs(sv.output)/sqrt(2))
      sv
    }).toList
  }

  val REGEX_EXTRACT_CHI2 = new Regex("((χ.|\\[chi\\]|\\[delta\\]g)\\s?|(\\s[^trf ]\\s?)|([^trf]2\\s?))2?\\(\\s?(\\d*\\.?\\d+)\\s?(,\\s?n\\s?\\=\\s?(\\d*\\,?\\d*\\,?\\d+)\\s?)?\\)\\s?([<>=])\\s?\\s?(\\d*,?\\d*\\.?\\d+)\\s?,\\s?(([^a-z]ns)|(p\\s?([<>=])\\s?(\\d?\\.\\d+e?-?\\d*)))")
  def extractChi2Values(text: String): List[ExtractedStatValues] = {
    val regexExpr = Pattern.compile(REGEX_EXTRACT_CHI2.regex,Pattern.UNICODE_CASE).matcher(text)
    val extractedStatValuesList = new ListBuffer[ExtractedStatValues]
    while(regexExpr.find()) {
      var input2 = 0.0
      if(regexExpr.group(7)!=null){
        input2 = regexExpr.group(7).toDouble
      }
      val sv = new ExtractedStatValues("chi2",regexExpr.group(5).toDouble,input2,regexExpr.group(8),regexExpr.group(9).toDouble,regexExpr.group(13),parsePValue(regexExpr.group(14)))
      sv.pCalculated = 1 - new ChiSquaredDistribution(sv.input1).cumulativeProbability(sv.output)
      extractedStatValuesList.append(sv)
    }
    extractedStatValuesList.toList
    /*REGEX_EXTRACT_CHI2.findAllIn(text).matchData.map({m =>
      var input2 = 0.0
      if(m.group(7)!=null){
        input2 = m.group(7).toDouble
      }
      val sv = new ExtractedStatValues("chi2",m.group(5).toDouble,input2,m.group(8),m.group(9).toDouble,m.group(13),parsePValue(m.group(14)))
      sv.pCalculated = 1 - new ChiSquaredDistribution(sv.input1).cumulativeProbability(sv.output)
      sv
    }).toList*/
  }

  val REGEX_E_DIGIT_TO_DOUBLE = new Regex("([^a-z]ns)|(p\\s?([<>=])\\s?((\\d?\\.\\d+)e?(-?\\d*)))")
  def parsePValue(text: String): Double = {
    REGEX_E_DIGIT_TO_DOUBLE.findFirstMatchIn(text).map({m =>
      if(m.group(1)!=null) {
        alpha
      } else if(m.group(6)!="") {
        m.group(5).toDouble * pow(10,m.group(6).toDouble)
      } else {
        m.group(5).toDouble
      }
    }).getOrElse(alpha)
  }

}

class ExtractedStatValues(val statName: String, val input1: Double, val input2: Double, val ioComp: String,
                          val output: Double, val pComp: String, val pExtracted: Double, var pCalculated: Double = 0,
                          var error : Boolean = false) {
  override def toString(): String = {
    statName + " " + input1 + " " + input2 + " " + ioComp + " " + output + " " + pComp + " " + pExtracted +
      " " + pCalculated + " " + error
  }
}


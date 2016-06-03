package helper.statcheck

import java.io._
import java.text.DecimalFormat
import java.util.regex.Pattern

import breeze.linalg.{min, max}
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
    oneTailedTxt = extractIsOneSided(text)
    basicStats(paper,text,paperResultService)
    recalculateStats(paper,text,paperResultService)
  }

  def run(text: String) : String = {
    extractFValues(text).mkString(";")
  }

  def convertPDFtoText(paper: Papers): String = {
    val paperLink = PreprocessPDF.INPUT_DIR + "/" + Commons.getSecretHash(paper.secret) + "/" + paper.name
    val contents = new PDFTextExtractor(paperLink).pages//map(_.toLowerCase)
    val text = contents.mkString(" ").replaceAll("\u0000"," ")
    val pw = new PrintWriter(new File(paperLink+".txt"))
    pw.write(text)
    pw.close()
    text
  }

  def basicStats(paper:Papers, text: String, paperResultService: PaperResultService) {
    val sampleSize = extractSampleSizeStated(text)
    val sampleSizeDescr = "Sample size stated in text"
    if(sampleSize) {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_SAMPLE_SIZE, sampleSizeDescr,"<b>Detected!</b>",PaperResult.SYMBOL_OK)
    } else {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_SAMPLE_SIZE, sampleSizeDescr,"Could <b>not</b> be <b>detected!</b>",PaperResult.SYMBOL_WARNING)
    }

    val statTermError = extractStatTermError(text)
    val statTermErrorDescr = "Incorrect use of statistical terminology"
    if(statTermError.isEmpty) {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_ERROR_TERMS, statTermErrorDescr,"<b>Nothing Detected!</b>",PaperResult.SYMBOL_OK)
    } else {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_ERROR_TERMS, statTermErrorDescr,"Errorous terms: " + statTermError.mkString(", "),PaperResult.SYMBOL_OK)
    }

    val pVals = extractPValues(text)
    val pInTextDescr = "Text contains p-values"
    if(pVals.isEmpty){
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_P_VALUES, pInTextDescr,"<b>No p-values</b> found in text",PaperResult.SYMBOL_OK)
    } else {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_P_VALUES, pInTextDescr,"Text contains <b>"+pVals.size+" p-values</b>",PaperResult.SYMBOL_WARNING)
      val wrongPDescr = "- Wrong p-values (out of range [0,1])"
      if(pVals.exists(_ < 0) || pVals.exists(_ > 1)) {
        paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_RANGE_P_VALUES, wrongPDescr,"Some of the <b>p-values are out of range</b>",PaperResult.SYMBOL_WARNING)
      } else {
        paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_RANGE_P_VALUES, wrongPDescr,"All <b>p-values are in range [0,1]</b>)",PaperResult.SYMBOL_OK)
      }
      val imprecisePDescr = "- Imprecise or unnecessary precise p-values"
      val wrongDoubles = evaluateTooHighDoublePrecision(pVals)
      if(extractPValuesNs(text) || wrongDoubles.nonEmpty) {
        paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_PRECISION_P_VALUES, imprecisePDescr,"<b>Detected!</b> "+wrongDoubles.mkString(","),PaperResult.SYMBOL_WARNING)
      } else {
        paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_PRECISION_P_VALUES, imprecisePDescr,"All <b>p-values are ok</b>",PaperResult.SYMBOL_OK)

      }
    }

    if(extractHasTTest(text)) {
      val sidedDistDescr = "Information about distribution direction"
      if(extractSidedDistribution(text)) {
        paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_SIDED_DISTRIBUTION, sidedDistDescr,"<b>Detected!</b>",PaperResult.SYMBOL_OK)
      } else {
        paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_SIDED_DISTRIBUTION, sidedDistDescr,"<b>Direction is not stated</b> in text",PaperResult.SYMBOL_WARNING)
      }
    }

    val meanWithoutVariance = extractMeanWithoutVariance(text)
    val meanWithoutVarianceDescr = "Mean without Variance"
    val resultPrevSize = 15
    if(meanWithoutVariance.nonEmpty) {
        val result = meanWithoutVariance.map(m => "e.g. ..."+text.substring(max(0,m.head-resultPrevSize),min(text.length,m(1)+resultPrevSize))+"...<br>")
        paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_MEAN_WITHOUT_VARIANCE, meanWithoutVarianceDescr,"<b>Detected!</b><br>"+result.head,PaperResult.SYMBOL_WARNING)
    } else {
        paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_MEAN_WITHOUT_VARIANCE, meanWithoutVarianceDescr,"<b>Nothing Detected!</b>",PaperResult.SYMBOL_OK)
    }

    val varianceIfNotNormalDescr = "Variance if not normal"
    if(extractVarianceIfNotNormal(text)) {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_VARIANCE_IFNOT_NORMAL, varianceIfNotNormalDescr,"<b>Detected!</b><br>",PaperResult.SYMBOL_WARNING)
    } else {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_VARIANCE_IFNOT_NORMAL, varianceIfNotNormalDescr,"<b>Nothing Detected!</b><br>",PaperResult.SYMBOL_OK)
    }

    val goodnessOfFitDescr = "Fit without goodness of fit"
    if(extractGoodnessOfFit(text)) {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_FIT_WITHOUT_GOF, goodnessOfFitDescr,"<b>Detected!</b><br>",PaperResult.SYMBOL_WARNING)
    } else {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_FIT_WITHOUT_GOF, goodnessOfFitDescr,"<b>Nothing Detected!</b><br>",PaperResult.SYMBOL_OK)
    }

    val powerEffectDescr = "Power/effect size stated"
    if(extractPowerEffectSize(text)) {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_POWER_EFFECT, powerEffectDescr,"Power and/or effect size is <b>not stated!</b><br>",PaperResult.SYMBOL_WARNING)
    } else {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_POWER_EFFECT, powerEffectDescr,"Power/effect size<b></b>detected!<br>",PaperResult.SYMBOL_OK)
    }

  }

  val MAX_DOUBLE_PRECISION = 3
  def evaluateTooHighDoublePrecision(doubles: List[Double]) : List[Double] = {
    var tooPreciseDouble : ListBuffer[Double] = ListBuffer()
    doubles.foreach(d => {
        val numbers = d.toString.replace("0","").replace(".","").replace(",","").replace("-","")
        if(numbers.size > MAX_DOUBLE_PRECISION) {
          tooPreciseDouble += d
        }
    })
    tooPreciseDouble.toList
  }


  val REGEX_SAMPLE_SIZE = new Regex("sample\\s?size|\\d*\\s?participants|\\d*\\s?subjects|n\\s?=\\s?\\d*")
  def extractSampleSizeStated(text: String): Boolean = {
    val sampleSize = REGEX_SAMPLE_SIZE.findFirstIn(text)
    if(sampleSize.isDefined) true else false
  }

  val REGEX_STAT_TERM_ERROR = new Regex("(arc\\s?sinus\\s?transformation|impaired\\s?t-test|variance\\s?analysis|multivariate\\s?analysis)")
  def extractStatTermError(text: String): List[String] = {
    REGEX_STAT_TERM_ERROR.findAllIn(text).matchData.map(m =>
      m.group(0)
    ).toList
  }

  val REGEX_CONTAINS_T_TEST = new Regex("t.?test")
  def extractHasTTest(text: String): Boolean = {
    val hasTTest = REGEX_CONTAINS_T_TEST.findFirstIn(text)
    if(hasTTest.isDefined) true else false
  }

  val REGEX_SIDED_DIST = new Regex("one.?sided|one.?tailed|directional|two.?sided|two.?tailed")
  def extractSidedDistribution(text: String): Boolean = {
    val isSidedDist = REGEX_SIDED_DIST.findFirstIn(text)
    if(isSidedDist.isDefined) true else false
  }

  val REGEX_MEAN = new Regex("(mean|average|µ|⌀)")
  val REGEX_VARIANCE = new Regex("(±|var|variance|standard\\s?deviation|standard\\s?error|sd|se|SE|SD)")
  val REGEX_NO_DIGIT = new Regex("\\d[.,]\\d")
  val MEAN_VARIANCE_TRES = 200
  def extractMeanWithoutVariance(text: String): List[List[Int]] = {
    REGEX_MEAN.findAllMatchIn(text).map(m => {
      val varClose = REGEX_VARIANCE.findFirstIn(text.substring(max(m.start(0)-MEAN_VARIANCE_TRES,0)
        ,min(text.length,m.end(0)+MEAN_VARIANCE_TRES))).isDefined
      val varDigit = REGEX_NO_DIGIT.findFirstIn(text.substring(max(m.start(0)-MEAN_VARIANCE_TRES,0)
        ,min(text.length,m.end(0)+MEAN_VARIANCE_TRES))).isDefined
      if(!varClose && varDigit) {
        List(m.start(0),m.end(0))
      } else {
        null
      }
    }).toList.filter(_ != null)
  }

  val REGEX_CONTAINS_NORMAL = new Regex("normality|normal\\s?distribution|normally\\s?distributed|Q-Q plot|skewness|kurtosis|Shapiro-Wilk|Kolmogorov-Smirnov|Q-Q plot|gaussian|normal\\s?error")
  def extractVarianceIfNotNormal(text:String): Boolean = {
    val containsNormal = REGEX_VARIANCE.findFirstIn(text).isDefined
    val containsVariance = REGEX_VARIANCE.findFirstIn(text).isDefined
    !containsNormal && containsVariance
  }

  val REGEX_CONTAINS_FIT = new Regex("fit")
  val REGEX_CONTAINS_GOF = new Regex("goodness\\s?of\\s?fit|GoF|GFI")
  def extractGoodnessOfFit(text:String): Boolean = {
    val containsFit = REGEX_CONTAINS_FIT.findFirstIn(text).isDefined
    val containsGoF = REGEX_CONTAINS_GOF.findFirstIn(text).isDefined
    !containsGoF && containsFit
  }

  val REGEX_CONTAINS_POWER_EFFECT_METHODS = new Regex("regression|t.?test|Wilcoxon.?rank-sum|Mann-Whitney|Wilcoxon.?signed-rank|ANOVA")
  val REGEX_CONTAINS_POWER_EFFECT = new Regex("power|effect\\s?size")
  def extractPowerEffectSize(text:String): Boolean = {
    val containsPowerEffectMethods = REGEX_CONTAINS_POWER_EFFECT_METHODS.findFirstIn(text).isDefined
    val containsPowerEffect = REGEX_CONTAINS_POWER_EFFECT.findFirstIn(text).isDefined
    !containsPowerEffect && containsPowerEffectMethods
  }

  def recalculateStats(paper:Papers, text: String, paperResultService: PaperResultService) = {
    writeRecalcStatResultsToDB(paper, extractChi2Values(text), PaperResult.TYPE_STATCHECK_CHI2, paperResultService)
    writeRecalcStatResultsToDB(paper, extractFValues(text), PaperResult.TYPE_STATCHECK_F, paperResultService)
    writeRecalcStatResultsToDB(paper, extractRValues(text), PaperResult.TYPE_STATCHECK_R, paperResultService)
    writeRecalcStatResultsToDB(paper, extractTValues(text), PaperResult.TYPE_STATCHECK_T, paperResultService)
    writeRecalcStatResultsToDB(paper, extractZValues(text), PaperResult.TYPE_STATCHECK_Z, paperResultService)
  }

  def writeRecalcStatResultsToDB(paper : Papers, extractedStats:List[ExtractedStatValues], resultType: Int,
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

  val REGEX_EXTRACT_P_NS = new Regex("([^a-z]ns)")
  def extractPValuesNs(text: String): Boolean = {
    val pVals = REGEX_EXTRACT_P_NS.findFirstIn(text)
    if(pVals.isDefined) true else false
  }

  val REGEX_EXTRACT_P = new Regex("([^a-z]ns)|(p\\s?[<>=]\\s?-?\\s?(\\d?\\.\\d+e?-?\\d*))")
  def extractPValues(text: String): List[Double] = {
    val pVals = REGEX_EXTRACT_P.findAllIn(text).matchData.map({m =>
      try {
        parsePValue(m.group(3))
      } catch {
        case _:Throwable => {1.0}
      }
    })
    pVals.toList
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


package helper.statcheck

import java.io._
import java.util.regex.Pattern

import breeze.linalg.{min, max}
import breeze.numerics._
import breeze.stats.distributions.FDistribution
import helper.Commons
import helper.pdfpreprocessing.PreprocessPDF
import helper.pdfpreprocessing.pdf.PDFTextExtractor
import models.{PaperResult, PaperResultService, Papers}
import org.apache.commons.math3.distribution.{ChiSquaredDistribution, TDistribution}

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
    val textList = convertPDFtoText(paper).map(_.toLowerCase())
    oneTailedTxt = extractIsOneSided(textList.mkString("\n"))
    basicStats(paper,textList,paperResultService)
    recalculateStats(paper,textList,paperResultService)
  }

  def run(text: String) : String = {
    extractFValues(text,0).mkString(";")
  }

  def convertPDFtoText(paper: Papers): List[String] = {
    val paperLink = PreprocessPDF.INPUT_DIR + "/" + Commons.getSecretHash(paper.secret) + "/" + paper.name
    val text = new PDFTextExtractor(paperLink).pages
    //val text = contents.mkString(" ").replaceAll("\u0000"," ")
    if(!new File(paperLink+".text").exists()){
      val pw = new PrintWriter(new File(paperLink+".txt"))
      pw.write(text.map(_.toLowerCase()).mkString("\n\n"))
      pw.close()
    }
    text
  }

  def basicStats(paper:Papers, textList: List[String], paperResultService: PaperResultService) {
    val text = textList.mkString("\n")
    val sampleSizePos = extractSampleSizeStated(textList)
    val sampleSizeDescr = "Sample size stated in text"
    if(sampleSizePos.isEmpty) {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_SAMPLE_SIZE, sampleSizeDescr,
        "Could <b>not</b> be <b>detected!</b>",PaperResult.SYMBOL_WARNING,sampleSizePos)
    } else {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_SAMPLE_SIZE, sampleSizeDescr,
        "<b>Detected!</b>",PaperResult.SYMBOL_OK,sampleSizePos)
    }

    val statTermErrorPos = extractStatTermError(textList)
    val statTermErrorDescr = "Incorrect use of statistical terminology"
    if(statTermErrorPos.isEmpty) {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_ERROR_TERMS, statTermErrorDescr,
        "<b>Nothing Detected!</b>",PaperResult.SYMBOL_OK,statTermErrorPos)
    } else {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_ERROR_TERMS, statTermErrorDescr,
        "Errorous terms found!",PaperResult.SYMBOL_OK,statTermErrorPos)
    }

    val pValsAndPos = extractPValues(textList)
    val pInTextDescr = "Text contains p-values"
    if(pValsAndPos.isEmpty){
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_P_VALUES, pInTextDescr,"<b>No p-values</b> found in text",PaperResult.SYMBOL_OK,"")
    } else {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_P_VALUES, pInTextDescr,"Text contains <b>"+pValsAndPos.size+" p-values</b>",PaperResult.SYMBOL_WARNING,pValsAndPos.keys.mkString(","))
      val wrongPDescr = "- Wrong p-values (out of range [0,1])"
      if(pValsAndPos.exists(_._2 < 0) || pValsAndPos.exists(_._2 > 1)) {
        val positions = pValsAndPos.filter(_._2 < 0) ++ pValsAndPos.filter(_._2 > 1)
        paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_RANGE_P_VALUES, wrongPDescr,"Some of the <b>p-values are out of range</b>",PaperResult.SYMBOL_WARNING,positions.keys.mkString(","))
      } else {
        paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_RANGE_P_VALUES, wrongPDescr,"All <b>p-values are in range [0,1]</b>)",PaperResult.SYMBOL_OK,"")
      }
      val imprecisePDescr = "- Imprecise or unnecessary precise p-values"
      val wrongDoubles = evaluateTooHighDoublePrecision(pValsAndPos)
      val nsValues = extractPValuesNs(textList)
      val resultMap = wrongDoubles ++ nsValues
      if(resultMap.isEmpty) {
        paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_PRECISION_P_VALUES, imprecisePDescr,"All <b>p-values are ok</b>",PaperResult.SYMBOL_OK,"")
      } else {
        paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_PRECISION_P_VALUES, imprecisePDescr,"<b>Detected!</b> "+wrongDoubles.mkString(","),PaperResult.SYMBOL_WARNING,resultMap.keys.mkString(","))

      }
    }

    if(extractTTest(text)) {
      val sidedDistDescr = "Information about distribution direction"
      val sidedDistributionPos = extractSidedDistribution(textList)
      if(sidedDistDescr.isEmpty) {
        paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_SIDED_DISTRIBUTION, sidedDistDescr,"<b>Direction is not stated</b> in text",PaperResult.SYMBOL_WARNING,sidedDistributionPos)
      } else {
        paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_SIDED_DISTRIBUTION, sidedDistDescr,"<b>Detected!</b>",PaperResult.SYMBOL_OK,sidedDistributionPos)
      }
    }

    val meanWithoutVariancePos = extractMeanWithoutVariance(textList)
    val meanWithoutVarianceDescr = "Mean without Variance"
    if(meanWithoutVariancePos.isEmpty) {
        paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_MEAN_WITHOUT_VARIANCE, meanWithoutVarianceDescr,"<b>Nothing Detected!</b>",PaperResult.SYMBOL_OK,meanWithoutVariancePos)
    } else {
        paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_MEAN_WITHOUT_VARIANCE, meanWithoutVarianceDescr,"<b>Detected!</b>",PaperResult.SYMBOL_WARNING,meanWithoutVariancePos)
    }

    val varianceIfNotNormalDescr = "Variance if not normal"
    val variancePos = extractVarianceIfNotNormal(textList)
    if(variancePos.isEmpty) {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_VARIANCE_IFNOT_NORMAL, varianceIfNotNormalDescr,"<b>Nothing Detected!</b><br>",PaperResult.SYMBOL_OK,variancePos)
    } else {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_VARIANCE_IFNOT_NORMAL, varianceIfNotNormalDescr,"<b>Detected!</b><br>",PaperResult.SYMBOL_WARNING,variancePos)
    }

    val gofPos = extractGoodnessOfFit(textList)
    val goodnessOfFitDescr = "Fit without goodness of fit"
    if(gofPos.isEmpty) {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_FIT_WITHOUT_GOF, goodnessOfFitDescr,"<b>Detected!</b><br>",PaperResult.SYMBOL_WARNING,gofPos)
    } else {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_FIT_WITHOUT_GOF, goodnessOfFitDescr,"<b>Nothing Detected!</b><br>",PaperResult.SYMBOL_OK,gofPos)
    }

    val powerEffectDescr = "Power/effect size stated"
    if(extractPowerEffectSize(text)) {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_POWER_EFFECT, powerEffectDescr,"Power and/or effect size is <b>not stated!</b><br>",PaperResult.SYMBOL_WARNING,"")
    } else {
      paperResultService.create(paper.id.get, PaperResult.TYPE_BASICS_POWER_EFFECT, powerEffectDescr,"Power/effect size<b></b>detected!<br>",PaperResult.SYMBOL_OK,"")
    }

  }

  val MAX_DOUBLE_PRECISION = 3
  def evaluateTooHighDoublePrecision(doubles: Map[String,Double]) : Map[String,Double] = {
    var tooPreciseDouble : Map[String,Double] = Map()
    doubles.foreach(d => {
        val numbers = d._2.toString.replace("0","").replace(".","").replace(",","").replace("-","")
        if(numbers.length > MAX_DOUBLE_PRECISION) {
          tooPreciseDouble += d._1->d._2
        }
    })
    tooPreciseDouble
  }


  val REGEX_SAMPLE_SIZE = new Regex("sample\\s?size|\\d+\\s?participants|\\d+\\s?subjects|n\\s?=\\s?\\d+")
  def extractSampleSizeStated(textList: List[String]): String = {
    textList.zipWithIndex.flatMap{ case (text, page) =>
      REGEX_SAMPLE_SIZE.findAllIn(text).matchData.map(m =>
        page + ":" + m.start(0) + "-" + m.end(0)
      )
    }.mkString(",")

  }

  val REGEX_STAT_TERM_ERROR = new Regex("(arc\\s?sinus\\s?transformation|impaired\\s?t.?test|variance\\s?analysis|multivariate\\s?analysis)")
  def extractStatTermError(textList: List[String]): String = {
    textList.zipWithIndex.flatMap { case (text, page) =>
      REGEX_STAT_TERM_ERROR.findAllIn(text).matchData.map(m =>
        page + ":" + m.start(0) + "-" + m.end(0)
      )
    }.mkString(",")
  }

  val REGEX_CONTAINS_T_TEST = new Regex("\\s?t.?test")
  def extractTTest(text: String): Boolean = {
    val hasTTest = REGEX_CONTAINS_T_TEST.findFirstIn(text)
    if(hasTTest.isDefined) true else false
  }

  val REGEX_SIDED_DIST = new Regex("one.?sided|one.?tailed|two.?sided|two.?tailed")
  def extractSidedDistribution(textList: List[String]): String = {
    textList.zipWithIndex.flatMap { case (text, page) =>
      REGEX_SIDED_DIST.findAllIn(text).matchData.map(m => {
        page + ":" + m.start(0) + "-" + m.end(0)
      })
    }.mkString(",")
  }

  val REGEX_MEAN = new Regex("(mean|average|µ|⌀)")
  val REGEX_VARIANCE = new Regex("(±|[^a-z]var[^a-z]|variance|standard\\s?deviation|standard\\s?error|[^a-z]sd[^a-z]|[^a-z]se[^a-z])")
  val REGEX_NO_DIGIT = new Regex("\\d[.,]\\d")
  val MEAN_VARIANCE_TRES = 200
  def extractMeanWithoutVariance(textList: List[String]): String = {
    textList.zipWithIndex.flatMap { case (text, page) =>
      REGEX_MEAN.findAllMatchIn(text).map(m => {
        val varClose = REGEX_VARIANCE.findFirstIn(text.substring(max(m.start(0) - MEAN_VARIANCE_TRES, 0)
          , min(text.length, m.end(0) + MEAN_VARIANCE_TRES))).isDefined
        val varDigit = REGEX_NO_DIGIT.findFirstIn(text.substring(max(m.start(0) - MEAN_VARIANCE_TRES, 0)
          , min(text.length, m.end(0) + MEAN_VARIANCE_TRES))).isDefined
        if (!varClose && varDigit) {
          page + ":" + m.start(0) + "-" + m.end(0)
        } else {
          null
        }
      })
    }.filter(_ != null).mkString(",")
  }

  val REGEX_CONTAINS_NORMAL = new Regex("normality|normal\\s?distribution|normally\\s?distributed|Q.?Q\\s?plot|skewness|kurtosis|shapiro.?wilk|kolmogorov.?smirnov|gaussian|normal\\s?error")
  def extractVarianceIfNotNormal(textList: List[String]): String = {
    val containsNormal = REGEX_VARIANCE.findFirstIn(textList.mkString("\n")).isDefined
    if(!containsNormal) {
      textList.zipWithIndex.flatMap { case (text, page) =>
        REGEX_VARIANCE.findAllMatchIn(text).map(m => {
          page + ":" + m.start(0) + "-" + m.end(0)
        })
      }.mkString(",")
    } else {
      ""
    }
  }

  val REGEX_CONTAINS_FIT = new Regex("fitting|fitted")
  val REGEX_CONTAINS_GOF = new Regex("goodness\\s?of\\s?fit|GoF|GFI")
  def extractGoodnessOfFit(textList: List[String]): String = {
    val containsFit = textList.zipWithIndex.flatMap { case (text, page) =>
      REGEX_CONTAINS_FIT.findAllMatchIn(text).map({ m =>
        page + ":" + m.start(0) + "-" + m.end(0)
      })
    }
    if(containsFit.nonEmpty){
      (textList.zipWithIndex.flatMap { case (text, page) =>
        REGEX_CONTAINS_GOF.findAllMatchIn(text).map(m=>{
          page+":"+m.start(0)+"-"+m.end(0)
        })
      }++ containsFit).mkString(",")
    } else {
      ""
    }
  }

  val REGEX_CONTAINS_POWER_EFFECT_METHODS = new Regex("regression|\\s?t.?test|wilcoxon.?rank.?sum|mann.?whitney|wilcoxon.?signed-rank|anova")
  val REGEX_CONTAINS_POWER_EFFECT = new Regex("power|effect\\s?size")
  def extractPowerEffectSize(text:String): Boolean = {
    val containsPowerEffectMethods = REGEX_CONTAINS_POWER_EFFECT_METHODS.findFirstIn(text).isDefined
    val containsPowerEffect = REGEX_CONTAINS_POWER_EFFECT.findFirstIn(text).isDefined
    !containsPowerEffect && containsPowerEffectMethods
  }

  def recalculateStats(paper:Papers, textList: List[String], paperResultService: PaperResultService) = {
    textList.zipWithIndex.foreach({case(text,page)=>
      writeRecalcStatResultsToDB(paper, extractChi2Values(text,page), PaperResult.TYPE_STATCHECK_CHI2, paperResultService)
      writeRecalcStatResultsToDB(paper, extractFValues(text,page), PaperResult.TYPE_STATCHECK_F, paperResultService)
      writeRecalcStatResultsToDB(paper, extractRValues(text,page), PaperResult.TYPE_STATCHECK_R, paperResultService)
      writeRecalcStatResultsToDB(paper, extractTValues(text,page), PaperResult.TYPE_STATCHECK_T, paperResultService)
      writeRecalcStatResultsToDB(paper, extractZValues(text,page), PaperResult.TYPE_STATCHECK_Z, paperResultService)
    })
  }

  def writeRecalcStatResultsToDB(paper : Papers, extractedStats:Map[String,ExtractedStatValues], resultType: Int,
                       paperResultService: PaperResultService) = {
    extractedStats.foreach({es =>
      val resultDifference = es._2.pCalculated-es._2.pExtracted
      if(es._2.pComp == ">" && es._2.pCalculated <= es._2.pExtracted) {
        es._2.error = true
      } else if(es._2.pComp == "<" && es._2.pCalculated >= es._2.pExtracted) {
        es._2.error = true
      } else if(es._2.pComp == "=" && abs(resultDifference) > 0.05) {
        es._2.error = true
      }
      val formattedpCalc = "%.5f".format(es._2.pCalculated)
      val resultDescr = es._2.statName+"-Stats: p calculated ="+formattedpCalc+", p claimed " + es._2.pComp+es._2.pExtracted
      if(es._2.error){
        paperResultService.create(paper.id.get,resultType,resultDescr,"",PaperResult.SYMBOL_ERROR,es._1)
      } else {
        paperResultService.create(paper.id.get,resultType,resultDescr,"",PaperResult.SYMBOL_OK,es._1)
      }
    })
  }

  val REGEX_EXTRACT_P_NS = new Regex("([^a-z]ns)")
  def extractPValuesNs(textList: List[String]): Map[String,Double] = {
    textList.zipWithIndex.flatMap { case (text, page) =>
      REGEX_EXTRACT_P_NS.findAllIn(text).matchData.map(m => {
        (page + ":" + m.start(0) + "-" + m.end(0)) -> 1.0
      })
    }.toMap
  }

  val REGEX_EXTRACT_P = new Regex("([^a-z]ns)|(p\\s?[<>=]\\s?-?\\s?(\\d?\\.\\d+e?-?\\d*))")
  def extractPValues(textList: List[String]): Map[String,Double] = {
    textList.zipWithIndex.flatMap { case (text, page) =>
      REGEX_EXTRACT_P.findAllIn(text).matchData.map({ m =>
        try {
          page + ":" + m.start(0) + "-" + m.end(0) -> parsePValue(m.group(3))
        } catch {
          case _: Throwable => "" -> 1.0
        }
      })
    }.toMap
  }

  val REGEX_ONE_SIDED = new Regex("one.?sided|one.?tailed")
  def extractIsOneSided(text: String): Boolean = {
    val isOneSided = REGEX_ONE_SIDED.findFirstIn(text)
    if(isOneSided.isDefined) true else false
  }

  val REGEX_EXTRACT_T = new Regex("t\\s?\\(\\s?(\\d*\\.?\\d+)\\s?\\)\\s?([<>=])\\s?[^a-z\\d]{0,3}\\s?(\\d*,?\\d*\\.?\\d+)\\s?,\\s?(([^a-z]ns)|(p\\s?([<>=])\\s?(\\d?\\.\\d+e?-?\\d*)))")
  def extractTValues(text: String, page: Int): Map[String,ExtractedStatValues] = {
    REGEX_EXTRACT_T.findAllIn(text).matchData.map({ m =>
      val sv = new ExtractedStatValues("t",m.group(1).toDouble,0,m.group(2),m.group(3).toDouble,m.group(7),parsePValue(m.group(4)))
      sv.pCalculated = new TDistribution(sv.input1).cumulativeProbability(-1*Math.abs(sv.output))*2
      page+":"+m.start(0)+"-"+m.end(0) -> sv
    }).toMap
  }

  val REGEX_EXTRACT_F = new Regex("f\\s?\\(\\s?(\\d*\\.?(I|l|\\d+))\\s?,\\s?(\\d*\\.?\\d+)\\s?\\)\\s?([<>=])\\s?(\\d*,?\\d*\\.?\\d+)\\s?,\\s?(([^a-z]ns)|(p\\s?([<>=])\\s?(\\d?\\.\\d+e?-?\\d*)))")
  def extractFValues(text: String,page:Int): Map[String,ExtractedStatValues] = {
    REGEX_EXTRACT_F.findAllIn(text).matchData.map({m =>
      val sv = new ExtractedStatValues("F",m.group(1).replace("I","1").replace("l","1").toDouble,m.group(3).toDouble,m.group(4), m.group(5).toDouble,m.group(9),parsePValue(m.group(10)))
      sv.pCalculated = 1 - new FDistribution(sv.input1,sv.input2).cdf(sv.output)
      page+":"+m.start(0)+"-"+m.end(0) -> sv
    }).toMap
  }

  val REGEX_EXTRACT_R = new Regex("r\\s?\\(\\s?(\\d*\\.?\\d+)\\s?\\)\\s?([<>=])\\s?[^a-z\\d]{0,3}\\s?(\\d*\\.?\\d+)\\s?,\\s?(([^a-z]ns)|(p\\s?([<>=])\\s?(\\d?\\.\\d+e?-?\\d*)))")
  def extractRValues(text: String,page:Int): Map[String,ExtractedStatValues] = {
    REGEX_EXTRACT_R.findAllIn(text).matchData.map({m =>
      val sv = new ExtractedStatValues("r",m.group(1).toDouble,0,m.group(2),m.group(3).toDouble,m.group(7),parsePValue(m.group(8)))
      val r2t = sv.output / sqrt((1 - pow(sv.output, 2)) / sv.input1)
      sv.pCalculated =  Math.min(new TDistribution(sv.input1).cumulativeProbability(-1*abs(r2t))*2,1)
      page+":"+m.start(0)+"-"+m.end(0) -> sv
    }).toMap
  }

  val REGEX_EXTRACT_Z = new Regex("[^a-z]z\\s?([<>=])\\s?[^a-z\\d]{0,3}\\s?(\\d*,?\\d*\\.?\\d+)\\s?,\\s?(([^a-z]ns)|(p\\s?([<>=])\\s?(\\d?\\.\\d+e?-?\\d*)))")
  def extractZValues(text: String,page:Int): Map[String,ExtractedStatValues] = {
    REGEX_EXTRACT_Z.findAllIn(text).matchData.map({m =>
      val sv = new ExtractedStatValues("z",0,0,m.group(1),m.group(2).toDouble,m.group(6),parsePValue(m.group(7)))
      sv.pCalculated = erfc(abs(sv.output)/sqrt(2))
      page+":"+m.start(0)+"-"+m.end(0) -> sv
    }).toMap
  }

  val REGEX_EXTRACT_CHI2 = new Regex("((χ.|\\[chi\\]|\\[delta\\]g)\\s?|(\\s[^trf ]\\s?)|([^trf]2\\s?))2?\\(\\s?(\\d*\\.?\\d+)\\s?(,\\s?n\\s?\\=\\s?(\\d*\\,?\\d*\\,?\\d+)\\s?)?\\)\\s?([<>=])\\s?\\s?(\\d*,?\\d*\\.?\\d+)\\s?,\\s?(([^a-z]ns)|(p\\s?([<>=])\\s?(\\d?\\.\\d+e?-?\\d*)))")
  def extractChi2Values(text: String,page:Int): Map[String,ExtractedStatValues] = {
    val regexExpr = Pattern.compile(REGEX_EXTRACT_CHI2.regex,Pattern.UNICODE_CASE).matcher(text)
    var extractedStatValuesMap: Map[String,ExtractedStatValues]= Map()
    while(regexExpr.find()) {
      var input2 = 0.0
      if(regexExpr.group(7)!=null){
        input2 = regexExpr.group(7).toDouble
      }
      val sv = new ExtractedStatValues("chi2",regexExpr.group(5).toDouble,input2,regexExpr.group(8),regexExpr.group(9).toDouble,regexExpr.group(13),parsePValue(regexExpr.group(14)))
      sv.pCalculated = 1 - new ChiSquaredDistribution(sv.input1).cumulativeProbability(sv.output)
      extractedStatValuesMap += (page+":"+regexExpr.start(0)+"-"+regexExpr.end(0) -> sv)
    }
    extractedStatValuesMap
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


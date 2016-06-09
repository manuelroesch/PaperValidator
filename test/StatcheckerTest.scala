import java.io._

import helper.pdfpreprocessing.pdf.PDFTextExtractor
import helper.statcheck.Statchecker
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.scalatestplus.play._

import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex


class StatcheckerTest extends PlaySpec {
  "extractPValues Test" should {
    "extract one p Value" in {
      val pValue = Statchecker.extractPValues("This ist an example sentence with a p value that is p=0.05 approximately.")
      pValue mustBe 1
    }
  }

  "extractIsOneSided Test" should {
    "be true" in {
      val pValue = Statchecker.extractIsOneSided("This ist an example sentence with one-sided distirubution.")
      pValue mustBe true
    }
  }

  "extractTValues Test" should {
    "extract one t Value" in {
      val tValue = Statchecker.extractTValues("This is t(1)=2,p=0.05.")
      tValue.head.input1 mustBe 1
      tValue.head.ioComp mustBe "="
      tValue.head.output mustBe 2
      tValue.head.pComp mustBe "="
      tValue.head.pExtracted mustBe 0.05
      Math.round(tValue.head.pCalculated*100000) mustBe 29517
    }
  }

  "extractTValues Test" should {
    "extract one e^-1 t Value" in {
      val tValue = Statchecker.extractTValues("This is t(1)=2.0,p=5.0e-2.")
      tValue.head.input1 mustBe 1
      tValue.head.ioComp mustBe "="
      tValue.head.output mustBe 2
      tValue.head.pComp mustBe "="
      tValue.head.pExtracted mustBe 0.05
      Math.round(tValue.head.pCalculated*100000) mustBe 29517
    }
  }

  "extractTValues Test" should {
    "extract one ns t Value" in {
      val tValue = Statchecker.extractTValues("This is t(1)=2.0, ns")
      tValue.head.input1 mustBe 1
      tValue.head.ioComp mustBe "="
      tValue.head.output mustBe 2
      tValue.head.pComp mustBe null
      tValue.head.pExtracted mustBe 0.05
      Math.round(tValue.head.pCalculated*100000) mustBe 29517
    }
  }

  "extractFValues Test" should {
    "extract one F Value" in {
      val fValue = Statchecker.extractFValues("This is F(1,2)=3,p=0.05.")
      fValue.head.input1 mustBe 1
      fValue.head.input2 mustBe 2
      fValue.head.ioComp mustBe "="
      fValue.head.output mustBe 3
      fValue.head.pComp mustBe "="
      fValue.head.pExtracted mustBe 0.05
      Math.round(fValue.head.pCalculated*100000) mustBe 22540
    }
  }

  "extractRValues Test" should {
    "extract one r Value" in {
      val rValue = Statchecker.extractRValues("This is r(3)=0.222,p=0.05.")
      rValue.head.input1 mustBe 3
      rValue.head.ioComp mustBe "="
      rValue.head.output mustBe 0.222
      rValue.head.pComp mustBe "="
      rValue.head.pExtracted mustBe 0.05
      Math.round(rValue.head.pCalculated*100000) mustBe 71968
    }
  }

  "extractZValues Test" should {
    "extract one z Value" in {
      val zValue = Statchecker.extractZValues("This is z=2,p=0.05.")
      zValue.head.ioComp mustBe "="
      zValue.head.output mustBe 2
      zValue.head.pComp mustBe "="
      zValue.head.pExtracted mustBe 0.05
      Math.round(zValue.head.pCalculated*100000) mustBe 4550
    }
  }

  "extractChi2Values Test" should {
    "extract one chi 2 Value" in {
      val chi2Value = Statchecker.extractChi2Values("This is X2(2,N = 170)=14.14,p<0.05")
      chi2Value.head.input1 mustBe 2
      chi2Value.head.input2 mustBe 170
      chi2Value.head.ioComp mustBe "="
      chi2Value.head.output mustBe 14.14
      chi2Value.head.pComp mustBe "<"
      chi2Value.head.pExtracted mustBe 0.05
      Math.round(chi2Value.head.pCalculated*10000000) mustBe 8502
    }
  }

  "testAllPapers" should {
    "complete" in {
      val testFiles = new File("C:\\Users\\manuel\\Desktop\\test")
      val allFiles = testFiles.listFiles().toList
      allFiles.par.foreach({ file =>
        var text = ""
        val pdfOrTxt = "pdf"
        if (file.getPath.endsWith(".pdf") && pdfOrTxt == "pdf") {
          //val text = new PDFTextExtractor(file.getPath).pages.mkString("")//map(_.toLowerCase)

          val doc = PDDocument.load(file)
          try {
            val pdfStripper = new PDFTextStripper()
            text = pdfStripper.getText(doc).replaceAll("\u0000", " ")
          } catch {
            case e: IOException => {
              e.printStackTrace()
            }
          } finally {
            doc.close()
          }

          if (text != "") {
            val pw = new PrintWriter(new File(file.getPath + ".txt"))
            pw.write(text)
            pw.close()
          }
        } else if(file.getPath.endsWith(".txt") && pdfOrTxt=="txt") {
          text = scala.io.Source.fromFile(file.getPath).mkString
        }
        val output = Statchecker.run(text.toLowerCase())
        if (output != "") {
          print(file.getPath)
          println("\t" + output)
        }
      })
      1 mustBe 1
    }
  }

  val REGEX_TERMS = new Regex("sd|se|var")
  //val REGEX_TERMS = new Regex("(MANOVA|multivariate\\s?Analysis\\s?of\\s?variance|ANOVA|Analysis\\s?of\\s?variance|ANCOVA|Analysis\\s?of\\s?covariance|Linear\\s?regression|Multiple\\s?regression|Logistic\\s?regression|Correlation:|Pearson|Spearman|U\\s?test|U-test|U\\s?-\\s?test|Mann-Whitney|Mann\\s?-\\s?Whitney|Mann\\s?Whitney\\s?=\\s?Wilcoxon\\s?Rank\\s?sum\\s?test|Wilcoxon\\s?Signed-Ranks\\s?test|Wilcoxon\\s?test|Wilcoxon-test|Wilcoxon\\s?Signed\\s?Rank|Wilcoxon\\s?Signed-Rank|Kruskal-Wallis|Kruskal\\s?Wallis|Kruskal\\s?-\\s?Wallis|McNemar|Mc\\s?Nemar|Friedman's\\s?test|Friedman\\s?test|chi\\s?square|chi-square|Fisher.s\\s?Exact|t-test|t\\s?test\\s?for\\s?independent\\s?samples\\s?(as\\s?opposed\\s?to\\s?paired\\s?t\\s?test?)|paired\\s?t\\s?test|Dum's\\s?test|Dum\\s?test\\s?never\\s?heard\\s?of\\s?that\\s?test\\s?before\\s?so\\s?don't\\s?know|Receiver\\s?Operating\\s?Characteristic\\s?Curve|ROC|C-statistic|Hosmer–Lemeshow|Hosmer\\s?–\\s?Lemeshow|Odds\\s?ratio|Relative\\s?risk|Likelihood\\s?ratio|Kappa|\\s?Kendall\\s?Tau|Bland–Altman|Youden\\s?J|Bland\\s?Altman|Youden-J|Kendall-Tau|Survival\\s?analysis|Life\\s?tables|Log\\s?rank\\s?test|Sensitivity|Specificity|Positive\\s?predictive\\s?value|Negative\\s?predictive\\s?value|Post\\s?hoc\\s?analysis|Tukey|Newman–Kuels|Duncan|Cluster\\s?analysis|Factor\\s?analysis|Classification\\s?and\\s?regression\\s?tree\\s?analysis|tree\\s?analysis|Mantel–Haenszel|Mantel\\s?–\\s?Haenszel|Mantel\\s?Haenszel|Miettine|Decision\\s?tree\\s?analysis|Meta-analysis|Meta\\s?analysis|Content\\s?analysis|Content-analysis|Confidence\\s?interval|CI|Confidence-interval|Random\\s?effect|Random-effect|Cronbach's\\s?alpha|Cronbachs\\s?alpha|Cronbach\\s?alpha|linearity|normal|homogenity\\s?of|homoscedasticity|homogeneity\\s?of\\s?variance|same\\s?variance|equal\\s?variance|sphericity|constant\\s?variance|heteroscedasticity|Breusch–Pagan|Koenker–Basset|Könker–Basset|Goldfeld–Quandt|Levene|normality|normal\\s?distribution|normally\\s?distributed|Q-Q\\s?plot|skewness|kurtosis|Shapiro-Wilk|Kolmogorov-Smirnov|Q-Q\\s?plot|gaussian|normal\\s?error|multicollinearity|auto-correlation|auto\\s?correlation|variance-covariance|var-covar|same\\s?variance|equal\\s?variance|f-test|f\\s?test|Bartlett|Brown–Forsythe|Welch|independen|randomly|homoscedasticity|homogeneity\\s?of\\s?variance|same\\s?variance|equal\\s?variance|sphericity|Constant\\s?variance|homogeneity\\s?of\\s?regression\\s?slopes|normal|gaussian|bell|normal\\s?error|multivariate\\s?normality|interval|ratio|multicollinearity|linearity|linearity\\s?of\\s?dependent\\s?variable|categorical|dichotomous|homogenity\\s?of\\s?variance-covariance|homogenity\\s?of\\s?variance-covariance|homogenity\\s?of\\s?var-covar)")

  "extractAllTerms" should {
    "complete" in {
      var i = 0
      val testFiles = new File("C:\\Users\\manuel\\Desktop\\MasterThesis\\upload-all")
      val allFiles = testFiles.listFiles().toList
      var mapStatTermCount : Map[String,Int] = Map()
      var mapStatTerms : Map[String,String] = Map()
      allFiles.par.foreach(file => {
        i += 1
        println(i)
        var text = ""
        if (file.getPath.endsWith(".pdf")) {
          text = new PDFTextExtractor(file.getPath).pages.mkString("")//map(_.toLowerCase)
          val doc = PDDocument.load(file)
          try {
            if(i < 1000) {
              val pdfStripper = new PDFTextStripper()
              text = pdfStripper.getText(doc).replaceAll("\u0000", " ").replaceAll("\\s+"," ").toLowerCase()
            }
          } catch {
            case e: IOException => {
              e.printStackTrace()
            }
          } finally {
            doc.close()
          }

          var j = 0
          var nSize = 0
          REGEX_TERMS.findAllMatchIn(text).foreach(f => {
            val textSnippet = text.substring(Math.max(0,f.start(0)-50),Math.min(text.length,f.end(0)+50)).replaceAll("\\s", " ")
            if(mapStatTermCount.isDefinedAt(f.group(0))) {
              val newCount = mapStatTermCount(f.group(0))+1
              mapStatTermCount += (f.group(0) -> newCount)
            } else {
              mapStatTermCount += (f.group(0) -> 1)
            }
            if(mapStatTerms.isDefinedAt(f.group(0))) {
              val oldTerm = mapStatTerms(f.group(0))
              mapStatTerms += (f.group(0) -> (oldTerm + "\n" + textSnippet))
            } else {
              mapStatTerms += (f.group(0) -> textSnippet)
            }
          })
        }
      })
      val pw = new PrintWriter(new File("C:\\Users\\manuel\\Desktop\\out.txt"))
      mapStatTermCount.foreach(m => {
        pw.write(m._1 + " \t " + m._2 + "\n")
      })
      pw.write("\n\n\n\n\n\n")
      mapStatTerms.foreach(m => {
        pw.write(m._1 + " \n----------\n " + m._2 + "\n\n\n----------------------------------------------\n\n\n")
      })
      pw.close()
      1 mustBe 1
    }
  }

  val REGEX_TERMS_SAMPLE_SIZE= new Regex("n ?= ?\\d+[\\.,']?\\d+|\\d+[\\.,]?\\d+ subjects|\\d+[\\.,]?\\d+ participants|sample size of \\d+[\\.,]?\\d+")
  val REGEX_EXTRACT_DIGITS = new Regex("\\d+")

  "extractSampleSize" should {
    "complete" in {
      var i = 0
      val testFiles = new File("C:\\Users\\manuel\\Desktop\\MasterThesis\\upload-all")
      val allFiles = testFiles.listFiles().toList
      var mapStatTermCount : Map[String,List[Int]] = Map("" -> List(1,2,3,4,5))
      var mapStatTerms : Map[String,String] = Map()
      allFiles.par.foreach(file => {
        i += 1
        println(i)
        var text = ""
        if (file.getPath.endsWith(".pdf")) {
          val doc = PDDocument.load(file)
          try {
            if(i < 1000) {
              val pdfStripper = new PDFTextStripper()
              text = pdfStripper.getText(doc).replaceAll("\u0000", " ").replaceAll("\\s+"," ").toLowerCase()
            }
          } catch {
            case e: IOException => {
              println("errorPDFread!!!!!!!")
              e.printStackTrace()
            }
          } finally {
            doc.close()
          }

          var j = 0
          var nSize = 0
          var nList:ListBuffer[Int] = ListBuffer()
          REGEX_TERMS_SAMPLE_SIZE.findAllMatchIn(text).foreach(f => {
            var extractedN = 0
            try {
              extractedN = REGEX_EXTRACT_DIGITS.findFirstIn(f.group(0)).get.replace(",","").replace("'","").toInt
              nSize += extractedN
              nList += extractedN
              j+=1
              val textSnippet = text.substring(Math.max(0,f.start(0)-50),
                Math.min(text.length,f.end(0)+50)).replaceAll("\\s", " ")
              mapStatTerms += ((file.getName+"\t"+j) -> textSnippet)
            } catch {
              case e: Exception => {
                println(REGEX_EXTRACT_DIGITS.findFirstIn(f.group(0)).get)
              }
            }
          })
          nList = nList.sorted
          var calc = -1
          if(j > 0) {
            calc = nSize/j
          }
          nList += calc
          mapStatTermCount += (file.getName -> nList.toList)
        }
      })
      val pw = new PrintWriter(new File("C:\\Users\\manuel\\Desktop\\out.txt"))
      mapStatTermCount.foreach(m => {
        pw.write(m._1 + "\t\t" + m._2 + "\n")
      })
      pw.write("\n\n\n\n\n")
      mapStatTerms.foreach(m => {
        pw.write(m._1 + "\t\t" + m._2 + "\n")
      })
      pw.close()
      1 mustBe 1
    }
  }

}
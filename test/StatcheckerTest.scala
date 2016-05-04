import java.io._

import helper.statcheck.Statchecker
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.scalatestplus.play._


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
      val testFiles = new File("C:\\Users\\manuel\\Desktop\\upload-all")
      val allFiles = testFiles.listFiles().toList
      allFiles.foreach({ file =>
        var text = ""
        val pdfOrTxt = "txt"
        if (file.getPath.endsWith(".pdf") && pdfOrTxt == "pdf") {
          //val text = new PDFTextExtractor(file.getPath).pages.mkString("")//map(_.toLowerCase)

          val doc = PDDocument.load(file)
          try {
            val pdfStripper = new PDFTextStripper()
            text = pdfStripper.getText(doc).replaceAll("\0", " ")
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

}
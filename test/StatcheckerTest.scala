import helper.statcheck.Statchecker
import org.scalatestplus.play._


class StatcheckerTest extends PlaySpec {
  "extractPValues()" must {
    "Simple Extraction Test" in {
      val pValue = Statchecker.extractPValues("This ist an example sentence with a p value that is p=0.05")(0)
      pValue mustBe 0.05
    }
  }
}
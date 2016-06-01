package helper

import helper.statcheck.Statchecker
import models.Papers

import scala.collection.JavaConversions._
import org.languagetool.JLanguageTool
import org.languagetool.language.BritishEnglish

/**
  * Created by manuel on 29.05.2016.
  */
object SpellChecker {

  def check(paper:Papers): String = {

    //TODO: Create central text conversion proccess
    val text = Statchecker.convertPDFtoText(paper).replaceAll("\\s+"," ")

    val langTool = new JLanguageTool(new BritishEnglish())
    val result = langTool.check(text).map(m => {
        if(text.substring(m.getFromPos,m.getToPos) == text.substring(m.getFromPos,m.getToPos).toLowerCase() &&
          !m.getSuggestedReplacements.isEmpty) {
            val errorInTextSnippetSize = 30
            val snippetFrom = if (m.getFromPos-errorInTextSnippetSize < 0) 0 else m.getFromPos-errorInTextSnippetSize
            val snippetTo = if (m.getToPos+errorInTextSnippetSize > text.length) text.length else m.getToPos + errorInTextSnippetSize
              "<b>" + m.getMessage + "</b><br>" +
                "..." + text.substring(snippetFrom,m.getFromPos) + "<u>" + text.substring(m.getFromPos,m.getToPos) + "</u>" +
                text.substring(m.getToPos,snippetTo) +
                "...<br>Suggested correction: " + m.getSuggestedReplacements + "<br>"
        } else {
            " "
        }
    }
      )
    result.mkString(" ")
  }

}

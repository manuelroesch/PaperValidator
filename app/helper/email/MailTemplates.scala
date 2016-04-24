package helper.email

import play.api.Logger

/**
  * Created by manuel on 19.04.2016.
  */
object MailTemplates {

  def sendConferenceMail(conferenceName : String, conferenceLink : String, toEmail : String): Unit = {
    val subject = "PaperValidator: About Conference '" + conferenceName + "'"
    val content =
      s"""Dear user of PaperValidator,<br><br>
        |
        |Here is the link to your Conference '"$conferenceName"':<br>
        |<b>$conferenceLink</b><br><br>
        |
        |You can edit the conference settings or delete the conference under this link.
        |
        |Have fun using PaperValidator!
      """.stripMargin
    Logger.debug(content)
    //MailService.sendMail(toEmail,subject,content)
  }

  def sendPaperAnalyzedMail(paperName: String, paperLink : String, permutations: Int, toEmail: String): Unit = {
    val subject = "PaperValidator: '" + paperName + "' analyzed!"
    val content =
      s"""Dear user of PaperValidator,<br><br>
          |
        |Your paper '$paperName' has been analyzed.<br>
          |There where $permutations found. Confirm with the following link that you would like to process the paper:
          |<b>$paperLink</b><br><br>
          |
        |Have fun using PaperValidator!
      """.stripMargin
    Logger.debug(content)
    //MailService.sendMail(toEmail,subject,content)
  }


  def sendPaperCompletedMail(paperName: String, paperLink : String, toEmail: String): Unit = {
    val subject = "PaperValidator: '" + paperName + "' completed!"
    val content =
      s"""Dear user of PaperValidator,<br><br>
          |
        |The analysis of your paper '$paperName' has been completed.<br>
          |You can checkout the results under this link:
          |<b>$paperLink</b><br><br>
          |
        |Have fun using PaperValidator!
      """.stripMargin
    Logger.debug(content)
    //MailService.sendMail(toEmail,subject,content)
  }

}

package helper.email

/**
  * Created by manuel on 19.04.2016.
  */
object MailTemplates {

  def sendConferenceMail(conferenceName : String, conferenceLink : String, toEmail : String): Unit = {
    val subject = "PaperValidator: About Conference '" + conferenceName + "'"
    val content =
      s"""Dear user of PaperValidator,<br><br>
        |
        |Here is the link to your Conference "$conferenceName":<br>
        |<b>$conferenceLink</b><br><br>
        |
        |You can edit the conference settings or delete the conference under this link.
        |
        |Have fun using PaperValidator!
      """.stripMargin
    MailService.sendMail(toEmail,subject,content)
  }

}

package helper.email

import controllers.routes
import helper.Commons
import models.EmailService
import org.joda.time.{Hours, DateTime}
import play.Configuration
import play.api.Logger

/**
  * Created by manuel on 19.04.2016.
  */
object MailTemplates {

  def sendAccountMail(toEmail : String, configuration: Configuration, emailService: EmailService): Unit = {
    var isNewEmailAddress = false
    val email = emailService.findByEmail(toEmail).getOrElse({
      emailService.create(toEmail,Commons.generateSecret())
      isNewEmailAddress = true
      emailService.findByEmail(toEmail).get
    })
    val link = configuration.getString("hcomp.ballot.baseURL") + routes.Account.account(email.id.get,email.secret).url
    val subject = "PaperValidator: Your papers and conferences"
    val content =
      s"""Dear user of PaperValidator,<br><br>
          |
        |Here is the link to your paper and conferences:<br>
          |<a href="$link">
          | <b>$link</b>
          |</a><br><br>
          |
        |You can edit your papers and conferences under this link.
          |
        |Have fun using PaperValidator!
      """.stripMargin
    Logger.debug(content)
    if(Hours.hoursBetween(email.lastMail,DateTime.now()).getHours > 12 || isNewEmailAddress) {
      Logger.debug("Email would be sent!")
      emailService.setLastMailNow(email.id.getOrElse(-1),email.secret)
      //MailSendingService.sendMail(toEmail,subject,content)
    } else {
      Logger.debug("Email would not be sent! Daycount:" + Hours.hoursBetween(email.lastMail,DateTime.now()).getHours)
    }
  }

  def sendConferenceMail(conferenceName : String, conferenceLink : String, toEmail : String): Unit = {
    val subject = "PaperValidator: About Conference " + conferenceName
    val content =
      s"""Dear user of PaperValidator,<br><br>
        |
        |Here is the link to your Conference '"$conferenceName"':<br>
        |<a href="$conferenceLink">
        | <b>$conferenceLink</b>
        |</a><br><br>
        |
        |You can edit the conference settings or delete the conference under this link.
        |
        |Have fun using PaperValidator!
      """.stripMargin
    Logger.debug(content)
    //MailSendingService.sendMail(toEmail,subject,content)
  }

  def sendPaperAnalyzedMail(paperName: String, paperLink : String, permutations: Int, toEmail: String, comment: String): Unit = {
    val subject = "PaperValidator: " + paperName + " analyzed!"
    val content =
      s"""Dear user of PaperValidator,<br><br>
          |
        |Your paper '$paperName' has been analyzed.<br>
          |There where $permutations permutations found. Confirm with the following link that you would like to process the paper:<br>
          |<a href="$paperLink">
          | <b>$paperLink</b>
          |</a><br><br>
          |
          |$comment
          |
        |Have fun using PaperValidator!
      """.stripMargin
    Logger.debug(content)
    //MailSendingService.sendMail(toEmail,subject,content)
  }


  def sendPaperCompletedMail(paperName: String, paperLink : String, toEmail: String): Unit = {
    val subject = "PaperValidator: " + paperName + " completed!"
    val content =
      s"""Dear user of PaperValidator,<br><br>
          |
        |The analysis of your paper '$paperName' has been completed.<br>
          |You can checkout the results under this link:
          |<a href="$paperLink">
          | <b>$paperLink</b>
          |</a><br><br>
          |
        |Have fun using PaperValidator!
      """.stripMargin
    Logger.debug(content)
    //MailSendingService.sendMail(toEmail,subject,content)
  }

}

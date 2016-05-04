package models

import javax.inject.Inject

import anorm.SqlParser._
import anorm._
import org.joda.time.DateTime
import play.api.db.Database

/**
  * Created by pdeboer on 20/11/15.
  */
case class Email(id: Option[Int], emailAddress: String, secret: String, lastMail: DateTime) extends Serializable

class EmailService @Inject()(db:Database) {

	private val emailParser: RowParser[Email] =
		get[Option[Int]]("id") ~
		get[String]("email_address") ~
		get[String]("secret") ~
		get[DateTime]("last_mail") map {
		case id ~ email_address ~ secret ~ last_mail =>
			Email(id, email_address, secret, last_mail)
	}

	def findById(id: Int, secret: String): Option[Email] =
	db.withConnection { implicit c =>
		SQL("SELECT * FROM email WHERE id = {id} And secret = {secret}").on(
			'id -> id,
			'secret -> secret
		).as(emailParser.singleOpt)
	}

	def findByEmail(emailAddress: String): Option[Email] =
	db.withConnection { implicit c =>
		SQL("SELECT * FROM email WHERE email_address = {email_address}").on(
			'email_address -> emailAddress
		).as(emailParser.singleOpt)
	}


	def findByEmailAndSecret(emailAddress: String, secret: String): Option[Email] =
	db.withConnection { implicit c =>
		SQL("SELECT * FROM email WHERE email_address = {email_address} and secret = {secret}").on(
			'email_address -> emailAddress,
			'secret -> secret
		).as(emailParser.singleOpt)
	}

	def create(emailAddress: String, secret: String): Unit = {
		db.withConnection { implicit c =>
			SQL("INSERT INTO email (email_address, secret, last_mail) VALUES ({email_address}, {secret}, NOW())").on(
				'email_address -> emailAddress,
				'secret -> secret
			).executeInsert()
		}
	}

	def setLastMailNow(id: Int, secret: String): Unit = {
		db.withConnection { implicit c =>
			SQL("UPDATE email SET last_mail = NOW() WHERE id = {id} and secret = {secret}").on(
				'id -> id,
				'secret -> secret
			).executeUpdate()
		}
	}
}

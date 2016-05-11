package models

import javax.inject.Inject

import anorm.SqlParser._
import anorm._
import play.api.db.Database

/**
  * Created by manuel on 19.04.16.
  */
case class Conference(id: Option[Int], name: String, email: String, secret: String) extends Serializable

class ConferenceService  @Inject()(db:Database) {

	private val answerParser: RowParser[Conference] =
		get[Option[Int]]("id") ~
			get[String]("name") ~
			get[String]("email") ~
			get[String]("secret") map {
			case id ~ name ~ email ~ secret =>
				Conference(id, name, email, secret)
		}

	def findById(id: Int): Option[Conference] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM conference WHERE id = {id}").on(
				'id -> id
			).as(answerParser.singleOpt)
		}

	def findByIdAndSecret(id: Int, secret: String): Option[Conference] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM conference WHERE id = {id} AND secret = {secret}").on(
				'id -> id,
				'secret -> secret
			).as(answerParser.singleOpt)
		}

	def findByEmail(email: String): List[Conference] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM conference WHERE email = {email} ORDER BY name").on(
				'email -> email
			).as(answerParser *)
		}

	def findAll(): List[Conference] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM conference ORDER BY name").as(answerParser *)
		}


	def create(name: String, email: String, secret: String) : Int =
		db.withConnection { implicit c =>
			SQL("INSERT INTO conference(name, email, secret) VALUES ({name}, {email}, {secret})").on(
				'name -> name,
				'email -> email,
				'secret -> secret
			).executeInsert(scalar[Int].single)
		}

}
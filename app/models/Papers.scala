package models

import javax.inject.{Singleton, Inject}

import anorm.SqlParser._
import anorm._
import helper.PaperProcessingManager
import org.joda.time.DateTime
import anorm.JodaParameterMetaData._
import play.api.db.Database

/**
  * Created by manuel on 19.04.16.
  */
case class Papers(id: Option[Int], name: String, email: String, status: Int, lastModified: DateTime, secret: String) extends Serializable

class PapersService @Inject()(db:Database) {

	private val answerParser: RowParser[Papers] =
		get[Option[Int]]("id") ~
				get[String]("name") ~
				get[String]("email") ~
				get[Int]("status") ~
				get[DateTime]("last_modified") ~
				get[String]("secret") map {
			case id ~ name ~ email ~ status ~ last_modified ~ secret =>
				Papers(id, name, email, status, last_modified, secret)
		}

	def findById(id: Int): Option[Papers] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM papers WHERE id = {id}").on(
				'id -> id
			).as(answerParser.singleOpt)
		}

	def findByIdAndSecret(id: Int, secret: String): Option[Papers] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM papers WHERE id = {id} AND secret = {secret}").on(
				'id -> id,
				'secret -> secret
			).as(answerParser.singleOpt)
		}

	def findAll(): List[Papers] = {
		db.withConnection { implicit c =>
			SQL("SELECT * FROM papers").as(answerParser *)
		}
	}

	def findByStatus(status: Int): List[Papers] = {
		db.withConnection { implicit c =>
			SQL("SELECT * FROM papers WHERE status = {status} ORDER BY last_modified DESC").on(
				'status -> status
			).as(answerParser *)
		}
	}

	def findProcessablePapers(): List[Papers] = {
		db.withConnection { implicit c =>
			SQL("SELECT * FROM papers WHERE status = " + PaperProcessingManager.PAPER_STATUS_NEW + " OR status = " +
				PaperProcessingManager.PAPER_STATUS_IN_PPLIB_QUEUE + " ORDER BY last_modified DESC").as(answerParser *)
		}
	}

	def create(name: String, email: String, secret: String) =
		db.withConnection { implicit c =>
			SQL("INSERT INTO papers(name, email, status, last_modified, secret) " +
				"VALUES ({name},{email},{status},{last_modified},{secret})").on(
				'name -> name,
				'email -> email,
				'status -> PaperProcessingManager.PAPER_STATUS_NEW,
				'last_modified -> DateTime.now(),
				'secret -> secret
			).executeInsert()
		}

	def updateStatus(id: Int, status: Int) =
		db.withConnection { implicit c =>
			SQL("UPDATE papers SET status={status},last_modified={last_modified} WHERE id={id}").on(
				'id -> id,
				'status -> status,
				'last_modified -> DateTime.now()
			).executeUpdate()
		}

	def delete(id: Int) =
		db.withConnection { implicit c =>
			SQL("DELETE FROM papers WHERE id={id}").on(
				'id -> id
			).executeUpdate()
		}

}
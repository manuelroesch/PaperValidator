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
object Papers {
	val STATUS_NEW = 0
	val STATUS_AWAIT_CONFIRMATION = 1
	val STATUS_IN_PPLIB_QUEUE = 2
	val STATUS_COMPLETED = 3
	val STATUS_ERROR = 4
}

case class Papers(id: Option[Int], name: String, email: String, conferenceId: Int, status: Int, permutations: Int,
									lastModified: DateTime, secret: String) extends Serializable

class PapersService @Inject()(db:Database) {

	private val answerParser: RowParser[Papers] =
		get[Option[Int]]("id") ~
				get[String]("name") ~
				get[String]("email") ~
				get[Int]("conference_id") ~
				get[Int]("status") ~
				get[Int]("permutations") ~
				get[DateTime]("last_modified") ~
				get[String]("secret") map {
			case id ~ name ~ email ~ conference_id ~ status ~ permutations ~ last_modified ~ secret =>
				Papers(id, name, email, conference_id, status, permutations, last_modified, secret)
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

	def findByEmail(email: String): List[Papers] = {
		db.withConnection { implicit c =>
			SQL("SELECT * FROM papers WHERE email = {email}").on(
				'email -> email
			).as(answerParser *)
		}
	}

	def findAll(): List[Papers] = {
		db.withConnection { implicit c =>
			SQL("SELECT * FROM papers ORDER BY name").as(answerParser *)
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
			SQL("SELECT * FROM papers WHERE status = " + Papers.STATUS_NEW + " OR status = " +
				Papers.STATUS_IN_PPLIB_QUEUE + " ORDER BY last_modified DESC").as(answerParser *)
		}
	}

	def countPapersByConference(conferenceId: Int): Int = {
		db.withConnection { implicit c =>
			SQL("SELECT count(*) FROM papers WHERE conference_id = {conference_id}").on(
				'conference_id -> conferenceId
			).as(scalar[Int].single)
		}
	}

	def create(name: String, email: String, conferenceId: Int, secret: String) =
		db.withConnection { implicit c =>
			SQL("INSERT INTO papers(name, email, conference_id, status, permutations, last_modified, secret) " +
				"VALUES ({name},{email},{conference_id},{status},{permutations},{last_modified},{secret})").on(
				'name -> name,
				'email -> email,
				'conference_id -> conferenceId,
				'status -> Papers.STATUS_NEW,
				'permutations -> 0,
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

	def updatePermutations(id: Int, permutations: Int) =
		db.withConnection { implicit c =>
			SQL("UPDATE papers SET permutations={permutations},last_modified={last_modified} WHERE id={id}").on(
				'id -> id,
				'permutations -> permutations,
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
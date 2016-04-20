package models

import javax.inject.Inject

import anorm.SqlParser._
import anorm._
import anorm.JodaParameterMetaData._
import org.joda.time.DateTime
import play.api.db.DBApi

/**
  * Created by manuel on 19.04.16.
  */
case class Assumption(id: Option[Int], name: String, synonyms: String) extends Serializable


@javax.inject.Singleton
class AssumptionService @Inject()(dbapi: DBApi) {
	private val db = dbapi.database("default")

	private val answerParser: RowParser[Assumption] =
		get[Option[Int]]("id") ~
				get[String]("name") ~
				get[String]("synonyms") map {
			case id ~ name ~  synonyms =>
				Assumption(id, name, synonyms)
		}

	def findById(id: Int): Option[Assumption] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM assumptions WHERE id = {id}").on(
				'id -> id
			).as(answerParser.singleOpt)
		}

	def findAll(): List[Assumption] = {
		db.withConnection { implicit c =>
			SQL("SELECT * FROM assumptions ORDER BY name ASC").as(answerParser *)
		}
	}

	def create(name: String, synonyms: String = "") =
		db.withConnection { implicit c =>
			SQL("INSERT INTO assumptions(name, synonyms) VALUES ({name}, {synonyms})").on(
				'name -> name,
				'synonyms -> synonyms
			).executeInsert()
		}

	def update(id: Int, name: String, synonyms: String) =
		db.withConnection { implicit c =>
			SQL("UPDATE assumptions SET name={name}, synonyms={synonyms} WHERE id={id}").on(
				'id -> id,
				'name -> name,
				'synonyms -> synonyms
			).executeUpdate()
		}

	def delete(id: Int) =
		db.withConnection { implicit c =>
			SQL("DELETE FROM assumptions WHERE id={id}").on(
				'id -> id
			).executeUpdate()
		}

}
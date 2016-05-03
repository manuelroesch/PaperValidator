package models

import javax.inject.{Inject, Singleton}

import anorm.SqlParser._
import anorm._
import play.api.db.Database

/**
  * Created by manuel on 19.04.16.
  */
case class Assumption(id: Option[Int], conferenceId: Int, name: String, synonyms: String) extends Serializable


class AssumptionService @Inject()(db:Database) {

	private val answerParser: RowParser[Assumption] =
		get[Option[Int]]("id") ~
				get[Int]("conference_id") ~
				get[String]("name") ~
				get[String]("synonyms") map {
			case id ~ conference_id ~ name ~  synonyms =>
				Assumption(id, conference_id, name, synonyms)
		}

	def findById(id: Int, conferenceId: Int): Option[Assumption] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM assumptions WHERE id = {id} AND conference_id = {conference_id}").on(
				'id -> id,
				'conference_id -> conferenceId
			).as(answerParser.singleOpt)
		}

	def findByName(conferenceId: Int, name: String): Option[Assumption] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM assumptions WHERE name = {name} AND conference_id = {conference_id}").on(
				'name -> name,
				'conference_id -> conferenceId
			).as(answerParser.singleOpt)
		}

	def findAll(conference_id: Int): List[Assumption] = {
		db.withConnection { implicit c =>
			SQL("SELECT * FROM assumptions " +
				"WHERE conference_id = {conference_id} " +
				"ORDER BY name ASC").on(
				'conference_id -> conference_id
			).as(answerParser *)
		}
	}

	def create(conferenceId: Int, name: String, synonyms: String = "") =
		db.withConnection { implicit c =>
			SQL("INSERT INTO assumptions(conference_id, name, synonyms) VALUES ({conference_id}, {name}, {synonyms})").on(
				'conference_id -> conferenceId,
				'name -> name,
				'synonyms -> synonyms
			).executeInsert()
		}

	def update(id: Int, conferenceId: Int, name: String, synonyms: String) =
		db.withConnection { implicit c =>
			SQL("UPDATE assumptions SET name={name}, synonyms={synonyms} WHERE id={id}").on(
				'id -> id,
				'conference_id -> conferenceId,
				'name -> name,
				'synonyms -> synonyms
			).executeUpdate()
		}

	def delete(id: Int, conferenceId: Int) =
		db.withConnection { implicit c =>
			SQL("DELETE FROM assumptions WHERE id={id} AND conference_id={conference_id}").on(
				'id -> id,
				'conference_id -> conferenceId
			).executeUpdate()
		}

}
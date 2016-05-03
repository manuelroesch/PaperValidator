package models

import javax.inject.{Inject, Singleton}

import anorm.SqlParser._
import anorm._
import play.api.db.Database

/**
  * Created by manuel on 19.04.16.
  */
case class Method(id: Option[Int], conferenceId: Int, name: String, delta: Int, synonyms: String) extends Serializable


class MethodService @Inject()(db:Database) {

	private val answerParser: RowParser[Method] =
		get[Option[Int]]("id") ~
				get[Int]("conference_id") ~
				get[String]("name") ~
				get[Int]("delta") ~
				get[String]("synonyms") map {
			case id ~ conference_id ~ name ~ delta ~ synonyms =>
				Method(id, conference_id, name, delta, synonyms)
		}

	def findById(id: Int, conferenceId: Int): Option[Method] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM methods WHERE id = {id} AND conference_id = {conference_id}").on(
				'id -> id,
				'conference_id -> conferenceId
			).as(answerParser.singleOpt)
		}

	def findByName(conferenceId: Int, name: String): Option[Method] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM methods WHERE name = {name} AND conference_id = {conference_id}").on(
				'name -> name,
				'conference_id -> conferenceId
			).as(answerParser.singleOpt)
		}

	def findAll(conferenceId: Int): List[Method] = {
		db.withConnection { implicit c =>
			SQL("SELECT * FROM methods " +
				"WHERE conference_id = {conference_id} " +
				"ORDER BY name ASC").on(
				'conference_id -> conferenceId
			).as(answerParser *)
		}
	}

	def create(conferenceId: Int, name: String, delta: Int = 0, synonyms: String = "") =
		db.withConnection { implicit c =>
			SQL("INSERT INTO methods(conference_id, name, delta, synonyms) VALUES ({conference_id}, {name}, {delta}, {synonyms})").on(
				'conference_id -> conferenceId,
				'name -> name,
				'delta -> delta,
				'synonyms -> synonyms
			).executeInsert()
		}

	def update(id: Int, conferenceId: Int, name: String, delta: Int, synonyms: String) =
		db.withConnection { implicit c =>
			SQL("UPDATE methods SET name={name}, delta={delta}, synonyms={synonyms} " +
				"WHERE id={id} AND conference_id={conference_id}").on(
				'id -> id,
				'conference_id -> conferenceId,
				'name -> name,
				'delta -> delta,
				'synonyms -> synonyms
			).executeUpdate()
		}

	def delete(id: Int, conferenceId: Int) =
		db.withConnection { implicit c =>
			SQL("DELETE FROM methods " +
				"WHERE id={id} AND conference_id={conference_id}").on(
				'id -> id,
				'conference_id -> conferenceId
			).executeUpdate()
		}

}
package models

import javax.inject.{Inject, Singleton}

import anorm.SqlParser._
import anorm._
import play.api.db.Database

/**
  * Created by manuel on 19.04.16.
  */
case class Method(id: Option[Int], name: String, delta: Int, synonyms: String) extends Serializable


class MethodService @Inject()(db:Database) {

	private val answerParser: RowParser[Method] =
		get[Option[Int]]("id") ~
				get[String]("name") ~
				get[Int]("delta") ~
				get[String]("synonyms") map {
			case id ~ name ~ delta ~ synonyms =>
				Method(id, name, delta, synonyms)
		}

	def findById(id: Int): Option[Method] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM methods WHERE id = {id}").on(
				'id -> id
			).as(answerParser.singleOpt)
		}

	def findAll(): List[Method] = {
		db.withConnection { implicit c =>
			SQL("SELECT * FROM methods ORDER BY name ASC").as(answerParser *)
		}
	}

	def create(name: String, delta: Int = 0, synonyms: String = "") =
		db.withConnection { implicit c =>
			SQL("INSERT INTO methods(name, delta, synonyms) VALUES ({name}, {delta}, {synonyms})").on(
				'name -> name,
				'delta -> delta,
				'synonyms -> synonyms
			).executeInsert()
		}

	def update(id: Int, name: String, delta: Int, synonyms: String) =
		db.withConnection { implicit c =>
			SQL("UPDATE methods SET name={name}, delta={delta}, synonyms={synonyms} WHERE id={id}").on(
				'id -> id,
				'name -> name,
				'delta -> delta,
				'synonyms -> synonyms
			).executeUpdate()
		}

	def delete(id: Int) =
		db.withConnection { implicit c =>
			SQL("DELETE FROM methods WHERE id={id}").on(
				'id -> id
			).executeUpdate()
		}

}
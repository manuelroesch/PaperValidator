package models

import javax.inject.{Inject, Singleton}

import anorm.SqlParser._
import anorm._
import org.joda.time.DateTime
import anorm.JodaParameterMetaData._
import play.api.db.Database

/**
  * Created by mattia on 02.07.15.
  */
case class User(id: Option[Long], turkerId: String, firstSeenDateTime: DateTime)

class UserService @Inject()(db:Database) {

	private val userParser: RowParser[User] =
		get[Option[Long]]("id") ~
				get[String]("turker_id") ~
				get[DateTime]("first_seen_date_time") map {
			case id ~ turker_id ~ first_seen_date_time =>
				User(id, turker_id, first_seen_date_time)
		}

	def findById(id: Long): Option[User] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM users WHERE id = {id}").on(
				'id -> id
			).as(userParser.singleOpt)
		}

	def findByTurkerId(turkerId: String): Option[User] =
		if (turkerId == null) None
		else
			db.withConnection { implicit c =>
				SQL("SELECT * FROM users WHERE turker_id = {turkerId}").on(
					'turkerId -> turkerId
				).as(userParser.singleOpt)
			}

	def create(turkerId: String, firstSeenDateTime: DateTime): Option[Long] =
		db.withConnection { implicit c =>
			SQL("INSERT INTO users(turker_id, first_seen_date_time) VALUES ({turkerId}, {firstSeenDateTime})").on(
				'turkerId -> turkerId,
				'firstSeenDateTime -> firstSeenDateTime
			).executeInsert()
		}
}
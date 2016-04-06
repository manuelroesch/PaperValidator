package models

import anorm.SqlParser._
import anorm._
import anorm.JodaParameterMetaData._
import org.joda.time.DateTime
import play.api.Play.current
import play.api.db.DB

/**
  * Created by mattia on 02.07.15.
  */
case class User(id: Pk[Long], turkerId: String, firstSeenDateTime: DateTime)

object UserDAO {
	private val userParser: RowParser[User] =
		get[Pk[Long]]("id") ~
				get[String]("turker_id") ~
				get[DateTime]("first_seen_date_time") map {
			case id ~ turker_id ~ first_seen_date_time =>
				User(id, turker_id, first_seen_date_time)
		}

	def findById(id: Long): Option[User] =
		DB.withConnection { implicit c =>
			SQL("SELECT * FROM user WHERE id = {id}").on(
				'id -> id
			).as(userParser.singleOpt)
		}

	def findByTurkerId(turkerId: String): Option[User] =
		if (turkerId == null) None
		else
			DB.withConnection { implicit c =>
				SQL("SELECT * FROM user WHERE turker_id = {turkerId}").on(
					'turkerId -> turkerId
				).as(userParser.singleOpt)
			}

	def create(turkerId: String, firstSeenDateTime: DateTime): Option[Long] =
		DB.withConnection { implicit c =>
			SQL("INSERT INTO user(turker_id, first_seen_date_time) VALUES ({turkerId}, {firstSeenDateTime})").on(
				'turkerId -> turkerId,
				'firstSeenDateTime -> firstSeenDateTime
			).executeInsert()
		}
}
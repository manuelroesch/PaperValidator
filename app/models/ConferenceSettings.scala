package models

import javax.inject.{Inject, Singleton}

import anorm.SqlParser._
import anorm._
import play.api.db.Database

/**
  * Created by manuel on 19.04.16.
  */
case class ConferenceSettings(id: Option[Int], method2AssumptionId: Int, methodName: String, assumptionName: String, flag: Option[Int]) extends Serializable


class ConferenceSettingsService @Inject()(db:Database) {

	private val answerParser: RowParser[ConferenceSettings] =
		get[Option[Int]]("id") ~
			get[Int]("method2assumption_id") ~
			get[String]("method_name") ~
			get[String]("assumption_name") ~
			get[Option[Int]]("flag") map {
			case id ~ method2assumption_id ~ method_name ~ assumption_name ~ flag =>
				ConferenceSettings(id, method2assumption_id, method_name, assumption_name, flag)
		}

	def findById(id: Int, conferenceId: Int): Option[ConferenceSettings] =
		db.withConnection { implicit c =>
			SQL("SELECT cs.id, m2a.id methods2assumption_id, m2a.method_name, m2a.assumption_name, cs.flag " +
						"FROM (SELECT m2a.id, m2a.method_id, m.name method_name, m2a.assumption_id, a.name assumption_name " +
						"FROM methods2assumptions m2a,methods m,assumptions a " +
						"WHERE m2a.method_id=m.id AND m2a.assumption_id=a.id) m2a " +
				"LEFT JOIN conference_settings cs ON m2a.id = cs.method2assumption_id " +
				"AND cs.conference_id={conference_id} AND cs.id = {id}").on(
				'id -> id,
				'conference_id -> conferenceId
			).as(answerParser.singleOpt)
		}

	def findAllByConference(conferenceId : Int): List[ConferenceSettings] = {
		db.withConnection { implicit c =>
			SQL("SELECT cs.id, m2a.id method2assumption_id, m2a.method_name, m2a.assumption_name, cs.flag " +
						"FROM (SELECT m2a.id m2aid, m2a.method_id, m.name method_name, m2a.assumption_id, a.name assumption_name " +
						"FROM methods2assumptions m2a,methods m,assumptions a " +
						"WHERE m2a.method_id=m.id AND m2a.assumption_id=a.id) m2a " +
				"LEFT JOIN conference_settings cs ON m2a.m2aid = cs.method2assumption_id AND cs.conference_id={conference_id} " +
				"ORDER BY method_name ASC, assumption_name ASC").on(
				'conference_id -> conferenceId
			).as(answerParser *)
		}
	}

	def create(conferenceId : Int, method2AssumptionId : Int, flag : Int) =
		db.withConnection { implicit c =>
			SQL("INSERT INTO conference_settings(conference_id, method2assumption_id, flag) " +
				"VALUES ({conference_id}, {method2assumption_id}, {flag})").on(
				'conference_id -> conferenceId,
				'method2assumption_id -> method2AssumptionId,
				'flag -> flag
			).executeInsert()
		}

	def update(id: Int, flag : Int) =
		db.withConnection { implicit c =>
			SQL("UPDATE conference_settings SET flag={flag} WHERE id={id}").on(
				'id -> id,
				'flag -> flag
			).executeUpdate()
		}

	def delete(id: Int) =
		db.withConnection { implicit c =>
			SQL("DELETE FROM conference_settings WHERE id={id}").on(
				'id -> id
			).executeUpdate()
		}

}
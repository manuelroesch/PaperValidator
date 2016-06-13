package models

import javax.inject.{Inject, Singleton}

import anorm.SqlParser._
import anorm._
import play.api.db.Database

/**
  * Created by manuel on 19.04.16.
  */
case class Method2Assumption(id: Option[Int], conferenceId: Int, methodId: Int, methodName: String, assumptionId: Int, assumptionName: String, question: String) extends Serializable


class Method2AssumptionService @Inject()(db:Database) {

	private val answerParser: RowParser[Method2Assumption] =
		get[Option[Int]]("id") ~
				get[Int]("conference_id") ~
				get[Int]("method_id") ~
				get[String]("method_name") ~
				get[Int]("assumption_id") ~
				get[String]("assumption_name") ~
			  get[String]("question") map {
			case id ~ conference_id ~ method_id ~ method_name ~ assumption_id ~ assumption_name ~ question =>
				Method2Assumption(id, conference_id, method_id, method_name, assumption_id, assumption_name, question)
		}

	def findById(id: Int, conferenceId: Int): Option[Method2Assumption] =
		db.withConnection { implicit c =>
			SQL("SELECT m2a.id, m2a.method_id, m.name method_name, m2a.assumption_id, a.name assumption_name " +
				"FROM methods2assumptions m2a,methods m,assumptions a " +
				"WHERE m2a.id = {id} AND m2a.method_id=m.id AND m2a.assumption_id=a.id " +
        "AND m2a.conference_id = {conference_id} AND m.conference_id = {conference_id} " +
        "AND a.conference_id = {conference_id}").on(
				'id -> id,
        'conference_id -> conferenceId
			).as(answerParser.singleOpt)
		}

	def findByMethodAndAssumptionName(method: String, assumption: String, conferenceId: Int) : Option[Method2Assumption] =
		db.withConnection { implicit c =>
			SQL("SELECT m2a.id, m2a.conference_id, m2a.method_id, m.name method_name, m2a.assumption_id, a.name assumption_name, " +
				"m2a.question  " +
				"FROM methods2assumptions m2a,methods m,assumptions a " +
				"WHERE m2a.method_id=m.id AND m2a.assumption_id=a.id AND " +
				"(m.name = {method} OR CONCAT(',',m.synonyms,',') LIKE {method_syn}) AND " +
				"(a.name = {assumption} OR CONCAT(',',a.synonyms,',') LIKE {assumption_syn}) " +
        "AND m2a.conference_id = {conference_id} AND m.conference_id = {conference_id} " +
        "AND a.conference_id = {conference_id}").on(
				'method -> method,
				'method_syn -> ("%,"+method+",%"),
				'assumption -> assumption,
				'assumption_syn -> ("%,"+assumption+",%"),
        'conference_id -> conferenceId
			).as(answerParser.singleOpt)
		}

	def findAll(conferenceId: Int): List[Method2Assumption] = {
		db.withConnection { implicit c =>
			SQL("SELECT m2a.id, m2a.conference_id, m2a.method_id, m.name method_name, " +
				"m2a.assumption_id, a.name assumption_name, m2a.question " +
				"FROM methods2assumptions m2a,methods m,assumptions a " +
				"WHERE m2a.method_id=m.id AND m2a.assumption_id=a.id " +
				"AND m2a.conference_id={conference_id} AND m.conference_id={conference_id} " +
				"AND a.conference_id={conference_id} " +
				"ORDER BY method_name ASC, assumption_name ASC").on(
        'conference_id -> conferenceId
			).as(answerParser *)
		}
	}

	def create(conferenceId: Int, methodId : Int, assumptionId : Int, question: String) =
		db.withConnection { implicit c =>
			SQL("INSERT INTO methods2assumptions(conference_id, method_id, assumption_id, question) " +
				"VALUES ({conference_id}, {method_id}, {assumption_id}, {question})").on(
        'conference_id -> conferenceId,
				'method_id -> methodId,
				'assumption_id -> assumptionId,
				'question -> question
			).executeInsert()
		}

	def update(id: Int, conference_id: Int, methodId : Int, assumptionId : Int, question: String) =
		db.withConnection { implicit c =>
			SQL("UPDATE methods2assumptions SET method_id={method_id}, assumption_id={assumption_id}, " +
				"question={question} WHERE id={id} AND conference_id = {conference_id}").on(
				'id -> id,
        'confrence_id -> conference_id,
				'method_id -> methodId,
				'assumption_id -> assumptionId,
				'question -> question
			).executeUpdate()
		}

	def deleteByMethodId(methodId: Int) =
		db.withConnection { implicit c =>
			SQL("DELETE FROM methods2assumptions WHERE method_id={method_id}").on(
				'method_id -> methodId
			).executeUpdate()
		}

	def deleteByAssumptionId(assumptionId: Int) =
		db.withConnection { implicit c =>
			SQL("DELETE FROM methods2assumptions WHERE assumption_id={assumption_id}").on(
				'assumption_id -> assumptionId
			).executeUpdate()
		}

	def delete(id: Int, conference_id: Int) =
		db.withConnection { implicit c =>
			SQL("DELETE FROM methods2assumptions WHERE id={id} AND conference_id={conference_id}").on(
				'id -> id,
        'conference_id -> conference_id
			).executeUpdate()
		}

}
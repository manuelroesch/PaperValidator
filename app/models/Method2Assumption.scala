package models

import javax.inject.{Inject, Singleton}

import anorm.SqlParser._
import anorm._
import play.api.db.Database

/**
  * Created by manuel on 19.04.16.
  */
case class Method2Assumption(id: Option[Int], methodId: Int, methodName: String, assumptionId: Int, assumptionName: String, question: String, answers: String) extends Serializable


class Method2AssumptionService @Inject()(db:Database) {

	private val answerParser: RowParser[Method2Assumption] =
		get[Option[Int]]("id") ~
				get[Int]("method_id") ~
				get[String]("method_name") ~
				get[Int]("assumption_id") ~
				get[String]("assumption_name") ~
			  get[String]("question") ~
			  get[String]("answers") map {
			case id ~ method_id ~ method_name ~ assumption_id ~ assumption_name ~ question ~ answers =>
				Method2Assumption(id, method_id, method_name, assumption_id, assumption_name, question, answers)
		}

	def findById(id: Int): Option[Method2Assumption] =
		db.withConnection { implicit c =>
			SQL("SELECT m2a.id, m2a.method_id, m.name method_name, m2a.assumption_id, a.name assumption_name " +
				"FROM methods2assumptions m2a,methods m,assumptions a " +
				"WHERE m2a.id = {id} AND m2a.method_id=m.id AND m2a.assumption_id=a.id").on(
				'id -> id
			).as(answerParser.singleOpt)
		}

	def findAll(): List[Method2Assumption] = {
		db.withConnection { implicit c =>
			SQL("SELECT m2a.id, m2a.method_id, m.name method_name, " +
				"m2a.assumption_id, a.name assumption_name, m2a.question, m2a.answers " +
				"FROM methods2assumptions m2a,methods m,assumptions a " +
				"WHERE m2a.method_id=m.id AND m2a.assumption_id=a.id " +
				"ORDER BY method_name ASC, assumption_name ASC"
			).as(answerParser *)
		}
	}

	def create(methodId : Int, assumptionId : Int, question: String, answers: String) =
		db.withConnection { implicit c =>
			SQL("INSERT INTO methods2assumptions(method_id, assumption_id, question, answers) " +
				"VALUES ({method_id}, {assumption_id}, {question}, {answers})").on(
				'method_id -> methodId,
				'assumption_id -> assumptionId,
				'question -> question,
				'answers -> answers
			).executeInsert()
		}

	def update(id: Int, methodId : Int, assumptionId : Int, question: String, answers: String) =
		db.withConnection { implicit c =>
			SQL("UPDATE methods2assumptions SET method_id={method_id}, assumption_id={assumption_id}, " +
				"question={question}, answers={answers} WHERE id={id}").on(
				'id -> id,
				'method_id -> methodId,
				'assumption_id -> assumptionId,
				'question -> question,
				'answers -> answers
			).executeUpdate()
		}

	def delete(id: Int) =
		db.withConnection { implicit c =>
			SQL("DELETE FROM methods2assumptions WHERE id={id}").on(
				'id -> id
			).executeUpdate()
		}

}
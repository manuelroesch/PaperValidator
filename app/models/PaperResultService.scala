package models

import javax.inject.Inject

import anorm.SqlParser._
import anorm._
import play.api.db.Database

/**
	* Created by manuel on 19.04.16.
	*/

object PaperResult {
	val SYMBOL_OK = 0
	val SYMBOL_WARNING = 1
	val SYMBOL_ERROR = 2

	val TYPE_M2A = 0
	val TYPE_STATCHECK = 1000
	val TYPE_STATCHECK_CHI2 = 1010
	val TYPE_STATCHECK_F = 1020
	val TYPE_STATCHECK_R = 1030
	val TYPE_STATCHECK_T = 1040
	val TYPE_STATCHECK_Z = 1050
}

case class PaperResult(id: Option[Long], paperId: Int, resultType: Int, descr: String, result: String, symbol: Int) extends Serializable

class PaperResultService @Inject()(db:Database) {

	private val answerParser: RowParser[PaperResult] =
		get[Option[Long]]("id") ~
				get[Int]("paper_id") ~
				get[Int]("result_type") ~
				get[String]("descr") ~
				get[String]("result") ~
				get[Int]("symbol") map {
			case id ~ paper_id ~ result_type ~ descr ~ result ~ symbol =>
				PaperResult(id, paper_id, result_type, descr, result, symbol)
		}

	def findById(id: Long): Option[PaperResult] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM paper_results WHERE id = {id}").on(
				'id -> id
			).as(answerParser.singleOpt)
		}


	def findByPaperId(paperId: Int): List[PaperResult] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM paper_results WHERE paper_id = {paper_id} ORDER BY result_type").on(
				'paper_id -> paperId
			).as(answerParser *)
		}

	def create(paperId: Int, resultType: Int, descr: String, result: String, symbol: Int) =
		db.withConnection { implicit c =>
			SQL("INSERT INTO paper_results(paper_id, result_type, descr, result, symbol) " +
				"VALUES ({paper_id},{result_type},{descr},{result},{symbol})").on(
				'paper_id -> paperId,
				'result_type -> resultType,
				'descr -> descr,
				'result -> result,
				'symbol -> symbol
			).executeInsert()
		}

	def delete(id: Long) =
		db.withConnection { implicit c =>
			SQL("DELETE FROM paper_results WHERE id={id}").on(
				'id -> id
			).executeUpdate()
		}

}
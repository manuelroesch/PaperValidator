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

	val TYPE_BASICS = 0
	val TYPE_BASICS_SAMPLE_SIZE = 10
	val TYPE_BASICS_ERROR_TERMS = 20
	val TYPE_BASICS_P_VALUES = 30
	val TYPE_BASICS_RANGE_P_VALUES = 40
	val TYPE_BASICS_PRECISION_P_VALUES = 50
	val TYPE_BASICS_SIDED_DISTRIBUTION = 60

	val TYPE_M2A = 1000

	val TYPE_STATCHECK = 2000
	val TYPE_STATCHECK_CHI2 = 2010
	val TYPE_STATCHECK_F = 2020
	val TYPE_STATCHECK_R = 2030
	val TYPE_STATCHECK_T = 2040
	val TYPE_STATCHECK_Z = 2050

	val TYPE_LAYOUT = 3000
	val TYPE_LAYOUT_BORDER = 3010
	val TYPE_LAYOUT_COLORS = 3020
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

	def countByConferenceTotal(conferenceId: Int) : Int = {
		db.withConnection { implicit c =>
			SQL("SELECT count(*) FROM paper_results r, papers p " +
				"WHERE r.paper_id = p.id AND (symbol = " + PaperResult.SYMBOL_WARNING + " OR " +
				"symbol = " + PaperResult.SYMBOL_ERROR + ") AND p.conference_id = {conference_id}").on(
				'conference_id -> conferenceId
			).as(scalar[Int].single)
		}
	}

	def countByConferencePapers(conferenceId: Int) : Int = {
		db.withConnection { implicit c =>
			SQL("SELECT count(DISTINCT r.paper_id) FROM paper_results r, papers p " +
				"WHERE r.paper_id = p.id AND (symbol = " + PaperResult.SYMBOL_WARNING + " OR " +
				"symbol = " + PaperResult.SYMBOL_ERROR + ") AND p.conference_id = {conference_id}").on(
				'conference_id -> conferenceId
			).as(scalar[Int].single)
		}
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
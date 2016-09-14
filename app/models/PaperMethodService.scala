package models

import javax.inject.Inject

import anorm.SqlParser._
import anorm._
import play.api.db.Database

case class PaperMethod(id: Option[Long], paperId: Int, method: String, pos: String) extends Serializable


class PaperMethodService @Inject()(db:Database) {

	private val answerParser: RowParser[PaperMethod] =
		get[Option[Long]]("id") ~
			get[Int]("paper_id") ~
			get[String]("method") ~
			get[String]("pos") map {
			case id ~ paper_id ~ method ~ pos =>
				PaperMethod(id, paper_id, method, pos)
		}

	def findById(id: Long): Option[PaperMethod] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM paper_methods WHERE id = {id}").on(
				'id -> id
			).as(answerParser.singleOpt)
		}

	def countByConferenceTotal(conferenceId: Int) : Int = {
		db.withConnection { implicit c =>
			SQL("SELECT count(*) FROM paper_methods m, papers p " +
				"WHERE m.paper_id = p.id AND p.conference_id = {conference_id}").on(
				'conference_id -> conferenceId
			).as(scalar[Int].single)
		}
	}

	def countByConferencePapers(conferenceId: Int) : Int = {
		db.withConnection { implicit c =>
			SQL("SELECT count(DISTINCT m.paper_id) FROM paper_methods m, papers p " +
				"WHERE m.paper_id = p.id AND p.conference_id = {conference_id}").on(
				'conference_id -> conferenceId
			).as(scalar[Int].single)
		}
	}

	def findByPaperId(paperId: Int) : List[PaperMethod] =
	db.withConnection { implicit c =>
		SQL("SELECT * FROM paper_methods WHERE paper_id = {paper_id}").on(
			'paper_id -> paperId
		).as(answerParser *)
	}

	def create(paperId: Int, method: String, pos: String) =
		db.withConnection { implicit c =>
			SQL("INSERT INTO paper_methods(paper_id, method, pos) " +
				"VALUES ({paper_id},{method},{pos})").on(
				'paper_id -> paperId,
				'method -> method,
				'pos -> pos
			).executeInsert()
		}

	def delete(id: Long) =
		db.withConnection { implicit c =>
			SQL("DELETE FROM paper_methods WHERE id={id}").on(
				'id -> id
			).executeUpdate()
		}

}
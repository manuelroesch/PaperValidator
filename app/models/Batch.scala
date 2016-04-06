package models

import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db.DB

/**
  * Created by mattia on 02.07.15.
  */
case class Batch(id: Pk[Long], allowedAnswersPerTurker: Int) extends Serializable

object BatchDAO {
	private val batchParser: RowParser[Batch] =
		get[Pk[Long]]("id") ~
				get[Int]("allowed_answers_per_turker") map {
			case id ~ allowed_answers_per_turker =>
				Batch(id, allowed_answers_per_turker)
		}

	def findById(id: Long): Option[Batch] =
		DB.withConnection { implicit c =>
			SQL("SELECT * FROM batch WHERE id = {id}").on(
				'id -> id
			).as(batchParser.singleOpt)
		}

}

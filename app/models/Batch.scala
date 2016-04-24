package models

import javax.inject.{Singleton, Inject}

import anorm.SqlParser._
import anorm._
import play.api.db.Database

/**
  * Created by mattia on 02.07.15.
  */
case class Batch(id: Option[Long], allowedAnswersPerTurker: Int) extends Serializable

class BatchService @Inject()(db:Database) {


	private val batchParser: RowParser[Batch] =
		get[Option[Long]]("id") ~
				get[Int]("allowed_answers_per_turker") map {
			case id ~ allowed_answers_per_turker =>
				Batch(id, allowed_answers_per_turker)
		}

	def findById(id: Long): Option[Batch] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM batch WHERE id = {id}").on(
				'id -> id
			).as(batchParser.singleOpt)
		}

}

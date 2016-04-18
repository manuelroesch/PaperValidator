package models

import javax.inject.Inject

import anorm.SqlParser._
import anorm._
import play.api.db.DBApi

/**
  * Created by mattia on 02.07.15.
  */
case class Batch(id: Option[Long], allowedAnswersPerTurker: Int) extends Serializable

@javax.inject.Singleton
class BatchService @Inject()(dbapi: DBApi) {

	private val db = dbapi.database("default")

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

package models

import javax.inject.Inject

import anorm._
import anorm.SqlParser._
import org.joda.time.DateTime
import anorm.JodaParameterMetaData._
import play.api.db.Database

/**
  * Created by mattia on 02.07.15.
  */
case class Question(id: Option[Long], html: String, batchId: Long, createTime: DateTime, uuid: String, secret: String)

class QuestionService @Inject()(db:Database) {

	private val questionParser: RowParser[Question] =
		get[Option[Long]]("id") ~
				get[String]("html") ~
				get[Long]("batch_id") ~
				get[DateTime]("create_time") ~
				get[String]("uuid") ~ get[String]("secret") map {
			case id ~ html ~ batch_id ~ create_time ~ uuid ~ secret =>
				Question(id, html, batch_id, create_time, uuid, secret)
		}


	def findById(id: Long): Option[Question] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM question WHERE id = {id}").on(
				'id -> id
			).as(questionParser.singleOpt)
		}

	def findIdByUUID(uuid: String): Long = {
		try {
			db.withConnection { implicit c =>
				SQL("SELECT * FROM question WHERE uuid = {uuid}").on(
					'uuid -> uuid
				).as(questionParser.singleOpt).get.id.get
			}
		} catch {
			case e: Exception => -1
		}
	}

	def findByAssetId(assetId: Long): List[Question] = {
		try {
			db.withConnection { implicit c =>
				SQL("SELECT q.* FROM question q INNER JOIN question2assets q2a ON (q2a.asset_id = {assetId})").on(
					'assetId -> assetId
				).as(questionParser *)
			}
		} catch {
			case e: Exception => Nil
		}
	}

	def create(html: String, batchId: Long, uuid: String, permutation: Long, secret: String) : Long =
		db.withConnection { implicit c =>
			SQL("INSERT INTO question(batch_id, html, create_time, uuid, permutation, secret) " +
				"VALUES ({batch_id},{html},{create_time},{uuid},{permutation},{secret})").on(
				'batch_id -> batchId,
				'html -> html,
				'create_time -> DateTime.now(),
				'uuid -> uuid,
				'permutation -> permutation,
				'secret -> secret
			).executeInsert(scalar[Long].single)
		}

	def findQuestionImgPathById(id: Long): String =
		db.withConnection { implicit c =>
			SQL("SELECT snippet_filename FROM question q,permutations p WHERE q.id = {id} AND q.permutation = p.id").on(
				'id -> id
			).as(str("snippet_filename").single)
		}
}
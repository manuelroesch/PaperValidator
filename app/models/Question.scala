package models

import anorm.SqlParser._
import anorm._
import org.joda.time.DateTime
import play.api.Play.current
import play.api.db.DB

/**
  * Created by mattia on 02.07.15.
  */
case class Question(id: Pk[Long], html: String, batchId: Long, createTime: DateTime, uuid: String, secret: String)

object QuestionDAO {
	private val questionParser: RowParser[Question] =
		get[Pk[Long]]("id") ~
				get[String]("html") ~
				get[Long]("batch_id") ~
				get[DateTime]("create_time") ~
				get[String]("uuid") ~ get[String]("secret") map {
			case id ~ html ~ batch_id ~ create_time ~ uuid ~ secret =>
				Question(id, html, batch_id, create_time, uuid, secret)
		}


	def findById(id: Long): Option[Question] =
		DB.withConnection { implicit c =>
			SQL("SELECT * FROM question WHERE id = {id}").on(
				'id -> id
			).as(questionParser.singleOpt)
		}

	def findIdByUUID(uuid: String): Long = {
		try {
			DB.withConnection { implicit c =>
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
			DB.withConnection { implicit c =>
				SQL("SELECT q.* FROM question q INNER JOIN question2assets q2a WHERE q2a.asset_id = {assetId}").on(
					'assetId -> assetId
				).as(questionParser *)
			}
		} catch {
			case e: Exception => Nil
		}
	}

}
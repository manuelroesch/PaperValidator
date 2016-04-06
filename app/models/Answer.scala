package models

import anorm.SqlParser._
import anorm._
import anorm.JodaParameterMetaData._
import org.joda.time.DateTime
import play.api.Play.current
import play.api.db.DB

/**
  * Created by mattia on 02.07.15.
  */
case class Answer(id: Pk[Long], questionId: Long, userId: Long, time: DateTime, answerJson: String, expectedOutputCode: Long, accepted: Boolean) extends Serializable

object AnswerDAO {
	private val answerParser: RowParser[Answer] =
		get[Pk[Long]]("id") ~
				get[Long]("question_id") ~
				get[Long]("user_id") ~
				get[DateTime]("time") ~
				get[String]("answer_json") ~
				get[Long]("expected_output_code") ~
				get[Boolean]("accepted") map {
			case id ~ question_id ~ user_id ~ time ~ answer_json ~ expected_output_code ~ accepted =>
				Answer(id, question_id, user_id, time, answer_json, expected_output_code, accepted)
		}

	def findById(id: Long): Option[Answer] =
		DB.withConnection { implicit c =>
			SQL("SELECT * FROM answer WHERE id = {id}").on(
				'id -> id
			).as(answerParser.singleOpt)
		}

	def findAllByQuestionId(questionId: Long): List[Answer] =
		DB.withConnection { implicit c =>
			SQL("SELECT * FROM answer WHERE question_id = {questionId}").on(
				'questionId -> questionId
			).as(answerParser *)
		}

	def findByUserId(userId: Long): List[Answer] =
		DB.withConnection { implicit c =>
			SQL("SELECT * FROM answer WHERE user_id = {userId}").on(
				'userId -> userId
			).as(answerParser *)
		}

	def create(questionId: Long, userId: Long, time: DateTime, answerJson: String, expected_output_code: Long, accepted: Boolean = false) =
		DB.withConnection { implicit c =>
			SQL("INSERT INTO answer(question_id, user_id, time, answer_json, expected_output_code, accepted) VALUES ({questionId}, {userId}, {time}, {answerJson}, {expected_output_code}, {accepted})").on(
				'questionId -> questionId,
				'userId -> userId,
				'time -> time,
				'answerJson -> answerJson,
				'expected_output_code -> expected_output_code,
				'accepted -> accepted
			).executeInsert()
		}

	def countUserAnswersForBatch(userId: Long, batchId: Long): Int = {
		DB.withConnection { implicit c =>
			SQL("SELECT * FROM answer a INNER JOIN question q ON a.question_id = q.id WHERE a.user_id = {userId}  AND q.batch_id = {batchId} ").on(
				'userId -> userId,
				'batchId -> batchId
			).as(answerParser *).size
		}
	}

	def countAnswersForQuestion(questionId: Long): Int = {
		DB.withConnection { implicit c =>
			SQL("SELECT * FROM answer as a WHERE a.question_id = {questionId}").on(
				'questionId -> questionId
			).as(answerParser *).size
		}
	}

	def existsAnswerForQuestionAndUser(userId: Long, questionId: Long): Boolean = {
		DB.withConnection { implicit c =>
			SQL("SELECT * FROM answer WHERE question_id = {questionId} AND user_id = {userId} ").on(
				'userId -> userId,
				'questionId -> questionId
			).as(answerParser *).size != 0
		}
	}

	def existsAcceptedAnswerForQuestionId(questionId: Long): Boolean = {
		findAllByQuestionId(questionId).exists(answer => answer.accepted == true)
	}

}
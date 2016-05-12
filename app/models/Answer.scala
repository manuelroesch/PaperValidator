package models

import javax.inject.{Singleton, Inject}

import anorm.SqlParser._
import anorm._
import anorm.JodaParameterMetaData._
import org.joda.time.DateTime
import play.api.Logger
import play.api.db.Database

/**
  * Created by mattia on 02.07.15.
  */
case class Answer(id: Option[Long], questionId: Long, userId: Long, time: DateTime, isRelated: Boolean,
									isCheckedBefore: Boolean, extraAnswer: Boolean, answerJson: String, expectedOutputCode: Long,
									accepted: Boolean) extends Serializable
case class AnswerM2A(method: String, assumption: String, isRelated: Boolean, isCheckedBefore: Boolean,
										 extraAnswer: Boolean, flag: Int) extends Serializable

class AnswerService @Inject()(db:Database) {

	private val answerParser: RowParser[Answer] =
		get[Option[Long]]("id") ~
				get[Long]("question_id") ~
				get[Long]("user_id") ~
				get[DateTime]("time") ~
				get[Boolean]("is_related") ~
				get[Boolean]("is_checked_before") ~
				get[Boolean]("extra_answer") ~
				get[String]("answer_json") ~
				get[Long]("expected_output_code") ~
				get[Boolean]("accepted") map {
				case id ~ question_id ~ user_id ~ time ~ is_related ~ is_checked_before ~ extra_answer ~ answer_json ~ expected_output_code ~ accepted =>
					Answer(id, question_id, user_id, time, is_related, is_checked_before, extra_answer, answer_json,expected_output_code, accepted)
		}

	private val answerM2AParser: RowParser[AnswerM2A] =
		get[String]("method") ~
			get[String]("assumption") ~
			get[Boolean]("is_related") ~
			get[Boolean]("is_checked_before") ~
			get[Boolean]("extra_answer") ~
			get[Int]("flag") map {
			case method ~ assumption ~ isRelated ~ isCheckedBefore ~ extraAnswer ~ flag =>
				AnswerM2A(method, assumption, isRelated, isCheckedBefore, extraAnswer, flag)
		}

	def findById(id: Long): Option[Answer] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM answer WHERE id = {id}").on(
				'id -> id
			).as(answerParser.singleOpt)
		}

	def findAllByQuestionId(questionId: Long): List[Answer] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM answer WHERE question_id = {questionId}").on(
				'questionId -> questionId
			).as(answerParser *)
		}

	def findByUserId(userId: Long): List[Answer] =
		db.withConnection { implicit c =>
			SQL("SELECT * FROM answer WHERE user_id = {userId}").on(
				'userId -> userId
			).as(answerParser *)
		}

	def findByPaperId(paperId: Int): List[AnswerM2A] = {
		db.withConnection { implicit c =>
			val answers = SQL("SELECT method_index method,group_name assumption, a.is_related, is_checked_before, extra_answer, 0 flag " +
				"FROM question q,answer a,permutations pe,papers pa " +
				"WHERE q.id = a.question_id AND q.permutation = pe.id AND pe.paper_id = pa.id AND pa.id = {paper_id}").on(
				'paper_id -> paperId
			).as(answerM2AParser *)
			answers.map(answer => {
				val method = answer.method.split("_")(0)
				val assumption = answer.assumption.split("/")(1)
				new AnswerM2A(method, assumption, answer.isRelated, answer.isCheckedBefore, answer.extraAnswer, answer.flag)
			})
		}
	}

	def countAnswersByConferenceTotal(conferenceId: Int): Int = {
		db.withConnection { implicit c =>
			SQL("SELECT count(*)" +
				"FROM question q,answer a,permutations pe,papers pa " +
				"WHERE q.id = a.question_id AND q.permutation = pe.id AND pe.paper_id = pa.id " +
				"AND a.is_related AND pa.conference_id = {conference_id}").on(
				'conference_id -> conferenceId
			).as(scalar[Int].single)
		}
	}

	def countAnswersByConferencePaper(conferenceId: Int): Int = {
		db.withConnection { implicit c =>
			SQL("SELECT count(DISTINCT pa.id)" +
				"FROM question q,answer a,permutations pe,papers pa " +
				"WHERE q.id = a.question_id AND q.permutation = pe.id AND pe.paper_id = pa.id " +
				"AND a.is_related AND pa.conference_id = {conference_id}").on(
				'conference_id -> conferenceId
			).as(scalar[Int].single)
		}
	}

	def create(questionId: Long, userId: Long, time: DateTime, isRelated: Boolean, isCheckedBefore: Boolean,
						 extraAnswer: Boolean, confidence: Int, answerJson: String, expected_output_code: Long, accepted: Boolean = false) =
		db.withConnection { implicit c =>
			SQL("INSERT INTO answer(question_id, user_id, time, is_related, is_checked_before, extra_answer, confidence, " +
				"answer_json, expected_output_code, accepted) " +
				"VALUES ({question_id}, {user_id}, {time}, {is_related}, {is_checked_before}, {extra_answer}, {confidence}, " +
				"{answer_json}, {expected_output_code}, {accepted})").on(
				'question_id -> questionId,
				'user_id -> userId,
				'time -> time,
				'is_related -> isRelated,
				'is_checked_before -> isCheckedBefore,
				'extra_answer -> extraAnswer,
				'confidence -> confidence,
				'answer_json -> answerJson,
				'expected_output_code -> expected_output_code,
				'accepted -> accepted
			).executeInsert()
		}

	def countUserAnswersForBatch(userId: Long, batchId: Long): Int = {
		db.withConnection { implicit c =>
			SQL("SELECT * FROM answer a INNER JOIN question q ON a.question_id = q.id WHERE a.user_id = {userId} " +
				"AND q.batch_id = {batchId} ").on(
				'userId -> userId,
				'batchId -> batchId
			).as(answerParser *).size
		}
	}

	def countAnswersForQuestion(questionId: Long): Int = {
		db.withConnection { implicit c =>
			SQL("SELECT * FROM answer as a WHERE a.question_id = {questionId}").on(
				'questionId -> questionId
			).as(answerParser *).size
		}
	}

	def existsAnswerForQuestionAndUser(userId: Long, questionId: Long): Boolean = {
		db.withConnection { implicit c =>
			SQL("SELECT * FROM answer WHERE question_id = {questionId} AND user_id = {userId} ").on(
				'userId -> userId,
				'questionId -> questionId
			).as(answerParser *).size != 0
		}
	}

	def existsAcceptedAnswerForQuestionId(questionId: Long): Boolean = {
		findAllByQuestionId(questionId).exists(answer => answer.accepted == true)
	}

	def getAll(): List[Answer] = {
		db.withConnection { implicit c =>
			SQL("SELECT * FROM answer").as(answerParser *)
		}
	}

}
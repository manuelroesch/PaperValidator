package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.{Answer, Permutation, Question}
import org.joda.time.DateTime
import scalikejdbc._

/**
 * Created by mattia on 06.07.15.
 */
class BallotDAO extends DAO {

  override def countAllAnswers(): Int = {
    DB readOnly { implicit session =>
      sql"SELECT count(*) AS count FROM answer".map(rs => rs.int("count")).single().apply().get
    }
  }

  override def countAllBatches(): Int = {
    DB readOnly { implicit session =>
      sql"SELECT count(*) AS count FROM batch".map(rs => rs.int("count")).single().apply().get
    }
  }

  override def countAllQuestions(): Int = {
    DB readOnly { implicit session =>
      sql"SELECT count(*) AS count FROM question".map(rs => rs.int("count")).single().apply().get
    }
  }

  override def createBatch(allowedAnswersPerTurker: Int, uuid: UUID): Long = {
    DB localTx { implicit session =>
      sql"INSERT INTO batch(allowed_answers_per_turker, uuid) VALUES(${allowedAnswersPerTurker}, ${uuid.toString})".updateAndReturnGeneratedKey().apply()
    }
  }

  override def getAnswerByQuestionId(questionId: Long): Option[String] = {
    DB readOnly { implicit session =>
      sql"SELECT answer_json FROM answer WHERE question_id = ${questionId}".map(rs => rs.string("answer_json")).single.apply()
    }
  }

  override def getAnswerIdByOutputCode(insertOutput: String): Option[Long] = {
    DB readOnly { implicit session =>
      sql"SELECT id FROM answer WHERE expected_output_code = ${insertOutput.toLong}".map(rs => rs.long("id")).single().apply()
    }
  }

  override def getAnswerById(id: Long): Option[Answer] = {
    DB readOnly { implicit session =>
      sql"SELECT * FROM answer WHERE id = ${id}".map(rs => Answer(rs.long("id"), rs.jodaDateTime("time"), rs.long("question_id"), rs.string("answer_json"), rs.boolean("accepted"))).single().apply()
    }
  }

  override def getExpectedOutputCodeFromAnswerId(ansId: Long): Option[Long] = {
    DB readOnly { implicit session =>
      sql"SELECT expected_output_code FROM answer WHERE id = ${ansId}".map(rs => rs.long("expected_output_code")).single().apply()
    }
  }

  override def createQuestion(html: String, batchId: Long, uuid: UUID = UUID.randomUUID(), dateTime: DateTime = new DateTime(), permutationId: Long, secret: String = ""): Long = {
    DB localTx { implicit session =>
      sql"INSERT INTO question(batch_id, html, create_time, uuid, permutation, secret) VALUES(${batchId}, ${html}, ${dateTime}, ${uuid.toString}, ${permutationId}, ${secret})".updateAndReturnGeneratedKey().apply()
    }
  }

  override def getQuestionIdByUUID(uuid: String): Option[Long] = {
    DB readOnly { implicit session =>
      sql"SELECT id FROM question WHERE uuid = ${uuid}".map(rs => rs.long("id")).single().apply()
    }
  }

  override def getQuestionUUID(questionId: Long): Option[String] = {
    DB readOnly { implicit session =>
      sql"SELECT uuid FROM question WHERE id = ${questionId}".map(rs => rs.string("uuid")).single().apply()
    }
  }

  override def getBatchIdByUUID(uuid: UUID): Option[Long] = {
    DB readOnly { implicit session =>
      sql"SELECT id FROM batch WHERE uuid = ${uuid.toString}".map(rs => rs.long("id")).single().apply()
    }
  }

  override def createAsset(binary: Array[Byte], contentType: String, filename: String): Long = {
    val hashCode = java.security.MessageDigest.getInstance("SHA-1").digest(binary).map("%02x".format(_)).mkString

    val possibleMatch = findAssetsIdByHashCode(hashCode).map(id => id -> getAssetsContentById(id))
        .find(p => p._2.equalsIgnoreCase(contentType))

    val id = if (possibleMatch.nonEmpty) {
      possibleMatch.get._1
    } else {
      DB localTx { implicit session =>
        sql"INSERT INTO assets(hash_code, byte_array, content_type, filename) VALUES(${hashCode}, ${binary}, ${contentType}, ${filename})"
            .updateAndReturnGeneratedKey().apply()
      }
    }
    id
  }

  override def mapQuestionToAssets(qId: Long, assetId: Long): Long = {
    DB localTx { implicit session =>
      sql"INSERT INTO question2assets(question_id, asset_id) VALUES(${qId}, ${assetId})".updateAndReturnGeneratedKey().apply()
    }
  }

  override def getAssetsContentById(id: Long): String = {
    DB readOnly { implicit session =>
      sql"SELECT content_type FROM assets WHERE id = ${id}".map(rs =>
        rs.string("content_type")).single().apply().get
    }
  }

  override def findAssetsIdByHashCode(hc: String): List[Long] = {
    DB readOnly { implicit session =>
      sql"SELECT id FROM assets WHERE hash_code = ${hc}".map(rs => rs.long("id")).list().apply()
    }
  }

  override def updateAnswer(answerId: Long, accepted: Boolean) = {
    DB localTx { implicit session =>
      sql"UPDATE answer SET accepted = ${accepted} WHERE id = ${answerId}"
          .update().apply()
    }
  }

  override def getAssetIdsByQuestionId(questionId: Long): List[Long] = {
    DB readOnly { implicit session =>
      sql"SELECT * FROM question2assets WHERE question_id = ${questionId}".map(rs => rs.long("asset_id")).list().apply()
    }
  }

  override def loadPermutationsCSV(csv: String): Boolean = {
    DB localTx { implicit session =>

      val time = new DateTime()
      sql"""LOAD DATA LOCAL INFILE ${csv}
      INTO TABLE permutations
      COLUMNS TERMINATED BY ','
      OPTIONALLY ENCLOSED BY '"'
      ESCAPED BY '"'
      LINES TERMINATED BY '\n'
      IGNORE 1 LINES
        (group_name, method_index, snippet_filename, pdf_path, method_on_top ,relative_height_top, relative_height_bottom)
        SET create_time = $time""".update().apply()
    }
    true
  }

  override def createPermutation(permutation: Permutation): Long = {
    DB localTx { implicit session =>
      sql"""INSERT INTO permutations(create_time, group_name, method_index, snippet_filename, pdf_path, method_on_top, relative_height_top, relative_height_bottom)
      VALUES(NOW(), ${permutation.groupName}, ${permutation.methodIndex}, ${permutation.snippetFilename}, ${permutation.pdfPath}, ${permutation.methodOnTop},
      ${permutation.relativeHeightTop}, ${permutation.relativeHeightBottom})"""
          .updateAndReturnGeneratedKey().apply()
    }
  }

  override def getAllPermutations(): List[Permutation] = {
    DB readOnly { implicit session =>
      sql"SELECT * FROM permutations".map(rs =>
        Permutation(rs.long("id"), rs.string("group_name"), rs.string("method_index"), rs.string("snippet_filename"), rs.string("pdf_path"), rs.boolean("method_on_top"), rs.long("state"), rs.int("excluded_step"), rs.double("relative_height_top"), rs.double("relative_height_bottom"), rs.long("distanceMinIndexMax"))
      ).list().apply()
    }
  }

  override def getPermutationById(id: Long): Option[Permutation] = {
    DB readOnly { implicit session =>
      sql"SELECT * FROM permutations WHERE id = ${id}".map(rs =>
        Permutation(rs.long("id"), rs.string("group_name"), rs.string("method_index"), rs.string("snippet_filename"), rs.string("pdf_path"), rs.boolean("method_on_top"), rs.long("state"), rs.int("excluded_step"), rs.double("relative_height_top"), rs.double("relative_height_bottom"), rs.long("distanceMinIndexMax"))
      ).single().apply()
    }
  }

  override def getAllOpenByGroupName(groupName: String): List[Permutation] = {
    DB readOnly { implicit session =>
      sql"SELECT * FROM permutations WHERE group_name = ${groupName} AND state = 0".map(rs =>
        Permutation(rs.long("id"), rs.string("group_name"), rs.string("method_index"), rs.string("snippet_filename"), rs.string("pdf_path"), rs.boolean("method_on_top"), rs.long("state"), rs.int("excluded_step"), rs.double("relative_height_top"), rs.double("relative_height_bottom"), rs.long("distanceMinIndexMax"))
      ).list().apply()
    }
  }

  override def updateStateOfPermutationId(id: Long, becauseOfId: Long, excludedByStep: Int = 0) {
    DB localTx { implicit session =>
      sql"UPDATE permutations SET state = ${becauseOfId}, excluded_step = ${excludedByStep} WHERE id = ${id}"
          .update().apply()
    }
  }

  override def getAllOpenGroupsStartingWith(partialGroupName: String): List[Permutation] = {
    getAllPermutationsWithStateEquals(0).filter(r => r.groupName.startsWith(partialGroupName))
  }

  override def getAllQuestions: List[Question] = {
    DB readOnly { implicit session =>
      sql"SELECT * FROM question".map(rs => Question(rs.long("id"), rs.long("permutation"))).list().apply()
    }
  }

  override def getAllPermutationsWithStateEquals(state: Long): List[Permutation] = {
    DB readOnly { implicit session =>
      sql"SELECT * FROM permutations WHERE state = ${state}".map(rs =>
        Permutation(rs.long("id"), rs.string("group_name"), rs.string("method_index"), rs.string("snippet_filename"), rs.string("pdf_path"), rs.boolean("method_on_top"), rs.long("state"), rs.int("excluded_step"), rs.double("relative_height_top"), rs.double("relative_height_bottom"), rs.long("distanceMinIndexMax"))).list().apply()
    }
  }

  override def allAnswers(): List[Answer] = {
    DB readOnly { implicit session =>
      sql"SELECT * FROM answer WHERE accepted = 1".map(rs =>
        Answer(rs.long("id"), rs.jodaDateTime("time"), rs.long("question_id"), rs.string("answer_json"), rs.boolean("accepted"))
      ).list().apply()
    }
  }

  override def getPermutationIdByQuestionId(qId: Long): Option[Long] = {
    DB readOnly { implicit session =>
      sql"SELECT permutation FROM question WHERE id = ${qId}".map(rs =>
        rs.long("permutation")).single().apply()
    }
  }

  override def getAllAnswersForSnippet(fileName: String): List[Answer] = {
    allAnswers.filter(f => f.answerJson.contains(fileName))
  }

  override def getQuestionIDsAnsweredSince(date: DateTime): List[Long] = DB readOnly { implicit session =>
    sql"SELECT question_id FROM answer WHERE time > ${date}".map(rs => rs.long("question_id")).list().apply()
  }
}

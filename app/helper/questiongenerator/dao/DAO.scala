package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.dao

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence.{Answer, Permutation, Question}
import org.joda.time.DateTime

/**
  * Created by mattia on 06.07.15.
  */
trait DAO {

	def getQuestionIDsAnsweredSince(date: DateTime): List[Long]

	def getAssetIdsByQuestionId(questionId: Long): List[Long]

	def createAsset(binary: Array[Byte], contentType: String, filename: String): Long

	def createBatch(allowedAnswerPerTurker: Int, uuid: UUID): Long

	def createQuestion(html: String, batchId: Long, uuid: UUID = UUID.randomUUID(), dateTime: DateTime = new DateTime(), permutationId: Long, secret: String): Long

	def getAnswerByQuestionId(questionId: Long): Option[String]

	def getQuestionUUID(questionId: Long): Option[String]

	def getBatchIdByUUID(uuid: UUID): Option[Long]

	def updateAnswer(answerId: Long, accepted: Boolean)

	def getAnswerById(id: Long): Option[Answer]

	def getAnswerIdByOutputCode(insertOutput: String): Option[Long]

	def getExpectedOutputCodeFromAnswerId(ansId: Long): Option[Long]

	def getQuestionIdByUUID(uuid: String): Option[Long]

	def countAllAnswers(): Int

	def countAllBatches(): Int

	def countAllQuestions(): Int

	def getAllQuestions: List[Question]

	def getAssetPDFFileNameByQuestionId(qId: Long): Option[String] = {
		val perm: Long = getPermutationIdByQuestionId(qId).getOrElse(-1L)
		getAllPermutations().find(p => p.id == perm).map(p => p.groupName.substring(0, p.groupName.indexOf("/")))
	}

	def allAnswers(): List[Answer]

	def mapQuestionToAssets(qId: Long, assetId: Long): Long

	def getAssetsContentById(id: Long): String

	def findAssetsIdByHashCode(hashCode: String): List[Long]

	def loadPermutationsCSV(csv: String, paperId: Long): Boolean

	def createPermutation(permutation: Permutation, paperId: Long): Long

	def getAllPermutations(): List[Permutation]

	def getPermutationById(id: Long): Option[Permutation]

	def getAllOpenByGroupName(groupName: String): List[Permutation]

	def updateStateOfPermutationId(id: Long, becauseOfId: Long, excludedByStep: Int = 0)

	def getAllOpenGroupsStartingWith(partialGroupName: String): List[Permutation]

	def getAllPermutationsWithStateEquals(state: Long): List[Permutation]

	def getPermutationIdByQuestionId(qId: Long): Option[Long]

	def getAllAnswersForSnippet(fileName: String): List[Answer]

}

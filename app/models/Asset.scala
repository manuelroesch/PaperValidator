package models

import java.sql.SQLException

import anorm.SqlParser._
import anorm._
import com.mysql.jdbc.Blob
import play.api.Play.current
import play.api.db.DB

/**
  * Created by mattia on 02.07.15.
  */
case class Asset(id: Pk[Long], hash_code: String, byteArray: Array[Byte], contentType: String, filename: String) extends Serializable

case class Question2Assets(id: Pk[Long], questionId: Long, assetId: Long) extends Serializable

object AssetDAO {
	private val assetParser: RowParser[Asset] =
		get[Pk[Long]]("id") ~
			get[String]("hash_code") ~
			bytes("byte_array") ~
			get[String]("content_type") ~
			get[String]("filename") map {
			case id ~ hash_code ~ byte_array ~ content_type ~ filename =>
				Asset(id, hash_code, byte_array, content_type, filename)
		}

	private val question2AssetsParser: RowParser[Question2Assets] =
		get[Pk[Long]]("id") ~
			get[Long]("question_id") ~
			get[Long]("asset_id") map {
			case id ~ question_id ~ asset_id =>
				Question2Assets(id, question_id, asset_id)
		}

	/**
	  * Attempt to convert a SQL value into a byte array.
	  */
	private def valueToByteArrayOption(value: Any): Option[Array[Byte]] = {
		value match {
			case bytes: Array[Byte] => Some(bytes)
			case blob: Blob => try {
				Some(blob.getBytes(1, blob.length.asInstanceOf[Int]))
			}
			catch {
				case e: SQLException => None
			}
			case _ => None
		}
	}

	/**
	  * Implicitly convert an Anorm row to a byte array.
	  */
	def rowToByteArray: Column[Array[Byte]] = {
		Column.nonNull[Array[Byte]] { (value, meta) =>
			val MetaDataItem(qualified, nullable, clazz) = meta
			valueToByteArrayOption(value) match {
				case Some(bytes) => Right(bytes)
				case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Byte Array for column " + qualified))
			}
		}
	}

	/**
	  * Build a RowParser factory for a byte array column.
	  */
	def bytes(columnName: String): RowParser[Array[Byte]] = {
		get[Array[Byte]](columnName)(rowToByteArray)
	}

	def findById(id: Long): Option[Asset] =
		DB.withConnection { implicit c =>
			SQL("SELECT * FROM assets WHERE id = {id}").on(
				'id -> id
			).as(assetParser.singleOpt)
		}

	def findByQuestionId(questionId: Long): List[Asset] = {
		val assetIds = DB.withConnection { implicit c =>
			SQL("SELECT * FROM question2assets WHERE question_id = {questionId}").on(
				'questionId -> questionId
			).as(question2AssetsParser *)
		}

		assetIds.map(q2a => {
			findById(q2a.assetId).get
		})
	}

	def getAllIdByQuestionId(questionId: Long): List[Long] = findByQuestionId(questionId).map(_.id.get)

	def getAllAssetsByQuestionId(questionId: Long): List[Asset] = findByQuestionId(questionId)
}
package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence

import org.joda.time.DateTime

/**
  * Created by mattia on 24.08.15.
  */
case class Answer(id: Long, time: DateTime, questionId: Long, answerJson: String, accepted: Boolean)

case class Permutation(id: Long, groupName: String, methodIndex: String, snippetFilename: String, pdfPath: String,
					   methodOnTop: Boolean, state: Long, excluded_step: Int, relativeHeightTop: Double, relativeHeightBottom: Double, distanceMinIndexMax: Long)

object Permutation {
	def fromCSVLine(line: String, idToGive: Long = -1): Permutation = {
		val cols = line.split(",")
		//group_name,method_index,snippet_filename,pdf_path,method_on_top,relative_height_top,relative_height_bottom
		Permutation(idToGive, cols(0), cols(1), cols(2), cols(3), cols(4) == "1", 0, 0, cols(5).toDouble, cols(6).toDouble, cols(7).toLong)
	}
}

case class Question(id: Long, permutationId: Long)
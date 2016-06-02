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
case class Permutations(id: Option[Long], createTime: DateTime, groupName: String, methodIndex: String,
												snippetFilename: String, pdfPath: String, methodOnTop: Boolean, state: Long, excludedStep: Int,
												relHeightTop: Double, relHeightBottom: Double, distanceMinIndexMax: Long, paperId: Int)

class PermutationsServcie @Inject()(db:Database) {

	def create(groupName: String, methodIndex: String, paperId: Int) : Long =
		db.withConnection { implicit c =>
			SQL("INSERT INTO permutations(create_time, group_name, method_index, snippet_filename,pdf_path, method_on_top, " +
				"state, excluded_step, relative_height_top, relative_height_bottom, distanceMinIndexMax, paper_id) " +
				"VALUES ({create_time},{group_name},{method_index},'','',false,0,0,0.0,0.0,0,{paper_id})").on(
				'create_time -> DateTime.now(),
				'group_name -> groupName,
				'method_index -> methodIndex,
				'paper_id -> paperId
			).executeInsert(scalar[Long].single)
		}
}
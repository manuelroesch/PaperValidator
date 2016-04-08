package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence

import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import scalikejdbc._

/**
  * Created by mattia on 07.07.15.
  */
object DBInitializer extends LazyLogger {

	def run() {
		DB readOnly { implicit s =>
			//user TABLE
			try {
				sql"SELECT 1 FROM user LIMIT 1".map(_.long(1)).single.apply()
				logger.debug("Table user already initialized")
			}
			catch {
				case e: java.sql.SQLException =>
					DB autoCommit { implicit s =>
						sql"CREATE TABLE user (id BIGINT NOT NULL AUTO_INCREMENT,turker_id VARCHAR(255) NOT NULL, first_seen_date_time DATETIME NOT NULL, PRIMARY KEY(id));".execute().apply()
						logger.debug("Table user created")
					}
			}

			//batch TABLE
			try {
				sql"SELECT 1 FROM batch LIMIT 1".map(_.long(1)).single.apply()
				logger.debug("Table batch already initialized")
			}
			catch {
				case e: java.sql.SQLException =>
					DB autoCommit { implicit s =>
						sql"CREATE TABLE batch (id BIGINT NOT NULL AUTO_INCREMENT,allowed_answers_per_turker INT NOT NULL, uuid VARCHAR(255) NOT NULL, PRIMARY KEY(id));".execute().apply()
						logger.debug("Table batch created")
					}
			}


			//log TABLE
			try {
				sql"SELECT 1 FROM log LIMIT 1".map(_.long(1)).single.apply()
				logger.debug("Table permutation already initialized")
			}
			catch {
				case e: java.sql.SQLException =>
					DB autoCommit { implicit s =>
						sql"CREATE TABLE log (id INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,`accesstime` DATETIME NOT NULL,`url` VARCHAR(1024) NOT NULL DEFAULT '',`ip` VARCHAR(254) NOT NULL DEFAULT '',`USER` INT(11) DEFAULT NULL,PRIMARY KEY (`id`),KEY `accesstime` (`accesstime`) ) ENGINE=InnoDB DEFAULT CHARSET=utf8;".execute().apply()
						logger.debug("Table log created")
					}
			}

			//permutations TABLE
			try {
				sql"SELECT 1 FROM permutations LIMIT 1".map(_.long(1)).single.apply()
				logger.debug("Table permutation already initialized")
			}
			catch {
				case e: java.sql.SQLException =>
					DB autoCommit { implicit s =>
						sql"CREATE TABLE permutations (id BIGINT NOT NULL AUTO_INCREMENT, create_time DATETIME NOT NULL, group_name VARCHAR(255) NOT NULL, method_index VARCHAR(255) NOT NULL, snippet_filename VARCHAR(255) NOT NULL, pdf_path VARCHAR(255) NOT NULL, method_on_top BOOL NOT NULL, state BIGINT NOT NULL DEFAULT 0, excluded_step INT DEFAULT 0, relative_height_top DOUBLE(5,2) NOT NULL, relative_height_bottom DOUBLE(5,2) NOT NULL, distanceMinIndexMax BIGINT NOT NULL DEFAULT 0, PRIMARY KEY(id));".execute().apply()
						logger.debug("Table permutations created")
					}
			}

			//Question TABLE
			try {
				sql"SELECT 1 FROM question LIMIT 1".map(_.long(1)).single.apply()
				logger.debug("Table question already initialized")
			}
			catch {
				case e: java.sql.SQLException =>
					DB autoCommit { implicit s =>
						sql"CREATE TABLE question (id BIGINT NOT NULL AUTO_INCREMENT, batch_id BIGINT NOT NULL, html LONGTEXT NOT NULL, create_time DATETIME NOT NULL, uuid VARCHAR(255) NOT NULL, permutation BIGINT NOT NULL, secret VARCHAR(1024) NOT NULL, PRIMARY KEY(id), FOREIGN KEY(batch_id) REFERENCES batch(id), FOREIGN KEY(permutation) REFERENCES permutations(id));".execute().apply()
						logger.debug("Table question created")
					}
			}

			//assets TABLE
			try {
				sql"SELECT 1 FROM assets LIMIT 1".map(_.long(1)).single.apply()
				logger.debug("Table assets already initialized")
			}
			catch {
				case e: java.sql.SQLException =>
					DB autoCommit { implicit s =>
						sql"CREATE TABLE assets (id BIGINT NOT NULL AUTO_INCREMENT, hash_code VARCHAR(255) NOT NULL, byte_array LONGBLOB NOT NULL, content_type VARCHAR(255) NOT NULL, filename VARCHAR(300) NOT NULL, PRIMARY KEY(id));".execute().apply()
						logger.debug("Table assets created")
					}
			}

			//question2assets TABLE
			try {
				sql"SELECT 1 FROM question2assets LIMIT 1".map(_.long(1)).single.apply()
				logger.debug("Table question2assets already initialized")
			}
			catch {
				case e: java.sql.SQLException =>
					DB autoCommit { implicit s =>
						sql"CREATE TABLE question2assets (id BIGINT NOT NULL AUTO_INCREMENT, question_id BIGINT NOT NULL, asset_id BIGINT NOT NULL, PRIMARY KEY(id), FOREIGN KEY(question_id) REFERENCES question(id), FOREIGN KEY(asset_id) REFERENCES assets(id));".execute().apply()
						logger.debug("Table question2assets created")
					}
			}

			//answer TABLE
			try {
				sql"SELECT 1 FROM answer LIMIT 1".map(_.long(1)).single.apply()
				logger.debug("Table answer already initialized")
			}
			catch {
				case e: java.sql.SQLException =>
					DB autoCommit { implicit s =>
						sql"CREATE TABLE answer (id BIGINT NOT NULL AUTO_INCREMENT, question_id BIGINT NOT NULL, user_id BIGINT NOT NULL, time DATETIME NOT NULL, answer_json LONGTEXT NOT NULL, expected_output_code BIGINT NOT NULL, accepted BOOL NOT NULL DEFAULT 0, PRIMARY KEY(id), FOREIGN KEY(question_id) REFERENCES question(id), FOREIGN KEY(user_id) REFERENCES user(id),  KEY `time` (`time`));".execute().apply()
						logger.debug("Table answer created")
					}
			}
		}
	}
}

package models

import javax.inject.Inject

import anorm._
import anorm.JodaParameterMetaData._
import org.joda.time.DateTime
import play.api.db.DBApi

/**
  * Created by pdeboer on 20/11/15.
  */
@javax.inject.Singleton
class Log @Inject()(dbapi: DBApi) {

	private val db = dbapi.database("default")

	def createEntry(url: String, ip: String, userId: Long): Unit = {
		db.withConnection { implicit c =>
			SQL("INSERT INTO log (accesstime, url, ip, users) VALUES (NOW(), {url}, {ip}, {userId})").on(
				'url -> url,
				'ip -> ip,
				'userId -> userId
			).executeInsert()
		}
	}

	def ipLogEntriesSince(ip: String, since: DateTime): Either[List[Throwable], Long] = {
		db.withConnection { implicit c =>
			SQL("SELECT count(*) AS cnt FROM log WHERE accesstime >= {accesstime} AND ip = {ip}").on(
				'accesstime -> since,
				'ip -> ip
			).fold(0L) { (c, _) => c + 1 }
		}
	}
}

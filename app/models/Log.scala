package models

import javax.inject.{Inject, Singleton}

import anorm.JodaParameterMetaData._
import anorm._
import org.joda.time.DateTime
import play.api.db.Database

/**
  * Created by pdeboer on 20/11/15.
  */
class Log @Inject()(db:Database) {

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

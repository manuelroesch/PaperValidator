package models

import anorm._
import org.joda.time.DateTime
import play.api.Play.current
import play.api.db.DB

/**
  * Created by pdeboer on 20/11/15.
  */
object Log {
	def createEntry(url: String, ip: String, userId: Long): Unit = {
		DB.withConnection { implicit c =>
			SQL("INSERT INTO log (accesstime, url, ip, user) VALUES (NOW(), {url}, {ip}, {userId})").on(
				'url -> url,
				'ip -> ip,
				'userId -> userId
			).executeInsert()
		}
	}

	def ipLogEntriesSince(ip: String): Long = {
		val head = DB.withConnection { implicit c =>
			SQL("SELECT count(*) AS cnt FROM log WHERE accesstime >= {accesstime} AND ip = {ip}").on(
				'ip -> ip
			).apply().head
		}
		head[Long]("cnt")
	}
}

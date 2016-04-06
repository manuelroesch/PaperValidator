package models

import anorm._
import org.joda.time.DateTime
import play.api.Play.current
import play.api.db.DB

/**
  * Created by pdeboer on 20/11/15.
  */
object Log {
	def createEntry(url: String, ip: String, userId: Int): Unit = {
		DB.withConnection { implicit c =>
			SQL("INSERT INTO log (accesstime, url, ip) VALUES (NOW(), {url}, {ip})").on(
				'url -> url,
				'ip -> ip
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

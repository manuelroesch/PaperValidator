package controllers

import models.UserDAO
import org.joda.time.DateTime
import play.api.mvc._

class Login extends Controller {

	def logout = Action { request =>
		Ok(views.html.login()).withNewSession
	}

	def login = Action { request =>
		val turkerId = getTurkerIDFromRequest(request)

		if (UserDAO.findByTurkerId(turkerId).isEmpty) {
			UserDAO.create(turkerId, new DateTime())
		}

		// Redirect if necessary otherwise just go to index
		request.session.get("redirect").map { redirect =>
			Redirect(redirect).withSession(request.session - "redirect" + (Application.TURKER_ID_KEY -> turkerId))
		}.getOrElse {
			Ok(views.html.index(turkerId)).withSession(request.session + (Application.TURKER_ID_KEY -> turkerId))
		}
	}


	def getTurkerIDFromRequest(request: Request[AnyContent]): String = {
		request.queryString.getOrElse("TurkerID", List("")).head
		//request.body.asFormUrlEncoded.get("TurkerID").head
	}
}
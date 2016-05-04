package controllers

import javax.inject.Inject

import models.UserService
import org.joda.time.DateTime
import play.api.mvc._

class Login @Inject() (userService: UserService) extends Controller {

	def logout = Action { request =>
		Ok(views.html.login()).withNewSession
	}

	def login = Action { request =>
		val turkerId = getTurkerIDFromRequest(request)

		if (userService.findByTurkerId(turkerId).isEmpty) {
			userService.create(turkerId, new DateTime())
		}

		// Redirect if necessary otherwise just go to index
		request.session.get("redirect").map { redirect =>
			Redirect(redirect).withSession(request.session - "redirect" + (Mturk.TURKER_ID_KEY -> turkerId))
		}.getOrElse {
			Ok(views.html.index()).withSession(request.session + (Mturk.TURKER_ID_KEY -> turkerId))
		}
	}


	def getTurkerIDFromRequest(request: Request[AnyContent]): String = {
		request.queryString.getOrElse("TurkerID", List("")).head
		//request.body.asFormUrlEncoded.get("TurkerID").head
	}
}
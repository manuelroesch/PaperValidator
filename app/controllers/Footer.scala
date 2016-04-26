package controllers

import play.api.mvc._

class Footer extends Controller {

	def faq = Action { request =>
		Ok(views.html.footer.faq())
	}

	def about = Action { request =>
		Ok(views.html.footer.about())
	}

	def impressum = Action { request =>
		Ok(views.html.footer.impressum())
	}


}
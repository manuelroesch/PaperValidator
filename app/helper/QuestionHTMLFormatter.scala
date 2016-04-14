package helper

import play.Configuration

/**
  * Created by pdeboer on 20/11/15.
  */
class QuestionHTMLFormatter(val html: String, val assetPrefix: String = Configuration.root().getString("url.prefix")) {
	def format = html.replaceAll("asset://", assetPrefix + "/assetsBallot/")
}

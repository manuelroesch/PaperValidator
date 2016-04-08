package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.snippet

import com.typesafe.scalalogging.LazyLogging

import scala.xml._

/**
 * Created by mattia on 02.09.15.
 */
case class SnippetHTMLValidator(baseURL: String) extends LazyLogging {

	def fixFormAttributes(ns: NodeSeq): NodeSeq = {
		val htmlToDisplayOnBallotPage: NodeSeq = ns(0).seq.map(updateForm(_))
		htmlToDisplayOnBallotPage
	}

	def updateForm(node: Node): Node = node match {
		case elem@Elem(_, "form", _, _, child@_*) => {
			elem.asInstanceOf[Elem] % Attribute(None, "action", Text(baseURL + "/storeAnswer"), Null) %
				Attribute(None, "method", Text("get"), Null) copy (child = child map updateForm)
		}
		case elem@Elem(_, _, _, _, child@_*) => {
			elem.asInstanceOf[Elem].copy(child = child map updateForm)
		}
		case other => other
	}

	def hasInvalidInputElements(form: NodeSeq): Boolean = {
		val supportedFields = List[(String, Map[String, List[String]])](
			"input" -> Map("type" -> List[String]("submit", "radio", "hidden")),
			"textarea" -> Map("name" -> List.empty[String]),
			"button" -> Map("type" -> List[String]("submit")),
			"select" -> Map("name" -> List.empty[String]))

		val definedSupportedFields = supportedFields.map(supportedField => {
			if ((form \\ supportedField._1).nonEmpty) {
				(form \\ supportedField._1) -> supportedField._2
			}
		}).collect { case fieldAttributes: (NodeSeq, Map[String, List[String]]) => fieldAttributes }

		if (definedSupportedFields.isEmpty) {
			logger.error("The form doesn't contain any input, select, textarea or button element.")
			true
		} else {
			definedSupportedFields.forall(htmlElement => !hasInvalidAttributes(htmlElement._1, htmlElement._2))
		}
	}

	private def hasInvalidAttributes(inputElement: NodeSeq, possibleValidAttributes: Map[String, List[String]]): Boolean = {
		possibleValidAttributes.exists(attribute => {
			inputElement.exists(element =>
				element.attribute(attribute._1).exists(attributeValue => {
					if (attribute._2.isEmpty) {
						true
					} else {
						attribute._2.contains(attributeValue.text) || attribute._2.isEmpty
					}
				})
			)
		})
	}

}

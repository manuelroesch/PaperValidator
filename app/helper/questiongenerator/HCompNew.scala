package helper.questiongenerator

import java.io.{File, FileInputStream}
import java.security.SecureRandom

import ch.uzh.ifi.pdeboer.pplib.hcomp.HComp._
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompPortal, HCompPortalAdapter}
import ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower.CrowdFlowerPortalAdapter
import ch.uzh.ifi.pdeboer.pplib.hcomp.mturk.MechanicalTurkPortalAdapter
import ch.uzh.ifi.pdeboer.pplib.hcomp.randomportal.RandomHCompPortal
import ch.uzh.ifi.pdeboer.pplib.util.{U, LazyLogger}
import com.typesafe.config.ConfigFactory
import org.apache.commons.codec.binary.Base64

import scala.collection.mutable

/**
 * Created by mattia on 28.08.15.
 */
object HCompNew extends LazyLogger{
	private val portals = new mutable.HashMap[String, HCompPortalAdapter]()

	def addPortal(portal: HCompPortalAdapter) {
		addPortal(portal.getDefaultPortalKey, portal)
	}

	def addPortal(key: String, portal: HCompPortalAdapter) {
		if (!portals.contains(key)) {
			logger.info(s"adding portaladapter ${portal.getClass.getSimpleName} with key ${portal.getDefaultPortalKey}")
			portals += (key -> portal)
		}
	}

	def allDefinedPortals = portals.values.filter(_ != null).toList

	def apply(key: String) = portals(key)

	//convenience methods, may be removed at a later stage
	def crowdFlower: CrowdFlowerPortalAdapter = portals.get(CrowdFlowerPortalAdapter.PORTAL_KEY).get.asInstanceOf[CrowdFlowerPortalAdapter]

	def mechanicalTurk: MechanicalTurkPortalAdapter = portals.get(MechanicalTurkPortalAdapter.PORTAL_KEY).get.asInstanceOf[MechanicalTurkPortalAdapter]

	def randomPortal: RandomHCompPortal = portals.get(RandomHCompPortal.PORTAL_KEY).get.asInstanceOf[RandomHCompPortal]

	def autoloadConfiguredPortals() {
		val config = ConfigFactory.load()

		val classes = U.findClassesInPackageWithProcessAnnotation("ch.uzh.ifi.pdeboer.pplib.hcomp", classOf[HCompPortal])
			.asInstanceOf[Set[Class[HCompPortalAdapter]]]
		val annotations = classes.map(_.getAnnotation(classOf[HCompPortal])).filter(a => a != null && a.autoInit)
		val builders = annotations.map(_.builder().newInstance())
		builders.toList.sortBy(_.order).foreach(b => {
			try {
				b.loadConfig(config)
				val portal: HCompPortalAdapter = b.build
				addPortal(portal.getDefaultPortalKey, portal)
			}
			catch {
				case e: Throwable => {
					val errorMessage: String = s"Skipped automatic initialization of ${b.getClass.getSimpleName} due to missing / invalid configuration."
					//logger.error(errorMessage, e)
					logger.debug(errorMessage, e)
				}
			}
		})

	}

}

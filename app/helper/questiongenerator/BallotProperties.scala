package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot

import java.util.UUID

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQueryProperties

/**
 * Created by mattia on 06.07.15.
 */
class BallotProperties(
						  val batch: Batch,
						  val assets: List[Asset],
						  val permutationId: Long,
						  val propertiesForDecoratedPortal: HCompQueryProperties = new HCompQueryProperties()) extends HCompQueryProperties(0) {
	override val paymentCents = propertiesForDecoratedPortal.paymentCents
}

case class Batch(allowedAnswersPerTurker: Int = 0, uuid: UUID = UUID.randomUUID())

case class Asset(binary: Array[Byte], contentType: String, filename: String, url: String = "asset://" + UUID.randomUUID())
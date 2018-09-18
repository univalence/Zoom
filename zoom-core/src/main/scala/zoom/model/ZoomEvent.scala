package zoom.model

import java.net.{InetAddress, UnknownHostException}
import java.time.Instant
import java.util.UUID

import zoom.ZoomEventSerde
import zoom.ZoomEventSerde.ToJson

import scala.util.Try

sealed trait ZoomEvent {
  def toJson: ToJson = ZoomEvent.toJson(this)
}

object ZoomEvent {

  def fromJson(str: String): Try[ZoomEvent] = {
    ZoomEventSerde.fromJson(str)
  }

  def cleanIdPack(idPack: String): String =
    idPack.replace("Some(", "").replace(")", "")

  def toJson(e: ZoomEvent): ToJson = ZoomEventSerde.toJson(e)
}

case class StartedNewNode(
    node_id: UUID,
    startup_inst: Instant,
    environment: Environment,
    prg_name: String,
    prg_organization: String,
    prg_version: String,
    prg_commit: String,
    prg_buildAt: Instant,
    node_hostname: String,
    more: Map[String, String]
) extends ZoomEvent

object StartedNewNode {

  def fromBuild(
      buildInfo: BuildInfo,
      environment: Environment,
      node_id: UUID
  ): StartedNewNode = {

    StartedNewNode(
      node_id          = node_id,
      startup_inst     = Instant.now,
      environment      = environment,
      prg_name         = buildInfo.name,
      prg_organization = buildInfo.organization,
      prg_version      = buildInfo.version,
      prg_commit       = buildInfo.commit,
      prg_buildAt      = buildInfo.buildAt,
      node_hostname    = getHostname,
      more             = Map.empty
    )
  }

  private def getHostname: String =
    try {
      InetAddress.getLocalHost.getHostName
    } catch {
      case e: UnknownHostException ⇒ "unknown_host"
    }

}

case class StoppedNode(
    node_id: UUID,
    stop_inst: Instant,
    cause: String,
    more: Map[String, String]
) extends ZoomEvent

case class BuildInfo(
    name: String,
    organization: String,
    version: String,
    commit: String,
    buildAt: Instant
)

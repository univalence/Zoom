package zoom

import java.net.{ InetAddress, UnknownHostException }
import java.time.Instant
import java.util.UUID
import java.util.UUID.fromString

import zoom._
import org.apache.kafka.common.header.Headers
import shapeless.tag
import shapeless.tag.@@
import zoom.ZoomEventSerde.ToJson
import zoom.Environment._
//import utils.Utils._

import scala.util.Try

sealed trait EventFormat

object EventFormat {

  def fromString(value: String): EventFormat = {
    Vector(Raw, Json, CCJson, XML).find(_.toString == value).get
  }

  case object Raw extends EventFormat

  case object Json extends EventFormat

  case object CCJson extends EventFormat {
    override def toString = "cc+Json"
  }

  case object XML extends EventFormat

  //case object Avro extends EventFormat

}

sealed trait Environment {
  def shortname: String = this match {
    case Production        ⇒ "prod"
    case Integration       ⇒ "int"
    case RecetteTransverse ⇒ "rect"
    case Recette           ⇒ "rec"
    case Local             ⇒ "local"
  }
}

object Environment {

  //REVIEW : Il doit y avoir un moyen dans Circe / Shapeless de s'occuper des CoProduits
  def fromString(value: String): Environment = {
    all.find(_.toString == value).get
  }

  def fromShortname(env: String): Environment = {
    env match {
      case "prod"  ⇒ Production
      case "rec"   ⇒ Recette
      case "rect"  ⇒ RecetteTransverse
      case "int"   ⇒ Integration
      case "local" ⇒ Local
    }
  }

  val all = Vector(Production, Integration, RecetteTransverse, Recette, Local)

  case object Production extends Environment

  case object Integration extends Environment

  case object RecetteTransverse extends Environment

  case object Recette extends Environment

  case object Local extends Environment

}

trait NewTracing

object Tracing {
  @deprecated
  type TracingContext = Tracing

  def newId(): String = UUID.randomUUID().toString

  def toTracing(tracingContext: TracingContext): Tracing = {
    import tracingContext._
    Tracing(
      trace_id = getTraceId.getOrElse(""),
      parent_span_id = getParentSpanId,
      previous_span_id = getPreviousSpanId,
      span_id = getSpanId.getOrElse("")
    )
  }
}

case class Tracing(
  trace_id:         String         = Tracing.newId(),
  parent_span_id:   Option[String] = None,
  previous_span_id: Option[String] = None,
  span_id:          String         = Tracing.newId(),
  on_behalf_of:     Option[String] = None
) {
  def getTraceId = Option(trace_id)

  def getParentSpanId: Option[String] = parent_span_id

  def getPreviousSpanId: Option[String] = previous_span_id

  def getSpanId: Option[String] = Option(span_id)

  def getOnBehalfOf: Option[String] = on_behalf_of

  def toTracing: Tracing = Tracing(
    trace_id = getTraceId.getOrElse(""),
    parent_span_id = getParentSpanId,
    previous_span_id = getPreviousSpanId,
    span_id = getSpanId.getOrElse("")
  )

  def newChild: @@[Tracing, NewTracing] =
    tag[NewTracing](this.copy(parent_span_id = getSpanId, span_id = UUID.randomUUID().toString))

  def newFollowFrom: @@[Tracing, NewTracing] =
    tag[NewTracing](this.copy(previous_span_id = getSpanId, span_id = UUID.randomUUID().toString))
}

object CCUtils {

  /*
  def toMapSS(cc: AnyRef with Product): Seq[(String, String)] = {
    //toMapSSWithF(cc,{case _ if false => Nil})
    ???
  }*/

  /*
  def toMapSSWithF(cc: AnyRef with Product, ext: PartialFunction[(String, Any), Seq[(String, String)]]): Seq[(String, String)] = {
    //    val res:Seq[(String,String)] = cc.getClass.getDeclaredFields.map(_.getName) // all field names
    //      .zip(cc.productIterator.toSeq).flatMap(x => {
    //      ext.orElse({
    //        case (k, None) => Nil
    //        case (k, Some(v)) => Seq(k -> v)
    //        case (k, v) => Seq(k -> v)
    //      })(x).map({case (k,v) => (k,v.toString)})
    //
    //    })
    //    res
    Nil

  }*/

  private def rekey(str: String) = {
    str.map(s ⇒ {
      if (s.isUpper) "_" + s.toLower else s.toString
    }).mkString
  }

  //FIXME : Rendre + générique !
  def getCCParams2(ref: AnyRef): Map[String, String] =
    (Map[String, String]() /: ref.getClass.getDeclaredFields) { (a, f) ⇒
      f.setAccessible(true)

      val pair = {
        f.get(ref) match {
          case Seq()           ⇒ Map.empty
          case Some(v: String) ⇒ Map(f.getName -> v)
          case None            ⇒ Map.empty
          case Some(subref: AnyRef) ⇒ {
            val subMap = getCCParams2(subref)
            subMap.map(sm ⇒ f.getName + "." + sm._1 -> sm._2)
          }
          case _ ⇒ Map(f.getName -> f.get(ref).toString)
        }
      }

      a ++ pair

    }.map(t ⇒ rekey(t._1) -> t._2).filter(_._2.nonEmpty)
}

case class EventMetadata(
  event_id:         UUID,
  event_type:       String, //models.eventmetadata
  event_format:     EventFormat,
  trace_id:         Option[String],
  parent_span_id:   Option[String],
  previous_span_id: Option[String],
  span_id:          Option[String],
  source_ids:       Seq[(Option[String], String)] = Vector.empty,
  node_id:          UUID,
  env:              Environment,
  callsite:         Option[Callsite],
  on_behalf_of:     Option[String]
) {

  def getTracing: Tracing = {
    trace_id.map(t ⇒ Tracing(trace_id = t, span_id = span_id.getOrElse(Tracing.newId()),
      parent_span_id = parent_span_id, previous_span_id = previous_span_id)).getOrElse(
      //Calcul de la trace à partir de l'id de l'event
      Tracing(trace_id = event_id.toString, span_id = event_id.toString)
    )

  }

  def toStringMap: Map[String, String] = {
    CCUtils.getCCParams2(this)
  }
}

object EventMetadata {

  def fromHeaders(headers: Headers): Try[EventMetadata] = {
    EventMetadata.fromStringMap(headers.toArray.map(h ⇒ h.key -> new String(h.value())).toMap)
  }

  def fromStringMap(map: Map[String, String]): Try[EventMetadata] = {
    Try(EventMetadata(
      event_id = fromString(map("event_id")),
      event_type = map("event_type"),
      event_format = EventFormat.fromString(map("event_format")),
      trace_id = map.get("trace_id"),
      parent_span_id = map.get("parent_span_id"),
      previous_span_id = map.get("previous_span_id"),
      span_id = map.get("span_id"),
      node_id = fromString(map("node_id")),
      env = Environment.fromString(map("env")),
      callsite = Try {
        Callsite(
          enclosingClass = map("callsite.enclosing_class"),
          enclosingMethod = map.get("callsite.enclosing_method"),
          file = map("callsite.file"),
          line = map("callsite.line").toInt
        )
      }.toOption,
      on_behalf_of = map.get("on_behalf_of")
    ))
  }
}

sealed trait ZoomEvent {
  def toJson: ToJson = ZoomEvent.toJson(this)
}

object ZoomEvent {

  def fromJson(str: String): Try[ZoomEvent] = {
    ZoomEventSerde.fromJson(str)
  }

  def cleanIdPack(idPack: String): String = idPack.replace("Some(", "").replace(")", "")

  def toJson(e: ZoomEvent): ToJson = ZoomEventSerde.toJson(e)
}

case class StartedNewNode(
  node_id:          UUID,
  startup_inst:     Instant,
  environment:      Environment,
  prg_name:         String,
  prg_organization: String,
  prg_version:      String,
  prg_commit:       String,
  prg_buildAt:      Instant,
  node_hostname:    String,
  more:             Map[String, String]
) extends ZoomEvent

case class BuildInfo(name: String, organization: String, version: String, commit: String, buildAt: Instant)

object StartedNewNode {

  def fromBuild(buildInfo: BuildInfo, environment: Environment, node_id: UUID): StartedNewNode = {

    StartedNewNode(
      node_id = node_id,
      startup_inst = Instant.now,
      environment = environment,
      prg_name = buildInfo.name,
      prg_organization = buildInfo.organization,
      prg_version = buildInfo.version,
      prg_commit = buildInfo.commit,
      prg_buildAt = buildInfo.buildAt,
      node_hostname = getHostname,
      more = Map.empty
    )
  }

  private def getHostname(): String = {
    try {
      InetAddress.getLocalHost().getHostName()
    } catch {
      case e: UnknownHostException ⇒ "unknown_host"
    }
  }

}

case class StoppedNode(
  node_id:   UUID,
  stop_inst: Instant,
  cause:     String,
  more:      Map[String, String]
) extends ZoomEvent

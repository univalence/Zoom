package zoom.model

import java.util.UUID
import java.util.UUID.fromString

import org.apache.kafka.common.header.Headers
import callsite.CallSiteInfo
import zoom.util.CCUtils

import scala.util.Try

case class EventMetadata(
    event_id: UUID,
    event_type: String, //models.eventmetadata
    event_format: EventFormat,
    trace_id: Option[String],
    parent_span_id: Option[String],
    previous_span_id: Option[String],
    span_id: Option[String],
    source_ids: Seq[(Option[String], String)] = Vector.empty,
    node_id: UUID,
    env: Environment,
    callsite: Option[CallSiteInfo],
    on_behalf_of: Option[String]
) {

  def getTracing: Tracing =
    trace_id
      .map(
        t ⇒
          Tracing(
            trace_id         = t,
            span_id          = span_id.getOrElse(Tracing.newId()),
            parent_span_id   = parent_span_id,
            previous_span_id = previous_span_id
        )
      )
      .getOrElse(
        //Calcul de la trace à partir de l'id de l'event
        Tracing(trace_id = event_id.toString, span_id = event_id.toString)
      )

  def toStringMap: Map[String, String] =
    CCUtils.getCCParams2(this)
}

object EventMetadata {

  def fromHeaders(headers: Headers): Try[EventMetadata] =
    EventMetadata.fromStringMap(
      headers.toArray.map(h ⇒ h.key → new String(h.value())).toMap
    )

  def fromStringMap(map: Map[String, String]): Try[EventMetadata] =
    Try(
      EventMetadata(
        event_id         = fromString(map("event_id")),
        event_type       = map("event_type"),
        event_format     = EventFormat.fromString(map("event_format")),
        trace_id         = map.get("trace_id"),
        parent_span_id   = map.get("parent_span_id"),
        previous_span_id = map.get("previous_span_id"),
        span_id          = map.get("span_id"),
        node_id          = fromString(map("node_id")),
        env              = Environment.fromString(map("env")),
        callsite = Try {
          CallSiteInfo(
            enclosingClass = map("callsite.enclosing_class"),
            file           = map("callsite.file"),
            line           = map("callsite.line").toInt,
            commit         = map.getOrElse("callsite.commit", "no_commit"),
            buildAt        = map.get("callsite.build_at").map(_.toLong).getOrElse(0L),
            clean          = map.get("callsite.clean").exists(_.toBoolean),
            fileContent    = map.get("callsite.file_content")
          )
        }.toOption,
        on_behalf_of = map.get("on_behalf_of")
      )
    )

}

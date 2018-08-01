package models

import java.util.UUID

import org.scalatest.{FunSuiteLike, Matchers}
import zoom.Environment.Production
import zoom.EventFormat.Json
import zoom.EventMetadata
import zoom.callsite.CallSiteInfo

import scala.util.Success

class EventMetadataTest extends FunSuiteLike with Matchers {

  val uuid: UUID = UUID.randomUUID()

  val testEventMetadata: EventMetadata =
    EventMetadata(
      event_id = uuid,
      event_type = "2",
      event_format = Json,
      trace_id = Some("4"),
      parent_span_id = Some("5"),
      previous_span_id = Some("7"),
      span_id = Some("6"),
      node_id = uuid,
      env = Production,
      callsite = Some(
        CallSiteInfo(enclosingClass = "ec", file = "fi", line = 2, commit = "no_commit", buildAt = 0, clean = false)),
      on_behalf_of = None
    )

  val testMap: Map[String, String] =
    Map(
      "event_id"                 → uuid.toString,
      "event_type"               → "2",
      "event_format"             → "Json",
      "trace_id"                 → "4",
      "parent_span_id"           → "5",
      "previous_span_id"         → "7",
      "span_id"                  → "6",
      "node_id"                  → uuid.toString,
      "env"                      → "Production",
      "callsite.enclosing_class" → "ec",
      "callsite.file"            → "fi",
      "callsite.line"            → "2",
      "callsite.build_at"        → "0",
      "callsite.commit"          → "no_commit",
      "callsite.clean"           → "false"
    )

  test("should convert an event into a string map") {
    val result = testEventMetadata.toStringMap

    result should be(testMap)
  }

  test("should convert a string map into event") {
    val result = EventMetadata.fromStringMap(testMap)
    result should be(Success(testEventMetadata))
  }

  test("toStringMap and fromStringMap should be symmetrical") {
    (EventMetadata
      .fromStringMap(testEventMetadata.toStringMap)
      .get
      should be(testEventMetadata))
  }

}

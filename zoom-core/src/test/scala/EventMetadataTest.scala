package models

import java.util.UUID

import zoom._
import org.scalatest.FunSuite
import zoom.Environment.Production
import zoom.EventFormat.Json

import scala.util.Success

class EventMetadataTest extends FunSuite {

  val testEventMetadata = EventMetadata(
    event_id = UUID.fromString("38400000-8cf0-11bd-b23e-10b96e4ef00d"),
    event_type = "2",
    event_format = Json,
    trace_id = Some("4"),
    parent_span_id = Some("5"),
    previous_span_id = Some("7"),
    span_id = Some("6"),
    node_id = UUID.fromString("38400000-8cf0-11bd-b23e-10b96e4ef00e"),
    env = Production,
    callsite = Some(Callsite("ec", Some("em"), "fi", 2)),
    on_behalf_of = None
  )

  val testMap = Map("event_id" -> "38400000-8cf0-11bd-b23e-10b96e4ef00d", "event_type" -> "2",
    "event_format" -> "Json", "trace_id" -> "4", "parent_span_id" -> "5", "previous_span_id" -> "7", "span_id" -> "6",
    "node_id" -> "38400000-8cf0-11bd-b23e-10b96e4ef00e", "env" -> "Production", "callsite.enclosing_class" -> "ec",
    "callsite.enclosing_method" -> "em", "callsite.file" -> "fi", "callsite.line" -> "2")

  test("toStringMap") {
    assert(testEventMetadata.toStringMap == testMap)
  }

  test("fromStringMap Success") {
    assert(EventMetadata.fromStringMap(testMap) == Success(testEventMetadata))
  }

  test("serde invariant") {
    assert(EventMetadata.fromStringMap(testEventMetadata.toStringMap).get == testEventMetadata)
  }

}

package zoom.model

import java.util.UUID

import shapeless.tag
import shapeless.tag.@@

trait NewTracing

object Tracing {
  @deprecated("TODO: message", "TODO: since when?")
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
    trace_id: String = Tracing.newId(),
    parent_span_id: Option[String] = None,
    previous_span_id: Option[String] = None,
    span_id: String = Tracing.newId(),
    on_behalf_of: Option[String] = None
) {
  def getTraceId = Option(trace_id)

  def getParentSpanId: Option[String] = parent_span_id

  def getPreviousSpanId: Option[String] = previous_span_id

  def getSpanId: Option[String] = Option(span_id)

  def getOnBehalfOf: Option[String] = on_behalf_of

  def toTracing: Tracing =
    Tracing(
      trace_id = getTraceId.getOrElse(""),
      parent_span_id = getParentSpanId,
      previous_span_id = getPreviousSpanId,
      span_id = getSpanId.getOrElse("")
    )

  def newChild: @@[Tracing, NewTracing] =
    tag[NewTracing](
      this.copy(
        parent_span_id = getSpanId,
        span_id = UUID.randomUUID().toString
      )
    )

  def newFollowFrom: @@[Tracing, NewTracing] =
    tag[NewTracing](
      this.copy(
        previous_span_id = getSpanId,
        span_id = UUID.randomUUID().toString
      )
    )
}

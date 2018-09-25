package zoom

import java.util.UUID

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import zoom.Level.{Fatal, Info, Warn}
import callsite.CallSiteInfo
import zoom.model.Tracing
//import utils.Configuration

import scala.concurrent.Future

//trait KafkaConfiguration

case class KafkaConfiguration(
    kafkaPort: Int,
    kafkaHost: String,
    customBrokerProperties: Map[String, String]   = Map.empty,
    customProducerProperties: Map[String, String] = Map.empty
) {

  def kafkaBrokers: String = s"$kafkaHost:$kafkaPort"

}

object KafkaConfiguration {
  def localKafkaConfiguration: KafkaConfiguration =
    new KafkaConfiguration(9092, "localhost")
}

trait LoggerWithCtx[Context] {

  protected def log(message: => String, level: Level)(implicit context: Context): Unit

  final def info(message: => String)(implicit context: Context): Unit = log(message, Info)

  final def warn(message: => String)(implicit context: Context): Unit = log(message, Warn)

  final def fatal(message: => String)(implicit context: Context): Unit = log(message, Fatal)

  final def error(message: => String)(implicit context: Context): Unit = log(message, Level.Error)

  final def debug(message: => String)(implicit context: Context): Unit = log(message, Level.Debug)
}

case class TracingAndCallSite(implicit val tracing: Tracing, implicit val callsite: CallSiteInfo)

object TracingAndCallSite {
  implicit def fromTracingAndCallSite(implicit tracing: Tracing, callsite: CallSiteInfo): TracingAndCallSite =
    TracingAndCallSite()
}

trait Logger extends LoggerWithCtx[TracingAndCallSite]

@deprecated("TODO: message", "TODO: since when?")
trait NodeLogger extends LoggerWithCtx[CallSiteInfo]

trait TraceLogger extends LoggerWithCtx[CallSiteInfo]

case class NodeInfo(node_id: UUID)

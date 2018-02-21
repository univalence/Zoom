package zoom

import java.nio.charset.Charset
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

import org.apache.kafka.clients.consumer.{ ConsumerConfig, KafkaConsumer }
import org.apache.kafka.clients.producer._
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.{ ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer }
import zoom.OutTopics.GroupEnv
import zoom._

import scala.concurrent.{ Await, Future, Promise }
import scala.language.postfixOps
import scala.concurrent._
import scala.util.Try

case class OutTopics(log: String, event: String, raw: String)

object OutTopics {

  case class GroupEnv(group: String, environment: Environment)

  type Strategy = GroupEnv ⇒ OutTopics

  @deprecated(message = "please use default strategy")
  val oldStrategy: Strategy = {
    case GroupEnv(_, env) ⇒
      val shortname = env.shortname
      OutTopics(
        log = "logs." + shortname,
        event = "data.event." + shortname,
        raw = "data.raw." + shortname
      )
  }

  val defaultStrategy: Strategy = {
    case GroupEnv(group, env) ⇒
      val shortname = env.shortname
      OutTopics(
        log = s"$shortname.$group.log",
        event = s"$shortname.$group.event",
        raw = s"$shortname.$group.raw"
      )
  }
}

trait NCSer[Event] {
  def serialize(event: Event): Array[Byte]

  def format: EventFormat

  def eventType(event: Event): String
}

object NCSer {

  val empty: NCSer[Unit] = new NCSer[Unit] {
    override def format: EventFormat = ???
    override def eventType(event: Unit): String = ???
    override def serialize(event: Unit): Array[Byte] = ???
  }

  @deprecated(message = "to move in trafic garanti")
  val tgEventSerde: NCSer[ZoomEvent] = new NCSer[ZoomEvent] {
    override def serialize(event: ZoomEvent): Array[Byte] =
      ZoomEventSerde.toJson(event).
        payload.
        getBytes(Charset.forName("UTF_8"))

    override def format: EventFormat = EventFormat.CCJson

    override def eventType(event: ZoomEvent): String = event.getClass.getName
  }

}

object NodeContextV2 {

  def createAndStart[Event](
    group:              String,
    environment:        Environment,
    kafkaConfiguration: KafkaConfiguration,
    buildInfo:          BuildInfo,
    eventSer:           NCSer[Event],
    topicStrategy:      OutTopics.Strategy = OutTopics.defaultStrategy
  ): Try[NodeContextV2[Event]] = {

    val zoomGroupName: String = "zoom"

    new NodeContextV2[Event](group, environment, kafkaConfiguration, buildInfo, eventSer, topicStrategy, zoomGroupName).init

  }

}

object CheckKafkaProducerConfiguration {

  case class ConfigurationIssue(msg: String, description: String, isError: Boolean)
  def checkConfiguration(kafkaConfig: Map[String, Object]): Seq[ConfigurationIssue] = {

    if (kafkaConfig(ProducerConfig.MAX_BLOCK_MS_CONFIG).toString.toLong < kafkaConfig(ProducerConfig.RETRY_BACKOFF_MS_CONFIG).toString.toLong)
      Seq(ConfigurationIssue(
        "MAX_BLOCK < RETRY_BACKOFF",
        "MAX BLOCK should be greater than RETRY BACKOFF, else NodeContextV2 doesn't work on new topics",
        isError = true
      ))
    else Seq.empty
  }

}

final class NodeContextV2[Event] protected (
  val group:              String,
  val environment:        Environment,
  val kafkaConfiguration: KafkaConfiguration,
  val buildInfo:          BuildInfo,
  val eventSer:           NCSer[Event],
  val topicStrategy:      OutTopics.Strategy = OutTopics.defaultStrategy,
  val zoomGroupName:      String             = "zoom"
) extends Serializable {

  import scala.concurrent.ExecutionContext.Implicits.global

  private var isRunning: Boolean = false

  private val groupOutTopics: OutTopics = topicStrategy(GroupEnv(group, environment))

  private val UTF8_CHARSET: Charset = java.nio.charset.Charset.forName("UTF-8")

  private val baseProducerConfig: Map[String, Object] = Map[String, Object](
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> kafkaConfiguration.kafkaBrokers,
    ProducerConfig.MAX_BLOCK_MS_CONFIG -> 10000.toString,
    ProducerConfig.RETRY_BACKOFF_MS_CONFIG -> 1000.toString
  ) ++ kafkaConfiguration.customProducerProperties

  private val baseConsumerConfig: Map[String, Object] = Map[String, Object](
    //ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG -> 100000.toString,
    ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG -> 10000.toString,
    ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG -> 10000.toString,
    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> kafkaConfiguration.kafkaBrokers
  )

  private val producer: KafkaProducer[String, Array[Byte]] = {
    import scala.collection.JavaConverters._
    new KafkaProducer(baseProducerConfig.asJava, new StringSerializer(), new ByteArraySerializer())
  }

  private val consumer: KafkaConsumer[String, Array[Byte]] = {
    import scala.collection.JavaConverters._

    new KafkaConsumer[String, Array[Byte]](baseConsumerConfig.asJava, new StringDeserializer(), new ByteArrayDeserializer())

  }

  def init: Try[NodeContextV2[Event]] = {

    Try {
      assert(!CheckKafkaProducerConfiguration.
        checkConfiguration(baseProducerConfig).
        exists(_.isError))
      nodeElement.checkTopicExistanceAndLog()
      checkTopicExistanceAndLog_!()
      nodeElement.start()
      isRunning = true
      this
    }
  }

  private def isTopicCreated(topic: String): Boolean = {
    import scala.concurrent.duration._
    Await.result(Future(consumer.listTopics().containsKey(topic)), 15 seconds)
  }

  def nodeId: UUID = nodeElement.nodeId

  private def checkTopicExistanceAndLog_!(): Unit = {
    val logTopic = groupOutTopics.log
    if (!isTopicCreated(logTopic)) {
      rootLogger.warn(s"$group log topic ($logTopic) does not exist")
    }

    val eventTopic = groupOutTopics.event
    if (!isTopicCreated(eventTopic)) {
      rootLogger.warn(s"$group event topic ($eventTopic) does not exist")
    }
  }

  private object nodeElement {
    val nodeId: UUID = UUID.randomUUID()
    private val nodeTracingContext: Tracing = Tracing()
    protected val nodeOutTopics: OutTopics = topicStrategy(GroupEnv("zoom", environment))

    def checkTopicExistanceAndLog(): Unit = {
      val logTopic = nodeElement.nodeOutTopics.log
      if (!isTopicCreated(logTopic)) {
        logger.warn(s"zoom log topic ($logTopic) does not exist (and we are trying to log in it, why not)")
      }

      val eventTopic = nodeElement.nodeOutTopics.event
      if (!isTopicCreated(eventTopic)) {
        logger.warn(s"zoom event topic ($eventTopic) does not exist (and we will use it in a couple of ms)")
      }
    }

    def start(): Unit = {
      val json = ZoomEventSerde.toJson(StartedNewNode.fromBuild(
        buildInfo = buildInfo,
        environment = environment,
        node_id = nodeId
      ))

      val future = publishLow(
        topic = nodeOutTopics.event,
        content = json.payload.getBytes(UTF8_CHARSET),
        format = EventFormat.CCJson,
        eventType = json.event_type,
        tracing = nodeTracingContext,
        callsite = implicitly[Callsite]
      )

      import scala.concurrent._
      import scala.concurrent.duration._
      //Await.result(future,20 seconds)

      {
        Runtime.getRuntime.addShutdownHook(new Thread() {
          override def run(): Unit = {
            nodeElement.stop()
          }
        })
      }

    }

    def stop(): Unit = {

      if (isRunning) {
        val json = ZoomEventSerde.toJson(StoppedNode(
          node_id = nodeId,
          stop_inst = Instant.now,
          cause = "shutdown hook",
          more = Map.empty
        ))

        publishLow(
          topic = nodeOutTopics.event,
          content = json.payload.getBytes(UTF8_CHARSET),
          format = EventFormat.CCJson,
          eventType = json.event_type,
          tracing = nodeTracingContext,
          callsite = implicitly[Callsite]
        )
        isRunning = false
      } else {
        throw new Exception("node context already stopped")
      }

    }

    @deprecated
    val logger = new NodeLogger with LoggerWithCtx[Callsite] {
      override def log(message: ⇒ String, level: Level)(implicit context: Callsite): Unit = {
        logF(message, level)
      }
    }

    def logF(message: ⇒ String, level: Level)(implicit callsite: Callsite) = {
      implicit val t = nodeTracingContext

      println(level + ":" + message)
      publishLow(
        topic = nodeOutTopics.log,
        content = message.getBytes(UTF8_CHARSET),
        format = EventFormat.Raw,
        eventType = s"logs/$level" + this.getClass.getName,
        tracing = nodeTracingContext,
        callsite = implicitly[Callsite]
      )
    }
  }

  def stop(): Unit = {

    nodeElement.stop()
  }

  @deprecated("use publishEvent")
  def saveEvent(event: Event)(implicit tracing: Tracing, callsite: Callsite): Future[Unit] = publishEvent(event)

  def publishEvent(event: Event)(implicit tracing: Tracing, callsite: Callsite): Future[Unit] = {

    val content = eventSer.serialize(event)
    val eventType = eventSer.eventType(event)

    publishLow(groupOutTopics.event, content, EventFormat.CCJson, eventType, tracing, callsite)

    import scala.concurrent.ExecutionContext.Implicits.global

    Future[Unit]()
  }

  private def publishLow(topic: String, content: Array[Byte], format: EventFormat, eventType: String, tracing: Tracing, callsite: Callsite): Unit = {

    val meta = EventMetadata(
      event_id = UUID.randomUUID(),
      event_type = eventType,
      event_format = format,
      trace_id = tracing.getTraceId,
      parent_span_id = tracing.getParentSpanId,
      previous_span_id = tracing.getPreviousSpanId,
      span_id = tracing.getSpanId,
      node_id = nodeElement.nodeId,
      env = environment, callsite = Some(callsite),
      on_behalf_of = tracing.getOnBehalfOf
    )

    import scala.collection.JavaConverters._
    import scala.concurrent.ExecutionContext.Implicits.global
    val headers = meta.toStringMap.mapValues(_.getBytes).toSeq

    val hdrs = headers.map(t ⇒ new RecordHeader(t._1, t._2).asInstanceOf[Header])

    val record: ProducerRecord[String, Array[Byte]] = new ProducerRecord[String, Array[Byte]](
      topic,
      null,
      new java.util.Date().getTime,
      null,
      content,
      hdrs.asJava
    )

    producer.send(record).get

  }

  def publishRaw(
    content:   Array[Byte],
    format:    EventFormat,
    eventType: String
  )(
    implicit
    tracing:  Tracing,
    callsite: Callsite
  ): Future[Unit] = {
    publishLow(groupOutTopics.raw, content, EventFormat.Raw, eventType, tracing, callsite)
    import scala.concurrent.ExecutionContext.Implicits.global
    Future[Unit]()

  }

  def logger: Logger = {
    new LoggerWithCtx[TracingAndCallSite] with Logger {
      override def log(message: ⇒ String, level: Level)(implicit context: TracingAndCallSite): Unit = {
        logImpl(message, level)
      }
    }
  }

  private def logImpl(message: ⇒ String, level: Level)(implicit context: TracingAndCallSite) = {
    publishLow(
      topic = groupOutTopics.log,
      content = message.getBytes(UTF8_CHARSET),
      format = EventFormat.Raw,
      eventType = s"logs/${level.toString}/" + context.callsite.enclosingClass,
      tracing = context.tracing,
      callsite = context.callsite
    )
  }

  @deprecated(message = "use @trace instead")
  def getLogger(logClass: Class[_]): Logger = logger

  private def rootLogger: NodeLogger = nodeElement.logger

  @deprecated(message = "use @global_log_no_tracing instead")
  def useRootLogger(really_? :IamLoggingWithoutTracing.type): NodeLogger = nodeElement.logger

  def kafkaBrokers: String = kafkaConfiguration.kafkaBrokers

  def global_log_no_tracing(message: ⇒ String, level: Level)(implicit callsite: Callsite): Unit = {
    nodeElement.logF(message, level)
  }

  //TODO MACROS ? // SCALA 2.12 ?
  def trace[T](block: TraceEffect[Event] ⇒ T): T = {

    //NAIVE IMPL
    implicit val tracing = Tracing()
    val nc = this

    block(new TraceEffect[Event] {
      override def log: TraceLogger = new TraceLogger {
        override protected def log(message: ⇒ String, level: Level)(implicit context: Callsite): Unit = {
          val msg = message
          println(s"${level.toString} at  ${context.file}:${context.line}  $msg ")
          nc.logImpl(msg, level)
        }
      }

      override def publishEvent(event: Event)(implicit callsite: Callsite): Unit = {
        nc.publishEvent(event)
      }

      override def publishRaw(content: Array[Byte], format: EventFormat, eventType: String)(implicit callsite: Callsite): Unit = {
        nc.publishRaw(content, format, eventType)
      }
    })
  }
}

trait TraceEffect[E] {
  def publishEvent(event: E)(implicit callsite: Callsite): Unit
  def publishRaw(
    content:   Array[Byte],
    format:    EventFormat,
    eventType: String
  )(implicit callsite: Callsite)

  def log: TraceLogger
}

sealed trait Level

object Level {
  case object Debug extends Level
  case object Info extends Level
  case object Warn extends Level
  case object Error extends Level
  case object Fatal extends Level
}

@deprecated
object IamLoggingWithoutTracing


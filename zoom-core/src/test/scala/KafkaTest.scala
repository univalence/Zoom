package models

import java.util.{ Properties, UUID }

import zoom._
import net.manub.embeddedkafka.{ EmbeddedKafka, EmbeddedKafkaConfig, KafkaUnavailableException }
import org.apache.kafka.clients.consumer.{ KafkaConsumer, OffsetAndMetadata }
import org.apache.kafka.clients.producer.{ KafkaProducer, ProducerConfig, ProducerRecord }
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.{ Deserializer, Serializer, StringDeserializer, StringSerializer }
import org.apache.kafka.common.{ KafkaException, TopicPartition }
import org.scalatest.{ BeforeAndAfterAll, FunSuite }

import scala.collection.mutable.ListBuffer
import scala.concurrent.TimeoutException
import scala.concurrent.duration.SECONDS
import scala.util.Try
import utils.RandomizePostKafka

class KafkaTest extends FunSuite with EmbdedKafkaCustom with EmbeddedKafka with BeforeAndAfterAll {

  val customBrokerConfig = Map.empty[String, String]
  val customProducerConfig = Map(
    "key.serializer" -> classOf[StringSerializer].getName,
    "value.serializer" -> classOf[StringSerializer].getName
  )

  val customConsumerConfig = Map("max.partition.fetch.bytes" -> "2000000")

  implicit val customKafkaConfig: EmbeddedKafkaConfig = RandomizePostKafka.changePortKafkaConfiguration_!(
    EmbeddedKafkaConfig(
      customBrokerProperties = customBrokerConfig,
      customProducerProperties = customProducerConfig,
      customConsumerProperties = customConsumerConfig
    )
  )

  implicit val keySerializer = new StringSerializer
  implicit val stringDe = new StringDeserializer

  override def beforeAll(): Unit = {
    EmbeddedKafka.stop()
    EmbeddedKafka.start
  }

  override def afterAll(): Unit = {
    EmbeddedKafka.stop()
  }

  test("kafka") {

    publishToKafka("mytopic", UUID.randomUUID.toString, "<hello><world/></hello>")

    val firstStr = consumeFirstMessageFrom[String]("mytopic")
    assert(firstStr == "<hello><world/></hello>")
  }

  test("kafkaWithHeaders") {

    val json: ZoomEventSerde.ToJson = ZoomEventSerde.toJson(BuildInfoTest.startedNewNode)

    val callsite: Callsite = implicitly[Callsite]

    val meta: EventMetadata = EventMetadata(
      event_id = UUID.randomUUID(),
      event_type = json.event_type,
      event_format = EventFormat.CCJson,
      trace_id = None,
      parent_span_id = None,
      previous_span_id = None,
      span_id = None,
      node_id = BuildInfoTest.startedNewNode.node_id,
      env = Environment.Local,
      callsite = Some(callsite),
      on_behalf_of = None
    )

    publishToKafkaWithHeaders("withHeaders", "",
      json.payload, meta.toStringMap.mapValues(_.getBytes))

    val res: Map[String, List[(String, Map[String, Array[Byte]])]] = consumeNumberMessagesFromTopicsWithHeaders(Set("withHeaders"), 1)

    val map: Map[String, Array[Byte]] = res("withHeaders").head._2

    val readMeta = EventMetadata.fromStringMap(map.mapValues(b ⇒ new String(b))).get

    assert(readMeta == meta)

  }

}

trait EmbdedKafkaCustom {
  this: EmbeddedKafka ⇒

  def baseProducerConfig(implicit config: EmbeddedKafkaConfig): Map[String, Object] = Map[String, Object](
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> s"localhost:${config.kafkaPort}",
    ProducerConfig.MAX_BLOCK_MS_CONFIG -> 10000.toString,
    ProducerConfig.RETRY_BACKOFF_MS_CONFIG -> 1000.toString
  ) ++ config.customProducerProperties

  def publishToKafkaWithHeaders[K, T](topic: String, key: K, message: T, headers: Map[String, Array[Byte]])(
    implicit
    config:        EmbeddedKafkaConfig,
    keySerializer: Serializer[K],
    serializer:    Serializer[T]
  ): Unit = {
    import scala.collection.JavaConverters._
    publishToKafkaLow(
      new KafkaProducer(baseProducerConfig.asJava, keySerializer, serializer),
      new ProducerRecord[K, T](
        topic,
        null,
        key,
        message,
        headers.map(t ⇒ new RecordHeader(t._1, t._2).asInstanceOf[Header]).asJava
      )
    )
  }

  def publishToKafkaLow[K, T](
    kafkaProducer: KafkaProducer[K, T],
    record:        ProducerRecord[K, T]
  ): Unit = {
    val sendFuture = kafkaProducer.send(record)
    val sendResult = Try {
      sendFuture.get(10, SECONDS)
    }

    kafkaProducer.close()

    if (sendResult.isFailure)
      throw new KafkaUnavailableException(sendResult.failed.get)
  }

  import scala.concurrent.duration._

  private def baseConsumerConfig(
    implicit
    config: EmbeddedKafkaConfig
  ): Properties = {
    import scala.collection.JavaConverters._
    val props = new Properties()
    props.put("group.id", s"embedded-kafka-spec")
    props.put("bootstrap.servers", s"localhost:${config.kafkaPort}")
    props.put("auto.offset.reset", "earliest")
    props.put("enable.auto.commit", "false")
    props.putAll(config.customConsumerProperties.asJava)
    props
  }

  type MessageWithHeader[T] = (T, Map[String, Array[Byte]])

  def consumeNumberMessagesFromTopicsWithHeaders[T](
    topics:                    Set[String],
    number:                    Int,
    autoCommit:                Boolean     = false,
    timeout:                   Duration    = 5.seconds,
    resetTimeoutOnEachMessage: Boolean     = true
  )(
    implicit
    config:       EmbeddedKafkaConfig,
    deserializer: Deserializer[T]
  ): Map[String, List[MessageWithHeader[T]]] = {

    import scala.collection.JavaConverters._

    val props = baseConsumerConfig
    props.put("enable.auto.commit", autoCommit.toString)

    var timeoutNanoTime = System.nanoTime + timeout.toNanos
    val consumer =
      new KafkaConsumer[String, T](props, new StringDeserializer, deserializer)

    val messages = Try {
      val messagesBuffers: Map[String, ListBuffer[MessageWithHeader[T]]] = topics.map(_ -> ListBuffer.empty[MessageWithHeader[T]]).toMap
      var messagesRead = 0
      consumer.subscribe(topics.asJava)
      topics.foreach(consumer.partitionsFor)

      while (messagesRead < number && System.nanoTime < timeoutNanoTime) {
        val records = consumer.poll(1000)
        val recordIter = records.iterator()
        if (resetTimeoutOnEachMessage && recordIter.hasNext) {
          timeoutNanoTime = System.nanoTime + timeout.toNanos
        }
        while (recordIter.hasNext && messagesRead < number) {
          val record = recordIter.next()
          val topic = record.topic()

          val mes: MessageWithHeader[T] = (record.value(), record.headers().toArray.map(h ⇒ h.key() -> h.value()).toMap)

          messagesBuffers(topic) += mes
          val tp = new TopicPartition(topic, record.partition())
          val om = new OffsetAndMetadata(record.offset() + 1)
          consumer.commitSync(Map(tp -> om).asJava)
          messagesRead += 1
        }
      }
      if (messagesRead < number) {
        throw new TimeoutException(s"Unable to retrieve $number message(s) from Kafka in $timeout")
      }
      messagesBuffers.map { case (topic, messages) ⇒ topic -> messages.toList }
    }

    consumer.close()
    messages.recover {
      case ex: KafkaException ⇒ throw new KafkaUnavailableException(ex)
    }.get
  }
}


package models

import java.util.concurrent.TimeUnit

import kafka.admin.AdminUtils
import kafka.utils.ZkUtils
import net.manub.embeddedkafka.{ EmbeddedKafka, EmbeddedKafkaConfig }
import org.apache.kafka.clients.producer.{ KafkaProducer, ProducerConfig, ProducerRecord }
import org.apache.kafka.common.serialization.{ ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer }
import org.scalatest.{ BeforeAndAfterAll, FunSuite }
import zoom.OutTopics.GroupEnv
import zoom._

import scala.collection.mutable
import scala.util.Try
import utils.RandomizePostKafka

/*object RandomizePostKafka {

  import java.io.IOException
  import java.net.ServerSocket

  private val givenPort: collection.mutable.HashSet[Int] = mutable.HashSet.empty

  def newFreePort_!(from: Int): Int = {
    givenPort.synchronized({
      val newPort = (from to 65535).view.filterNot(givenPort).filter(isLocalPortFree_!).head
      givenPort.add(newPort)
      println(s"on port : $newPort")
      newPort
    })
  }

  private def isLocalPortFree_!(port: Int) = try {
    new ServerSocket(port).close()
    true
  } catch {
    case e: IOException ⇒
      false
  }

  def changePortKafkaConfiguration_!(kafkaConfiguration: EmbeddedKafkaConfig): EmbeddedKafkaConfig = {
    kafkaConfiguration.copy(
      kafkaPort = newFreePort_!(kafkaConfiguration.kafkaPort),
      zooKeeperPort = newFreePort_!(kafkaConfiguration.zooKeeperPort)
    )

    kafkaConfiguration
  }

}*/

class StartedNewNodeTestV2 extends FunSuite with EmbdedKafkaCustom with EmbeddedKafka with BeforeAndAfterAll {

  implicit val embdedKafkaConfig: EmbeddedKafkaConfig =
    RandomizePostKafka.changePortKafkaConfiguration_!(EmbeddedKafkaConfig.defaultConfig)

  private val kafkaPort: Int = embdedKafkaConfig.kafkaPort

  implicit val keySerializer = new StringSerializer
  implicit val stringDe = new StringDeserializer
  implicit val byteArrDe = new ByteArrayDeserializer

  override def beforeAll(): Unit = {
    EmbeddedKafka.start

    EmbeddedKafka.publishStringMessageToKafka("totoTopic", "toto")
    assert(EmbeddedKafka.consumeFirstMessageFrom[String]("totoTopic") == "toto")

  }

  override def afterAll(): Unit = {
    EmbeddedKafka.stop()
  }

  test("testFromBuild") {

    import BuildInfoTest._

    val inJson = ZoomEventSerde.toJson(startedNewNode)

    assert(inJson.event_type == "zoom.StartedNewNode")

    assert(inJson.payload.contains("StartedNewNode"))

    assert(ZoomEventSerde.fromJson[StartedNewNode](inJson.payload).get == startedNewNode)

  }

  val testKafkaConfig = KafkaConfiguration(kafkaPort, "localhost")

  test("produce low level") {

    val topic = "otherTopic"
    val record: ProducerRecord[String, Array[Byte]] = new ProducerRecord[String, Array[Byte]](
      topic,
      null,
      new java.util.Date().getTime,
      null,
      "hello".getBytes,
      null
    )

    def baseProducerConfig: Map[String, Object] = Map[String, Object](
      ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> testKafkaConfig.kafkaBrokers,
      ProducerConfig.MAX_BLOCK_MS_CONFIG -> 10000.toString,
      ProducerConfig.RETRY_BACKOFF_MS_CONFIG -> 1000.toString,
      ProducerConfig.RETRIES_CONFIG -> 4.toString,
      ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION -> 1.toString
    ) ++ testKafkaConfig.customProducerProperties

    val producer: KafkaProducer[String, Array[Byte]] = {
      import scala.collection.JavaConverters._
      val config = baseProducerConfig
      new KafkaProducer(config.asJava, new StringSerializer(), new ByteArraySerializer())
    }

    import scala.collection.JavaConverters._

    Try {
      //producer.partitionsFor("local.zoom.event")
    }
    val metadata = producer.send(record).get(5, TimeUnit.SECONDS)
    println(metadata)

  }

  test("Don't start nodeContext if Kafka is not present") {

    val buildInfo = BuildInfoTest.buildInfo
    val t = NodeContextV2.createAndStart[Unit](
      group = "datav2",
      environment = Environment.Local,
      kafkaConfiguration = testKafkaConfig.copy(kafkaPort = 1),
      buildInfo = buildInfo,
      eventSer = new NCSer[Unit] {
        override def format: EventFormat = EventFormat.Raw
        override def eventType(event: Unit): String = "unit"
        override def serialize(event: Unit): Array[Byte] = Array()
      }
    )

    assert(t.isFailure)
  }

  test("decode startedEvent") {
    val event = "{\"node_id\":\"959d85cc-2137-4fb5-bad8-c8066a584c28\",\"startup_inst\":\"2018-01-22T22:45:06.614Z\",\"environment\":\"Local\",\"prg_name\":\"zm\",\"prg_organization\":\"zm\",\"prg_version\":\"fd6f56c3d115bf3bd3e7a041410cb9f3828b5ec4-SNAPSHOT\",\"prg_commit\":\"fd6f56c3d115bf3bd3e7a041410cb9f3828b5ec4\",\"prg_buildAt\":\"2018-01-22T22:19:17.249Z\",\"node_hostname\":\"Blaze-2.local\",\"more\":{},\"_typehint\":\"StartedNewNode\"}"

    assert(ZoomEvent.fromJson(event).get.isInstanceOf[StartedNewNode])

  }

  test("Publish To Node") {
    val buildInfo = BuildInfoTest.buildInfo

    val nc = NodeContextV2.createAndStart[Unit](
      group = "datav2",
      environment = Environment.Local,
      kafkaConfiguration = testKafkaConfig,
      buildInfo = buildInfo,
      eventSer = new NCSer[Unit] {
        override def format: EventFormat = EventFormat.Raw
        override def eventType(event: Unit): String = "unit"
        override def serialize(event: Unit): Array[Byte] = Array()
      },
      topicStrategy = OutTopics.defaultStrategy
    ).get

    val eventTopic = OutTopics.defaultStrategy(GroupEnv("zoom", Environment.Local)).event

    assert(eventTopic == "local.zoom.event")
    val dummyEvent = """{"toto":"titi"}"""
    EmbeddedKafka.publishStringMessageToKafka("local.zoom.event", dummyEvent)

    def getNextEvent() = {
      new String(
        consumeFirstMessageFrom[Array[Byte]](
          eventTopic
        )
      )
    }

    val firstEvent = getNextEvent()

    println(firstEvent)

    assert(firstEvent != dummyEvent)

    val startedNewNode = ZoomEvent.fromJson(firstEvent).get.asInstanceOf[StartedNewNode]

    //println(new String(consumeFirstMessageFrom[Array[Byte]](eventTopic)))
    assert(startedNewNode.environment == Environment.Local)
    assert(startedNewNode.node_id == nc.nodeId)

    nc.stop()

    assert(getNextEvent() == dummyEvent)

    val nextEvent = getNextEvent()
    val sn: StoppedNode = ZoomEvent.fromJson(nextEvent).get.asInstanceOf[StoppedNode]

    assert(sn.node_id == nc.nodeId)
    //assert(sn.stop_inst > startedNewNode.startup_inst)

  }

}

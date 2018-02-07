package zoom

import org.apache.kafka.clients.consumer.{ ConsumerRecord, ConsumerRecords }
import scala.collection.JavaConverters._

import scala.util.Try

object KafkaStruct {

  def fromConsumerRecord(cr: ConsumerRecord[Array[Byte], Array[Byte]]): KafkaStruct = {
    KafkaStruct(cr.topic(), cr.partition(), cr.offset(), Option(cr.key()),
      Option(cr.value()), cr.headers().toArray.toSeq.map(h ⇒ h.key() -> h.value()),
      cr.timestamp())
  }
}

case class KafkaStruct(
  topic:     String,
  partition: Int,
  offset:    Long,
  key:       Option[Array[Byte]],
  value:     Option[Array[Byte]],
  header:    Seq[(String, Array[Byte])],
  ts:        Long
) {
  def pretty: String = {

    import Pretty._

    val headerS = header.map({ case (k, b) ⇒ s"""${prettyString(k)} -> ${prettyAB(b)}""" }).mkString("Seq(", ",", ")")

    s"""KafkaStruct(topic=${prettyString(topic)},partition=$partition,offset=${offset}L,key=${prettyOAB(key)},value=${prettyOAB(value)},header=$headerS,ts=${ts}L)"""

  }

  def valueAsString: Option[String] = Try {
    new String(value.get)
  }.toOption

  def replaceInStr(str: String, regex: String, replaceWith: String): Option[String] = Try {
    val rgx = regex.r
    val ret = rgx.replaceAllIn(str, replaceWith)
    ret
  }.toOption

  def getHeaderString(name: String): Option[String] =
    header.find(_._1.toString == name).map(_._2).map(new String(_))

  def getMetaData: Option[EventMetadata] = EventMetadata.
    fromStringMap(header.map(x ⇒ (x._1, new String(x._2))).toMap).toOption
}

object Pretty {

  def prettyString(str: String): String = {
    "\"" + str.replaceAll(""""""", """\\"""") + "\""
  }

  def prettyAB(ab: Array[Byte]): String = {
    prettyString(new String(ab)) + ".getBytes"
  }

  def prettyOAB(oab: Option[Array[Byte]]): String = {
    oab match {
      case None     ⇒ "None"
      case Some(ab) ⇒ "Option(" + prettyAB(ab) + ")"
    }
  }
}

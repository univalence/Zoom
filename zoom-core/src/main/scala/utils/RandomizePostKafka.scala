package utils

import net.manub.embeddedkafka.EmbeddedKafkaConfig

import scala.collection.mutable

object RandomizePostKafka {

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

}
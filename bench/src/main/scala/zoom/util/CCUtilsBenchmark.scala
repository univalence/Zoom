package zoom.util
import java.time.LocalDateTime
import java.util.UUID

import org.openjdk.jmh.annotations.{Benchmark, Scope, Setup, State}
import zoom.util.CCUtilsLib.FastLevel1CCToMap

import scala.util.Random

@State(Scope.Benchmark)
class CCUtilsBenchmark {

  var entity: Level1CC = _

  @Setup
  def setup(): Unit = {
    entity = Level1CCGenerator.generate
  }

  @Benchmark
  def toMap_Level1CC_specific_baseline: Map[String, String] =
    FastLevel1CCToMap.toMap(entity)

  @Benchmark
  def toMap_current_production_implementation: Map[String, String] =
    CCUtils.toMap(entity)

}

case class Level2CC(timestamp: LocalDateTime, value: Int)
case class Level1CC(id: UUID, sub1: Level2CC, sub2: Option[Level2CC])

object Level1CCGenerator {

  def generateLevel2 =
    Level2CC(
      timestamp = LocalDateTime.now(),
      value     = Random.nextInt(1000000)
    )

  def generate: Level1CC =
    Level1CC(
      id   = UUID.randomUUID(),
      sub1 = generateLevel2,
      sub2 = Some(generateLevel2)
    )

}

package zoom.util
import java.time.LocalDateTime
import java.util.UUID

import org.openjdk.jmh.annotations.{Benchmark, Scope, Setup, State}

import scala.util.Random

@State(Scope.Benchmark)
class CCUtilsBenchmark {

  var entity: Level1CC = _

  @Setup
  def setup(): Unit = {
    entity = Level1CCGenerator.generate
  }

  @Benchmark
  def getCCParams2: Map[String, String] =
    CCUtils.getCCParams2(entity)

}

case class Level2CC(timestamp: LocalDateTime, value: Int)

case class Level1CC(id: UUID, sub: Level2CC)

object Level1CCGenerator {

  def generate: Level1CC =
    Level1CC(
      id = UUID.randomUUID(),
      sub = Level2CC(
        timestamp = LocalDateTime.now(),
        value     = Random.nextInt(1000000)
      )
    )

}

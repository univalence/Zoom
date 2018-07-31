package zoom.bench.sandbox
import java.time.LocalDateTime
import java.util.UUID

import org.openjdk.jmh.annotations._

import scala.util.Random

@State(Scope.Benchmark)
class ToMapBench {

  var entity: Level1CC = _

  @Setup
  def setup(): Unit = {
    entity = Level1CCGenerator.generate
  }

  @Benchmark
  def toMap_byHand_baseline: Map[String, Any] =
    ToMap_byHand.toMap(entity)

  @Benchmark
  def toMap_javaReflection: Map[String, Any] =
    ToMap_javaReflection.toMap(entity)

  @Benchmark
  def toMap_shapeless: Map[String, Any] = {
    import sandbox.ToTypelessMap._

    toMap(entity)
  }

// FIXME: ToMapBench.scala:41:10: could not find implicit value for evidence parameter of type sandbox.ToMap[zoom.bench.sandbox.Level1CC]
//  @Benchmark
//  def toMap_magnolia: Map[String, Any] = {
//    import sandbox.ToMap._
//    import sandbox.ToMapoid._
//
//    toMap(entity)
//  }

}

case class Level2CC(timestamp: LocalDateTime, value: Int)
case class Level1CC(id: UUID, sub: Level2CC)

object Level1CCGenerator {

  def generate: Level1CC =
    Level1CC(
      id = UUID.randomUUID(),
      sub = Level2CC(
        timestamp = LocalDateTime.now(),
        value = Random.nextInt(1000000)
      )
    )

}

object ToMap_byHand {

  def toMap(entity: Level1CC): Map[String, Any] =
    Map(
      ("id", entity.id),
      ("sub.timestamp", entity.sub.timestamp),
      ("sub.value", entity.sub.value)
    )

}

object ToMap_javaReflection {

  def toMap(entity: AnyRef): Map[String, Any] = {
    def toMapCC(entity: AnyRef): Map[String, Any] =
      entity.getClass.getDeclaredFields.foldLeft(Map(): Map[String, Any]) {
        case (m, field) ⇒
          val accessible = field.isAccessible
          field.setAccessible(true)
          val subMap =
            field.get(entity) match {
              case v: Product ⇒
                val sm = toMapCC(v)
                sm.map { case (sk, sv) ⇒ (field.getName + "." + sk) → sv }
              case v ⇒
                Map(field.getName → v)
            }
          field.setAccessible(accessible)
          m ++ subMap
      }

    toMapCC(entity)
  }

  def main(args: Array[String]): Unit = {
    val entity = Level1CCGenerator.generate
    println(entity)
    println(toMap(entity))
  }

}

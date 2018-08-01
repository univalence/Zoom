package zoom.bench.sandbox

import java.time.LocalDateTime
import java.util.UUID

import org.openjdk.jmh.annotations._

import scala.collection.Iterator
import scala.collection.immutable.Map
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

object ToMap_byHand2 {

  class ToMap_byHandWrapper(val entity: Level1CC) extends  Map[String, Any]  {
    type A = String
    type B = Any

    override def size = 3

    def get(key: String): Option[Any] =
      key match {
        case "id" => Some(entity.id)
        case "sub.timestamp" => Some(entity.sub.timestamp)
        case "sub.value" => Some(entity.sub.value)
        case _ => None
      }

    def iterator = Iterator(
      ("id", entity.id),
      ("sub.timestamp", entity.sub.timestamp),
      ("sub.value", entity.sub.value))


    lazy val proxyMap: Map[A, Any] = iterator.toMap

    override def updated[B1 >: B](key: A, value: B1): Map[A, B1] = proxyMap.updated(key, value)

    def +[B1 >: B](kv: (A, B1)): Map[A, B1] = updated(kv._1, kv._2)

    def -(key: A): Map[A, B] = proxyMap - key

    override def foreach[U](f: ((A, B)) => U): Unit = {
      f(("id",entity.id))
      f(("sub.timestamp", entity.sub.timestamp))
      f(("sub.value", entity.sub.value))
    }
  }

  def toMap(entity: Level1CC): Map[String, Any] = new ToMap_byHandWrapper(entity)
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

package sandbox
/*
import scala.language.implicitConversions

object ShapelessMain {

  import ToMap2.toMap

  def main(args: Array[String]): Unit = {
    println(toMap(Whatever1("hello")))
    println(toMap(Whatever2(Whatever1("world"))))
  }

}

case class Whatever1(value: String)

case class Whatever2(value: Whatever1)

trait LowPriorityToMap {
  implicit def toMapLP[A](entity: A): Map[Option[String], Any] = Map(None → entity)
}

object HighPriorityToMap extends LowPriorityToMap {
  import shapeless._
  import shapeless.ops.record._

  def toMapHP[A <: Product, L <: HList](entity: A)(implicit gen: LabelledGeneric.Aux[A, L],
                                                   tmr: ToMap[L]): Map[Option[String], Any] = {
    val m: Map[tmr.Key, tmr.Value] = tmr(gen.to(entity))
    m.flatMap {
      case (k: Symbol, v) ⇒
        val subMap: Map[Option[String], Any] = v
        println(subMap)
        subMap.map { case (sk, sv) ⇒ Option((k.name +: sk.toList).mkString(".")) → v }
    }
  }
}

object ToMap2 {
  def toMap[A <: Product](entity: A): Map[String, Any] = {
    import sandbox.HighPriorityToMap._

    val map: Map[Option[String], Any] = toMapHP(entity)

    map.map { case (ok, v) ⇒ ok.getOrElse(s"noname:$v") → v }
  }
}
 */

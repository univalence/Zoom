package sandbox

sealed trait Path[+A]
case class Destination[+A](value: A)                    extends Path[A]
case class Crossroad[+A](left: Path[A], right: Path[A]) extends Path[A]
case class OffRoad[+A](path: Option[Path[A]])           extends Path[A]

object Path {

  def main(args: Array[String]): Unit = {
    val x: Path[Int] = OffRoad(Some(Destination(1)))
    println(Mapoid.toMapoid(x))
  }

}

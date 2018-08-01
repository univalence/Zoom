package zoom.model

sealed trait EventFormat

object EventFormat {

  def fromString(value: String): EventFormat =
    Vector(Raw, Json, CCJson, XML).find(_.toString == value).get

  case object Raw extends EventFormat

  case object Json extends EventFormat

  case object CCJson extends EventFormat {
    override def toString = "cc+Json"
  }

  case object XML extends EventFormat

  //case object Avro extends EventFormat

}

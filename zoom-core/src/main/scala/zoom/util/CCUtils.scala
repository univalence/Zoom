package zoom.util

object CCUtils {

  def rekey(str: String): String =
    str
      .map(s ⇒ if (s.isUpper) "_" + s.toLower else s.toString)
      .mkString

  def toMap[A:zoom.util.ToMap](entity: A): Map[String, String] = {

    ToMap.toMap(entity).mapValues(_.toString)
  }

}

package zoom.util

object CCUtils {

  def rekey(str: String): String =
    str
      .map(s ⇒ if (s.isUpper) "_" + s.toLower else s.toString)
      .mkString

  def toMap[A](entity: A): Map[String, String] = {
    import ToMapMagnolia.gen

    ToMap.toMap(entity).mapValues(_.toString)
  }

}

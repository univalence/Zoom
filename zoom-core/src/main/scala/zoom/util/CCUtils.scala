package zoom.util

object CCUtils {

  def rekey(str: String): String =
    str
      .map(s => if (s.isUpper) "_" + s.toLower else s.toString)
      .mkString

  def toMap[A](entity: A): Map[String, String] =
    entity.getClass.getDeclaredFields
      .foldLeft(Map.empty[String, Any]) { (mapAcc, field) =>
        field.setAccessible(true)

        val pair: Map[String, Any] =
          field.get(entity) match {
            case Seq()           => Map.empty
            case Some(v: String) => Map(field.getName -> v)
            case None            => Map.empty
            case Some(subref: AnyRef) =>
              val subMap = toMap(subref)
              subMap.map(sm => (field.getName + "." + sm._1) -> sm._2)
            case _ =>
              Map(field.getName -> field.get(entity).toString)
          }

        mapAcc ++ pair
      }
      .map(t => rekey(t._1) -> t._2.toString)
      .filter(_._2.nonEmpty)
}

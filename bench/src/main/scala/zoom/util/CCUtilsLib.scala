package zoom.util

object CCUtilsLib {

  object ReflectionBasedToMap {

    def rekey(str: String): String =
      str
        .map(s ⇒ if (s.isUpper) "_" + s.toLower else s.toString)
        .mkString

    def toMap(entity: AnyRef): Map[String, String] =
      entity.getClass.getDeclaredFields
        .foldLeft(Map[String, String]()) { (a, field) ⇒
          field.setAccessible(true)

          val pair: Map[String, String] =
            field.get(entity) match {
              case Seq() | None | Nil ⇒ Map.empty

              case Some(v: String) ⇒ Map(field.getName → v)

              case Some(subref: AnyRef) ⇒
                val subMap = toMap(subref)
                subMap.map(sm ⇒ (field.getName + "." + sm._1) → sm._2)

              case _ ⇒ Map(field.getName → field.get(entity).toString)
            }

          a ++ pair
        }
        .map(t ⇒ rekey(t._1) → t._2)
        .filter(_._2.nonEmpty)

  }

  object FastLevel1CCToMap {

    class ToMap_byHandWrapper(val entity: Level1CC) extends Map[String, String] {
      type A = String

      override def size = 3

      def get(key: String): Option[String] =
        key match {
          case "id"             ⇒ Some(entity.id.toString)
          case "sub1.timestamp" ⇒ Some(entity.sub1.timestamp.toString)
          case "sub1.value"     ⇒ Some(entity.sub1.value.toString)
          case "sub2.timestamp" ⇒ entity.sub2.map(_.timestamp.toString)
          case "sub2.value"     ⇒ entity.sub2.map(_.value.toString)
          case _                ⇒ None
        }

      def iterator: Iterator[(A, A)] =
        (Iterator(("id", entity.id.toString),
                  ("sub1.timestamp", entity.sub1.timestamp.toString),
                  ("sub1.value", entity.sub1.value.toString))
          ++ entity.sub2.map(e ⇒ ("sub2.timestamp", e.timestamp.toString)).toIterator
          ++ entity.sub2.map(e ⇒ ("sub2.value", e.value.toString)).toIterator)

      lazy val proxyMap: Map[A, A] = iterator.toMap

      override def updated[B1 >: A](key: A, value: B1): Map[A, B1] = proxyMap.updated(key, value)

      def +[B1 >: A](kv: (A, B1)): Map[A, B1] = updated(kv._1, kv._2)

      def -(key: A): Map[A, A] = proxyMap - key

      override def foreach[U](f: ((A, A)) ⇒ U): Unit = {
        f(("id", entity.id.toString))
        f(("sub1.timestamp", entity.sub1.timestamp.toString))
        f(("sub1.value", entity.sub1.value.toString))
        entity.sub2.foreach(e ⇒ f(("sub2.timestamp", e.timestamp.toString)))
        entity.sub2.foreach(e ⇒ f(("sub2.value", e.value.toString)))
      }
    }

    def toMap(entity: Level1CC): Map[String, String] = new ToMap_byHandWrapper(entity)
  }

}

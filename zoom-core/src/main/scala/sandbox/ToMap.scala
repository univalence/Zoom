package sandbox

/***
  *
  * ToMap, the real deal !!
  */
trait ToMap[T] {
  def toMap(t: T): Map[String, Any]
}

object ToMap {
  private def mmapToMapString(mmap: MMap): Map[String, Any] = {
    mmap.map.flatMap({
      case (k, v) ⇒
        val res: Map[String, Any] = v match {
          case mmap2: MMap             ⇒ mmapToMapString(mmap2).map({ case (k2, v2) ⇒ s"$k.$k2" → v2 })
          case MOption(None)           ⇒ Map.empty
          case MOption(Some(s: MMap))  ⇒ mmapToMapString(s)
          case MOption(Some(MLeaf(a))) ⇒ Map(k → a)
          case MLeaf(a)                ⇒ Map(k → a)
        }
        res
    })
  }

  implicit def toMapCC[A <: Product: Mapoid.Typeclass]: ToMap[A] = (t: A) ⇒ {
    implicitly[Mapoid.Typeclass[A]].show(t) match {
      case mmap: MMap ⇒ mmapToMapString(mmap)
      case _          ⇒ throw new IllegalStateException("should not be possible, how did you do it ? #sarcasm")
    }
  }

  def toMap[A: ToMap](a: A): Map[String, Any] = implicitly[ToMap[A]].toMap(a)
}

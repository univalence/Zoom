package zoom.util

object CCUtils {

  /*
  def toMapSS(cc: AnyRef with Product): Seq[(String, String)] = {
    //toMapSSWithF(cc,{case _ if false => Nil})
    ???
  }*/

  /*
  def toMapSSWithF(cc: AnyRef with Product, ext: PartialFunction[(String, Any), Seq[(String, String)]]): Seq[(String, String)] = {
    //    val res:Seq[(String,String)] = cc.getClass.getDeclaredFields.map(_.getName) // all field names
    //      .zip(cc.productIterator.toSeq).flatMap(x => {
    //      ext.orElse({
    //        case (k, None) => Nil
    //        case (k, Some(v)) => Seq(k -> v)
    //        case (k, v) => Seq(k -> v)
    //      })(x).map({case (k,v) => (k,v.toString)})
    //
    //    })
    //    res
    Nil

  }*/

  private def rekey(str: String) =
    str
      .map(s ⇒ if (s.isUpper) "_" + s.toLower else s.toString)
      .mkString

  //FIXME : Rendre + générique !
  def getCCParams2(entity: AnyRef): Map[String, String] =
    entity.getClass.getDeclaredFields
      .foldLeft(Map[String, String]()) { (a, field) ⇒
        field.setAccessible(true)

        val pair: Map[String, String] =
          field.get(entity) match {
            case Seq()           ⇒ Map.empty
            case Some(v: String) ⇒ Map(field.getName → v)
            case None            ⇒ Map.empty
            case Some(subref: AnyRef) ⇒
              val subMap = getCCParams2(subref)
              subMap.map(sm ⇒ (field.getName + "." + sm._1) → sm._2)
            case _ ⇒ Map(field.getName → field.get(entity).toString)
          }

        a ++ pair

      }
      .map(t ⇒ rekey(t._1) → t._2)
      .filter(_._2.nonEmpty)
}

package sandbox

object MagnoliaMain {

  def main(args: Array[String]): Unit = {
    val entity = EmbedCaseClass(SimpleCaseClass("Hello"))

    println(ToMap.toMap(entity))

    println(ToMap.toMap(WithOption(entity, None)))

    val entity2 = WithOptionCC("", None, None)

    Mapoid.gen[WithOptionCC]
    println(Mapoid.toMapoid(entity2))

  }

}

case class SimpleCaseClass(field: String)
case class EmbedCaseClass(sub_entity: SimpleCaseClass)
case class WithOption(field: EmbedCaseClass, opt: Option[String])
case class WithOptionCC(field: String, opt: Option[String], opt2: Option[SimpleCaseClass])

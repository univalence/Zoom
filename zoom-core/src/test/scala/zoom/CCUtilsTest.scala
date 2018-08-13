package zoom
import org.scalatest.{FunSuiteLike, Matchers}
import sandbox.ToTypelessMap

case class CaseClassSimple(field: String)

case class CaseClassWithEmbed(sub_entity: Option[CaseClassSimple])

trait CCBehavior extends FunSuiteLike with Matchers {

  def toMap(ccs: CaseClassSimple): Map[String, Any]
  def toMap(caseClassWithEmbed: CaseClassWithEmbed): Map[String, Any]

  test("should get map of simple entity") {
    val entity: CaseClassSimple = CaseClassSimple("Hello")

    val result = toMap(entity)

    result should have size 1
    result("field") should be("Hello")
  }

  test("should get map of entity with present subentity") {
    val sub_entity = CaseClassSimple("Hello")
    val entity     = CaseClassWithEmbed(Some(sub_entity))

    val result = toMap(entity)

    result should have size 1

    result should be(Map("sub_entity.field" → "Hello"))
  }

  test("should get map of entity with absent subentity") {
    val entity = CaseClassWithEmbed(None)

    val result = toMap(entity)

    result shouldBe empty
  }

}

class CCUtilsTest extends CCBehavior {
  override def toMap(ccs: CaseClassSimple): Map[String, Any] = CCUtils.getCCParams2(ccs)
  override def toMap(caseClassWithEmbed: CaseClassWithEmbed): Map[String, Any] =
    CCUtils.getCCParams2(caseClassWithEmbed)
}

/*
class MagnoliaImplCCUtilsTest extends CCBehavior {
  override def toMap(ccs: CaseClassSimple): Map[String, Any] = ToMap.toMap(ccs)
  override def toMap(caseClassWithEmbed: CaseClassWithEmbed): Map[String, Any] = {
    //TODO don't compile at the moment
    ??? //ToMap.toMap(caseClassWithEmbed)
  }
}
 */

class ShapelessImplCCUtilsTest extends CCBehavior {
  override def toMap(ccs: CaseClassSimple): Map[String, Any]                   = ToTypelessMap.toMap(ccs)
  override def toMap(caseClassWithEmbed: CaseClassWithEmbed): Map[String, Any] = ToTypelessMap.toMap(caseClassWithEmbed)
}

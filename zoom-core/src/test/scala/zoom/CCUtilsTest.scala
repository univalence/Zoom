package zoom
import org.scalatest.{FunSuiteLike, Matchers}

class CCUtilsTest extends FunSuiteLike with Matchers {

  test("should get map of simple entity") {
    val entity = CaseClassSimple("Hello")

    val result = CCUtils.getCCParams2(entity)

    result should have size 1
    result("field") should be("Hello")
  }

  test("should get map of entity with present subentity") {
    val sub_entity = CaseClassSimple("Hello")
    val entity     = CaseClassWithEmbed(Some(sub_entity))

    val result = CCUtils.getCCParams2(entity)

    result should have size 1
    result("sub_entity.field") should be("Hello")
  }

  test("should get map of entity with absent subentity") {
    val entity = CaseClassWithEmbed(None)

    val result = CCUtils.getCCParams2(entity)

    result shouldBe empty
  }

}

case class CaseClassSimple(field: String)

case class CaseClassWithEmbed(sub_entity: Option[CaseClassSimple])

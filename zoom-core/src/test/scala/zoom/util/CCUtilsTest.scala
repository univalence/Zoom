package zoom.util

import org.scalatest.{FunSuiteLike, Matchers}

class CCUtilsTest extends FunSuiteLike with Matchers {

  def toMap[A](entity: A): Map[String, String] = CCUtils.toMap(entity)

  test("should get map of simple entity") {
    val entity = CaseClassSimple("Hello")

    val result = toMap(entity)

    result should have size 1
    result("field") should be("Hello")
  }

  test("should get map of entity with defined option") {
    val entity = CaseClassWithOption(Some("Hello"))

    val result = toMap(entity)

    result should have size 1
    result("field") should be("Hello")
  }

  test("should get map of entity with empty option") {
    val entity = CaseClassWithOption(None)

    val result = toMap(entity)

    result shouldBe empty
  }

  test("should get map of entity with present subentity") {
    val sub_entity = CaseClassSimple("Hello")
    val entity     = CaseClassWithEmbed(Some(sub_entity))

    val result = toMap(entity)

    result should have size 1
    result("sub_entity.field") should be("Hello")
  }

  test("should get map of entity with absent subentity") {
    val entity = CaseClassWithEmbed(None)

    val result = toMap(entity)

    result shouldBe empty
  }

}

case class CaseClassSimple(field: String)
case class CaseClassWithOption(field: Option[String])
case class CaseClassWithEmbed(sub_entity: Option[CaseClassSimple])

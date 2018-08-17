package zoom.util

import java.util.UUID

import org.scalatest.{FunSuiteLike, Matchers}
import zoom.callsite.CallSiteInfo
import zoom.model.Environment.Production
import zoom.model.EventFormat.Json
import zoom.model.EventMetadata

class CCUtilsTest extends FunSuiteLike with Matchers {

  import CCUtils._

  test("should get map of simple entity") {
    val entity = CaseClassSimple("Hello")

    val result = getCCParams(entity)

    result should have size 1
    result("field") should be("Hello")
  }

  test("should get map of simple entity with long field name") {
    val entity = CaseClassWithLongFieldName("Hello")

    val result = getCCParams(entity)

    result should have size 1
    result("field_of_type_string") should be("Hello")
  }

  test("should get map of entity with defined option") {
    val entity = CaseClassWithOption(Some("Hello"))

    val result = getCCParams(entity)

    result should have size 1
    result("field") should be("Hello")
  }

  test("should get map of entity with empty option") {
    val entity = CaseClassWithOption(None)

    val result = getCCParams(entity)

    result shouldBe empty
  }

  test("should get map of entity with present subentity") {
    val sub_entity = CaseClassWithLongFieldName("Hello")
    val entity     = CaseClassWithEmbed(Some(sub_entity))

    val result = getCCParams(entity)

    result should have size 1
    result("sub_entity.field_of_type_string") should be("Hello")
  }

  test("should get map of entity with absent subentity") {
    val entity = CaseClassWithEmbed(None)

    val result = getCCParams(entity)

    result shouldBe empty
  }

}

case class CaseClassSimple(field: String)
case class CaseClassWithLongFieldName(fieldOfTypeString: String)
case class CaseClassWithOption(field: Option[String])
case class CaseClassWithEmbed(subEntity: Option[CaseClassWithLongFieldName])

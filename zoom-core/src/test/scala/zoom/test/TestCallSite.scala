package zoom.test

import org.scalatest.{FunSuiteLike, Matchers}
import zoom.callsite.CallSiteInfo

class TestCallSite extends FunSuiteLike with Matchers {

  val callsite: CallSiteInfo = implicitly[CallSiteInfo]

  test("callsite should on line it appears") {
    callsite.line should be(8)
  }
}

package models.v3.test

import models.v3.{Out, TestPlusZoom}
import org.scalatest.FunSuite

class TestPlusZoomTest extends FunSuite {

  test("testAsFunction") {

    val Out(state, Seq(event), _) = TestPlusZoom.asFunction(1,1)

    assert(state == 2)
    assert(event == "2")


  }

  test("should not compile") {


    //ill typed
    //TestPlusZoom.receive(???,???)
  }

}

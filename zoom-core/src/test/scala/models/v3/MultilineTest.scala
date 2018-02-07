package models.v3

import org.scalatest.FunSuite

class MultilineTest extends FunSuite {

  test("testSplitOn") {

    assert(Multiline.splitOn(Seq(1,2,3,4).toIterator)(_ => true).toSeq == Seq(Seq(1),Seq(2),Seq(3),Seq(4)))


  }

}

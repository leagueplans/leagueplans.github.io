package com.leagueplans.ui.dom.planning.details

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ListEditsTest extends AnyFreeSpec with Matchers {
  private val list = List("a", "b", "c")

  "ListEdits.delete removes only the item at that position" in {
    ListEdits.delete(List("a", "b", "a"), 2) shouldBe List("a", "b")
    ListEdits.delete(list, -1) shouldBe list
  }

  "ListEdits.replace swaps the item at that position" in {
    ListEdits.replace(list, 2, "z") shouldBe List("a", "b", "z")
    ListEdits.replace(list, 3, "z") shouldBe list
  }
}

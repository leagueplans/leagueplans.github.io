package com.leagueplans.uicommon.dom.form

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class SelectKeysTest extends AnyFreeSpec with Matchers {
  "SelectKeys" - {
    "move" - {
      "steps down and up by one" in {
        SelectKeys.move(from = 2, size = 5, "ArrowDown") shouldBe Some(3)
        SelectKeys.move(from = 2, size = 5, "ArrowUp") shouldBe Some(1)
      }

      "stops at the ends rather than wrapping" in {
        SelectKeys.move(from = 4, size = 5, "ArrowDown") shouldBe Some(4)
        SelectKeys.move(from = 0, size = 5, "ArrowUp") shouldBe Some(0)
      }

      "jumps to the ends with Home and End" in {
        SelectKeys.move(from = 2, size = 5, "Home") shouldBe Some(0)
        SelectKeys.move(from = 2, size = 5, "End") shouldBe Some(4)
      }

      "moves a page at a time, stopping at the ends" in {
        SelectKeys.move(from = 2, size = 30, "PageDown") shouldBe Some(2 + SelectKeys.pageSize)
        SelectKeys.move(from = 2, size = 30, "PageUp") shouldBe Some(0)
        SelectKeys.move(from = 25, size = 30, "PageDown") shouldBe Some(29)
      }

      "starts from the first entry going down, and the last going up, when nothing is marked" in {
        SelectKeys.move(from = -1, size = 5, "ArrowDown") shouldBe Some(0)
        SelectKeys.move(from = -1, size = 5, "ArrowUp") shouldBe Some(4)
      }

      "ignores other keys" in {
        SelectKeys.move(from = 2, size = 5, "a") shouldBe None
        SelectKeys.move(from = 2, size = 5, "Tab") shouldBe None
      }

      "doesn't move in an empty list" in {
        SelectKeys.move(from = -1, size = 0, "ArrowDown") shouldBe None
      }
    }

    "typeAhead" - {
      val skills = Vector("Agility", "Attack", "Construction", "Cooking", "Crafting", "Firemaking", "Fishing", "Fletching")

      "jumps to the first entry starting with a letter, ignoring case" in {
        SelectKeys.typeAhead(skills, from = 0, "f") shouldBe Some(5)
        SelectKeys.typeAhead(skills, from = 0, "F") shouldBe Some(5)
      }

      "looks past the marked entry for a single letter" in {
        SelectKeys.typeAhead(skills, from = 5, "f") shouldBe Some(6)
      }

      "wraps around to the start" in {
        SelectKeys.typeAhead(skills, from = 7, "a") shouldBe Some(0)
        SelectKeys.typeAhead(skills, from = 7, "f") shouldBe Some(5)
      }

      "keeps the marked entry while more of its name is typed" in {
        SelectKeys.typeAhead(skills, from = 5, "fi") shouldBe Some(5)
        SelectKeys.typeAhead(skills, from = 5, "fis") shouldBe Some(6)
      }

      "moves through the entries starting with a letter typed repeatedly" in {
        SelectKeys.typeAhead(skills, from = 5, "ff") shouldBe Some(6)
        SelectKeys.typeAhead(skills, from = 6, "fff") shouldBe Some(7)
      }

      "prefers an entry starting with the repeated letters" in {
        SelectKeys.typeAhead(Vector("Aardvark", "Apple", "Avocado"), from = 0, "aa") shouldBe Some(0)
      }

      "finds nothing when nothing matches" in {
        SelectKeys.typeAhead(skills, from = 0, "z") shouldBe None
        SelectKeys.typeAhead(skills, from = 0, "fz") shouldBe None
      }

      "searches from the start when nothing is marked" in {
        SelectKeys.typeAhead(skills, from = -1, "a") shouldBe Some(0)
        SelectKeys.typeAhead(skills, from = -1, "co") shouldBe Some(2)
      }

      "matches spaces" in {
        SelectKeys.typeAhead(Vector("Leagues V", "Main game", "Main menu"), from = -1, "main m") shouldBe Some(2)
      }

      "finds nothing in an empty list" in {
        SelectKeys.typeAhead(Vector.empty, from = -1, "a") shouldBe None
      }
    }

    "numbered" - {
      "counts on across groups" in {
        SelectKeys.numbered(List(List("a", "b"), List("c"), List("d", "e"))) shouldBe
          List(List("a" -> 0, "b" -> 1), List("c" -> 2), List("d" -> 3, "e" -> 4))
      }

      "keeps empty groups without using up numbers" in {
        SelectKeys.numbered(List(List("a"), List.empty, List("b"))) shouldBe
          List(List("a" -> 0), List.empty, List("b" -> 1))
      }

      "numbers nothing for no groups" in {
        SelectKeys.numbered(List.empty[List[String]]) shouldBe List.empty
      }
    }

    "isPrintable" - {
      "accepts a character typed alone" in {
        SelectKeys.isPrintable("a", ctrl = false, meta = false, alt = false) shouldBe true
        SelectKeys.isPrintable(" ", ctrl = false, meta = false, alt = false) shouldBe true
      }

      "rejects named keys and shortcuts" in {
        SelectKeys.isPrintable("Tab", ctrl = false, meta = false, alt = false) shouldBe false
        SelectKeys.isPrintable("a", ctrl = true, meta = false, alt = false) shouldBe false
        SelectKeys.isPrintable("a", ctrl = false, meta = true, alt = false) shouldBe false
        SelectKeys.isPrintable("a", ctrl = false, meta = false, alt = true) shouldBe false
      }
    }
  }
}

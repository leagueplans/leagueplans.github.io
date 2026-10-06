package com.leagueplans.ui.model.plan

import com.leagueplans.ui.model.plan.ItemQuantity.Problem
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ItemQuantityTest extends AnyFreeSpec with Matchers {
  "ItemQuantity.parseCount" - {
    "reads whole numbers, ignoring commas and spaces around them" in {
      ItemQuantity.parseCount("250") shouldBe Right(250)
      ItemQuantity.parseCount(" 1,250 ") shouldBe Right(1250)
    }

    "reads the bank's k, m and b shortcuts, in either case" in {
      ItemQuantity.parseCount("10k") shouldBe Right(10000)
      ItemQuantity.parseCount("10K") shouldBe Right(10000)
      ItemQuantity.parseCount("3m") shouldBe Right(3000000)
      ItemQuantity.parseCount("2b") shouldBe Right(2000000000)
    }

    "reads decimals, rounding the count down" in {
      ItemQuantity.parseCount("1.5k") shouldBe Right(1500)
      ItemQuantity.parseCount("1.2345k") shouldBe Right(1234)
      ItemQuantity.parseCount(".5m") shouldBe Right(500000)
      ItemQuantity.parseCount("2.9") shouldBe Right(2)
    }

    "rejects letters that combine, or that aren't shortcuts" in {
      ItemQuantity.parseCount("1km") shouldBe Left(Problem.Unreadable)
      ItemQuantity.parseCount("1e5") shouldBe Left(Problem.Unreadable)
      ItemQuantity.parseCount("k") shouldBe Left(Problem.Unreadable)
      ItemQuantity.parseCount("lots") shouldBe Left(Problem.Unreadable)
    }

    "rejects counts below 1" in {
      ItemQuantity.parseCount("0") shouldBe Left(Problem.BelowOne)
      ItemQuantity.parseCount("0k") shouldBe Left(Problem.BelowOne)
      ItemQuantity.parseCount("0.5") shouldBe Left(Problem.BelowOne)
      ItemQuantity.parseCount("-5") shouldBe Left(Problem.BelowOne)
    }

    "rejects counts a stack can't hold" in {
      ItemQuantity.parseCount("3b") shouldBe Left(Problem.AboveMax)
      ItemQuantity.parseCount("99999999999") shouldBe Left(Problem.AboveMax)
      ItemQuantity.parseCount("2147483647") shouldBe Right(Int.MaxValue)
    }
  }

  "ItemQuantity.parse" - {
    "reads max and all as Max" in {
      ItemQuantity.parse("Max") shouldBe Right(ItemQuantity.Max)
      ItemQuantity.parse(" all ") shouldBe Right(ItemQuantity.Max)
    }

    "reads counts as Exact" in {
      ItemQuantity.parse("25k") shouldBe Right(ItemQuantity.Exact(25000))
    }
  }
}

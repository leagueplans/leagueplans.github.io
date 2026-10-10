package com.leagueplans.ui.dom.planning.section.changelog

import com.leagueplans.ui.dom.planning.section.changelog.ChangelogEntry.Date
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ChangelogEntryTest extends AnyFreeSpec with Matchers {
  "ChangelogEntry" - {
    "Date" - {
      "should be displayed with a short month name" in {
        Date(2026, 10, 3).display shouldBe "3 Oct 2026"
      }

      "should be stored in a sortable form" in {
        Date(2026, 3, 14).stored shouldBe "2026-03-14"
      }

      "should be restored from its stored form" in {
        val date = Date(2026, 10, 3)
        Date.parse(date.stored) shouldBe Some(date)
      }

      "should not be restored from anything else" in {
        List("", "2026-1-3", "yesterday", "2026-13-01", "2026-10-32").foreach(
          Date.parse(_) shouldBe None
        )
      }

      "should be ordered by year, then month, then day" in {
        val dates = List(Date(2026, 10, 3), Date(2025, 12, 31), Date(2026, 3, 22), Date(2026, 10, 1))
        dates.sorted shouldBe List(Date(2025, 12, 31), Date(2026, 3, 22), Date(2026, 10, 1), Date(2026, 10, 3))
      }

      "should be after only the dates before it" in {
        Date(2026, 10, 3).isAfter(Date(2026, 10, 2)) shouldBe true
        Date(2026, 10, 3).isAfter(Date(2026, 10, 3)) shouldBe false
        Date(2026, 10, 3).isAfter(Date(2026, 11, 1)) shouldBe false
      }

      "should reject months and days that don't exist" in {
        an[IllegalArgumentException] should be thrownBy Date(2026, 0, 1)
        an[IllegalArgumentException] should be thrownBy Date(2026, 13, 1)
        an[IllegalArgumentException] should be thrownBy Date(2026, 1, 0)
      }
    }

    "ChangelogEntries" - {
      "should be listed newest first" in {
        val dates = ChangelogEntries.all.map(_.date)
        dates shouldBe dates.sorted(using Date.ordering.reverse)
      }
    }
  }
}

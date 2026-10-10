package com.leagueplans.ui.dom.planning.section.changelog

import com.leagueplans.ui.dom.planning.section.changelog.ChangelogEntry.Date
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ChangelogSeenTest extends AnyFreeSpec with Matchers {
  private val older = Date(2026, 10, 1)
  private val newest = Date(2026, 10, 3)

  "ChangelogSeen" - {
    "initialSeen" - {
      "should count everything as seen for a browser new to the site" in {
        ChangelogSeen.initialSeen(None, newest, returning = false) shouldBe newest
      }

      "should count only the landing page's entries as seen for a browser that used the site before" in {
        ChangelogSeen.initialSeen(None, newest, returning = true) shouldBe ChangelogSeen.lastSeenOnLandingPage
      }

      "should use what's stored" in {
        ChangelogSeen.initialSeen(Some(older), newest, returning = true) shouldBe older
      }
    }

    "isReturning" - {
      "should recognise what the planning page kept before the changelog moved" in {
        ChangelogSeen.isReturning(List("3f0c2a-selected-section")) shouldBe true
        ChangelogSeen.isReturning(List("planning-columns")) shouldBe true
      }

      "should ignore what's only kept since" in {
        ChangelogSeen.isReturning(List("last-plan", "planning-tips-hidden")) shouldBe false
      }
    }

    "isUnread" - {
      "should be true when the newest entry is after the last one seen" in {
        ChangelogSeen.isUnread(newest, older) shouldBe true
      }

      "should be false when the newest entry has been seen" in {
        ChangelogSeen.isUnread(newest, newest) shouldBe false
      }
    }
  }
}

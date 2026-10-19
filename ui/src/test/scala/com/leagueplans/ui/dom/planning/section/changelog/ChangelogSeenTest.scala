package com.leagueplans.ui.dom.planning.section.changelog

import com.leagueplans.ui.dom.planning.section.changelog.ChangelogEntry.Date
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable
import scala.scalajs.js

final class ChangelogSeenTest extends AnyFreeSpec with Matchers {
  private val older = Date(2026, 10, 1)
  private val newest = Date(2026, 10, 3)

  // ChangelogSeen reads local storage once, when it's first touched, so it has to be loaded here,
  // before any test touches it
  private val (loaded, storedAfterLoading) =
    withLocalStorage("planning-columns" -> "[]")(
      try Right(ChangelogSeen.unread) catch { case failure: Throwable => Left(failure) }
    )

  /** Tests run without local storage, so this stands one in for the browser's while `f` runs */
  private def withLocalStorage[T](stored: (String, String)*)(f: => T): (T, Map[String, String]) = {
    val entries = mutable.LinkedHashMap(stored*)
    val storage = js.Dynamic.literal(
      getItem = ((key: String) => entries.get(key).orNull): js.Function1[String, String],
      setItem = ((key: String, value: String) => entries(key) = value): js.Function2[String, String, Unit],
      key = ((index: Int) => entries.keys.drop(index).headOption.orNull): js.Function1[Int, String]
    )
    js.Object.defineProperty(
      storage.asInstanceOf[js.Object],
      "length",
      js.Dynamic.literal(get = (() => entries.size): js.Function0[Int]).asInstanceOf[js.PropertyDescriptor]
    )
    val global = js.Dynamic.global.globalThis
    global.updateDynamic("window")(js.Dynamic.literal(localStorage = storage))
    try (f, entries.toMap)
    finally js.special.delete(global, "window")
  }

  "ChangelogSeen" - {
    "when first loaded by a browser that used the site before the changelog moved" - {
      "should remember it's seen up to the landing page's last entry" in {
        loaded.left.toOption shouldBe None
        storedAfterLoading.get("changelog-seen") shouldBe Some(ChangelogSeen.lastSeenOnLandingPage.stored)
      }
    }

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

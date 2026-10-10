package com.leagueplans.ui.dom.planning.section.changelog

import com.leagueplans.ui.dom.planning.section.changelog.ChangelogEntry.Date
import com.leagueplans.ui.storage.local.LocalStorage
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var

/** Remembers, per browser, how much of the changelog has been read */
object ChangelogSeen {
  private val key = "changelog-seen"

  private val newest = ChangelogEntries.all.map(_.date).max

  private val seen: Var[Date] = {
    val initial = initialSeen(LocalStorage.get(key).flatMap(Date.parse), newest, isReturning(LocalStorage.keys()))
    LocalStorage.set(key, initial.stored)
    Var(initial)
  }

  /** Whether there are entries the viewer hasn't opened the changelog to see */
  val unread: Signal[Boolean] =
    seen.signal.map(isUnread(newest, _)).distinct

  /** Records that the viewer has now seen every entry.
    *
    * @return the date they'd seen up to before, so that entries after it can be marked as new
    */
  def markSeen(): Date = {
    val previous = seen.now()
    seen.set(newest)
    LocalStorage.set(key, newest.stored)
    previous
  }

  /** The last entry from before the changelog moved off the landing page, which is when this
    * browser started remembering what it had seen */
  private[changelog] val lastSeenOnLandingPage: Date = Date(2026, 10, 7)

  /** A browser with nothing stored is new to the site, so everything counts as seen. Browsers that
    * used the site before the changelog moved have nothing stored either, but they saw the
    * landing page's changelog, so they've seen up to its last entry. */
  def initialSeen(stored: Option[Date], newest: Date, returning: Boolean): Date =
    stored.getOrElse(if (returning) lastSeenOnLandingPage else newest)

  /** Whether the browser has used the site before, judged by what the planning page kept in local
    * storage before the changelog moved */
  def isReturning(storedKeys: Iterable[String]): Boolean =
    storedKeys.exists(key =>
      key == "planning-columns" ||
        key == "viewer-account" ||
        key.endsWith("-selected-section") ||
        key.endsWith("-collapsed-steps")
    )

  def isUnread(newest: Date, seen: Date): Boolean =
    newest.isAfter(seen)
}

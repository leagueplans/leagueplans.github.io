package com.leagueplans.ui.dom.planning.section.changelog

import com.leagueplans.ui.dom.planning.section.SectionKey

import scala.util.Try

/** One announcement on the changelog, written by hand in [[ChangelogEntries]] */
final case class ChangelogEntry(date: ChangelogEntry.Date, title: String)(val blocks: Block*)

object ChangelogEntry {
  /** @param month 1-based, so that October is 10 */
  final case class Date(year: Int, month: Int, day: Int) {
    require(month >= 1 && month <= 12, s"Month must be between 1 and 12, but was $month")
    require(day >= 1 && day <= 31, s"Day must be between 1 and 31, but was $day")

    /** For showing to users, such as "3 Oct 2026" */
    def display: String =
      s"$day ${Date.monthNames(month - 1)} $year"

    /** For storage, such as "2026-10-03". Sorts alphabetically in date order. */
    def stored: String =
      f"$year%04d-$month%02d-$day%02d"

    def isAfter(other: Date): Boolean =
      Date.ordering.gt(this, other)
  }

  object Date {
    given ordering: Ordering[Date] = Ordering.by(date => (date.year, date.month, date.day))

    private val monthNames =
      Vector("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    private val storedFormat = """(\d{4})-(\d{2})-(\d{2})""".r

    def parse(stored: String): Option[Date] =
      stored match {
        case storedFormat(year, month, day) =>
          Try(Date(year.toInt, month.toInt, day.toInt)).toOption
        case _ => None
      }
  }
}

/** Text within a block. Plain strings are shown as they are. */
type Inline = String | Keys | Link

/** A keyboard shortcut, shown as key caps joined by " + " */
final case class Keys(names: String*)

/** A link to another site */
final case class Link(text: String, href: String)

/** A bullet point is either a single [[Inline]] or a sequence of them */
type BulletItem = Inline | Seq[Inline]

sealed trait Block

final case class Para(inlines: Inline*) extends Block

final case class Bullets(items: BulletItem*) extends Block

/** @param src a bundled file from `images/changelog`
  * @param alt describes the image for those who can't see it
  */
final case class Image(src: String, alt: String, caption: Option[String] = None) extends Block

/** A short, silent, looping video. Shown with controls instead of playing by itself for people
  * who've asked their device for reduced motion.
  *
  * @param src a bundled file from `images/changelog`
  * @param alt describes the clip for those who can't see it
  */
final case class Clip(src: String, alt: String, caption: Option[String] = None) extends Block

/** A button that opens a section of the planning page, labelled with the section's icon and title */
final case class OpenSection(section: SectionKey) extends Block

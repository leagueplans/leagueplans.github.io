package com.leagueplans.ui.dom.planning

import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import org.scalajs.dom.window.localStorage

import scala.util.control.NonFatal

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object ColumnLayout {
  val planWidths: Range = 240 to 640
  val detailsWidths: Range = 260 to 620
  /** The width of the step details column while it's collapsed */
  val collapsedWidth: Int = 36
  /** How long the step details column takes to open or close, and the details to fade in or out.
    * PlanningPage.scala gives it to the stylesheets as --details-animation. */
  val detailsAnimation: FiniteDuration = 260.millis

  private val key = "planning-columns"
  private val defaults = (plan = 565, details = 350)

  /** Loads the widths last used in this browser. They apply to every plan. */
  def load(): ColumnLayout = {
    val stored =
      attempt(Option(localStorage.getItem(key))).flatten.flatMap(_.split(',') match {
        case Array(plan, details) =>
          for {
            p <- plan.toIntOption
            d <- details.toIntOption
          } yield (plan = clamp(p, planWidths), details = clamp(d, detailsWidths))
        case _ => None
      })
    new ColumnLayout(stored.getOrElse(defaults))
  }

  private def clamp(width: Int, range: Range): Int =
    width.max(range.min).min(range.max)

  // Browser storage can be unavailable, for example in private windows or when site data is
  // blocked. Remembering the layout is only a convenience, so failures are ignored.
  private def attempt[T](f: => T): Option[T] =
    try Some(f) catch { case NonFatal(_) => None }
}

/** The widths of the plan and step details columns, and whether the step details are collapsed.
  * The details column keeps its width while collapsed, so expanding it restores that width.
  *
  * The step details start collapsed, and only open when asked. Opening them along with the first
  * focused step would animate the column while the sections are busy rendering that step, so the
  * animation would stutter.
  */
final class ColumnLayout private (initial: (plan: Int, details: Int)) {
  import ColumnLayout.*

  // Kept outside the Vars because a Var set from an event handler only updates once the current
  // transaction ends, so reading the Vars back would save stale values
  private var current = (plan = initial.plan, details = initial.details, collapsed = true)

  private val planWidthVar = Var(current.plan)
  private val detailsWidthVar = Var(current.details)
  private val collapsedVar = Var(current.collapsed)

  private def update(plan: Int = current.plan, details: Int = current.details, collapsed: Boolean = current.collapsed): Unit = {
    current = (plan = plan, details = details, collapsed = collapsed)
    planWidthVar.set(plan)
    detailsWidthVar.set(details)
    collapsedVar.set(collapsed)
  }

  val planWidth: Signal[Int] = planWidthVar.signal
  val detailsCollapsed: Signal[Boolean] = collapsedVar.signal
  /** The width the details column has while it's expanded, even while it's collapsed */
  val expandedDetailsWidth: Signal[Int] = detailsWidthVar.signal
  /** The width the details column takes up, which is narrower while it's collapsed */
  val detailsWidth: Signal[Int] =
    Signal.combine(detailsWidthVar.signal, collapsedVar.signal).map((width, collapsed) =>
      if (collapsed) collapsedWidth else width
    )

  def currentPlanWidth(): Int = current.plan
  def currentDetailsWidth(): Int = current.details

  def resizePlan(requested: Int): Unit =
    update(plan = clamp(requested, planWidths))

  def resizeDetails(requested: Int): Unit =
    update(details = clamp(requested, detailsWidths))

  def toggleDetails(): Unit =
    update(collapsed = !current.collapsed)

  def collapseDetails(): Unit =
    if (!current.collapsed) update(collapsed = true)

  def save(): Unit =
    attempt(localStorage.setItem(key, s"${current.plan},${current.details}")): Unit
}

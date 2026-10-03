package com.leagueplans.ui.dom.planning.plan.history

import com.leagueplans.ui.model.common.forest.ForestHistory.Entry
import com.leagueplans.ui.model.plan.Step

/** Describes a change to a plan's steps, for example "Delete 12 steps" */
object StepChangeLabel {
  private val maxDescriptionLength = 30

  def describe(entry: Entry[Step.ID, Step]): String = {
    val states = entry.touched.nodes.toList.map(id =>
      (entry.before.nodes.get(id).flatten, entry.after.nodes.get(id).flatten)
    )
    val removed = states.count((before, after) => before.nonEmpty && after.isEmpty)
    val added = states.count((before, after) => before.isEmpty && after.nonEmpty)
    val kept = states.collect { case (Some(before), Some(after)) => (before, after) }
    val moved = kept.count((before, after) => before._2 != after._2)
    val edited = kept.collect { case ((before, _), (after, _)) if before != after => (before, after) }

    if (states.isEmpty) "Reorder steps"
    else if (removed == states.size) counted("Delete", removed)
    else if (added == states.size) counted("Add", added)
    else if (edited.isEmpty && moved == states.size) counted("Move", moved)
    else
      edited match {
        case List((before, after)) if states.size == 1 && moved == 0 =>
          s"${editLabel(before, after)} on \"${truncate(after.description)}\""
        case _ => counted("Edit", states.size)
      }
  }

  private def counted(verb: String, count: Int): String =
    if (count == 1) s"$verb step" else s"$verb $count steps"

  private def editLabel(before: Step, after: Step): String =
    List(
      Option.when(before.description != after.description)("Edit description"),
      Option.when(before.directEffects != after.directEffects)("Edit effects"),
      Option.when(before.requirements != after.requirements)("Edit requirements"),
      Option.when(before.repetitions != after.repetitions)("Edit repetitions"),
      Option.when(before.duration != after.duration)("Edit duration")
    ).flatten match {
      case List(label) => label
      case _ => "Edit step"
    }

  private def truncate(description: String): String =
    if (description.length <= maxDescriptionLength) description
    else s"${description.take(maxDescriptionLength - 1).trim}…"
}

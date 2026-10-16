package com.leagueplans.ui.dom.planning.details.repeat

import com.leagueplans.ui.dom.planning.details.TimingRows
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.history.UndoToasts
import com.leagueplans.ui.model.plan.{Repetitions, Step, StopCondition}
import com.leagueplans.ui.model.player.item.ItemMatcher
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.projection.calculation.loops.StopConditions
import com.leagueplans.ui.projection.calculation.loops.StopConditions.Leaf
import com.leagueplans.ui.projection.client.StepLoops
import com.leagueplans.ui.projection.model.StepTiming
import com.leagueplans.uicommon.dom.{Button, Popover, TextDraft, Tooltip}
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledWith
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.leagueplans.uicommon.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier, textToTextNode}
import com.raquo.laminar.codecs.StringAsIsCodec

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** How often the step repeats: a set number of times, or until conditions are met.
  *
  * Conditions are edited in place, as one group that stops when all or any of them are true. New
  * conditions start from the step's own effects (see [[ConditionDefaults]]), and those that need a
  * number typed wait as drafts, outside the plan, until it is. Choosing Until changes nothing until a
  * condition is complete, and going back to a set number restores the count the step had. A limit
  * on the repetitions is added as a condition is, and sits after them.
  *
  * Above the conditions, a line says how the loop ended, as the projection worker last found. It's
  * kept, dimmed, while the worker catches up with a change, so that it doesn't flicker.
  */
object RepeatRow {
  private enum Combinator {
    case All, Any
  }

  private final case class Draft(id: Int, condition: Leaf)

  /** The step's conditions as one flat group, or None if they're nested in a way the row can't edit */
  private def flatten(condition: StopCondition): Option[(List[Leaf], Combinator)] =
    condition match {
      case _: StopCondition.And =>
        Option.when(isChain(condition, Combinator.All))((StopConditions.leaves(condition), Combinator.All))
      case _: StopCondition.Or =>
        Option.when(isChain(condition, Combinator.Any))((StopConditions.leaves(condition), Combinator.Any))
      case leaf: Leaf =>
        Some((List(leaf), Combinator.All))
    }

  private def isChain(condition: StopCondition, combinator: Combinator): Boolean =
    (condition, combinator) match {
      case (StopCondition.And(l, r), Combinator.All) => isChain(l, combinator) && isChain(r, combinator)
      case (StopCondition.Or(l, r), Combinator.Any) => isChain(l, combinator) && isChain(r, combinator)
      case (_: StopCondition.And | _: StopCondition.Or, _) => false
      case _ => true
    }

  private def combine(leaves: List[Leaf], combinator: Combinator): StopCondition =
    leaves.reduceLeft[StopCondition]((left, right) =>
      combinator match {
        case Combinator.All => StopCondition.And(left, right)
        case Combinator.Any => StopCondition.Or(left, right)
      }
    )

  def apply(
    stepSignal: Signal[Step],
    forester: Forester[Step.ID, Step],
    timing: Signal[StepTiming],
    loops: StepLoops,
    playerBefore: Signal[Player],
    cache: Cache,
    editRequests: EventStream[Unit],
    popover: Popover,
    undoToasts: UndoToasts,
    tooltip: Tooltip
  ): L.Div = {
    val stepID = stepSignal.map(_.id).distinct
    // The details are reused as the focus moves between steps, so these say which step they're for
    val chosenUntil = Var(Option.empty[Step.ID])
    val drafts = Var(Option.empty[(Step.ID, List[Draft])])
    val limitDraft = Var(Option.empty[Step.ID])
    val lastCounts = mutable.Map.empty[Step.ID, Int]
    var nextDraftID = 0
    var current = Option.empty[Step]
    var player = Option.empty[Player]
    lazy val matcher = ItemMatcher(cache.items.values)

    val showsUntil =
      Signal.combine(stepSignal, chosenUntil.signal).map((step, chosen) =>
        step.repetitions.isInstanceOf[Repetitions.Until] || chosen.contains(step.id)
      ).distinct
    val stepDrafts =
      Signal.combine(stepID, drafts.signal).map((id, drafts) => drafts.filter(_._1 == id).fold(List.empty)(_._2))
    val group =
      stepSignal.map(_.repetitions match {
        case Repetitions.Until(condition, max) => (flatten(condition), max)
        case Repetitions.Fixed(_) => (Some((List.empty, Combinator.All)), None)
      }).distinct
    val result =
      Signal.combine(stepSignal.map(_.repetitions.isInstanceOf[Repetitions.Until]).distinct, loops.get(stepSignal)).map {
        case (false, _) => None
        case (true, None) => Some(None)
        case (true, Some(known)) => Some(Some((LoopSummary.result(known.loop, cache.items), known.current)))
      }
    val hasProblems = result.map(_.flatten.exists(_._1.isInstanceOf[LoopSummary.Result.Problems])).distinct

    def saveGroup(leaves: List[Leaf], combinator: Combinator, max: Option[Int]): Unit =
      current.foreach { step =>
        if (leaves.isEmpty) {
          forester.update(step.id, _.deepCopy(repetitions = Repetitions.once))
          chosenUntil.set(None)
          undoToasts.report("Removed the step's last condition", Some("It repeats once now"))
        } else
          forester.update(step.id, _.deepCopy(repetitions = Repetitions.Until(combine(leaves, combinator), max)))
      }

    def editGroup(f: (List[Leaf], Combinator, Option[Int]) => (List[Leaf], Combinator, Option[Int])): Unit =
      current.foreach(step =>
        step.repetitions match {
          case Repetitions.Until(condition, max) =>
            flatten(condition).foreach { (leaves, combinator) =>
              val (newLeaves, newCombinator, newMax) = f(leaves, combinator, max)
              saveGroup(newLeaves, newCombinator, newMax)
            }
          case Repetitions.Fixed(_) =>
            val (newLeaves, newCombinator, newMax) = f(List.empty, Combinator.All, None)
            if (newLeaves.nonEmpty) saveGroup(newLeaves, newCombinator, newMax)
        }
      )

    def add(leaf: Leaf): Unit =
      editGroup((leaves, combinator, max) => (leaves :+ leaf, combinator, max))

    def editDrafts(f: List[Draft] => List[Draft]): Unit =
      current.foreach(step =>
        drafts.update(existing => Some((step.id, f(existing.filter(_._1 == step.id).fold(List.empty)(_._2)))))
      )

    def start(start: ConditionDefaults.Start): Unit =
      if (start.complete) add(start.condition)
      else {
        nextDraftID += 1
        val draft = Draft(nextDraftID, start.condition)
        editDrafts(_ :+ draft)
      }

    def switchTo(until: Boolean): Unit =
      current.foreach { step =>
        (until, step.repetitions) match {
          case (true, _) =>
            chosenUntil.set(Some(step.id))
          case (false, Repetitions.Until(_, _)) =>
            val count = lastCounts.getOrElse(step.id, 1)
            chosenUntil.set(None)
            drafts.set(None)
            limitDraft.set(None)
            forester.update(step.id, _.deepCopy(repetitions = Repetitions.Fixed(count)))
            undoToasts.report(
              if (count == 1) "The step repeats once again" else s"The step repeats ${count.withCommas} times again",
              Some("Its conditions were removed")
            )
          case (false, Repetitions.Fixed(_)) =>
            chosenUntil.set(None)
            drafts.set(None)
            limitDraft.set(None)
        }
      }

    def subjectPicker(onChange: Leaf => Unit)(leaf: Leaf): Option[L.HtmlElement] =
      current.flatMap(step =>
        leaf match {
          case skill: StopCondition.SkillReaches =>
            Some(ConditionPicker.skills(ConditionDefaults.ownSkills(step), playerBefore, picked => {
              popover.close()
              onChange(ConditionFields.asLeaf(skill.copy(skill = picked)))
            }))
          case count: StopCondition.ItemCount =>
            Some(ConditionPicker.items(ConditionDefaults.ownItems(step), playerBefore, cache.items, () => matcher, picked => {
              popover.close()
              onChange(ConditionFields.fitted(ConditionFields.asLeaf(count.copy(item = picked.id)), cache.items))
            }))
          case _ =>
            None
        }
      )

    def picker(): L.HtmlElement =
      current.fold(L.div()) { step =>
        val own = ConditionDefaults.ownItems(step)
        def withPlayer(f: Player => ConditionDefaults.Start): Unit =
          player.foreach(p => start(f(p)))
        // A limit only applies once there's a condition, and a step has only one
        val canLimit = step.repetitions match {
          case Repetitions.Until(_, None) => !limitDraft.now().contains(step.id)
          case _ => false
        }
        ConditionPicker(
          ConditionDefaults.ownSkills(step),
          own,
          playerBefore,
          cache.items,
          () => matcher,
          canLimit,
          onKind = kind => {
            popover.close()
            kind match {
              case ConditionPicker.Kind.FreeSlots => start(ConditionDefaults.freeSlots)
              case ConditionPicker.Kind.Time => start(ConditionDefaults.time)
              case ConditionPicker.Kind.Limit => limitDraft.set(Some(step.id))
              case _ => ()
            }
          },
          onSkill = skill => {
            popover.close()
            withPlayer(ConditionDefaults.skill(skill, _))
          },
          onItem = item => {
            popover.close()
            withPlayer(ConditionDefaults.item(item, own.find(_.item == item.id), _, cache.items))
          }
        )
      }

    val savedRows =
      group.map(_._1.fold(List.empty)(_._1).zipWithIndex).split((leaf, index) => (index, kind(leaf))) {
        (key, initialAndIndex, leafAndIndex) =>
          val (index, initial) = (key._1, initialAndIndex._1)
          val replace = Observer[Leaf](leaf => editGroup((leaves, c, max) => (leaves.updated(index, leaf), c, max)))
          ConditionRow(
            initial,
            leafAndIndex.map(_._1),
            isDraft = false,
            cache.items,
            onChange = replace,
            onCommit = replace,
            onRemove = () => editGroup((leaves, c, max) => (leaves.patch(index, Nil, 1), c, max)),
            subjectPicker(replace.onNext),
            popover
          )
      }

    val draftRows =
      stepDrafts.split(_.id) { (id, initial, draft) =>
        val change = Observer[Leaf](leaf => editDrafts(_.map(d => if (d.id == id) d.copy(condition = leaf) else d)))
        val drop = () => editDrafts(_.filterNot(_.id == id))
        ConditionRow(
          initial.condition,
          draft.map(_.condition),
          isDraft = true,
          cache.items,
          onChange = change,
          onCommit = Observer(leaf => {
            drop()
            add(leaf)
          }),
          onRemove = drop,
          subjectPicker(change.onNext),
          popover
        )
      }

    L.div(
      L.cls(Styles.repeat),
      L.cls(Styles.hasProblems) <-- hasProblems,
      stepSignal --> { step =>
        current = Some(step)
        step.repetitions match {
          case Repetitions.Fixed(count) => lastCounts.update(step.id, count)
          case Repetitions.Until(_, _) => ()
        }
      },
      playerBefore --> (p => player = Some(p)),
      toTopRow(stepSignal, timing, showsUntil, forester, editRequests, switchTo, tooltip),
      L.child.maybe <-- showsUntil.map(Option.when(_)(
        L.div(
          L.cls(Styles.editor),
          L.child.maybe <-- result.map(_.map(toResultLine)),
          L.child.maybe <-- group.map((group, _) => group.filter(_._1.size >= 2).map((_, combinator) =>
            toCombinatorLine(combinator, picked => editGroup((leaves, _, max) => (leaves, picked, max)))
          )),
          L.child.maybe <-- Signal.combine(group, stepSignal).map {
            case ((None, _), step) =>
              // Plans can hold conditions nested in ways the row can't edit, though nothing makes them yet
              Some(L.p(
                L.cls(Styles.nested),
                step.repetitions match {
                  case Repetitions.Until(condition, _) => s"Until ${StopConditions.describe(condition, cache.items)}"
                  case Repetitions.Fixed(_) => ""
                }
              ))
            case _ =>
              None
          },
          L.children <-- savedRows,
          L.children <-- draftRows,
          L.child.maybe <-- Signal.combine(stepSignal, limitDraft.signal).map((step, draft) =>
            step.repetitions match {
              case Repetitions.Until(_, Some(_)) => Some(false)
              case Repetitions.Until(_, None) if draft.contains(step.id) => Some(true)
              case _ => None
            }
          ).distinct.map(_.map(isDraft =>
            ConditionRow.limit(
              stepSignal.map(_.repetitions match {
                case Repetitions.Until(_, Some(max)) => max.toString
                case _ => ""
              }),
              isDraft,
              onCommit = max => {
                if (max.nonEmpty) limitDraft.set(None)
                editGroup((leaves, c, _) => (leaves, c, max))
              },
              onRemove = () => {
                limitDraft.set(None)
                if (!isDraft) editGroup((leaves, c, _) => (leaves, c, None))
              }
            )
          )),
          L.button(
            L.cls(Styles.add),
            L.typ("button"),
            "+ Add a condition",
            popover.closesWithAnchor,
            L.inContext(node =>
              List(
                L.cls(Styles.open) <-- popover.isAnchoredTo(node.ref),
                L.onClick --> (_ =>
                  if (popover.isOpenOn(node.ref)) popover.close()
                  else popover.open(node.ref, picker(), below = true, alignStart = true)
                )
              )
            )
          )
        )
      ))
    )
  }

  private def kind(leaf: Leaf): Int =
    leaf match {
      case _: StopCondition.SkillReaches => 0
      case _: StopCondition.ItemCount => 1
      case _: StopCondition.FreeInventorySlots => 2
      case _: StopCondition.TimeElapsed => 3
    }

  /** The row's icon and label, the switch between a set number and Until, and the set number */
  private def toTopRow(
    stepSignal: Signal[Step],
    timing: Signal[StepTiming],
    showsUntil: Signal[Boolean],
    forester: Forester[Step.ID, Step],
    editRequests: EventStream[Unit],
    switchTo: Boolean => Unit,
    tooltip: Tooltip
  ): L.Div = {
    val count = stepSignal.map(_.repetitions match {
      case Repetitions.Fixed(n) => n
      case Repetitions.Until(_, _) => 1
    })
    val commits = EventBus[Int]()
    val draft = TextDraft[Int](_.toString, (_, text) => TimingRows.parseRepetitions(text), commits.writer)

    L.div(
      commits.events.withCurrentValueOf(stepSignal) --> { (reps, step) =>
        if (step.repetitions != Repetitions.Fixed(reps)) forester.update(step.id, _.deepCopy(repetitions = Repetitions.Fixed(reps)))
      },
      editRequests.sample(stepSignal).collect(_.repetitions match { case Repetitions.Fixed(n) => n }) --> draft.start,
      L.div(
        L.cls(Styles.top),
        icon(loopIconPath).amend(L.svg.cls(Styles.icon)),
        L.span(
          L.cls(Styles.label),
          "Repeat",
          tooltip.register(
            L.span(L.cls(Styles.tooltip), L.strong("Repeat"), L.span("How many times this step and its substeps happen, one after another.")),
            FloatingConfig.basicTooltip(Placement.left)
          )
        ),
        L.div(
          L.cls(Styles.switch),
          L.role("group"),
          L.aria.label("How the step repeats"),
          List(false -> "×N", true -> "Until").map((until, text) =>
            L.button(
              L.typ("button"),
              L.cls(Styles.on) <-- showsUntil.map(_ == until),
              L.aria.pressed <-- showsUntil.map(shows => (shows == until).toString),
              text,
              L.onClick --> (_ => switchTo(until))
            )
          )
        ),
        L.child.maybe <-- showsUntil.map(until => Option.when(!until)(
          L.span(
            L.cls(Styles.count),
            L.child <-- draft.isEditing.splitBoolean(
              whenFalse = _ =>
                Button(_.handledWith(_.sample(count)) --> draft.start).amend(
                  L.cls(Styles.countButton),
                  L.aria.label("Change how many times this step repeats"),
                  L.b(L.cls(Styles.value), L.text <-- count.map(n => s"×$n")),
                  L.child.maybe <-- timing.map(timing =>
                    Option.when(timing.insideLoop && timing.totalReps > 1)(
                      L.span(L.cls(Styles.note), s"×${timing.totalReps.withCommas} in its loop")
                    )
                  )
                ),
              whenTrue = _ =>
                L.input(L.cls(Styles.countInput), L.inputMode("numeric"), L.aria.label("Times to repeat"), draft.input)
            )
          )
        ))
      ),
      L.child.maybe <-- draft.isEditing.map(isEditing =>
        Option.when(isEditing)(
          L.div(
            L.cls(Styles.help),
            L.child.maybe <-- draft.status.map(_.map {
              case Right(n) => L.span(L.cls(Styles.valid), s"✓ ×$n · Enter to save, Esc to cancel")
              case Left(error) => L.span(L.cls(Styles.invalid), error)
            })
          )
        )
      )
    )
  }

  private def toCombinatorLine(combinator: Combinator, onPick: Combinator => Unit): L.Div =
    L.div(
      L.cls(Styles.line),
      "Stop when",
      L.div(
        L.cls(Styles.switch),
        L.role("group"),
        L.aria.label("Whether all or any of the conditions stop the step"),
        List(Combinator.All -> "all", Combinator.Any -> "any").map((option, text) =>
          L.button(
            L.typ("button"),
            L.when(option == combinator)(L.cls(Styles.on)),
            L.aria.pressed((option == combinator).toString),
            text,
            L.onClick --> (_ => if (option != combinator) onPick(option))
          )
        )
      ),
      "are true"
    )

  private def toResultLine(result: Option[(LoopSummary.Result, Boolean)]): L.Div =
    result match {
      case None =>
        L.div(L.cls(Styles.pending), "Working out how many times that takes…")
      case Some((LoopSummary.Result.Stops(text), current)) =>
        L.div(L.cls(Styles.met), L.when(!current)(L.cls(Styles.stale)), s"✓ $text")
      case Some((LoopSummary.Result.Problems(messages), current)) =>
        L.div(L.cls(Styles.problems), L.when(!current)(L.cls(Styles.stale)), messages.map(L.p(_)))
    }

  /** The repeat row's arrows */
  private val loopIconPath: String = "M3 7V6a2 2 0 0 1 2-2h8l-2-2M13 9v1a2 2 0 0 1-2 2H3l2 2"

  /** The arrows stopped by a bar, for a limit on the repetitions */
  private[repeat] val limitIconPath: String = "M2 7V6a2 2 0 0 1 2-2h6l-2-2M10 12H4l2 2M13.5 3v10"

  /** A stroked icon, drawn as the repeat row's arrows are */
  private[repeat] def icon(path: String): L.SvgElement =
    L.svg.svg(
      L.svg.viewBox("0 0 16 16"),
      L.svg.fill("none"),
      L.svg.stroke("currentColor"),
      L.svg.strokeWidth("1.6"),
      L.svg.strokeLineCap("round"),
      L.svg.strokeLineJoin("round"),
      L.svg.svgAttr("aria-hidden", StringAsIsCodec, namespace = None)("true"),
      L.svg.path(L.svg.d(path))
    )

  @js.native @JSImport("/styles/planning/details/repeatRow.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val repeat: String = js.native
    val hasProblems: String = js.native
    val top: String = js.native
    val icon: String = js.native
    val label: String = js.native
    val switch: String = js.native
    val on: String = js.native
    val count: String = js.native
    val countButton: String = js.native
    val countInput: String = js.native
    val value: String = js.native
    val note: String = js.native
    val help: String = js.native
    val valid: String = js.native
    val invalid: String = js.native
    val tooltip: String = js.native
    val editor: String = js.native
    val line: String = js.native
    val pending: String = js.native
    val nested: String = js.native
    val add: String = js.native
    val open: String = js.native
    val met: String = js.native
    val problems: String = js.native
    val stale: String = js.native
  }
}

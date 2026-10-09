package com.leagueplans.ui.dom.planning.details

import com.leagueplans.ui.dom.planning.StepEditor
import com.leagueplans.ui.dom.planning.details.RowSelection.{Command, Kind, Row}
import com.leagueplans.ui.dom.planning.drag.DragSession
import com.leagueplans.ui.dom.planning.drag.DragSession.Dragged
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.FocusController
import com.leagueplans.ui.model.plan.{Effect, ExpMultiplier, Plan, Requirement, Step}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.projection.calculation.{EffectResolver, TimeKeeper}
import com.leagueplans.ui.projection.model.StepError
import com.leagueplans.uicommon.dom.*
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, textToTextNode}
import org.scalajs.dom.{HTMLElement, KeyValue}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Everything about the focused step: where it sits in the plan, its description, timings,
  * problems, effects and requirements */
object StepDetails {
  /** @param settings the plan's settings, which say how effects apply
    * @param stepEditor changes the step's effects and requirements
    */
  def apply(
    cache: Cache,
    stepSignal: Signal[Step],
    errorsSignal: Signal[List[StepError]],
    forester: Forester[Step.ID, Step],
    focusController: FocusController,
    timeKeeper: TimeKeeper,
    selection: RowSelection,
    dragSession: DragSession,
    settings: Signal[Plan.Settings],
    playerBefore: Signal[Player],
    stepEditor: StepEditor,
    playerAfterAll: Signal[Player],
    fieldEditRequests: EventStream[EditRequest],
    contextMenu: ContextMenu,
    tooltip: Tooltip
  ): L.Div = {
    val effects = stepSignal.map(_.directEffects.underlying)
    val requirements = stepSignal.map(_.requirements)
    val editRequests = EventBus[Row]()

    // The row actions act outside any stream, so they read the step from here
    var current = Option.empty[Step]
    // Deleting, reordering or editing an effect can let others merge
    def updateEffects(f: List[Effect] => List[Effect]): Unit =
      current.foreach(step => stepEditor.editEffects(step.id)(f))
    def updateRequirements(f: List[Requirement] => List[Requirement]): Unit =
      current.foreach(step => stepEditor.editRequirements(step.id)(f))
    def update(kind: Kind)(edit: [T] => List[T] => List[T]): Unit =
      kind match {
        case Kind.Effects => updateEffects(edit(_))
        case Kind.Requirements => updateRequirements(edit(_))
      }

    def run(row: Row, command: Command): Unit = {
      val size = current.fold(0)(step =>
        if (row.kind == Kind.Effects) step.directEffects.underlying.size else step.requirements.size
      )
      command match {
        case Command.Delete =>
          update(row.kind)([T] => (list: List[T]) => ListEdits.delete(list, row.index))
          selection.select(Option.when(size > 1)(row.copy(index = row.index.min(size - 2))))
        case Command.EditAmount =>
          editRequests.emit(row)
      }
    }

    val errorsByKind =
      errorsSignal.map(errors =>
        errors.foldLeft((Map.empty[Int, List[String]], Map.empty[Int, List[String]])) {
          case ((effectErrors, requirementErrors), StepError(StepError.Source.Effect(i, _), message)) =>
            (effectErrors.updated(i, effectErrors.getOrElse(i, List.empty) :+ message), requirementErrors)
          case ((effectErrors, requirementErrors), StepError(StepError.Source.Requirement(i, _), message)) =>
            (effectErrors, requirementErrors.updated(i, requirementErrors.getOrElse(i, List.empty) :+ message))
        }
      )

    L.div(
      L.cls(Styles.details),
      stepSignal --> (step => current = Some(step)),
      // The selection belongs to the step it was made on
      stepSignal.map(_.id).distinct.changes --> (_ => selection.select(None)),
      L.onUnmountCallback(_ => selection.select(None)),
      selection.commands.withCurrentValueOf(selection.selected) --> {
        case (command, Some(row)) => run(row, command)
        case (_, None) => ()
      },
      L.documentEvents(_.onKeyDown).filter(event => event.key == KeyValue.Escape && !isTyping(event.target)) -->
        (_ => selection.select(None)),
      L.div(
        L.cls(Styles.header),
        Breadcrumbs(stepSignal, forester.signal, focusController, tooltip),
        DescriptionField(stepSignal, forester, fieldEditRequests.filter(_ == EditRequest.Description).mapToUnit),
        TimingRows(stepSignal, forester, timeKeeper, fieldEditRequests.filter(_ == EditRequest.Repetitions).mapToUnit, tooltip)
      ),
      L.sectionTag(
        L.cls(Styles.section),
        toHeader("Effects", effects.map(_.size), Kind.Effects, errorsByKind.map(_._1), selection),
        RowList[Effect](
          Kind.Effects,
          effects,
          Signal.combine(settings, playersAt(effects, playerBefore, settings, cache)).map((settings, playersAt) =>
            (effect, i) =>
              RowContent.of(
                effect,
                cache,
                ExpMultiplier.calculateMultiplier(settings.expMultipliers)(_, _, cache),
                playersAt.lift(i),
                contextMenu
              )
          ),
          RowAmounts.of(_, cache.items),
          RowAmounts.withAmount(cache.items),
          errors = errorsByKind.map(_._1),
          showMet = false,
          selection,
          editRequests.events.collect { case Row(Kind.Effects, i) => i },
          onReorder = Observer(reordered => updateEffects(_ => reordered)),
          onReplace = Observer((i, effect) => updateEffects(ListEdits.replace(_, i, effect))),
          onDelete = Observer(i => run(Row(Kind.Effects, i), Command.Delete)),
          onDragStarted = Observer((event, effect, i) =>
            current.foreach(step => dragSession.start(Dragged.DraggedEffect(step.id, i, effect), event))
          ),
          emptyText = "No effects",
          tooltip
        )
      ),
      L.sectionTag(
        L.cls(Styles.section),
        toHeader("Requirements", requirements.map(_.size), Kind.Requirements, errorsByKind.map(_._2), selection),
        RowList[Requirement](
          Kind.Requirements,
          requirements,
          Signal.fromValue((requirement, _) => RowContent.of(requirement, cache)),
          RowAmounts.of,
          RowAmounts.withAmount,
          errors = errorsByKind.map(_._2),
          showMet = true,
          selection,
          editRequests.events.collect { case Row(Kind.Requirements, i) => i },
          onReorder = Observer(reordered => updateRequirements(_ => reordered)),
          onReplace = Observer((i, requirement) => updateRequirements(ListEdits.replace(_, i, requirement))),
          onDelete = Observer(i => run(Row(Kind.Requirements, i), Command.Delete)),
          onDragStarted = Observer((event, requirement, i) =>
            current.foreach(step => dragSession.start(Dragged.DraggedRequirement(step.id, i, requirement), event))
          ),
          emptyText = "No requirements.",
          tooltip
        )
      ),
      SubstepSummaryElement(stepSignal, forester.signal, playerBefore, playerAfterAll, cache)
    )
  }

  /** The player each of the step's effects applies to, by position, worked out by applying the
    * step's effects in order to the player at its start. Rows use it for what an effect comes to
    * where it applies: the exp after the multiplier, and how many items Max is. Repeated steps show
    * the first repetition. */
  private def playersAt(
    effects: Signal[List[Effect]],
    playerBefore: Signal[Player],
    settings: Signal[Plan.Settings],
    cache: Cache
  ): Signal[Vector[Player]] =
    Signal.combine(effects, playerBefore, settings).map { (effects, player, settings) =>
      val resolver = EffectResolver(settings, cache)
      effects.scanLeft(player)((player, effect) => resolver.resolve(player, effect)).toVector
    }

  private def isTyping(target: org.scalajs.dom.EventTarget): Boolean =
    target match {
      case e: HTMLElement => Set("input", "textarea", "select").contains(e.tagName.toLowerCase) || e.isContentEditable
      case _ => false
    }

  // Effects and requirements are added from the sections: items' and skills' cards
  /** @param errors the problems with each row, by its index. Their count shows beside the
    *               section's, and selects the next row with a problem, so that a long list can be
    *               worked through. */
  private def toHeader(
    title: String,
    count: Signal[Int],
    kind: Kind,
    errors: Signal[Map[Int, List[String]]],
    selection: RowSelection
  ): L.HtmlElement =
    L.headerTag(
      L.cls(Styles.sectionHeader),
      L.h3(L.cls(Styles.sectionTitle), title),
      L.span(L.cls(Styles.count), L.text <-- count.map(_.toString)),
      L.child.maybe <-- errors.map(_.values.map(_.size).sum).distinct.map(problems =>
        Option.when(problems > 0)(
          Button(
            _.handled.compose(_.sample(errors, selection.selected)) --> { (errors, selected) =>
              val rows = errors.keys.toList.sorted
              val after = selected.collect { case Row(`kind`, i) => i }
              val next = after.flatMap(i => rows.find(_ > i)).getOrElse(rows.head)
              selection.select(Some(Row(kind, next)))
            }
          ).amend(
            L.cls(Styles.problemCount),
            L.aria.label(s"${if (problems == 1) "1 problem" else s"$problems problems"}. Select the next row with a problem."),
            if (problems == 1) "1 problem" else s"$problems problems"
          )
        )
      )
    )

  @js.native @JSImport("/styles/planning/details/stepDetails.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val details: String = js.native
    val header: String = js.native
    val section: String = js.native
    val sectionHeader: String = js.native
    val sectionTitle: String = js.native
    val count: String = js.native
    val problemCount: String = js.native
  }
}

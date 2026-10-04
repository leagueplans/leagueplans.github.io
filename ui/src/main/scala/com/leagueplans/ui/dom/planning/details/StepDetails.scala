package com.leagueplans.ui.dom.planning.details

import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.dom.planning.details.RowSelection.{Command, Kind, Row}
import com.leagueplans.ui.dom.planning.drag.DragSession
import com.leagueplans.ui.dom.planning.drag.DragSession.Dragged
import com.leagueplans.ui.dom.planning.editor.NewRequirementForm
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.FocusController
import com.leagueplans.ui.model.plan.{Effect, EffectList, Requirement, Step}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.projection.calculation.TimeKeeper
import com.leagueplans.ui.projection.model.StepError
import com.leagueplans.uicommon.dom.*
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handled
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.leagueplans.uicommon.wrappers.floatingui.FloatingConfig
import com.leagueplans.uicommon.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, textToTextNode}
import org.scalajs.dom.{HTMLElement, KeyValue}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Everything about the focused step: where it sits in the plan, its description, timings,
  * problems, effects and requirements */
object StepDetails {
  /** @param expMultiplierAt the exp multiplier for each skill at the start of the step */
  def apply(
    cache: Cache,
    itemFuse: Fuse[Item],
    stepSignal: Signal[Step],
    errorsSignal: Signal[List[StepError]],
    forester: Forester[Step.ID, Step],
    focusController: FocusController,
    timeKeeper: TimeKeeper,
    selection: RowSelection,
    dragSession: DragSession,
    expMultiplierAt: Signal[Skill => Double],
    playerBefore: Signal[Player],
    playerAfterAll: Signal[Player],
    descriptionFocusRequests: EventStream[Unit],
    contextMenu: ContextMenu,
    tooltip: Tooltip,
    modal: Modal
  ): L.Div = {
    val effectText = EffectText(cache)
    val effects = stepSignal.map(_.directEffects.underlying)
    val requirements = stepSignal.map(_.requirements)
    val editRequests = EventBus[Row]()

    // The row actions act outside any stream, so they read the step from here
    var current = Option.empty[Step]
    def updateEffects(f: List[Effect] => List[Effect]): Unit =
      current.foreach(step => forester.update(step.id, s => s.deepCopy(directEffects = EffectList(f(s.directEffects.underlying)))))
    def updateRequirements(f: List[Requirement] => List[Requirement]): Unit =
      current.foreach(step => forester.update(step.id, s => s.deepCopy(requirements = f(s.requirements))))
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
        DescriptionField(stepSignal, forester, descriptionFocusRequests),
        TimingRows(stepSignal, forester, timeKeeper, tooltip)
      ),
      ProblemList(
        errorsSignal,
        effectText,
        Observer {
          case StepError.Source.Effect(i, _) => selection.select(Some(Row(Kind.Effects, i)))
          case StepError.Source.Requirement(i, _) => selection.select(Some(Row(Kind.Requirements, i)))
        }
      ),
      L.sectionTag(
        L.cls(Styles.section),
        toHeader("Effects", effects.map(_.size), maybeAction = None),
        RowList[Effect](
          Kind.Effects,
          effects,
          expMultiplierAt.map(multiplierAt => RowContent.of(_, cache, multiplierAt, contextMenu)),
          RowAmounts.of,
          RowAmounts.withAmount,
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
        toHeader(
          "Requirements",
          requirements.map(_.size),
          maybeAction = Some(toAddRequirementButton(itemFuse, modal, tooltip, updateRequirements))
        ),
        RowList[Requirement](
          Kind.Requirements,
          requirements,
          Signal.fromValue(RowContent.of(_, cache, effectText)),
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

  private def isTyping(target: org.scalajs.dom.EventTarget): Boolean =
    target match {
      case e: HTMLElement => Set("input", "textarea", "select").contains(e.tagName.toLowerCase) || e.isContentEditable
      case _ => false
    }

  private def toHeader(title: String, count: Signal[Int], maybeAction: Option[L.Button]): L.HtmlElement =
    L.headerTag(
      L.cls(Styles.sectionHeader),
      L.h3(L.cls(Styles.sectionTitle), title),
      L.span(L.cls(Styles.count), L.text <-- count.map(_.toString)),
      maybeAction.map(button => L.div(L.cls(Styles.headerActions), button)).getOrElse(L.emptyNode)
    )

  /** Requirements are meant to come from the sections, but until they can all be made there,
    * the old form stays reachable from here */
  private def toAddRequirementButton(
    itemFuse: Fuse[Item],
    modal: Modal,
    tooltip: Tooltip,
    updateRequirements: (List[Requirement] => List[Requirement]) => Unit
  ): L.Button = {
    val formOpener = FormOpener(
      modal,
      NewRequirementForm(itemFuse),
      _.foreach(requirement => updateRequirements(_ :+ requirement))
    )
    Button(_.handled --> (_ => formOpener.open())).amend(
      L.cls(Styles.headerButton),
      L.aria.label("Add a requirement"),
      FontAwesome.icon(FreeSolid.faPlus),
      tooltip.register(
        L.span(L.cls(Styles.tooltip), "Add a requirement"),
        FloatingConfig.basicTooltip(Placement.left)
      )
    )
  }

  @js.native @JSImport("/styles/planning/details/stepDetails.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val details: String = js.native
    val header: String = js.native
    val section: String = js.native
    val sectionHeader: String = js.native
    val sectionTitle: String = js.native
    val count: String = js.native
    val headerActions: String = js.native
    val headerButton: String = js.native
    val tooltip: String = js.native
  }
}

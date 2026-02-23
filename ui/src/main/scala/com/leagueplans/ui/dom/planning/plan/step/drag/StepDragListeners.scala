package com.leagueplans.ui.dom.planning.plan.step.drag

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.step.drag.StepDraggingStatus.DropTarget.RelativePosition
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.laminar.EventPropOps.ifUnhandled
import com.raquo.airstream.core.{EventStream, Observable, Observer, Signal}
import com.raquo.laminar.api.{L, eventPropToProcessor, seqToModifier}
import com.raquo.laminar.keys.EventProp
import org.scalajs.dom.{DOMRect, DataTransferDropEffectKind, DataTransferEffectAllowedKind, DragEvent}

import scala.scalajs.js

object StepDragListeners {
  private val contentType = "application/step-id"

  def apply(
    stepID: Step.ID,
    hasSubsteps: Signal[Boolean],
    draggingStatusObserver: Observer[StepDraggingStatus],
    header: L.Element,
    closeSubsteps: () => Unit,
    forester: Forester[Step.ID, Step]
  ): L.Modifier[L.HtmlElement] =
    List(
      onDragStart(stepID, hasSubsteps, draggingStatusObserver, header, closeSubsteps),
      onDragEnterOver(hasSubsteps, draggingStatusObserver),
      onDragLeave(draggingStatusObserver),
      onDragEnd(draggingStatusObserver),
      onDrop(stepID, hasSubsteps, forester),
    )

  private def onDragStart(
    stepID: Step.ID,
    hasSubstepsSignal: Signal[Boolean],
    draggingStatusObserver: Observer[StepDraggingStatus],
    header: L.Element,
    closeSubsteps: () => Unit
  ): L.Modifier[L.Element] =
    L.inContext { ctx =>
      val events =
        L.onDragStart
          .filterByTarget(_ == ctx.ref)
          .compose(_.withCurrentValueOf(hasSubstepsSignal))
        
      events --> { (event, hasSubsteps) =>
        event.dataTransfer.setData(contentType, stepID)
        event.dataTransfer.effectAllowed = DataTransferEffectAllowedKind.move
        event.dataTransfer.setDragImage(header.ref, 0, 0)
        draggingStatusObserver.onNext(StepDraggingStatus.Dragging(currentDropTarget = None))
        if (hasSubsteps) closeSubsteps()
      }
    }

  private def onDragEnterOver(
    hasSubsteps: Signal[Boolean],
    draggingStatusObserver: Observer[StepDraggingStatus]
  ): L.Modifier[L.Element] =
    List(
      onDragEnterOrOver(L.onDragEnter, hasSubsteps, draggingStatusObserver),
      onDragEnterOrOver(L.onDragOver, hasSubsteps, draggingStatusObserver),
    )

  private def onDragEnterOrOver(
    prop: EventProp[DragEvent],
    hasSubsteps: Signal[Boolean],
    draggingStatusObserver: Observer[StepDraggingStatus]
  ): L.Modifier[L.Element] =
    withEventHandling(prop)((ctx, events) =>
      events.map((ctx, _)).withCurrentValueOf(hasSubsteps)
    )(
      draggingStatusObserver.contramap { (ctx, event, hasSubsteps) =>
        event.dataTransfer.dropEffect = DataTransferDropEffectKind.move
        val bounds = ctx.ref.getBoundingClientRect()
        val relativeDropPosition = calcRelativeDropPosition(event, bounds, hasSubsteps)
        StepDraggingStatus.Dragging(Some(
          StepDraggingStatus.DropTarget(bounds, relativeDropPosition)
        ))
      }
    )

  private def onDragLeave(draggingStatusObserver: Observer[StepDraggingStatus]): L.Modifier[L.Element] =
    L.onDragLeave
      .ifUnhandled
      .filter(_.dataTransfer.types.exists(_ == contentType))
      .preventDefault
      .mapToStrict(StepDraggingStatus.Dragging(currentDropTarget = None)) --> draggingStatusObserver

  private def onDragEnd(draggingStatusObserver: Observer[StepDraggingStatus]): L.Modifier[L.Element] =
    L.onDragEnd
      .ifUnhandled
      .filter(_.dataTransfer.types.exists(_ == contentType))
      .preventDefault
      .mapToStrict(StepDraggingStatus.NotDragging) --> draggingStatusObserver

  private def onDrop(
    stepID: Step.ID,
    hasSubsteps: Signal[Boolean],
    forester: Forester[Step.ID, Step]
  ): L.Modifier[L.Element] =
    withEventHandling(L.onDrop)((ctx, events) =>
      events
        .withCurrentValueOf(hasSubsteps, forester.signal)
        .map { (event, hasSubsteps, forest) =>
          val dropped = Step.ID.fromString(event.dataTransfer.getData(contentType))
          val bounds = ctx.ref.getBoundingClientRect()
          val dropPosition = calcRelativeDropPosition(event, bounds, hasSubsteps)
          (dropped, dropPosition, forest)
        }
    )(
      Observer((dropped, dropLocation, forest) =>
        resolveDrop(droppedOver = stepID, dropped, dropLocation, forest, forester)
      )
    )

  private def withEventHandling[T](prop: EventProp[DragEvent])(
    f: (L.Element, EventStream[DragEvent]) => Observable[T]
  )(observer: Observer[T]): L.Modifier[L.Element] =
    L.inContext(ctx =>
      prop
        .ifUnhandled
        .filter(_.dataTransfer.types.exists(_ == contentType))
        .preventDefault
        .compose(f(ctx, _)) --> observer
    )

  private def calcRelativeDropPosition(
    event: DragEvent,
    boundingDropCoords: DOMRect,
    hasSubsteps: Boolean
  ): RelativePosition = {
    val position = (event.pageY - boundingDropCoords.top) / boundingDropCoords.height
    if (hasSubsteps) {
      if (position < 0.5) RelativePosition.Before else RelativePosition.After
    } else {
      if (position < 0.25)
        RelativePosition.Before
      else if (position < 0.75)
        RelativePosition.Into
      else
        RelativePosition.After
    }
  }

  private def resolveDrop(
    droppedOver: Step.ID,
    dropped: Step.ID,
    relativeDropPosition: RelativePosition,
    forest: Forest[Step.ID, Step], 
    forester: Forester[Step.ID, Step],
  ): Unit =
    if (dropped != droppedOver) {
      val wouldCauseLoop = forest.ancestors(droppedOver).contains(dropped)
      if (!wouldCauseLoop) {
        relativeDropPosition match {
          case RelativePosition.Into =>
            forester.move(child = dropped, newParent = droppedOver)

          case RelativePosition.Before | RelativePosition.After =>
            val maybeParent = forest.toParent.get(droppedOver)
            val neighbours = maybeParent match {
              case Some(parent) => forest.toChildren(parent).filterNot(_ == dropped)
              case None => forest.roots.filterNot(_ == dropped)
            }
            // We earlier checked that dropped != droppedOver, so droppedOver is in the list
            val (before, `droppedOver` :: after) = neighbours.span(_ != droppedOver): @unchecked
            val newOrder =
              if (relativeDropPosition == RelativePosition.Before)
                ((before :+ dropped) :+ droppedOver) ++ after
              else
                ((before :+ droppedOver) :+ dropped) ++ after

            maybeParent match {
              case Some(parent) => forester.move(dropped, parent)
              case None => forester.promoteToRoot(dropped)
            }
            forester.reorder(newOrder)
        }
      }
    }
}

package com.leagueplans.ui.dom.planning.plan.step

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.laminar.EventPropOps.ifUnhandled
import com.raquo.airstream.core.{EventStream, Observable, Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor, seqToModifier}
import com.raquo.laminar.keys.EventProp
import org.scalajs.dom.{DataTransferDropEffectKind, DataTransferEffectAllowedKind, DragEvent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StepDragListeners {
  private val contentType = "application/step-id"

  private enum DropLocation {
    case Before, Into, After

    def style: String =
      this match {
        case Before => Styles.dropBefore
        case Into => Styles.dropInto
        case After => Styles.dropAfter
      }
  }

  def apply(
    stepID: Step.ID,
    hasSubsteps: Signal[Boolean],
    isDraggingObserver: Observer[Boolean],
    header: L.Element,
    closeSubsteps: () => Unit,
    forester: Forester[Step.ID, Step]
  ): L.Modifier[L.HtmlElement] = {
    val dropLocation = Var(Option.empty[DropLocation])

    List(
      L.child.maybe <-- toStyling(dropLocation.signal),
      onDragStart(stepID, hasSubsteps, isDraggingObserver, header, closeSubsteps),
      onDragEnterOver(hasSubsteps, dropLocation.someWriter),
      onDragLeave(dropLocation.writer),
      onDragEnd(isDraggingObserver),
      onDrop(stepID, hasSubsteps, dropLocation.writer, forester),
    )
  }

  @js.native @JSImport("/styles/planning/plan/step/drag.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val dropBefore: String = js.native
    val dropInto: String = js.native
    val dropAfter: String = js.native
  }

  private def toStyling(dropLocation: Signal[Option[DropLocation]]): Signal[Option[L.Div]] =
    dropLocation.signal.map(_.map(location =>
      L.div(L.cls(location.style))
    ))

  private def onDragStart(
    stepID: Step.ID,
    hasSubstepsSignal: Signal[Boolean],
    isDraggingObserver: Observer[Boolean],
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
        isDraggingObserver.onNext(true)
        if (hasSubsteps) closeSubsteps()
      }
    }

  private def onDragEnterOver(
    hasSubsteps: Signal[Boolean],
    locationObserver: Observer[DropLocation]
  ): L.Modifier[L.Element] =
    List(
      onDragEnterOrOver(L.onDragEnter, hasSubsteps, locationObserver),
      onDragEnterOrOver(L.onDragOver, hasSubsteps, locationObserver),
    )

  private def onDragEnterOrOver(
    prop: EventProp[DragEvent],
    hasSubsteps: Signal[Boolean],
    locationObserver: Observer[DropLocation]
  ): L.Modifier[L.Element] =
    withEventHandling(prop)((ctx, events) =>
      events.map((ctx, _)).withCurrentValueOf(hasSubsteps)
    )(
      locationObserver.contramap { (ctx, event, hasSubsteps) =>
        event.dataTransfer.dropEffect = DataTransferDropEffectKind.move
        calcDropLocation(event, ctx, hasSubsteps)
      }
    )

  private def onDragLeave(locationObserver: Observer[Option[DropLocation]]): L.Modifier[L.Element] =
    L.onDragLeave
      .ifUnhandled
      .filter(_.dataTransfer.types.exists(_ == contentType))
      .preventDefault
      .mapToStrict(None) --> locationObserver

  private def onDragEnd(isDraggingObserver: Observer[Boolean]): L.Modifier[L.Element] =
    L.onDragEnd
      .ifUnhandled
      .filter(_.dataTransfer.types.exists(_ == contentType))
      .preventDefault
      .mapToStrict(false) --> isDraggingObserver

  private def onDrop(
    stepID: Step.ID,
    hasSubsteps: Signal[Boolean],
    locationObserver: Observer[Option[DropLocation]],
    forester: Forester[Step.ID, Step]
  ): L.Modifier[L.Element] =
    withEventHandling(L.onDrop)((ctx, events) =>
      events
        .withCurrentValueOf(hasSubsteps, forester.signal)
        .map { (event, hasSubsteps, forest) =>
          val dropped = Step.ID.fromString(event.dataTransfer.getData(contentType))
          (dropped, calcDropLocation(event, ctx, hasSubsteps), forest)
        }
    )(
      Observer.combine(
        locationObserver.contramap(_ => None),
        Observer((dropped, dropLocation, forest) =>
          resolveDrop(droppedOver = stepID, dropped, dropLocation, forest, forester)
        )
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

  private def calcDropLocation(
    event: DragEvent,
    element: L.Element,
    hasSubsteps: Boolean
  ): DropLocation = {
    val boundingRect = element.ref.getBoundingClientRect()
    val position = (event.pageY - boundingRect.top) / boundingRect.height
    if (hasSubsteps) {
      if (position < 0.5) DropLocation.Before else DropLocation.After
    } else {
      if (position < 0.25)
        DropLocation.Before
      else if (position < 0.75)
        DropLocation.Into
      else
        DropLocation.After
    }
  }

  private def resolveDrop(
    droppedOver: Step.ID,
    dropped: Step.ID,
    location: DropLocation,
    forest: Forest[Step.ID, Step], 
    forester: Forester[Step.ID, Step],
  ): Unit =
    if (dropped != droppedOver) {
      val wouldCauseLoop = forest.ancestors(droppedOver).contains(dropped)
      if (!wouldCauseLoop) {
        location match {
          case DropLocation.Into =>
            forester.move(child = dropped, newParent = droppedOver)

          case DropLocation.Before | DropLocation.After =>
            val maybeParent = forest.toParent.get(droppedOver)
            val neighbours = maybeParent match {
              case Some(parent) => forest.toChildren(parent).filterNot(_ == dropped)
              case None => forest.roots.filterNot(_ == dropped)
            }
            // We earlier checked that dropped != droppedOver, so droppedOver is in the list
            val (before, `droppedOver` :: after) = neighbours.span(_ != droppedOver): @unchecked
            val newOrder =
              if (location == DropLocation.Before)
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

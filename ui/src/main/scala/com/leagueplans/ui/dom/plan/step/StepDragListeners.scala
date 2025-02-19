package com.leagueplans.ui.dom.plan.step

import com.leagueplans.ui.dom.forest.Forester
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.model.plan.Step.ID
import com.leagueplans.ui.utils.laminar.EventPropOps.ifUnhandled
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringSeqValueMapper, eventPropToProcessor, seqToModifier}
import com.raquo.laminar.keys.EventProp
import org.scalajs.dom.{DataTransferDropEffectKind, DataTransferEffectAllowedKind, DragEvent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StepDragListeners {
  private val contentType = "application/step-id"

  private enum DropLocation {
    case Before, Into, After
  }

  def apply(
    stepID: Step.ID,
    hasSubsteps: Signal[Boolean],
    header: L.Element,
    closeSubsteps: () => Unit,
    stepUpdater: Observer[Forester[Step.ID, Step] => Unit]
  ): L.Modifier[L.HtmlElement] = {
    val dropLocation = Var(Option.empty[DropLocation])

    List(
      L.cls <-- dropLocation.signal.map(_.map {
        case DropLocation.Before => Styles.dropBefore
        case DropLocation.Into => Styles.dropInto
        case DropLocation.After => Styles.dropAfter
      }.toList),
      onDragStart(stepID, hasSubsteps, header, closeSubsteps),
      onDragEnterOver(hasSubsteps, dropLocation.someWriter),
      onDragLeave(dropLocation.writer),
      onDrop(stepID, hasSubsteps, dropLocation.writer, stepUpdater)
    )
  }

  @js.native @JSImport("/styles/plan/stepDrag.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val dropBefore: String = js.native
    val dropInto: String = js.native
    val dropAfter: String = js.native
  }

  private def onDragStart(
    stepID: Step.ID,
    hasSubstepsSignal: Signal[Boolean],
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
    withDropLocation(prop, hasSubsteps)(
      locationObserver.contramap { (event, dropLocation) =>
        event.dataTransfer.dropEffect = DataTransferDropEffectKind.move
        dropLocation
      }
    )

  private def onDragLeave(locationObserver: Observer[Option[DropLocation]]): L.Modifier[L.Element] =
    L.onDragLeave
      .ifUnhandled
      .filter(_.dataTransfer.types.exists(_ == contentType))
      .preventDefault
      .mapToStrict(None) --> locationObserver

  private def onDrop(
    stepID: ID,
    hasSubsteps: Signal[Boolean],
    locationObserver: Observer[Option[DropLocation]],
    stepUpdater: Observer[Forester[ID, Step] => Unit]
  ): L.Modifier[L.Element] =
    withDropLocation(L.onDrop, hasSubsteps)(
      Observer.combine(
        locationObserver.contramap(_ => None),
        stepUpdater.contramap { (event, dropLocation) => forester =>
          resolveDrop(
            droppedOver = stepID,
            dropped = Step.ID.fromString(event.dataTransfer.getData(contentType)),
            dropLocation,
            forester
          )
        }
      )
    )

  private def withDropLocation(
    prop: EventProp[DragEvent],
    hasSubsteps: Signal[Boolean]
  )(observer: Observer[(DragEvent, DropLocation)]): L.Modifier[L.Element] =
    L.inContext(ctx =>
      prop
        .ifUnhandled
        .filter(_.dataTransfer.types.exists(_ == contentType))
        .preventDefault
        .compose(
          _.withCurrentValueOf(hasSubsteps).map((event, hasSubsteps) =>
            (event, calcDropLocation(event, ctx, hasSubsteps))
          )
        ) --> observer
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
    forester: Forester[Step.ID, Step]
  ): Unit =
    if (dropped != droppedOver) {
      val forest = forester.forestSignal.now()
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

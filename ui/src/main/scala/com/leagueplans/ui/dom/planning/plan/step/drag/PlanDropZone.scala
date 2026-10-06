package com.leagueplans.ui.dom.planning.plan.step.drag

import com.leagueplans.ui.dom.planning.StepEditor
import com.leagueplans.ui.dom.planning.drag.DragSession.Dragged
import com.leagueplans.ui.dom.planning.drag.{DragSession, DropRules}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.step.drag.StepDraggingStatus.DropTarget
import com.leagueplans.ui.dom.planning.plan.step.drag.StepDraggingStatus.DropTarget.RelativePosition
import com.leagueplans.ui.model.plan.Step
import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier}
import org.scalajs.dom.{DOMRect, DataTransferDropEffectKind, DragEvent, Element, Node}

import scala.collection.mutable

/** Lets things be dropped on the plan's steps: another step, to move it before, into or after
  * the one it's dropped on, or an effect or requirement from the step details, to move it into
  * that step.
  *
  * The whole plan is one drop zone, which works out the step under the pointer from the element
  * the pointer is over. Every point over the plan then belongs to a step, so the browser never
  * briefly shows the no-drop cursor as the pointer crosses the gaps and borders between them.
  */
object PlanDropZone {
  // Declared before the markers, which read them as the object is initialised
  private val stepAttribute = "step-id"
  private val headerAttribute = "step-header"

  /** Marks a step's element with its ID */
  def stepMarker(stepID: Step.ID): L.Modifier[L.HtmlElement] =
    L.dataAttr(stepAttribute)(stepID)

  /** Marks the element that effects and requirements are outlined against when dropped on a step */
  val headerMarker: L.Modifier[L.HtmlElement] =
    L.dataAttr(headerAttribute)("")

  /** @param stepEditor moves effects and requirements dropped on a step into it */
  def apply(
    forester: Forester[Step.ID, Step],
    session: DragSession,
    stepEditor: StepEditor,
    draggingStatusObserver: Observer[StepDraggingStatus]
  ): L.Modifier[L.HtmlElement] = {
    // Measured on dragenter, which fires once per element, rather than on every dragover, which
    // fires at the rate the mouse moves
    val cachedBounds = mutable.Map.empty[Element, DOMRect]
    def measure(fresh: Boolean)(element: Element): DOMRect = {
      if (fresh) cachedBounds -= element
      cachedBounds.getOrElseUpdate(element, element.getBoundingClientRect())
    }

    def onEnterOver(fresh: Boolean)(event: DragEvent): Unit =
      if (session.now().nonEmpty)
        toDropTarget(event, session, forester, measure(fresh)) match {
          case Some((_, target)) =>
            event.preventDefault()
            event.dataTransfer.dropEffect = DataTransferDropEffectKind.move
            draggingStatusObserver.onNext(StepDraggingStatus.Dragging(Some(target)))
          case None =>
            draggingStatusObserver.onNext(StepDraggingStatus.Dragging(currentDropTarget = None))
        }

    List(
      L.onDragEnter --> onEnterOver(fresh = true),
      L.onDragOver --> onEnterOver(fresh = false),
      L.inContext(zone =>
        L.onDragLeave.filter(event => session.now().nonEmpty && !isWithin(event.relatedTarget, zone.ref)) -->
          (_ => draggingStatusObserver.onNext(StepDraggingStatus.Dragging(currentDropTarget = None)))
      ),
      L.onDrop --> { event =>
        for {
          dragged <- session.now()
          (stepID, target) <- toDropTarget(event, session, forester, _.getBoundingClientRect())
        } {
          event.preventDefault()
          session.dropped(() => resolveDrop(stepID, dragged, target.relativePosition, forester, stepEditor))
        }
      },
      // The cached bounds would be stale by the next drag
      session.current.changes --> (_ => cachedBounds.clear())
    )
  }

  /** The step under the pointer, and where the dragged thing would land on it, if it can */
  private def toDropTarget(
    event: DragEvent,
    session: DragSession,
    forester: Forester[Step.ID, Step],
    measure: Element => DOMRect
  ): Option[(Step.ID, DropTarget)] =
    for {
      dragged <- session.now()
      stepElement <- stepUnder(event)
      stepID = Step.ID.fromString(stepElement.getAttribute(s"data-$stepAttribute"))
      forest = forester.signal.now()
      if DropRules.canDrop(dragged, stepID, forest)
    } yield {
      val target = dragged match {
        case _: Dragged.DraggedStep =>
          val bounds = measure(stepElement)
          val hasSubsteps = forest.toChildren.get(stepID).exists(_.nonEmpty)
          DropTarget(bounds, toRelativePosition(event, bounds, hasSubsteps))

        case _: Dragged.DraggedStepContent =>
          val header = Option(stepElement.querySelector(s":scope > [data-$headerAttribute]")).getOrElse(stepElement)
          DropTarget(measure(header), RelativePosition.Into)

        // Unreachable: DropRules rules out dropping items on steps
        case _: Dragged.DraggedItem =>
          DropTarget(measure(stepElement), RelativePosition.Into)
      }
      (stepID, target)
    }

  /** The innermost step containing the pointer. The pointer can also be over the list item that
    * wraps a step, at its edges, which counts as being over that step. */
  private def stepUnder(event: DragEvent): Option[Element] =
    event.target match {
      case element: Element =>
        Option(element.closest(s"[data-$stepAttribute]")).orElse(
          Option(element.closest("li")).flatMap(item => Option(item.querySelector(s":scope > [data-$stepAttribute]")))
        )
      case _ =>
        None
    }

  private def isWithin(target: org.scalajs.dom.EventTarget, element: Element): Boolean =
    target.isInstanceOf[Node] && element.contains(target.asInstanceOf[Node])

  private def toRelativePosition(
    event: DragEvent,
    bounds: DOMRect,
    hasSubsteps: Boolean
  ): RelativePosition = {
    val position = (event.clientY - bounds.top) / bounds.height
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
    dragged: Dragged,
    relativeDropPosition: RelativePosition,
    forester: Forester[Step.ID, Step],
    stepEditor: StepEditor
  ): Unit =
    dragged match {
      case Dragged.DraggedStep(dropped) =>
        resolveStepDrop(droppedOver, dropped, relativeDropPosition, forester)

      case content: Dragged.DraggedStepContent =>
        stepEditor.moveContent(content, droppedOver)

      // DropRules rules out dropping items on steps
      case _: Dragged.DraggedItem =>
        ()
    }

  private def resolveStepDrop(
    droppedOver: Step.ID,
    dropped: Step.ID,
    relativeDropPosition: RelativePosition,
    forester: Forester[Step.ID, Step]
  ): Unit =
    // Checked again, since the plan could have changed since the drop was shown as possible
    if (DropRules.canDrop(Dragged.DraggedStep(dropped), droppedOver, forester.signal.now())) {
      relativeDropPosition match {
        case RelativePosition.Into =>
          forester.move(child = dropped, newParent = droppedOver)

        case RelativePosition.Before | RelativePosition.After =>
          // Both steps could have been removed by another tab during the drag
          forester.batch(batch => if (batch.forest.contains(dropped) && batch.forest.contains(droppedOver)) {
            val maybeParent = batch.forest.toParent.get(droppedOver)
            val neighbours = maybeParent match {
              case Some(parent) => batch.forest.toChildren(parent).filterNot(_ == dropped)
              case None => batch.forest.roots.filterNot(_ == dropped)
            }
            // The drop rules keep a step from being dropped on itself, so droppedOver is in the list
            val (before, `droppedOver` :: after) = neighbours.span(_ != droppedOver): @unchecked
            val newOrder =
              if (relativeDropPosition == RelativePosition.Before)
                ((before :+ dropped) :+ droppedOver) ++ after
              else
                ((before :+ droppedOver) :+ dropped) ++ after

            maybeParent match {
              case Some(parent) => batch.move(dropped, parent)
              case None => batch.promoteToRoot(dropped)
            }
            batch.reorder(newOrder)
          })
      }
    }
}

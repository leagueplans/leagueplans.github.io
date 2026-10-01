package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.uicommon.dom.ToastHub
import com.leagueplans.uicommon.wrappers.Clipboard

import scala.concurrent.duration.DurationInt
import scala.scalajs.js

/** Copies, cuts and pastes steps along with their substeps */
final class StepClipboard(forester: Forester[Step.ID, Step], toastPublisher: ToastHub.Publisher) {
  private val clipboard =
    Clipboard[(Clipboard.Operation, Forest[Step.ID, Step])]("step", Decoder.decodeMessage)

  def isSupported: Boolean =
    clipboard.isSupported

  def copy(step: Step.ID): js.Promise[Unit] =
    write(step, Clipboard.Operation.Copy)

  /** The step is only moved once it's pasted */
  def cut(step: Step.ID): js.Promise[Unit] =
    write(step, Clipboard.Operation.Cut)

  /** Adds the steps on the clipboard as the last substeps of the parent */
  def paste(parent: Step.ID): js.Promise[Unit] =
    js.async {
      js.await(clipboard.read()).foreach((operation, forest) => handlePaste(parent, forest, operation))
    }

  private def write(step: Step.ID, operation: Clipboard.Operation): js.Promise[Unit] =
    clipboard.write((operation, forester.signal.now().subtree(step)))

  private def handlePaste(
    parent: Step.ID,
    forest: Forest[Step.ID, Step],
    operation: Clipboard.Operation
  ): Unit =
    (operation, forest.roots) match {
      case (Clipboard.Operation.Cut, List(step)) if forester.signal.now().contains(step) =>
        if (step == parent || forester.signal.now().ancestors(parent).contains(step))
          toastPublisher.publish(
            ToastHub.Type.Warning,
            5.seconds,
            "A step can't be pasted inside itself or one of its substeps"
          )
        else
          forester.move(step, parent)

      case _ =>
        val regeneratedForest = forest.map((_, step) => step.copy(id = Step.ID.generate()))
        forester.batch { batch =>
          regeneratedForest.roots.foreach(root =>
            regeneratedForest.get(root).foreach(
              batch.add(_, parent)
            )
          )
          regeneratedForest.foreachParent((parent, children) =>
            children.foreach(
              batch.add(_, parent.id)
            )
          )
        }
    }
}

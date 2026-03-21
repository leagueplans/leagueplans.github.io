package com.leagueplans.ui.projection.client

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.model.status.StatusTracker
import com.leagueplans.ui.projection.model.Projection
import com.leagueplans.ui.projection.worker.ProjectionProtocol
import com.leagueplans.ui.projection.worker.ProjectionProtocol.{Inbound, Outbound}
import com.leagueplans.ui.wrappers.workers.{MessagePortClient, WorkerFactory}
import com.raquo.airstream.state.{StrictSignal, Var}
import org.scalajs.dom.console

object ProjectionClient {
  val projectionStatusKey = "projection-client-projection"
  val errorDetectionStatusKey = "projection-client-error-detection"

  def apply(
    initialPlan: Forest[Step.ID, Step],
    initialSettings: Plan.Settings
  ): ProjectionClient = {
    val worker = WorkerFactory.projectionWorker()
    val port = MessagePortClient[Inbound, Outbound](worker)
    port.send(Inbound.Initialise(id = 0L, initialPlan, initialSettings))

    new ProjectionClient(
      port,
      Var(Projection(initialSettings)).distinct,
      _stepsWithErrors = Var(Map.empty).distinct,
      Var(StatusTracker.Status.Busy).distinct,
      Var(StatusTracker.Status.Busy).distinct,
      lastSentID = 0L,
      lastReceivedProjectionID = None,
      lastReceivedErrorID = None,
      lastSettledErrorDetectionStatus = StatusTracker.Status.Idle
    )
  }
}

final class ProjectionClient(
  port: MessagePortClient[Inbound, Outbound],
  _projection: Var[Projection],
  _stepsWithErrors: Var[Map[Step.ID, List[String]]],
  _projectionStatus: Var[StatusTracker.Status],
  _errorDetectionStatus: Var[StatusTracker.Status],
  private var lastSentID: Long,
  private var lastReceivedProjectionID: Option[Long],
  private var lastReceivedErrorID: Option[Long],
  private var lastSettledErrorDetectionStatus: StatusTracker.Status
) {
  export port.close

  port.setMessageHandler {
    case Outbound.Computed(id, res) =>
      if (lastReceivedProjectionID.forall(_ < id)) {
        lastReceivedProjectionID = Some(id)
        _projection.set(res)
      }

      if (lastSentID == id)
        _projectionStatus.set(StatusTracker.Status.Idle)

    case Outbound.ComputeFailed(id, reason) =>
      console.error(s"Failed to compute projection. Reason: $reason")
      if (lastSentID == id)
        _projectionStatus.set(StatusTracker.Status.Failed(reason))

    case Outbound.ErrorsComputed(id, errors) =>
      if (lastReceivedErrorID.forall(_ < id)) {
        lastReceivedErrorID = Some(id)
        _stepsWithErrors.set(errors)
      }

      if (lastSentID == id) {
        val settled = if (errors.isEmpty) StatusTracker.Status.Idle else StatusTracker.Status.Failed("Problems found")
        lastSettledErrorDetectionStatus = settled
        _errorDetectionStatus.set(settled)
      }

    case Outbound.ErrorDetectionSkipped(id) =>
      if (lastSentID == id)
        _errorDetectionStatus.set(lastSettledErrorDetectionStatus)

    case Outbound.ErrorDetectionFailed(id, reason) =>
      console.error(s"Failed to detect step errors. Reason: $reason")
      if (lastSentID == id) {
        val settled = StatusTracker.Status.Failed(reason)
        lastSettledErrorDetectionStatus = settled
        _errorDetectionStatus.set(settled)
      }
  }

  val projection: StrictSignal[Projection] =
    _projection.signal

  val projectionsStatus: StrictSignal[StatusTracker.Status] =
    _projectionStatus.signal

  val errorDetectionStatus: StrictSignal[StatusTracker.Status] =
    _errorDetectionStatus.signal

  val stepsWithErrors: StrictSignal[Map[Step.ID, List[String]]] =
    _stepsWithErrors.signal

  def initialise(forest: Forest[Step.ID, Step], settings: Plan.Settings): Unit =
    send(Inbound.Initialise(nextId(), forest, settings))

  def applyForestUpdate(update: Forest.Update[Step.ID, Step]): Unit =
    send(Inbound.ForestUpdated(nextId(), update))

  def updateSettings(settings: Plan.Settings): Unit =
    send(Inbound.SettingsChanged(nextId(), settings))

  def changeFocus(focusID: Option[Step.ID]): Unit =
    send(Inbound.FocusChanged(nextId(), focusID))

  private def send(msg: Inbound): Unit = {
    _projectionStatus.set(StatusTracker.Status.Busy)
    msg match {
      case _: Inbound.FocusChanged => // error detection doesn't depend on focus
      case _ => _errorDetectionStatus.set(StatusTracker.Status.Busy)
    }
    port.send(msg)
  }

  private def nextId(): Long = {
    lastSentID += 1
    lastSentID
  }
}

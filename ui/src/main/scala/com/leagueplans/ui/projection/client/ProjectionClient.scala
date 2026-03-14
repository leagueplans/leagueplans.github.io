package com.leagueplans.ui.projection.client

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.projection.model.Projection
import com.leagueplans.ui.projection.worker.ProjectionProtocol
import com.leagueplans.ui.projection.worker.ProjectionProtocol.{Inbound, Outbound}
import com.leagueplans.ui.wrappers.workers.{MessagePortClient, WorkerFactory}
import com.raquo.airstream.state.{StrictSignal, Var}
import org.scalajs.dom.console

object ProjectionClient {
  def apply(
    initialPlan: Forest[Step.ID, Step],
    initialSettings: Plan.Settings
  ): ProjectionClient = {
    val worker = WorkerFactory.projectionWorker()
    val port = MessagePortClient[Inbound, Outbound](worker)
    port.send(Inbound.Initialise(id = 0L, initialPlan, initialSettings))
    
    new ProjectionClient(
      port,
      Var(Projection(initialSettings)),
      Var(Status.Busy),
      lastSentID = 0L,
      lastReceivedID = None
    )
  }
  
  enum Status {
    case Idle, Busy, Failed
  }
}

final class ProjectionClient(
  port: MessagePortClient[Inbound, Outbound],
  _projection: Var[Projection],
  _status: Var[ProjectionClient.Status],
  private var lastSentID: Long,
  private var lastReceivedID: Option[Long]
) {
  export port.close
  
  port.setMessageHandler {
    case Outbound.Computed(id, res) =>
      if (lastReceivedID.forall(_ < id)) {
        lastReceivedID = Some(id)
        _projection.set(res)
      }
      
      if (lastSentID == id)
        _status.set(ProjectionClient.Status.Idle)
      
    case Outbound.ComputeFailed(id, reason) =>
      console.error(s"Failed to compute projection. Reason: $reason")
      if (lastReceivedID.forall(_ < id))
        lastReceivedID = Some(id)
        
      if (lastSentID == id)
        _status.set(ProjectionClient.Status.Failed)
  }
  
  val projection: StrictSignal[Projection] =
    _projection.signal
  
  val status: StrictSignal[ProjectionClient.Status] =
    _status.signal

  def initialise(forest: Forest[Step.ID, Step], settings: Plan.Settings): Unit =
    send(Inbound.Initialise(nextId(), forest, settings))

  def applyForestUpdate(update: Forest.Update[Step.ID, Step]): Unit =
    send(Inbound.ForestUpdated(nextId(), update))

  def updateSettings(settings: Plan.Settings): Unit =
    send(Inbound.SettingsChanged(nextId(), settings))

  def changeFocus(focusID: Option[Step.ID]): Unit =
    send(Inbound.FocusChanged(nextId(), focusID))

  private def send(msg: Inbound): Unit = {
    _status.set(ProjectionClient.Status.Busy)
    port.send(msg)
  }

  private def nextId(): Long = {
    lastSentID += 1
    lastSentID
  }
}

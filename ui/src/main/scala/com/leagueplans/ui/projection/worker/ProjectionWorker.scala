package com.leagueplans.ui.projection.worker

import com.leagueplans.ui.model.common.forest.{Forest, ForestResolver}
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.model.player.Cache
import com.leagueplans.ui.projection.calculation.Projector
import com.leagueplans.ui.projection.worker.ProjectionProtocol.{Inbound, Outbound}
import com.leagueplans.ui.projection.worker.ProjectionWorker.State
import com.leagueplans.ui.wrappers.workers.{DedicatedWorkerScope, MessagePortClient}
import org.scalajs.dom
import org.scalajs.dom.{AbortController, console}
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.util.boundary.{Label, break}
import scala.util.{Failure, Success, boundary}

private object ProjectionWorker {
  @JSExportTopLevel("run", moduleID = "projectionworker")
  def run(): Unit = {
    val scope = new DedicatedWorkerScope[Outbound, Inbound]
    val buffer = ListBuffer.empty[Inbound]
    // Buffer messages received before the cache finishes loading
    scope.port.setMessageHandler(buffer.addOne)

    js.async[Any](
      boundary {
        val cache = js.await(loadCache())
        val worker = new ProjectionWorker(cache, scope.port)
        scope.port.setMessageHandler(worker.handle)
        buffer.foreach(worker.handle)
      }
    ): Unit
  }

  private def loadCache()(using label: Label[Unit]): js.Promise[Cache] =
    js.async {
      val maybeCache = try js.await(Cache.load()) catch {
        case error: Throwable =>
          console.error("Failed to load cache", error)
          break()
      }

      maybeCache match {
        case Right(cache) => cache
        case Left((resource, error)) =>
          console.error(s"Failed to load cache ($resource)", error)
          break()
      }
    }

  private final case class State(
    forest: Forest[Step.ID, Step],
    settings: Plan.Settings,
    focusID: Option[Step.ID],
    projector: Projector,
    latestID: Long
  )
}

private final class ProjectionWorker(
  cache: Cache,
  port: MessagePortClient[Outbound, Inbound]
) {
  private var state: Option[State] = None
  private var computationPending: Boolean = false
  private var currentController: AbortController = new AbortController()

  def handle(message: Inbound): Unit = {
    currentController.abort()
    currentController = new AbortController()

    message match {
      case Inbound.Initialise(id, plan, settings) =>
        state = Some(State(plan, settings, focusID = None, Projector(settings, cache), id))

      case other if state.isEmpty =>
        port.send(Outbound.ComputeFailed(other.id, "Worker not yet initialised"))
        return

      case Inbound.ForestUpdated(id, update) =>
        state = state.map(s => s.copy(
          forest = ForestResolver.resolve(s.forest, update),
          latestID = id
        ))

      case Inbound.SettingsChanged(id, settings) =>
        state = state.map(_.copy(
          settings = settings,
          projector = Projector(settings, cache),
          latestID = id
        ))

      case Inbound.FocusChanged(id, focusID) =>
        state = state.map(_.copy(focusID = focusID, latestID = id))
    }

    scheduleComputation()
  }

  private def scheduleComputation(): Unit =
    if (!computationPending) {
      computationPending = true
      val signal = currentController.signal
      // MessageChannel macrotask — messages already in the queue still run first,
      // giving the same batching property as setTimeout(0) but with ~0ms overhead
      Future.unit.foreach(_ => computeAndSend(signal))
    }

  private def computeAndSend(signal: dom.AbortSignal): Unit = {
    computationPending = false
    state.foreach(s =>
      s.projector
        .computeAsync(s.forest, s.focusID, s.settings, signal)
        .onComplete {
          case Success(Some(projection)) => port.send(Outbound.Computed(s.latestID, projection))
          case Success(None) => // aborted; a newer computation is already scheduled
          case Failure(e) => 
            console.error("Failed to compute an updated projection", e)
            port.send(Outbound.ComputeFailed(s.latestID, e.getMessage))
        }
    )
  }
}

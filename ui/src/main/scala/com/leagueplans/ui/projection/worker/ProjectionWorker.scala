package com.leagueplans.ui.projection.worker

import com.leagueplans.ui.model.common.forest.{Forest, ForestResolver}
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.model.player.{Cache, ViewerAccount}
import com.leagueplans.ui.projection.calculation.{EffectResolver, Projector, StepErrorFinder}
import com.leagueplans.ui.projection.worker.ProjectionProtocol.{Inbound, Outbound}
import com.leagueplans.ui.projection.worker.ProjectionWorker.State
import com.leagueplans.uicommon.wrappers.workers.{DedicatedWorkerScope, MessagePortClient}
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
    focusID: Option[Step.ID],
    settings: Plan.Settings,
    account: ViewerAccount,
    projector: Projector,
    errorFinder: StepErrorFinder,
    latestID: Long,
    latestNonFocusID: Long,
    lastSuccessfulErrorID: Option[Long]
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
      case Inbound.Initialise(id, plan, settings, account) =>
        val (projector, errorFinder) = calculators(settings, account)
        state = Some(State(
          plan,
          focusID = None,
          settings,
          account,
          projector,
          errorFinder,
          latestID = id,
          latestNonFocusID = id,
          lastSuccessfulErrorID = None
        ))

      case other if state.isEmpty =>
        port.send(Outbound.ComputeFailed(other.id, "Worker not yet initialised"))
        return

      case Inbound.ForestUpdated(id, updates) =>
        state = state.map(s => s.copy(
          forest = ForestResolver.resolve(s.forest, updates),
          latestID = id,
          latestNonFocusID = id
        ))

      case Inbound.SettingsChanged(id, settings) =>
        state = state.map { s =>
          val (projector, errorFinder) = calculators(settings, s.account)
          s.copy(settings = settings, projector = projector, errorFinder = errorFinder, latestID = id, latestNonFocusID = id)
        }

      case Inbound.AccountChanged(id, account) =>
        state = state.map { s =>
          val (projector, errorFinder) = calculators(s.settings, account)
          s.copy(account = account, projector = projector, errorFinder = errorFinder, latestID = id, latestNonFocusID = id)
        }

      case Inbound.FocusChanged(id, focusID) =>
        state = state.map(_.copy(focusID = focusID, latestID = id))
    }

    scheduleComputation()
  }

  /** The viewer's account applies to the plan's starting player */
  private def calculators(settings: Plan.Settings, account: ViewerAccount): (Projector, StepErrorFinder) = {
    val resolver = EffectResolver(settings, cache)
    val initialPlayer = account.applyTo(settings.initialPlayer)
    (new Projector(initialPlayer, resolver), new StepErrorFinder(settings, initialPlayer, resolver, cache))
  }

  private def scheduleComputation(): Unit =
    if (!computationPending) {
      computationPending = true
      // MessageChannel macrotask — messages already in the queue still run first,
      // giving the same batching property as setTimeout(0) but with ~0ms overhead
      Future.unit.foreach(_ => computeProjection(currentController.signal))
    }

  private def computeProjection(signal: dom.AbortSignal): Unit = {
    computationPending = false
    state.foreach(s =>
      s.projector
        .computeAsync(s.forest, s.focusID, signal)
        .onComplete {
          case Success(Some(projection)) =>
            port.send(Outbound.Computed(s.latestID, projection))
            maybeComputeErrors()
          case Success(None) => // aborted; a newer computation is already scheduled
          case Failure(e) =>
            console.error("Failed to compute an updated projection", e)
            port.send(Outbound.ComputeFailed(s.latestID, e.getMessage))
            port.send(Outbound.ErrorDetectionFailed(s.latestID, "Unable to compute player state"))
        }
    )
  }

  private def maybeComputeErrors(): Unit = {
    val needsRecompute = state.exists(s => s.lastSuccessfulErrorID.forall(_ < s.latestNonFocusID))
    if (needsRecompute)
      Future.unit.foreach(_ => computeErrors(currentController.signal))
    else
      state.foreach(s => port.send(Outbound.ErrorDetectionSkipped(s.latestID)))
  }

  private def computeErrors(signal: dom.AbortSignal): Unit =
    state.foreach(s =>
      s.errorFinder
        .findAsync(s.forest, signal)
        .onComplete {
          case Success(Some(errors)) =>
            state = state.map(s => s.copy(lastSuccessfulErrorID = Some(s.latestNonFocusID)))
            port.send(Outbound.ErrorsComputed(s.latestID, errors))
          case Success(None) => // aborted; newer computation pending
          case Failure(e) =>
            console.error("Failed to compute step errors", e)
            port.send(Outbound.ErrorDetectionFailed(s.latestID, e.getMessage))
        }
    )
}

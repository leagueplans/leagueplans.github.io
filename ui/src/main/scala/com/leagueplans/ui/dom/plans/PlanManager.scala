package com.leagueplans.ui.dom.plans

import com.leagueplans.codec.decoding.DecodingFailure
import com.leagueplans.ui.dom.plans.PlanManager.*
import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.model.status.StatusTracker
import com.leagueplans.ui.storage.ExportedPlanDecoder
import com.leagueplans.ui.storage.client.{PendingSubscription, PlanSubscription, StorageClient}
import com.leagueplans.ui.storage.local.{LocalStorage, PlanLocalStorage}
import com.leagueplans.ui.storage.migrations.MigrationError
import com.leagueplans.ui.storage.model.errors.{FileSystemError, SubscriptionError}
import com.leagueplans.ui.storage.model.{LoadProgress, PlanID, PlanMetadata, SchemaVersion}
import com.leagueplans.uicommon.dom.ToastHub
import com.leagueplans.uicommon.utils.airstream.EventStreamOps.andThen
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.ownership.{ManualOwner, Owner, Subscription}
import com.raquo.airstream.state.{StrictSignal, Val, Var}

import scala.collection.mutable
import scala.concurrent.duration.DurationInt

object PlanManager {
  /** What the page shows */
  enum State {
    /** Reading the list of plans, before choosing one to open */
    case Starting
    /** Reading a plan from storage */
    case Opening(planID: PlanID, name: String, progress: Signal[Option[LoadProgress]])
    /** Bringing a plan made by an older version of the site up to date, before opening it */
    case Updating(planID: PlanID, name: String)
    case Open(plan: Plan, subscription: PlanSubscription)
    /** No plan is open, such as after deleting one, or when one couldn't be opened */
    case Picker(notice: Option[Notice])
    /** This browser has no plans */
    case Welcome
  }

  /** Shown above the plan picker, to say why no plan is open */
  enum Notice {
    case Info(text: String)
    case Problem(title: String, detail: String)
  }

  private val lastPlanKey = "last-plan"

  /** The plan to open when the site loads: the plan last open in this browser if it still
    * exists, otherwise the most recently changed plan */
  def choose(plans: Map[PlanID, PlanMetadata], lastOpen: Option[PlanID]): Option[PlanID] =
    lastOpen
      .filter(plans.contains)
      .orElse(plans.maxByOption((_, metadata) => metadata.timestamp.getTime()).map((id, _) => id))

  /** Combines statuses, so that a problem anywhere shows over work in progress, and work in
    * progress shows over idleness */
  def worst(a: StatusTracker.Status, b: StatusTracker.Status): StatusTracker.Status =
    (a, b) match {
      case (problem: StatusTracker.Status.Problem, _) => problem
      case (_, problem: StatusTracker.Status.Problem) => problem
      case (StatusTracker.Status.Busy, _) | (_, StatusTracker.Status.Busy) => StatusTracker.Status.Busy
      case _ => StatusTracker.Status.Idle
    }
}

/** Opens, switches between, creates and deletes plans. There's one for each tab, and it lasts as
  * long as the tab does.
  *
  * Each request to open or leave a plan replaces the one before it. A plan that's still being
  * read when the user picks another is abandoned, and results for replaced requests are dropped.
  */
final class PlanManager(val storage: StorageClient, val toasts: ToastHub.Publisher) {
  private given owner: Owner = new ManualOwner

  private val stateVar = Var[State](State.Starting)
  val state: StrictSignal[State] = stateVar.signal

  val plans: StrictSignal[Map[PlanID, PlanMetadata]] = storage.plansSignal

  /** Plans that couldn't be read, which can't be opened, but whose files can be downloaded */
  val unreadablePlans: StrictSignal[Set[PlanID]] = storage.unreadablePlansSignal

  private val waitingForSavesVar = Var(Option.empty[PlanID])
  /** The plan the user picked, while the open plan finishes saving its changes */
  val waitingForSaves: StrictSignal[Option[PlanID]] = waitingForSavesVar.signal

  /** The status of everything this tab is doing with storage: requests about the list of plans,
    * and saving changes to the open plan */
  val status: Signal[StatusTracker.Status] =
    Signal.combine(storage.status, openPlanStatus).map(worst).distinct

  /** Whether the open plan has changes that haven't been saved yet. Reading plans isn't counted,
    * since nothing's lost if the tab closes while one is being read. */
  val hasUnsavedChanges: Signal[Boolean] =
    openPlanStatus.map(_ == StatusTracker.Status.Busy).distinct

  private def openPlanStatus: Signal[StatusTracker.Status] =
    state.flatMapSwitch {
      case State.Open(_, subscription) => subscription.status
      case _ => Val(StatusTracker.Status.Idle)
    }

  private var currentRequest = 0
  private var pending = Option.empty[PendingSubscription]
  /** Plans being deleted, which can't be opened meanwhile */
  private val deleting = mutable.Set.empty[PlanID]
  /** Plans being brought up to date, with the request that last asked to open each. Asking again
    * mid-update waits for the same update, rather than starting another. */
  private val updates = mutable.Map.empty[PlanID, Int]

  /** Opens the plan last open in this browser, or the most recently changed plan */
  def start(): Unit = {
    val request = nextRequest()
    whenDone(storage.refreshPlans()) { result =>
      if (request == currentRequest)
        result match {
          case Left(error) =>
            stateVar.set(State.Picker(Some(Notice.Problem("Couldn't read your plans", error.message))))
          case Right(()) =>
            val unreadable = unreadablePlans.now().size
            choose(plans.now(), LocalStorage.get(lastPlanKey).map(PlanID.fromString)) match {
              case Some(planID) =>
                if (unreadable == 1)
                  warnOfUnreadablePlans("Couldn't read one of your plans", "its file")
                else if (unreadable > 1)
                  warnOfUnreadablePlans(s"Couldn't read $unreadable of your plans", "their files")
                open(planID)
              // The plan picker lists plans that couldn't be read, so that they don't look lost
              case None if unreadable > 0 => stateVar.set(State.Picker(None))
              case None => stateVar.set(State.Welcome)
            }
        }
    }
  }

  private def warnOfUnreadablePlans(title: String, files: String): Unit =
    toasts.publish(
      ToastHub.Type.Warning,
      20.seconds,
      title,
      Some(s"You can download $files from the plan list, to share when asking for help.")
    )

  /** Re-reads the list of plans, which doesn't hear about plans that other tabs create */
  def refreshPlans(): Unit =
    storage.refreshPlans(): Unit

  /** Does nothing if the plan is already open, or being opened or deleted */
  def open(planID: PlanID): Unit =
    if (deleting.contains(planID)) ()
    else if (isOpenOrOpening(planID))
      // Stops waiting to switch away, if the user had picked another plan
      if (waitingForSaves.now().nonEmpty) nextRequest(): Unit
      else ()
    else {
      val request = nextRequest()
      afterSaves(planID, request) {
        plans.now().get(planID) match {
          case Some(metadata) if updates.contains(planID) =>
            updates.update(planID, request)
            leave(State.Updating(planID, metadata.name))
          case Some(metadata) if metadata.schemaVersion != SchemaVersion.values.last =>
            update(request, planID, metadata)
          case maybeMetadata =>
            subscribe(request, planID, maybeMetadata.map(_.name).getOrElse("your plan"))
        }
      }
    }

  private def isOpenOrOpening(planID: PlanID): Boolean =
    stateVar.now() match {
      case State.Opening(id, _, _) => id == planID
      case State.Updating(id, _) => id == planID
      case State.Open(_, subscription) => subscription.planID == planID
      case _ => false
    }

  /** Saves a new plan and opens it. A name that's already taken has a number added. */
  def create(metadata: PlanMetadata, plan: Plan): StrictSignal[Option[Either[FileSystemError, PlanID]]] = {
    val name = uniqueName(metadata.name)
    val result = storage.create(metadata.copy(name = name), plan.copy(name = name))
    whenDone(result)(_.foreach(open))
    result
  }

  /** Closes the open plan and deletes it, then shows the plan picker
    *
    * @param name the plan's name, which may have changed since it was opened
    */
  def deleteOpenPlan(name: String): Unit =
    stateVar.now() match {
      case State.Open(_, subscription) =>
        val request = nextRequest()
        val planID = subscription.planID
        deleting += planID
        leave(State.Picker(Some(Notice.Info(s"Deleting “$name”…"))))
        whenDone(storage.delete(planID)) { result =>
          deleting -= planID
          result match {
            case Right(()) =>
              PlanLocalStorage.forget(planID)
              if (LocalStorage.get(lastPlanKey).contains(planID)) LocalStorage.remove(lastPlanKey)
            case Left(_) => ()
          }
          if (request == currentRequest)
            stateVar.set(result match {
              case Right(()) if (plans.now().keySet - planID).isEmpty && unreadablePlans.now().isEmpty => State.Welcome
              case Right(()) => State.Picker(Some(Notice.Info(s"Deleted “$name”.")))
              case Left(error) => State.Picker(Some(Notice.Problem(s"Couldn't delete “$name”", error.message)))
            })
        }

      case _ => ()
    }

  private def nextRequest(): Int = {
    currentRequest += 1
    pending.foreach(_.cancel())
    pending = None
    waitingForSavesVar.set(None)
    currentRequest
  }

  /** Runs `f` once the open plan has saved its changes, unless another request comes first */
  private def afterSaves(planID: PlanID, request: Int)(f: => Unit): Unit =
    stateVar.now() match {
      case State.Open(_, subscription) =>
        waitingForSavesVar.set(Some(planID))
        whenDone(subscription.status.map {
          case StatusTracker.Status.Busy => None
          case other => Some(other)
        }) { _ =>
          if (request == currentRequest) {
            waitingForSavesVar.set(None)
            f
          }
        }
      case _ => f
    }

  private def subscribe(request: Int, planID: PlanID, name: String): Unit = {
    val subscription = storage.subscribe(planID)
    pending = Some(subscription)
    leave(State.Opening(planID, name, subscription.progress))
    whenDone(subscription.result) {
      case Right((plan, planSubscription)) if request == currentRequest =>
        pending = None
        LocalStorage.set(lastPlanKey, planID)
        stateVar.set(State.Open(plan, planSubscription))
      case Right((_, planSubscription)) =>
        planSubscription.close()
      case Left(SubscriptionError.Cancelled) =>
        ()
      case Left(error) if request == currentRequest =>
        pending = None
        stateVar.set(State.Picker(Some(Notice.Problem(s"Couldn't open “$name”", error.message))))
      case Left(_) =>
        ()
    }
  }

  /** Plans made by older versions of the site are saved again in the current format, under a new
    * ID, and then opened. The update carries on if the user picks another plan meanwhile. */
  private def update(request: Int, oldID: PlanID, metadata: PlanMetadata): Unit = {
    updates.update(oldID, request)
    leave(State.Updating(oldID, metadata.name))
    val fetched = storage.fetch(oldID).changes.collectSome
    val updated =
      fetched
        .andThen[DecodingFailure | MigrationError | FileSystemError, (PlanMetadata, Plan)](ExportedPlanDecoder.decode)
        .andThen((metadata, plan) => storage.create(metadata, plan).changes.collectSome)

    whenDone(updated.map(Some(_)).toSignal(None)) { result =>
      // Whoever asked last, if it's still the latest request, gets the plan
      val latest = updates.remove(oldID).getOrElse(request)
      result match {
        case Left(error) =>
          if (latest == currentRequest)
            stateVar.set(State.Picker(Some(Notice.Problem(s"Couldn't update “${metadata.name}”", describe(error)))))

        case Right(newID) =>
          PlanLocalStorage.move(oldID, newID)
          if (LocalStorage.get(lastPlanKey).contains(oldID)) LocalStorage.set(lastPlanKey, newID)
          whenDone(storage.delete(oldID)) {
            case Left(error) =>
              toasts.publish(
                ToastHub.Type.Warning,
                15.seconds,
                s"Updated “${metadata.name}”, but couldn't delete the old copy",
                Some(error.message)
              )
            case Right(()) => ()
          }
          if (latest == currentRequest) {
            toasts.publish(
              ToastHub.Type.Success,
              8.seconds,
              s"Brought “${metadata.name}” up to date",
              Some("It was made with an older version of the site, so it was updated as it opened.")
            )
            subscribe(latest, newID, metadata.name)
          }
      }
    }
  }

  /** Moves to a state without the open plan, and closes the plan's subscription. The state
    * changes first, so that the page stops showing the plan's status before it's closed. */
  private def leave(next: State): Unit = {
    val previous = stateVar.now()
    stateVar.set(next)
    previous match {
      case State.Open(_, subscription) => subscription.close()
      case _ => ()
    }
  }

  private def uniqueName(name: String): String = {
    val taken = plans.now().values.map(_.name).toSet
    if (!taken.contains(name)) name
    else LazyList.from(2).map(n => s"$name ($n)").find(!taken.contains(_)).get
  }

  private def describe(error: DecodingFailure | MigrationError | FileSystemError): String =
    error match {
      case failure: DecodingFailure => failure.getMessage
      case migration: MigrationError => migration.message
      case fileSystem: FileSystemError => fileSystem.message
    }

  /** Calls `f` with the signal's first defined value, then stops listening */
  private def whenDone[T](signal: Signal[Option[T]])(f: T => Unit): Unit = {
    var done = false
    var subscription = Option.empty[Subscription]
    subscription = Some(signal.foreach {
      case Some(value) if !done =>
        done = true
        subscription.foreach(_.kill())
        f(value)
      case _ => ()
    })
    if (done) subscription.foreach(_.kill())
  }
}

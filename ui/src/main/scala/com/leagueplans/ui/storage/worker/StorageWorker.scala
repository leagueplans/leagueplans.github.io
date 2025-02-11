package com.leagueplans.ui.storage.worker

import com.leagueplans.ui.storage.model.errors.{DeletionError, UpdateError, FileSystemError as UIError}
import com.leagueplans.ui.storage.opfs.{PlansDirectory, RootDirectory}
import com.leagueplans.ui.storage.worker.StorageProtocol.{Inbound, Outbound}
import com.leagueplans.ui.storage.worker.StorageWorker.convert
import com.leagueplans.ui.utils.airstream.ObservableOps.flatMapConcat
import com.leagueplans.ui.wrappers.opfs.FileSystemError as OPFSError
import com.leagueplans.ui.wrappers.workers.DedicatedWorkerScope
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.ownership.ManualOwner

import scala.scalajs.js.annotation.JSExportTopLevel

private object StorageWorker {
  @JSExportTopLevel("run", moduleID = "storageworker")
  def run(): Unit = {
    val scope = new DedicatedWorkerScope[Outbound.ToCoordinator, Inbound.ToWorker]
    val messageBus = EventBus[Inbound.ToWorker]()

    createWorker(scope)
      // combineWith will cause messages to be dropped if they arrive before
      // the worker is available. The current design assumes at most one
      // message will be queued on the port at a time.
      .combineWith(messageBus.events)
      .flatMapConcat {
        case (Left(error), _) => EventStream.fromValue(error, emitOnce = true)
        case (Right(worker), message) => worker.handle(message)
      }
      .addObserver(
        Observer(scope.port.send)
      )(using new ManualOwner)

    scope.port.setMessageHandler(messageBus.writer.onNext)
  }

  private def createWorker(
    scope: DedicatedWorkerScope[Outbound.ToCoordinator, Inbound.ToWorker]
  ): EventStream[Either[Outbound.ToCoordinator, StorageWorker]] =
    RootDirectory.from(scope).map {
      case Left(error) => Left(Outbound.WorkerFailedToStart(convert(error)))
      case Right(root) => Right(StorageWorker(root.plans))
    }

  private def convert(error: OPFSError): UIError =
    error match {
      case OPFSError.DecodingError(name, cause) =>
        UIError.Decoding(s"Failed to decode a file: [$name] - [${cause.getMessage}]")
      case OPFSError.FileDoesNotExist(name) =>
        UIError.Unexpected(s"Tried to open a file that does not exist: [$name]")
      case OPFSError.InvalidDirectoryName(name) =>
        UIError.Unexpected(s"Tried to open a directory using an invalid name: [$name]")
      case OPFSError.InvalidFileName(name) =>
        UIError.Unexpected(s"Tried to open a file using an invalid name: [$name]")
      case OPFSError.PartialFileRead(name, _, _) =>
        UIError.Unexpected(s"Could not complete the read for a file: [$name]")
      case OPFSError.PartialFileWrite(name, bytesWritten, bytesLost) =>
        UIError.Unexpected(s"Could not complete the write for a file: [$name]")
      case OPFSError.ParsingFailure(name, cause) =>
        UIError.Unexpected(
          s"Failed to parse a file: [$name] - [${cause.getMessage}]"
        )
      case OPFSError.StorageQuotaExceeded =>
        UIError.OutOfSpace
      case OPFSError.UnableToAcquireFileLock(name: String) =>
        UIError.Unexpected(s"Attempted to acquire a file that was already locked: [$name]")
      case OPFSError.UnexpectedFileSystemError(cause: Throwable) =>
        UIError.Unexpected(s"Unexpected error: [${cause.getClass.getName}: ${cause.getMessage}]")
    }
}

private final class StorageWorker(directory: PlansDirectory) {
  def handle(message: Inbound.ToWorker): EventStream[Outbound.ToCoordinator] =
    message match {
      case Inbound.ListPlans => handleListPlans()
      case create: Inbound.Create => handleCreate(create)
      case fetch: Inbound.Fetch => handleFetch(fetch)
      case read: Inbound.Read => handleRead(read)
      case update: Inbound.Update => handleUpdate(update)
      case delete: Inbound.Delete => handleDelete(delete)
    }

  private def handleListPlans(): EventStream[Outbound.ToCoordinator] =
    directory.listPlans().map {
      case Left(error) => Outbound.ListPlansFailed(convert(error))
      case Right(plans) => Outbound.Plans(plans)
    }

  private def handleCreate(message: Inbound.Create): EventStream[Outbound.ToCoordinator] =
    directory.create(message.metadata, message.plan).map {
      case Left(error) => Outbound.CreateFailed(message.requestID, convert(error))
      case Right(planID) => Outbound.CreateSucceeded(message.requestID, planID)
    }

  private def handleFetch(message: Inbound.Fetch): EventStream[Outbound.ToCoordinator] =
    directory.fetch(message.planID).map {
      case Left(error) => Outbound.FetchFailed(message.planID, convert(error))
      case Right(plan) => Outbound.FetchSucceeded(message.planID, plan)
    }

  private def handleRead(message: Inbound.Read): EventStream[Outbound.ToCoordinator] =
    directory.read(message.planID).map {
      case Left(error) => Outbound.ReadFailed(message.planID, convert(error))
      case Right(plan) => Outbound.ReadSucceeded(message.planID, plan)
    }

  private def handleUpdate(message: Inbound.Update): EventStream[Outbound.ToCoordinator] =
    directory.applyUpdate(message.planID, message.update.merge).map {
      case Left(error) =>
        Outbound.UpdateFailed(
          message.planID,
          message.lamport,
          UpdateError.FileSystem(convert(error))
        )
      case Right(_) =>
        Outbound.UpdateSucceeded(message.planID, message.lamport)
    }

  private def handleDelete(message: Inbound.Delete): EventStream[Outbound.ToCoordinator] =
    directory.delete(message.planID).map {
      case Left(error) =>
        Outbound.DeleteFailed(
          message.planID,
          DeletionError.FileSystem(convert(error))
        )
      case Right(_) =>
        Outbound.DeleteSucceeded(message.planID)
    }
}

package com.leagueplans.ui.storage.worker

import com.leagueplans.ui.storage.model.errors.{DeletionError, UpdateError, FileSystemError as UIError}
import com.leagueplans.ui.storage.opfs.{PlansDirectory, RootDirectory}
import com.leagueplans.ui.storage.worker.PlanStorage.convert
import com.leagueplans.ui.storage.worker.StorageProtocol.{Inbound, Outbound}
import com.leagueplans.uicommon.wrappers.opfs.{DirectoryHandle, FileSystemError as OPFSError}
import com.leagueplans.uicommon.wrappers.workers.WorkerScope
import com.raquo.airstream.core.EventStream

private object PlanStorage {
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

/** Reads and writes plans in the OPFS. Requests must not overlap, so the coordinator
  * waits for each to finish before starting the next. */
private final class PlanStorage(scope: WorkerScope) {
  private var plansDirectory: Option[PlansDirectory[DirectoryHandle]] = None

  def handle(message: Inbound.ToWorker): EventStream[Outbound.ToCoordinator] =
    getPlansDirectory().flatMapSwitch {
      case Left(error) => EventStream.fromValue(failed(message, convert(error)), emitOnce = true)
      case Right(directory) => handle(directory, message)
    }

  // A failed attempt isn't remembered, so the next request will try again
  private def getPlansDirectory(): EventStream[Either[OPFSError, PlansDirectory[DirectoryHandle]]] =
    plansDirectory match {
      case Some(directory) =>
        EventStream.fromValue(Right(directory), emitOnce = true)
      case None =>
        RootDirectory.from(scope).map(_.map { root =>
          plansDirectory = Some(root.plans)
          root.plans
        })
    }

  private def failed(message: Inbound.ToWorker, error: UIError): Outbound.ToCoordinator =
    message match {
      case list: Inbound.ListPlans => Outbound.ListPlansFailed(list.requestID, error)
      case create: Inbound.Create => Outbound.CreateFailed(create.requestID, error)
      case fetch: Inbound.Fetch => Outbound.FetchFailed(fetch.requestID, fetch.planID, error)
      case read: Inbound.Read => Outbound.ReadFailed(read.planID, error)
      case update: Inbound.Update =>
        Outbound.UpdateFailed(update.planID, update.lamport, UpdateError.FileSystem(error))
      case delete: Inbound.Delete =>
        Outbound.DeleteFailed(delete.requestID, delete.planID, DeletionError.FileSystem(error))
    }

  private def handle(
    directory: PlansDirectory[DirectoryHandle],
    message: Inbound.ToWorker
  ): EventStream[Outbound.ToCoordinator] =
    message match {
      case list: Inbound.ListPlans => handleListPlans(directory, list)
      case create: Inbound.Create => handleCreate(directory, create)
      case fetch: Inbound.Fetch => handleFetch(directory, fetch)
      case read: Inbound.Read => handleRead(directory, read)
      case update: Inbound.Update => handleUpdate(directory, update)
      case delete: Inbound.Delete => handleDelete(directory, delete)
    }

  private def handleListPlans(
    directory: PlansDirectory[DirectoryHandle],
    message: Inbound.ListPlans
  ): EventStream[Outbound.ToCoordinator] =
    directory.listPlans().map {
      case Left(error) => Outbound.ListPlansFailed(message.requestID, convert(error))
      case Right(plans) => Outbound.Plans(message.requestID, plans)
    }

  private def handleCreate(
    directory: PlansDirectory[DirectoryHandle],
    message: Inbound.Create
  ): EventStream[Outbound.ToCoordinator] =
    directory.create(message.metadata, message.plan).map {
      case Left(error) => Outbound.CreateFailed(message.requestID, convert(error))
      case Right(planID) => Outbound.CreateSucceeded(message.requestID, planID)
    }

  private def handleFetch(
    directory: PlansDirectory[DirectoryHandle],
    message: Inbound.Fetch
  ): EventStream[Outbound.ToCoordinator] =
    directory.fetch(message.planID).map {
      case Left(error) => Outbound.FetchFailed(message.requestID, message.planID, convert(error))
      case Right(plan) => Outbound.FetchSucceeded(message.requestID, message.planID, plan)
    }

  private def handleRead(
    directory: PlansDirectory[DirectoryHandle],
    message: Inbound.Read
  ): EventStream[Outbound.ToCoordinator] =
    directory.read(message.planID).map {
      case Left(error) => Outbound.ReadFailed(message.planID, convert(error))
      case Right(plan) => Outbound.ReadSucceeded(message.planID, plan)
    }

  private def handleUpdate(
    directory: PlansDirectory[DirectoryHandle],
    message: Inbound.Update
  ): EventStream[Outbound.ToCoordinator] =
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

  private def handleDelete(
    directory: PlansDirectory[DirectoryHandle],
    message: Inbound.Delete
  ): EventStream[Outbound.ToCoordinator] =
    directory.delete(message.planID).map {
      case Left(error) =>
        Outbound.DeleteFailed(
          message.requestID,
          message.planID,
          DeletionError.FileSystem(convert(error))
        )
      case Right(_) =>
        Outbound.DeleteSucceeded(message.requestID, message.planID)
    }
}

package com.leagueplans.ui.storage.worker

import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.storage.model.{PlanExport, PlanID, PlanMetadata, StepUpdates}
import com.leagueplans.ui.storage.model.errors.FileSystemError as UIError
import com.leagueplans.ui.storage.opfs.{PlansDirectory, RootDirectory}
import com.leagueplans.ui.storage.worker.PlanStorage.convert
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

  def listPlans(): EventStream[Either[UIError, Map[PlanID, PlanMetadata]]] =
    withDirectory(_.listPlans())

  def create(metadata: PlanMetadata, plan: Plan): EventStream[Either[UIError, PlanID]] =
    withDirectory(_.create(metadata, plan))

  def fetch(planID: PlanID): EventStream[Either[UIError, PlanExport]] =
    withDirectory(_.fetch(planID))

  def read(planID: PlanID): EventStream[Either[UIError, Plan]] =
    withDirectory(_.read(planID))

  def applyUpdate(planID: PlanID, update: StepUpdates | Plan.Settings): EventStream[Either[UIError, ?]] =
    withDirectory(_.applyUpdate(planID, update))

  def delete(planID: PlanID): EventStream[Either[UIError, ?]] =
    withDirectory(_.delete(planID))

  private def withDirectory[T](
    f: PlansDirectory[DirectoryHandle] => EventStream[Either[OPFSError, T]]
  ): EventStream[Either[UIError, T]] =
    getPlansDirectory().flatMapSwitch {
      case Left(error) => EventStream.fromValue(Left(convert(error)), emitOnce = true)
      case Right(directory) => f(directory).map(_.left.map(convert))
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
}

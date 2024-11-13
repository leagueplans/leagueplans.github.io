package ddm.ui.storage.opfs

import com.raquo.airstream.core.EventStream
import ddm.ui.model.common.forest.Forest
import ddm.ui.model.plan.{Plan, Step}
import ddm.ui.storage.model.{PlanExport, PlanID, PlanMetadata}
import ddm.ui.utils.airstream.EventStreamOps.{andThen, safeSequence}
import ddm.ui.wrappers.opfs.FileSystemError.*
import ddm.ui.wrappers.opfs.{DirectoryHandle, FileSystemError}

import scala.util.chaining.scalaUtilChainingOps

final class PlansDirectory(underlying: DirectoryHandle) {
  def listPlans(): EventStream[Either[UnexpectedFileSystemError, Map[PlanID, PlanMetadata]]] =
    underlying.listSubDirectories().andThen(handles =>
      handles
        .map((planID, handle) => PlanDirectory(handle).readMetadata().map(planID -> _))
        .pipe(EventStream.safeSequence)
        .map(_.collect { case (planID, Right(metadata)) => PlanID.fromString(planID) -> metadata }.toMap)
        .map(Right(_))
    )

  def create(metadata: PlanMetadata, plan: Plan): EventStream[Either[FileSystemError, PlanID]] = {
    val planID = PlanID.generate()
    
    getPlanDirectory(planID).andThen {
      case None =>
        underlying
          .acquireSubDirectory(planID)
          .andThen(handle => PlanDirectory(handle).create(metadata, plan))
          .map(_.map(_ => planID))

      // This shouldn't ever happen since plan IDs are UUIDs, but just to be sure we
      // don't accidentally overwrite a user's data, let's check
      case Some(_) =>
        EventStream.fromValue(
          Left(UnexpectedFileSystemError(
            RuntimeException(s"Generated plan ID matches an already existing ID: [$planID]")
          ))
        )
    }
  }

  def fetch(planID: PlanID): EventStream[Either[FileSystemError, PlanExport]] =
    getPlanDirectory(planID).andThen {
      case None => EventStream.fromValue(Left(FileDoesNotExist(planID)), emitOnce = true)
      case Some(planDirectory) => planDirectory.fetch()
    }
  
  def read(planID: PlanID): EventStream[Either[FileSystemError, Plan]] =
    getPlanDirectory(planID).andThen {
      case None => EventStream.fromValue(Left(FileDoesNotExist(planID)), emitOnce = true)
      case Some(planDirectory) => planDirectory.readPlan()
    }
  
  def applyUpdate(
    planID: PlanID,
    update: Forest.Update[Step.ID, Step] | Plan.Settings
  ): EventStream[Either[FileSystemError, ?]] =
    getPlanDirectory(planID).andThen {
      case None => EventStream.fromValue(Left(FileDoesNotExist(planID)), emitOnce = true)
      case Some(planDirectory) => planDirectory.applyUpdate(update)
    }
  
  def delete(planID: PlanID): EventStream[Either[FileSystemError, ?]] =
    getPlanDirectory(planID).andThen {
      case None =>
        EventStream.fromValue(Right(()), emitOnce = true)

      case Some(planDirectory) =>
        planDirectory
          .deleteContents()
          .andThen(_ => underlying.removeDirectory(planID))
    }

  private def getPlanDirectory(planID: PlanID): EventStream[Either[FileSystemError, Option[PlanDirectory]]] =
    underlying
      .getSubDirectory(planID)
      .map(_.map(_.map(PlanDirectory(_))))
}

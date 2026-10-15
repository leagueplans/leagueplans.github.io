package com.leagueplans.ui.storage.opfs

import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.storage.model.{PlanExport, PlanID, PlanList, PlanMetadata, Rename, StepUpdates}
import com.leagueplans.uicommon.utils.airstream.EventStreamOps.{andThen, safeSequence}
import com.leagueplans.uicommon.wrappers.opfs.FileSystemError.*
import com.leagueplans.uicommon.wrappers.opfs.{DirectoryHandleLike, FileSystemError}
import com.raquo.airstream.core.EventStream

import scala.util.chaining.scalaUtilChainingOps

final class PlansDirectory[T : DirectoryHandleLike](underlying: T) {
  def listPlans(): EventStream[Either[UnexpectedFileSystemError, PlanList]] =
    underlying.listSubDirectories().andThen(handles =>
      handles
        .map((planID, handle) => PlanDirectory(handle).readMetadata().map(PlanID.fromString(planID) -> _))
        .pipe(EventStream.safeSequence)
        .map(results =>
          Right(PlanList(
            results.collect { case (planID, Right(metadata)) => planID -> metadata }.toMap,
            results.collect { case (planID, Left(_)) => planID }.toSet
          ))
        )
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
  
  def read(
    planID: PlanID,
    onProgress: (stepsRead: Int, totalSteps: Int) => Unit
  ): EventStream[Either[FileSystemError, Plan]] =
    getPlanDirectory(planID).andThen {
      case None => EventStream.fromValue(Left(FileDoesNotExist(planID)), emitOnce = true)
      case Some(planDirectory) => planDirectory.readPlan(onProgress)
    }
  
  def applyUpdate(
    planID: PlanID,
    update: StepUpdates | Plan.Settings | Rename
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

  private def getPlanDirectory(planID: PlanID): EventStream[Either[FileSystemError, Option[PlanDirectory[T]]]] =
    underlying
      .getSubDirectory(planID)
      .map(_.map(_.map(PlanDirectory(_))))
}

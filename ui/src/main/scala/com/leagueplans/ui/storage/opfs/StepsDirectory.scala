package com.leagueplans.ui.storage.opfs

import com.leagueplans.codec.Encoding
import com.leagueplans.ui.model.plan.{Step, StepDetails}
import com.leagueplans.ui.storage.opfs.StepsDirectory.*
import com.leagueplans.ui.utils.airstream.EventStreamOps.{andThen, safeSequence}
import com.leagueplans.ui.wrappers.opfs.{DirectoryHandleLike, FileSystemError}
import com.raquo.airstream.core.EventStream

import scala.util.chaining.scalaUtilChainingOps

object StepsDirectory {
  private def toFileName(id: Step.ID): String =
    s"$id.bin"
  
  private def toStepID(fileName: String): Step.ID =
    Step.ID.fromString(fileName.takeWhile(_ != '.'))
}

final class StepsDirectory[T : DirectoryHandleLike](underlying: T) {
  def write(step: Step): EventStream[Either[FileSystemError, ?]] =
    underlying.replaceFileContent(toFileName(step.id), step.details)
  
  def write(steps: Iterable[Step]): EventStream[Either[FileSystemError, ?]] =
    EventStream
      .safeSequence(steps.map(write).toSeq)
      .map(results =>
        results
          .collectFirst { case l @ Left(_) => l }
          .getOrElse(Right(()))
      )
  
  def remove(id: Step.ID): EventStream[Either[FileSystemError.UnexpectedFileSystemError, ?]] =
    underlying.removeFile(toFileName(id))
  
  def read(id: Step.ID): EventStream[Either[FileSystemError, Step]] =
    underlying
      .read[StepDetails](toFileName(id))
      .map(_.map(Step(id, _)))
  
  def read(ids: Iterable[Step.ID]): EventStream[Either[FileSystemError, Map[Step.ID, Step]]] = {
    val zero: Either[FileSystemError, Map[Step.ID, Step]] = Right(Map.empty)

    ids
      .map(id => read(id).map(id -> _)).toSeq
      .pipe(EventStream.safeSequence)
      .map(_.foldLeft(zero) { case (maybeAcc, (stepID, maybeStep)) =>
        for {
          acc <- maybeAcc
          step <- maybeStep
        } yield acc + (stepID -> step)
      })
  }

  def fetch(): EventStream[Either[FileSystemError, Map[Step.ID, Encoding]]] =
    underlying.listFiles().andThen(fileNames =>
      fileNames
        .map(fileName => underlying.read[Encoding](fileName).map(fileName -> _))
        .pipe(EventStream.safeSequence)
        .map(_.collect { case (fileName, Right(encoding)) => toStepID(fileName) -> encoding }.toMap)
        .map(Right(_))
    )
}

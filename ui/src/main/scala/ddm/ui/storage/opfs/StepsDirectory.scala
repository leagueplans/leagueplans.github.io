package ddm.ui.storage.opfs

import com.raquo.airstream.core.EventStream
import ddm.codec.Encoding
import ddm.ui.model.plan.{Step, StepDetails}
import ddm.ui.storage.opfs.StepsDirectory.*
import ddm.ui.utils.airstream.EventStreamOps.andThen
import ddm.ui.wrappers.opfs.{DirectoryHandle, FileSystemError}

import scala.util.chaining.scalaUtilChainingOps

object StepsDirectory {
  private def toFileName(id: Step.ID): String =
    s"$id.bin"
  
  private def toStepID(fileName: String): Step.ID =
    Step.ID.fromString(fileName.takeWhile(_ != '.'))
}

final class StepsDirectory(underlying: DirectoryHandle) {
  def write(step: Step): EventStream[Either[FileSystemError, ?]] =
    underlying.replaceFileContent(toFileName(step.id), step.details)
  
  def write(steps: Iterable[Step]): EventStream[Either[FileSystemError, ?]] =
    EventStream
      .sequence(steps.map(write).toSeq)
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
      .pipe(EventStream.sequence)
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
        .pipe(EventStream.sequence)
        .map(_.collect { case (fileName, Right(encoding)) => toStepID(fileName) -> encoding }.toMap)
        .map(Right(_))
    )
}

package ddm.ui.storage.opfs

import com.raquo.airstream.core.EventStream
import ddm.codec.Encoding
import ddm.ui.model.plan.{Step, StepDetails}
import ddm.ui.storage.opfs.StepsDirectory.*
import ddm.ui.utils.airstream.EventStreamOps.andThen
import ddm.ui.wrappers.opfs.{DirectoryHandle, FileSystemError}

object StepsDirectory {
  private def toFileName(id: Step.ID): String =
    s"$id.bin"
  
  private def toStepID(fileName: String): Step.ID =
    Step.ID.fromString(fileName.takeWhile(_ != '.'))
}

final class StepsDirectory(underlying: DirectoryHandle) {
  def write(step: Step): EventStream[Either[FileSystemError, ?]] =
    underlying.replaceFileContent(toFileName(step.id), step.details)
  
  def write(steps: Iterable[Step]): EventStream[Either[FileSystemError, ?]] = {
    val zero = EventStream.fromValue[Either[FileSystemError, ?]](Right(()), emitOnce = true)

    steps.foldLeft(zero)((acc, step) =>
      acc.andThen(_ => write(step))
    )
  }
  
  def remove(id: Step.ID): EventStream[Either[FileSystemError.UnexpectedFileSystemError, ?]] =
    underlying.removeFile(toFileName(id))
  
  def read(id: Step.ID): EventStream[Either[FileSystemError, Step]] =
    underlying
      .read[StepDetails](toFileName(id))
      .map(_.map(Step(id, _)))
  
  def read(ids: Iterable[Step.ID]): EventStream[Either[FileSystemError, Map[Step.ID, Step]]] = {
    val zero = EventStream.fromValue[Either[FileSystemError, Map[Step.ID, Step]]](Right(Map.empty), emitOnce = true)
    
    ids.foldLeft(zero)((streamAcc, id) =>
      streamAcc.andThen(acc =>
        read(id).map(_.map(step => acc + (id -> step)))
      )
    )
  }

  def fetch(): EventStream[Either[FileSystemError, Map[Step.ID, Encoding]]] =
    underlying.listFiles().andThen { fileNames =>
      val zero = EventStream.fromValue(Map.empty[Step.ID, Encoding], emitOnce = true)
      
      fileNames.foldLeft(zero)((streamAcc, fileName) =>
        streamAcc.flatMap(acc =>
          underlying.read[Encoding](fileName).map {
            case Left(_) => acc
            case Right(encoding) => acc + (toStepID(fileName) -> encoding)  
          }
        )
      ).map(Right(_))
    }
}

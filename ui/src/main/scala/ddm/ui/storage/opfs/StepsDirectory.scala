package ddm.ui.storage.opfs

import com.raquo.airstream.core.EventStream
import ddm.ui.model.plan.Step
import ddm.ui.model.plan.Step.ID
import ddm.ui.storage.opfs.StepsDirectory.*
import ddm.ui.utils.circe.JsonByteEncoder
import ddm.ui.wrappers.opfs.FileSystemOpOps.{andThen, readJson}
import ddm.ui.wrappers.opfs.{DirectoryHandle, FileSystemError}
import io.circe.Encoder

object StepsDirectory {
  private val stepEncoder =  {
    given Encoder[Step] = Step.minimisedEncoder
    JsonByteEncoder[Step](predictSize = true)
  }
  
  private def toFileName(id: Step.ID): String = s"$id.json"
}

final class StepsDirectory(underlying: DirectoryHandle) {
  def write(step: Step): EventStream[Either[FileSystemError, ?]] =
    underlying.replaceFileContent(
      toFileName(step.id),
      stepEncoder.encode(step)
    )
  
  def write(steps: Iterable[Step]): EventStream[Either[FileSystemError, ?]] = {
    val zero = EventStream.fromValue[Either[FileSystemError, ?]](Right(()), emitOnce = true)

    steps.foldLeft(zero)((acc, step) =>
      acc.andThen(_ => write(step))
    )
  }
  
  def remove(id: Step.ID): EventStream[Either[FileSystemError.UnexpectedFileSystemError, ?]] =
    underlying.removeFile(toFileName(id))
  
  def read(id: Step.ID): EventStream[Either[FileSystemError, Step]] =
    underlying.readJson[Step](toFileName(id))(using Step.minimisedDecoder(() => id))
  
  def read(ids: Iterable[Step.ID]): EventStream[Either[FileSystemError, Map[ID, Step]]] = {
    val zero = EventStream.fromValue[Either[FileSystemError, Map[Step.ID, Step]]](Right(Map.empty), emitOnce = true)
    
    ids.foldLeft(zero)((streamAcc, id) =>
      streamAcc.andThen(acc =>
        read(id).map(_.map(step => acc + (id -> step)))
      )
    )
  }
}

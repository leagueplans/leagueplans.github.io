package ddm.ui.wrappers.opfs

import com.raquo.airstream.core.EventStream
import ddm.ui.wrappers.opfs.FileSystemError.DecodingError
import io.circe.Decoder

object FileSystemOpOps {
  extension [E1, T](self: EventStream[Either[E1, T]]) {
    def andThen[E2 >: E1, S](
      f: T => EventStream[Either[E2, S]]
    ): EventStream[Either[E2, S]] =
      self.flatMap {
        case Left(error) => EventStream.fromValue(Left(error), emitOnce = true)
        case Right(t) => f(t)
      }
  }

  extension (self: DirectoryHandle) {
    def readJson[T : Decoder](fileName: String): EventStream[Either[FileSystemError, T]] =
      self
        .read(fileName, ByteArrayParser.json)
        .map(_.flatMap(json => json.as[T].left.map(DecodingError.apply)))
  }
}

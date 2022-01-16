package ddm.ui

import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder}
import org.scalajs.dom.Storage

import scala.util.Try

final class StorageManager[T : Encoder : Decoder](key: String, storage: Storage) {
  def save(t: T): Unit =
    storage.setItem(key, t.asJson.noSpaces)

  def load(): Option[Try[T]] =
    Option(storage.getItem(key))
      .map(decode[T](_).toTry)
}

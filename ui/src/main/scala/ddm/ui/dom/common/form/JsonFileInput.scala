package ddm.ui.dom.common.form

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L
import io.circe.Decoder
import io.circe.Error
import io.circe.parser.decode

object JsonFileInput {
  def apply[T : Decoder](id: String): (L.Input, L.Label, Signal[Option[Either[Error, T]]]) = {
    val (input, label, fileSignal) = FileInput(id, accept = ".json")

    val decodedSignal =
      fileSignal
        .flatMap {
          case Some(file) => Signal.fromJsPromise(file.text())
          case None => Signal.fromValue(None)
        }
        .map(_.map(decode[T]))

    (input, label, decodedSignal)
  }
}

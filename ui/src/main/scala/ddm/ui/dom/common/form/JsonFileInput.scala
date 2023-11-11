package ddm.ui.dom.common.form

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, enrichSource}
import io.circe.{Decoder, Error}
import io.circe.parser.decode
import org.scalajs.dom.console

object JsonFileInput {
  def apply[T : Decoder](id: String): (L.Input, L.Label, Signal[Option[T]]) = {
    val (input, label, fileSignal) = FileInput(id, accept = ".json")

    val decodedSignal =
      fileSignal
        .flatMap {
          case Some(file) => Signal.fromJsPromise(file.text())
          case None => Signal.fromValue(None)
        }
        .map(_.map(decode[T]))

    val validatedInput =
      input.amend(
        L.inContext(node =>
          decodedSignal --> Observer[Option[Either[Error, T]]] {
            case Some(Left(error)) =>
              console.error(s"Failed to parse uploaded file: [${error.getMessage}]")
              node.ref.setCustomValidity("Unable to parse the provided file")
            case _ =>
              node.ref.setCustomValidity("")
          }
        )
      )

    val collapsedSignal =
      decodedSignal.map(
        _.flatMap {
          case Right(value) => Some(value)
          case Left(_) => None
        }
      )

    (validatedInput, label, collapsedSignal)
  }
}

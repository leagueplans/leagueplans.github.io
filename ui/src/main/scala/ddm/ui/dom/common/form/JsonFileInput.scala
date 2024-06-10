package ddm.ui.dom.common.form

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.L
import io.circe.Decoder
import io.circe.parser.decode

object JsonFileInput {
  def apply[T : Decoder](id: String): (L.Input, L.Label, Signal[Option[T]]) =
    ValidatedFileInput(id, accept = ".json")(file =>
      EventStream.fromJsPromise(file.text()).map(decode[T])
    )
}

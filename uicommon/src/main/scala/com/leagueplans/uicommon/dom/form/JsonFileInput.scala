package com.leagueplans.uicommon.dom.form

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.L
import io.circe.Decoder
import io.circe.parser.decode
import org.scalajs.dom.console

object JsonFileInput {
  def apply[T : Decoder](id: String): (L.Input, L.Label, Signal[Option[T]]) =
    ValidatedFileInput(
      id,
      accept = ".json",
      onError = console.error("Failed to parse uploaded file", _)
    )(file =>
      EventStream.fromJsPromise(file.text()).map(decode[T])
    )
}

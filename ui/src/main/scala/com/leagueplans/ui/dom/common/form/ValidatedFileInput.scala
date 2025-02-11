package com.leagueplans.ui.dom.common.form

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, enrichSource}
import org.scalajs.dom.{File, console}

object ValidatedFileInput {
  def apply[T](id: String, accept: String)(
    parse: File => EventStream[Either[Throwable, T]]
  ): (L.Input, L.Label, Signal[Option[T]]) = {
    val (input, label, fileSignal) = FileInput(id, accept)
    val dataSignal = parseFile(fileSignal, parse)
    val validatedInput = input.amend(setValidity(dataSignal))
    (validatedInput, label, collapse(dataSignal))
  }
  
  private def parseFile[T](
    fileSignal: Signal[Option[File]],
    parse: File => EventStream[Either[Throwable, T]]
  ): Signal[Option[Either[Throwable, T]]] =
    fileSignal.flatMapSwitch {
      case Some(file) => parse(file).map(Some.apply).toSignal(initial = None)
      case None => Signal.fromValue(None)
    }
    
  private def setValidity(
    dataSignal: Signal[Option[Either[Throwable, ?]]]
  ): L.Modifier[L.Input] =
    L.inContext(node =>
      dataSignal --> {
        case Some(Left(error)) =>
          console.error(s"Failed to parse uploaded file: [${error.getMessage}]")
          node.ref.setCustomValidity("Unable to parse the provided file")
        case _ =>
          node.ref.setCustomValidity("")
      }
    )
    
  private def collapse[T](dataSignal: Signal[Option[Either[Throwable, T]]]): Signal[Option[T]] =
    dataSignal.map(
      _.flatMap {
        case Right(value) => Some(value)
        case Left(_) => None
      }
    )
}

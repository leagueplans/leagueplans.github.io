package ddm.ui.dom.common

import com.raquo.airstream.core.{Observable, Observer}
import com.raquo.laminar.api.{L, enrichSource}

object FormOpener {
  type Command = Any

  def apply[T](
    modalController: Modal.Controller,
    formObserver: Observer[T],
    toForm: () => (L.FormElement, Observable[T])
  ): Observer[Command] =
    modalController.toObserver.contramap[Command] { _ =>
      val (form, formSubmissions) = toForm()
      val selfClosingForm =
        form.amend(
          formSubmissions --> formObserver,
          formSubmissions.mapToStrict(None) --> modalController
        )
      Some(selfClosingForm)
    }
}

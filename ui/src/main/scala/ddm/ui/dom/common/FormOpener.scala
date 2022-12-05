package ddm.ui.dom.common

import com.raquo.airstream.core.{Observable, Observer, Sink}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, enrichSource}

object FormOpener {
  type Command = Any

  def apply[T](
    modalBus: WriteBus[Option[L.Element]],
    formObserver: Sink[T],
    toForm: () => (L.FormElement, Observable[T])
  ): Observer[Command] = {
    val (form, formSubmissions) = toForm()
    val selfClosingForm =
      form.amend(
        formSubmissions --> formObserver,
        formSubmissions.mapToStrict(None) --> modalBus
      )

    (modalBus.contramap[Command](_ => Some(selfClosingForm)))
  }
}

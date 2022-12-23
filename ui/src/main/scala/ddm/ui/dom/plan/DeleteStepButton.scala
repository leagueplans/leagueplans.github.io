package ddm.ui.dom.plan

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, eventPropToProcessor}
import ddm.ui.dom.common.Forester
import ddm.ui.facades.fontawesome.freeregular.FreeRegular
import ddm.ui.model.plan.Step
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.MouseEvent

import java.util.UUID

object DeleteStepButton {
  def apply(
    stepID: UUID,
    modalBus: WriteBus[Option[L.Element]],
    stepUpdater: Observer[Forester[UUID, Step] => Unit],
  ): L.Button = {
    val deleter = DeletionConfirmer(modalBus, stepUpdater.contramap[Unit](_ => _.remove(stepID)))
    L.button(
      L.`type`("button"),
      L.icon(FreeRegular.faTrashCan),
      L.ifUnhandled(L.onClick) --> deleter.contramap[MouseEvent](_.preventDefault())
    )
  }
}

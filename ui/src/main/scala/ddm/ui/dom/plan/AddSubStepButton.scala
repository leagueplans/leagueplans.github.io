package ddm.ui.dom.plan

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, eventPropToProcessor}
import ddm.ui.dom.common.Forester
import ddm.ui.facades.fontawesome.freesolid.FreeSolid
import ddm.ui.model.plan.Step
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.MouseEvent

import java.util.UUID

object AddSubStepButton {
  def apply(
    stepID: UUID,
    modalBus: WriteBus[Option[L.Element]],
    stepUpdater: Observer[Forester[UUID, Step] => Unit],
  ): L.Button = {
    val subStepCreator = SubStepCreator(
      modalBus,
      stepUpdater.contramap[Step](step => _.add(child = step, parent = stepID))
    )
    L.button(
      L.`type`("button"),
      L.icon(FreeSolid.faPlus),
      L.ifUnhandled(L.onClick) --> subStepCreator.contramap[MouseEvent](_.preventDefault())
    )
  }
}

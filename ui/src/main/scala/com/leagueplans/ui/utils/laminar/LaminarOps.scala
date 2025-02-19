package com.leagueplans.ui.utils.laminar

import com.leagueplans.ui.wrappers.animation.Animation
import com.raquo.laminar.api.{L, Laminar, eventPropToProcessor}
import com.raquo.laminar.keys.EventProcessor
import org.scalajs.dom.KeyboardEvent

object LaminarOps {
  extension (self: Laminar) {
    def onKey(keyCode: Int): EventProcessor[KeyboardEvent, KeyboardEvent] =
      self.onKeyDown.filter(_.keyCode == keyCode)

    def onMountAnimate[E <: L.Element](f: E => Animation.Instance): self.Modifier[E] =
      self.onMountUnmountCallbackWithState[E, Animation.Instance](
        mountContext => f(mountContext.thisNode),
        (_, maybeAnimation) => maybeAnimation.foreach(_.cancel())
      )
  }
}

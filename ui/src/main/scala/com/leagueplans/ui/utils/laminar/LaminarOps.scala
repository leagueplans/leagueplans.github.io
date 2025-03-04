package com.leagueplans.ui.utils.laminar

import com.leagueplans.ui.wrappers.animation.Animation
import com.raquo.laminar.api.{Laminar, eventPropToProcessor}
import com.raquo.laminar.keys.EventProcessor
import org.scalajs.dom.{KeyboardEvent, ResizeObserver, ResizeObserverEntry, ResizeObserverOptions}

object LaminarOps {
  extension (self: Laminar) {
    def onKey(keyCode: Int): EventProcessor[KeyboardEvent, KeyboardEvent] =
      self.onKeyDown.filter(_.keyCode == keyCode)

    def onMountAnimate[E <: self.Element](f: E => Animation.Instance): self.Modifier[E] =
      self.onMountUnmountCallbackWithState[E, Animation.Instance](
        mountContext => f(mountContext.thisNode),
        (_, maybeAnimation) => maybeAnimation.foreach(_.cancel())
      )

    def withResizeObserver[E <: self.Element](
      options: ResizeObserverOptions
    )(f: ResizeObserverEntry => Unit): self.Modifier[E] =
      self.onMountUnmountCallbackWithState[E, ResizeObserver](
        mount = ctx => {
          val observer = ResizeObserver((entries, _) => entries.headOption.foreach(f))
          observer.observe(ctx.thisNode.ref, options)
          observer
        },
        unmount = (ctx, maybeObserver) => 
          maybeObserver.foreach(_.unobserve(ctx.ref))
      )
  }
}

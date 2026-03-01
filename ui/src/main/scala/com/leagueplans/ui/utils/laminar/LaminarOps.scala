package com.leagueplans.ui.utils.laminar

import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.leagueplans.ui.wrappers.animation.Animation
import com.raquo.laminar.api.{Laminar, eventPropToProcessor}
import com.raquo.laminar.defs.eventProps.GlobalEventProps
import com.raquo.laminar.keys.EventProcessor
import org.scalajs.dom.{KeyboardEvent, ResizeObserver, ResizeObserverEntry, ResizeObserverOptions}

object LaminarOps {
  extension (self: GlobalEventProps) {
    def onKey(key: String): EventProcessor[KeyboardEvent, KeyboardEvent] =
      self.onKeyDown.filter(_.key == key)
  }
  
  extension (self: Laminar) {
    def onMountAnimate[E <: self.Element](f: E => Animation.Instance): self.Modifier[E] =
      self.onMountUnmountCallbackWithState[E, Animation.Instance](
        mountContext => f(mountContext.thisNode),
        (_, maybeAnimation) => maybeAnimation.foreach(_.cancel())
      )
    
    def selectOnFocus[E <: self.Input]: self.Modifier[E] =
      self.inContext(ctx =>
        self.onFocus.handled --> (_ => ctx.ref.select())
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

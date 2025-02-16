package com.leagueplans.ui.dom.common.collapse

import com.leagueplans.ui.wrappers.animation.Animation
import com.raquo.airstream.state.Var

final class InvertibleAnimationController(
  startOpen: Boolean,
  open: () => Animation.Instance,
  close: () => Animation.Instance,
  onOpen: () => Unit,
  onClose: () => Unit
) {
  private val state = Var((startOpen, Option.empty[Animation.Instance]))

  def toggle(): Unit =
    state.update { (isOpen, maybeAnim) =>
      val animation = maybeAnim match {
        case Some(ongoing) => ongoing.reverse(); ongoing
        case None if isOpen => close()
        case None => open()
      }
      registerFinishCallback(animation, !isOpen)
      (!isOpen, Some(animation))
    }

  private def registerFinishCallback(animation: Animation.Instance, endOpen: Boolean): Unit =
    animation.onfinish = { _ =>
      animation.commitStyles()
      animation.cancel()
      if (endOpen) onOpen() else onClose()
      state.set((endOpen, None))
    }
}

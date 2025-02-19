package com.leagueplans.ui.dom.plan

import com.leagueplans.ui.dom.common.Tooltip
import com.leagueplans.ui.facades.animation.KeyframeAnimationOptions
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.leagueplans.ui.utils.laminar.LaminarOps.onMountAnimate
import com.leagueplans.ui.wrappers.animation.{Animation, KeyframeProperty}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StepHeader {
  private val bounceIn = Animation(
    new KeyframeAnimationOptions {
      duration = 300
      easing = "cubic-bezier(0.49, 0.21, 0.7, 1.8)"
    },
    KeyframeProperty.scale(0, 1)
  )

  def apply(
    stepSignal: Signal[Step],
    isFocusedSignal: Signal[Boolean],
    draggableObserver: Observer[Boolean]
  ): L.Div =
    L.div(
      L.cls(Styles.header),
      isFocusedSignal.changes.filterNot(identity) --> draggableObserver,
      L.child <-- isFocusedSignal.splitBoolean(
        whenTrue = _ => toDragIcon(draggableObserver),
        whenFalse = _ => L.emptyNode
      ),
      L.p(
        L.cls(Styles.title),
        L.text <-- stepSignal.map(_.description)
      )
    )

  @js.native @JSImport("/styles/plan/stepHeader.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val header: String = js.native
    val dragIcon: String = js.native
    val title: String = js.native
  }

  private def toDragIcon(draggableObserver: Observer[Boolean]): L.Div =
    L.div(
      L.cls(Styles.dragIcon),
      FontAwesome.icon(FreeSolid.faGripVertical),
      L.onMountAnimate(bounceIn.play),
      L.onMouseEnter.mapToStrict(true) --> draggableObserver,
      L.onMouseLeave.mapToStrict(false) --> draggableObserver,
      Tooltip(L.span("Drag to reposition")),
    )
}

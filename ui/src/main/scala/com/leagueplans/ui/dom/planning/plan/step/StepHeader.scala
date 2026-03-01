package com.leagueplans.ui.dom.planning.plan.step

import com.leagueplans.ui.dom.common.Tooltip
import com.leagueplans.ui.dom.common.collapse.{CollapseButton, InvertibleAnimationController}
import com.leagueplans.ui.facades.animation.KeyframeAnimationOptions
import com.leagueplans.ui.facades.floatingui.Placement
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.leagueplans.ui.utils.laminar.LaminarOps.onMountAnimate
import com.leagueplans.ui.wrappers.animation.{Animation, KeyframeProperty}
import com.leagueplans.ui.wrappers.floatingui.FloatingConfig
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
    hasSubstepsSignal: Signal[Boolean],
    isFocusedSignal: Signal[Boolean],
    draggableObserver: Observer[Boolean],
    animationController: InvertibleAnimationController,
    tooltip: Tooltip
  ): L.Div =
    L.div(
      L.cls(Styles.header),
      L.child <-- toSubstepToggle(animationController, hasSubstepsSignal, tooltip),
      toTitle(stepSignal, isFocusedSignal, draggableObserver, tooltip)
    )

  @js.native @JSImport("/styles/planning/plan/step/header.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val header: String = js.native
    val substepsToggleIcon: String = js.native
    val substepsToggle: String = js.native
    val title: String = js.native
    val dragIcon: String = js.native
    val description: String = js.native
    val tooltip: String = js.native
  }
  
  private def toSubstepToggle(
    animationController: InvertibleAnimationController,
    hasSubstepsSignal: Signal[Boolean],
    tooltip: Tooltip
  ): Signal[L.Node] =
    // We choose the icon's orientation based on whether the animation controller is
    // currently open, so we need to make sure that we only create the button at the
    // point where we go to render.
    hasSubstepsSignal.distinct.map {
      case true =>
        CollapseButton(
          animationController,
          tooltipContents = "Toggle substep visibility",
          screenReaderDescription = "toggle substep visibility",
          L.svg.cls(Styles.substepsToggleIcon),
          tooltip
        ).amend(L.cls(Styles.substepsToggle))
      case false =>
        L.emptyNode
    }

  private def toTitle(
    stepSignal: Signal[Step],
    isFocusedSignal: Signal[Boolean],
    draggableObserver: Observer[Boolean],
    tooltip: Tooltip
  ): L.Div =
    L.div(
      L.cls(Styles.title),
      isFocusedSignal.changes.filterNot(identity) --> draggableObserver,
      L.child <-- isFocusedSignal.splitBoolean(
        whenTrue = _ => toDragIcon(draggableObserver, tooltip),
        whenFalse = _ => L.emptyNode
      ),
      L.p(
        L.cls(Styles.description),
        L.text <-- stepSignal.map(_.description)
      )
    )

  private def toDragIcon(
    draggableObserver: Observer[Boolean],
    tooltip: Tooltip
  ): L.Div =
    L.div(
      L.cls(Styles.dragIcon),
      FontAwesome.icon(FreeSolid.faGripVertical),
      L.onMountAnimate(bounceIn.play),
      L.onMouseEnter.mapToStrict(true) --> draggableObserver,
      L.onMouseLeave.mapToStrict(false) --> draggableObserver,
      tooltip.register(
        L.span(L.cls(Styles.tooltip), "Drag to reposition"),
        FloatingConfig.basicTooltip(placement = Placement.left)
      )
    )
}

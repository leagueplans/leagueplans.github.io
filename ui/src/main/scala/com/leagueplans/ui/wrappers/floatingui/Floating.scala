package com.leagueplans.ui.wrappers.floatingui

import com.leagueplans.ui.facades.floatingui.*
import com.raquo.airstream.core.Observer
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, nodeOptionToModifier, seqToModifier}
import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichOption
import scala.scalajs.js.annotation.JSImport

object Floating {
  private type ArrowPositioningData = (placement: Placement, coords: MiddlewareData.Arrow)
  private type State = (
    left: Var[Double],
    top: Var[Double],
    position: Var[Strategy],
    arrowData: Option[Observer[Option[ArrowPositioningData]]]
  )

  /** You must not call this multiple times for the same element (e.g. in response to a
    * mouse event). This registers a listener for mount events, and potentially adds a
    * child (the arrow). A new listener and child may be added each time the target is
    * mounted if this is triggered multiple times.
    */
  def anchorTo(targetX: Double, targetY: Double, config: FloatingConfig): L.Modifier[L.HtmlElement] = {
    val anchor = new VirtualElement {
      def getBoundingClientRect(): ClientRectObject =
        new ClientRectObject {
          var x: Double = targetX
          var y: Double = targetY
          var width: Double = 0
          var height: Double = 0
          var top: Double = targetY
          var right: Double = targetX
          var bottom: Double = targetY
          var left: Double = targetX
        }
    }

    val (arrow, state) = initialiseState(config.arrow)
    val positionConfig = translateConfig(config, arrow)
    val onMount = L.onMountCallback(ctx =>
      computePosition(anchor, ctx.thisNode, state, positionConfig)
    )
    convertToModifiers(state, arrow, onMount)
  }

  def anchorTo(target: ReferenceElement, config: FloatingConfig): L.Modifier[L.HtmlElement] = {
    val (arrow, state) = initialiseState(config.arrow)
    val positionConfig = translateConfig(config, arrow)
    val onMount =
      L.onMountUnmountCallbackWithState(
        mount = ctx => DOM.autoUpdate(
          target,
          ctx.thisNode.ref,
          () => computePosition(target, ctx.thisNode, state, positionConfig)
        ),
        unmount = (_, state) => state.foreach(cleanUp => cleanUp())
      )
    convertToModifiers(state, arrow, onMount)
  }

  private def initialiseState(config: Option[FloatingConfig.Arrow]): (Option[L.Div], State) = {
    val (arrow, arrowData) = config.map(toArrow).unzip
    (arrow, (Var(0.0), Var(0.0), Var(Strategy.absolute), arrowData))
  }

  private def toArrow(config: FloatingConfig.Arrow): (L.Div, Observer[Option[ArrowPositioningData]]) = {
    val data = Var(Option.empty[ArrowPositioningData])
    val sideOffset = -config.size / 2
    val arrow =
      L.div(
        L.cls(Styles.arrow),
        L.display <-- data.signal.map {
          case Some(data) if config.padding.isEmpty && data.coords.centerOffset != 0.0 => "none"
          case _ => ""
        },
        L.left <-- data.signal.map(translateArrowPosition(_) {
          case (_: Placement.AlongTheBottom, coords) => coords.x.toOption
          case (_: Placement.AlongTheTop, coords) => coords.x.toOption
          case (_: Placement.AlongTheRight, _) => Some(sideOffset)
        }),
        L.right <-- data.signal.map(translateArrowPosition(_) {
          case (_: Placement.AlongTheLeft, _) => Some(sideOffset)
        }),
        L.top <-- data.signal.map(translateArrowPosition(_) {
          case (_: Placement.AlongTheLeft, coords) => coords.y.toOption
          case (_: Placement.AlongTheRight, coords) => coords.y.toOption
          case (_: Placement.AlongTheBottom, _) => Some(sideOffset)
        }),
        L.bottom <-- data.signal.map(translateArrowPosition(_) {
          case (_: Placement.AlongTheTop, _) => Some(sideOffset)
        }),
        L.width(px(config.size)),
        L.height(px(config.size))
      )

    (arrow, data.writer)
  }

  private def translateArrowPosition(
    positioningData: Option[ArrowPositioningData]
  )(f: PartialFunction[ArrowPositioningData, Option[Double]]): String =
    positioningData.collect(f).flatten.map(px).getOrElse("")

  private def px(d: Double): String =
    s"${d}px"

  private def translateConfig(config: FloatingConfig, maybeArrow: Option[L.HtmlElement]): ComputePositionConfig =
    new ComputePositionConfig {
      placement = config.placement.orUndefined
      middleware = js.Array(
        config.offset.map(DOM.offset).orUndefined,
        config.flip.map(DOM.flip).orUndefined,
        config.shift.map(DOM.shift).orUndefined,
        maybeArrow.map(arrow =>
          DOM.arrow(new ArrowOptions {
            var element: Element = arrow.ref
            padding = config.arrow.flatMap(_.padding).orUndefined
          })
        ).orUndefined
      )
    }

  private def computePosition(
    anchor: ReferenceElement,
    floating: L.Element,
    state: State,
    positionConfig: ComputePositionConfig
  ): Unit =
    js.async[Any] {
      val result = js.await(DOM.computePosition(anchor, floating.ref, positionConfig))

      state.left.set(result.x)
      state.top.set(result.y)
      state.position.set(result.strategy)
      state.arrowData.foreach(_.onNext(
        result.middlewareData.arrow.toOption.map((result.placement, _))
      ))
    }: Unit

  private def convertToModifiers(
    state: State,
    arrow: Option[L.Div],
    onMount: L.Modifier[L.HtmlElement]
  ): L.Modifier[L.HtmlElement] =
    List(
      L.position <-- state.position,
      L.left <-- state.left.signal.map(px),
      L.top <-- state.top.signal.map(px),
      nodeOptionToModifier(arrow),
      onMount
    )

  @js.native @JSImport("/styles/floating/arrow.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val arrow: String = js.native
  }
}

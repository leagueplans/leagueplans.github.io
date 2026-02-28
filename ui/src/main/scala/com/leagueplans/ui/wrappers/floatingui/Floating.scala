package com.leagueplans.ui.wrappers.floatingui

import com.leagueplans.ui.facades.floatingui.*
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, nodeOptionToModifier, seqToModifier}
import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichOption
import scala.scalajs.js.annotation.JSImport

object Floating {
  private type ArrowPositioningData = (placement: Placement, coords: MiddlewareData.Arrow)

  def anchorTo(target: Element, config: FloatingConfig): L.Modifier[L.HtmlElement] = {
    val left = Var(0.0)
    val top = Var(0.0)
    val position = Var(Strategy.absolute)

    val (arrowData, arrow) = config.arrow.map { arrowConfig =>
      val data = Var(Option.empty[ArrowPositioningData])
      val element = toArrow(arrowConfig.size, data.signal)
      (data, element)
    }.unzip
    val positionConfig = translateConfig(config, arrow)

    val onMount =
      L.onMountUnmountCallbackWithState(
        mount = ctx => DOM.autoUpdate(
          target,
          ctx.thisNode.ref,
          () => js.async[Any] {
            val result = js.await(DOM.computePosition(target, ctx.thisNode.ref, positionConfig))

            left.set(result.x)
            top.set(result.y)
            position.set(result.strategy)
            arrowData.foreach(_.set(
              result.middlewareData.arrow.toOption.map((result.placement, _))
            ))
          }: Unit
        ),
        unmount = (_, state) => state.foreach(cleanUp => cleanUp())
      )

    List(
      L.position <-- position,
      L.left <-- left.signal.map(px),
      L.top <-- top.signal.map(px),
      nodeOptionToModifier(arrow),
      onMount
    )
  }

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

  private def toArrow(size: Double, dataSignal: Signal[Option[ArrowPositioningData]]): L.Div = {
    val sideOffset = -size / 2

    L.div(
      L.cls(Styles.arrow),
      L.display <-- dataSignal.map {
        case Some(data) if data.coords.centerOffset != 0.0 => "none"
        case _ => ""
      },
      L.left <-- dataSignal.map(translateArrowPosition(_) {
        case (_: Placement.AlongTheBottom, coords) => coords.x.toOption
        case (_: Placement.AlongTheTop, coords) => coords.x.toOption
        case (_: Placement.AlongTheRight, _) => Some(sideOffset)
      }),
      L.right <-- dataSignal.map(translateArrowPosition(_) {
        case (_: Placement.AlongTheLeft, _) => Some(sideOffset)
      }),
      L.top <-- dataSignal.map(translateArrowPosition(_) {
        case (_: Placement.AlongTheLeft, coords) => coords.y.toOption
        case (_: Placement.AlongTheRight, coords) => coords.y.toOption
        case (_: Placement.AlongTheBottom, _) => Some(sideOffset)
      }),
      L.bottom <-- dataSignal.map(translateArrowPosition(_) {
        case (_: Placement.AlongTheTop, _) => Some(sideOffset)
      }),
      L.width(px(size)),
      L.height(px(size))
    )
  }

  @js.native @JSImport("/styles/floating/arrow.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val arrow: String = js.native
  }

  private def translateArrowPosition(
    positioningData: Option[ArrowPositioningData]
  )(f: PartialFunction[ArrowPositioningData, Option[Double]]): String =
    positioningData.collect(f).flatten.map(px).getOrElse("")

  private def px(d: Double): String =
    s"${d}px"
}

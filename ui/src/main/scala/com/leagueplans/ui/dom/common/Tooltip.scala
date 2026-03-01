package com.leagueplans.ui.dom.common

import com.leagueplans.ui.facades.animation.KeyframeAnimationOptions
import com.leagueplans.ui.utils.laminar.LaminarOps.onMountAnimate
import com.leagueplans.ui.wrappers.animation.{Animation, KeyframeProperty}
import com.leagueplans.ui.wrappers.floatingui.{Floating, FloatingConfig}
import com.raquo.airstream.core.Source.SignalSource
import com.raquo.airstream.core.{Observer, Signal, Sink}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier}
import com.raquo.laminar.nodes.ChildNode
import org.scalajs.dom.{Element, MouseEvent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Tooltip {
  private type Key = Any

  private enum Command {
    case Open(key: Any, contents: ChildNode.Base)
    case Close(key: Any)
  }

  private val toggleTooltipDelayMs = 75

  private val fadeInAnimation: Animation = Animation(
    new KeyframeAnimationOptions {
      duration = 250
      easing = "ease-out"
    },
    KeyframeProperty.opacity(0, 1)
  )

  private val fadeIn: L.Modifier[L.Element] =
    L.onMountAnimate(fadeInAnimation.play)

  def apply(): (L.Div, Tooltip) = {
    val keyedContents = Var(Option.empty[(key: Key, contents: ChildNode.Base)]).distinct
    val hoveringOver = Var(Option.empty[Key])

    val tooltipContainer = L.div(
      L.cls(Styles.tooltipContainer),
      L.child.maybe <-- keyedContents.signal.map(_.map(_.contents)),
      registerTooltipHoverListeners(keyedContents.signal.map(_.map(_.key)), hoveringOver)
    )

    val contentsUpdater = keyedContents.updater[Command] {
      case (current @ Some((currentKey, _)), Command.Open(newKey, newContents)) =>
        if (newKey == currentKey) current else Some((newKey, newContents))

      case (current @ Some((currentKey, _)), Command.Close(closeKey)) =>
        if (closeKey == currentKey) None else current

      case (None, Command.Open(newKey, newContents)) =>
        Some((newKey, newContents))

      case (None, _: Command.Close) =>
        None
    }

    (tooltipContainer, new Tooltip(contentsUpdater, hoveringOver.signal))
  }

  @js.native @JSImport("/styles/common/tooltip.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val tooltipContainer: String = js.native
    val hasTooltip: String = js.native
    val tooltip: String = js.native
  }

  private def registerTooltipHoverListeners(
    currentContents: SignalSource[Option[Key]],
    hoveringOver: Sink[Option[Key]]
  ): L.Modifier[L.Element] =
    L.inContext(ctx =>
      List(
        L.onMouseEnter
          .filter(_.target == ctx.ref)
          .compose(_.sample(currentContents)) --> hoveringOver,
        L.onMouseLeave
          .filter(_.target == ctx.ref)
          .mapToStrict(None) --> hoveringOver
      )
    )
}

final class Tooltip private[Tooltip] (
  commandSink: Observer[Tooltip.Command],
  hoveringOverTooltipWithContents: Signal[Option[Tooltip.Key]]
) {
  def register(contents: L.HtmlElement, config: FloatingConfig): L.Modifier[L.HtmlElement] = {
    contents.amend(
      L.cls(Tooltip.Styles.tooltip),
      L.when(config.fadeIn)(Tooltip.fadeIn)
    )

    config.anchor match {
      case Some(anchor) =>
        contents.amend(Floating.anchorTo(anchor.ref, config))
        List(
          L.cls(Tooltip.Styles.hasTooltip),
          L.inContext(element => configureTooltipVisibility(element, contents))
        )

      case None =>
        List(
          L.cls(Tooltip.Styles.hasTooltip),
          L.inContext { anchor =>
            contents.amend(Floating.anchorTo(anchor.ref, config))
            configureTooltipVisibility(anchor, contents)
          }
        )
    }
  }

  // Allow a brief delay before both showing the tooltip, and before hiding it after the cursor moves away
  private def configureTooltipVisibility(element: L.Element, tooltip: ChildNode.Base): L.Modifier[L.Element] = {
    val shouldShowTooltip = Var(false).distinct
    val showTooltipUpdater =
      shouldShowTooltip.updater[(nowHovering: Boolean, wasHovering: Boolean)] {
        case (_, (true, true)) => true
        case (_, (false, false)) => false
        case (true, (nowHovering, wasHovering)) => nowHovering || wasHovering
        case (false, (nowHovering, wasHovering)) => nowHovering && wasHovering
      }

    val isHoveringOverElement = Var(false).distinct
    val isHoveringOverTooltip = hoveringOverTooltipWithContents.map(_.contains(element)).distinct
    val isHoveringOverEither = Signal.combine(isHoveringOverElement, isHoveringOverTooltip).map(_ || _)
    val wasHoveringOverEither = isHoveringOverEither.changes.delay(Tooltip.toggleTooltipDelayMs).toSignal(initial = false)

    List(
      registerElementHoverListeners(element.ref, isHoveringOverElement),
      Signal.combine(isHoveringOverEither, wasHoveringOverEither) --> showTooltipUpdater,
      shouldShowTooltip.signal.map {
        case true => Tooltip.Command.Open(key = element, tooltip)
        case false => Tooltip.Command.Close(key = element)
      } --> commandSink,
      // Blur events aren't reliably fired when an element unmounts, so we need a fallback
      L.onUnmountCallback(_ =>
        isHoveringOverElement.set(false)
        // We've unmounted, so we need to explicitly trigger the command
        commandSink.onNext(Tooltip.Command.Close(key = element))
      )
    )
  }

  private def registerElementHoverListeners(element: Element, isHovering: Sink[Boolean]): L.Modifier[L.Element] =
    List(
      L.onMouseOver
        .collect(targetExtractor)
        .map(isClosestListener(_, element)) --> isHovering,
      L.onMouseLeave.mapToStrict(false) --> isHovering,
      L.onFocus.mapToStrict(true) --> isHovering,
      L.onBlur.mapToStrict(false) --> isHovering
    )

  private val targetExtractor: PartialFunction[MouseEvent, Element] =
    Function.unlift(_.target match {
      case target: Element => Some(target)
      case _ => None
    })

  private def isClosestListener(target: Element, listener: Element): Boolean =
    target.closest(s".${Tooltip.Styles.hasTooltip}").contains(listener)
}

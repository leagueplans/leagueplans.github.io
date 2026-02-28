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

object TooltipV2 {
  private type Key = Any

  private enum Command {
    case Open(key: Any, contents: ChildNode.Base)
    case Close(key: Any)
  }

  private val toggleTooltipDelayMs = 75

  private val fadeInAnimation: Animation = Animation(
    new KeyframeAnimationOptions {
      duration = 450
      easing = "ease-out"
    },
    KeyframeProperty.opacity(0, 1)
  )

  val fadeIn: L.Modifier[L.Element] =
    L.onMountAnimate(fadeInAnimation.play)

  def apply(): (L.Div, Controller) = {
    val keyedContents = Var(Option.empty[(key: Key, contents: ChildNode.Base)]).distinct
    val hoveringOver = Var(Option.empty[Key])

    val tooltipContainer = L.div(
      L.cls(Styles.tooltipContainer),
      L.child.maybe <-- keyedContents.signal.map(_.map(_.contents)),
      registerTooltipHoverListeners(keyedContents.signal.map(_.map(_.key)), hoveringOver),
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

    (tooltipContainer, Controller(contentsUpdater, hoveringOver.signal))
  }

  final class Controller private[TooltipV2] (
    commandSink: Observer[Command],
    hoveringOverTooltip: Signal[Option[Key]]
  ) {
    def register(tooltip: L.HtmlElement, config: FloatingConfig): L.Modifier[L.HtmlElement] = {
      tooltip.amend(L.cls(Styles.tooltip))

      List(
        L.cls(Styles.hasTooltip),
        L.inContext { ctx =>
          tooltip.amend(Floating.anchorTo(ctx.ref, config))
          L.emptyMod
        },
        configureTooltipVisibility(tooltip, hoveringOverTooltip, commandSink)
      )
    }
  }

  // TODO Rename file
  @js.native @JSImport("/styles/common/tooltipv2.module.css", JSImport.Default)
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

  // Allow a brief delay before both showing the tooltip, and before hiding it after the cursor moves away
  private def configureTooltipVisibility(
    tooltip: ChildNode.Base,
    hoveringOverTooltip: Signal[Option[Key]],
    commandSink: Observer[Command]
  ): L.Modifier[L.Element] =
    L.inContext { ctx =>
      val shouldShowTooltip = Var(false).distinct
      val showTooltipUpdater =
        shouldShowTooltip.updater[(nowHovering: Boolean, wasHovering: Boolean)] {
          case (_, (true, true)) => true
          case (_, (false, false)) => false
          case (true, (nowHovering, wasHovering)) => nowHovering || wasHovering
          case (false, (nowHovering, wasHovering)) => nowHovering && wasHovering
        }

      val isHoveringOverElement = Var(false)
      val isHoveringOverTooltip = hoveringOverTooltip.map(_.contains(ctx))
      val isHoveringOverEither = Signal.combine(isHoveringOverElement, isHoveringOverTooltip).map(_ || _)
      val wasHoveringOverEither = isHoveringOverEither.changes.delay(toggleTooltipDelayMs).toSignal(initial = false)

      List(
        registerElementHoverListeners(ctx.ref, isHoveringOverElement),
        Signal.combine(isHoveringOverEither, wasHoveringOverEither) --> showTooltipUpdater,
        shouldShowTooltip.signal.map {
          case true => Command.Open(key = ctx, tooltip)
          case false => Command.Close(key = ctx)
        } --> commandSink
      )
    }

  private def registerElementHoverListeners(element: Element, isHovering: Sink[Boolean]): L.Modifier[L.Element] =
    List(
      L.onMouseOver
        .collect(targetExtractor)
        .map(isClosestListener(_, element)) --> isHovering,
      L.onMouseOut
        .collect(targetExtractor)
        .filter(hasTooltip)
        .map(target =>
          target != element && isClosestListenerOfParent(target, element)
        ) --> isHovering
    )

  private val targetExtractor: PartialFunction[MouseEvent, Element] =
    Function.unlift(_.target match {
      case target: Element => Some(target)
      case _ => None
    })

  private def isClosestListener(target: Element, listener: Element): Boolean =
    target.closest(s".${Styles.hasTooltip}").contains(listener)

  private def hasTooltip(target: Element): Boolean =
    target.classList.contains(Styles.hasTooltip)

  private def isClosestListenerOfParent(target: Element, listener: Element): Boolean =
    target.parentNode match {
      case parent: Element => isClosestListener(parent, listener)
      case _ => false
    }
}

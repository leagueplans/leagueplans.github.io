package com.leagueplans.ui.facades.floatingui

import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DOM {
  // https://github.com/floating-ui/floating-ui/blob/d729054f1ebcf1d7c3e85c0967d455f816985671/packages/dom/src/autoUpdate.ts#L148
  @js.native @JSImport("@floating-ui/dom", "autoUpdate")
  def autoUpdate(
    reference: Element,
    floating: Element,
    update: js.Function0[Unit],
    options: AutoUpdateOptions = js.native
  ): AutoUpdateCleanUp = js.native

  // https://github.com/floating-ui/floating-ui/blob/d729054f1ebcf1d7c3e85c0967d455f816985671/packages/core/src/computePosition.ts#L12
  @js.native @JSImport("@floating-ui/dom", "computePosition")
  def computePosition(
    reference: Element,
    floating: Element,
    config: ComputePositionConfig
  ): js.Promise[ComputePositionReturn] = js.native

  // https://github.com/floating-ui/floating-ui/blob/a82e45f175bb1a518d3934d747c1b767f558d171/packages/dom/src/middleware.ts#L109
  @js.native @JSImport("@floating-ui/dom", "arrow")
  def arrow(options: ArrowOptions): Middleware = js.native

  // https://github.com/floating-ui/floating-ui/blob/d729054f1ebcf1d7c3e85c0967d455f816985671/packages/dom/src/middleware.ts#L81
  @js.native @JSImport("@floating-ui/dom", "flip")
  def flip(options: FlipOptions): Middleware = js.native

  // https://github.com/floating-ui/floating-ui/blob/d729054f1ebcf1d7c3e85c0967d455f816985671/packages/dom/src/middleware.ts#L54
  @js.native @JSImport("@floating-ui/dom", "offset")
  def offset(options: Double | OffsetOptions): Middleware = js.native

  // https://github.com/floating-ui/floating-ui/blob/d729054f1ebcf1d7c3e85c0967d455f816985671/packages/dom/src/middleware.ts#L54
  @js.native @JSImport("@floating-ui/dom", "shift")
  def shift(options: ShiftOptions): Middleware = js.native
}

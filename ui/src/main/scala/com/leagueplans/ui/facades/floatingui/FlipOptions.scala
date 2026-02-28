package com.leagueplans.ui.facades.floatingui

import scala.scalajs.js

// https://github.com/floating-ui/floating-ui/blob/d729054f1ebcf1d7c3e85c0967d455f816985671/packages/core/src/middleware/flip.ts#L15
trait FlipOptions extends DetectOverflowOptions {
  /** The axis that runs along the side of the floating element. Determines
    * whether overflow along this axis is checked to perform a flip.
    * Default: true
    */
  var mainAxis: js.UndefOr[Boolean] = js.undefined

  /** The axis that runs along the alignment of the floating element. Determines
    * whether overflow along this axis is checked to perform a flip.
    * - `true`: Whether to check cross axis overflow for both side and alignment flipping.
    * - `false`: Whether to disable all cross axis overflow checking.
    * - `'alignment'`: Whether to check cross axis overflow for alignment flipping only.
    *
    * Default: true
    */
  var crossAxis: js.UndefOr[Boolean | FlipOptions.CrossAxis.alignment.type] = js.undefined

  /** Placements to try sequentially if the preferred `placement` does not fit.
    * Default: [oppositePlacement] (computed)
    */
  var fallbackPlacements: js.UndefOr[js.Array[Placement]] = js.undefined

  /** What strategy to use when no placements fit.
    * Default: 'bestFit'
    */
  var fallbackStrategy: js.UndefOr[FlipOptions.FallbackStrategy] = js.undefined

  /** Whether to allow fallback to the perpendicular axis of the preferred
    * placement, and if so, which side direction along the axis to prefer.
    * Default: 'none' (disallow fallback)
    */
  var fallbackAxisSideDirection: js.UndefOr[FlipOptions.FallbackAxisSideDirection] = js.undefined

  /** Whether to flip to placements with the opposite alignment if they fit
    * better.
    * Default: true
    */
  var flipAlignment: js.UndefOr[Boolean] = js.undefined
}

object FlipOptions {
  object CrossAxis {
    val alignment = "alignment"
  }

  type FallbackStrategy = FallbackStrategy.bestFit.type | FallbackStrategy.initialPlacement.type

  object FallbackStrategy {
    val bestFit = "bestFit"
    val initialPlacement = "initialPlacement"
  }

  type FallbackAxisSideDirection = 
    FallbackAxisSideDirection.none.type |
      FallbackAxisSideDirection.start.type |
      FallbackAxisSideDirection.end.type

  object FallbackAxisSideDirection {
    val none = "none"
    val start = "start"
    val end = "end"
  }
}

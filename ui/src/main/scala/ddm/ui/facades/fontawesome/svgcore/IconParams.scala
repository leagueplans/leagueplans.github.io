package ddm.ui.facades.fontawesome.svgcore

import ddm.ui.facades.fontawesome.commontypes.IconDefinition

import scala.scalajs.js

trait IconParams extends Params {
  val transform: js.UndefOr[Transform] = js.undefined
  val symbol: js.UndefOr[FaSymbol] = js.undefined
  val mask: js.UndefOr[IconDefinition] = js.undefined
  val maskId: js.UndefOr[String] = js.undefined
}

package ddm.ui.facades.fontawesome.svgcore

import ddm.ui.facades.fontawesome.commontypes.IconDefinition

import scala.scalajs.js

@js.native
trait Icon extends FontawesomeObject with IconDefinition {
  val `type`: String = js.native
}

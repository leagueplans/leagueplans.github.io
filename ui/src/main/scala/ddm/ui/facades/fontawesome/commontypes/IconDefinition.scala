package ddm.ui.facades.fontawesome.commontypes

import scala.scalajs.js

@js.native
trait IconDefinition extends IconLookup {
  /** (width, height, ligatures, unicode, svgPathData) */
  val icon: js.Tuple5[Double, Double, js.Array[String], String, IconPathData] = js.native
}

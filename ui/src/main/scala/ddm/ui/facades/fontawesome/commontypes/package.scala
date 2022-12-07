package ddm.ui.facades.fontawesome

import scala.scalajs.js
import scala.scalajs.js.|

package object commontypes {
  // This is an enum in the typescript encoding, but right now I've no need
  // to define the values
  type IconName = String
  type IconPathData = String | js.Array[String]
}

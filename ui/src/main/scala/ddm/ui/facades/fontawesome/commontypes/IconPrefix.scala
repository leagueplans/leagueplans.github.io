package ddm.ui.facades.fontawesome.commontypes

import scala.scalajs.js

@js.native
sealed trait IconPrefix extends js.Any

object IconPrefix {
  val fas: IconPrefix = "fas".asInstanceOf[IconPrefix]
  val far: IconPrefix = "far".asInstanceOf[IconPrefix]
  val fal: IconPrefix = "fal".asInstanceOf[IconPrefix]
  val fat: IconPrefix = "fat".asInstanceOf[IconPrefix]
  val fad: IconPrefix = "fad".asInstanceOf[IconPrefix]
  val fab: IconPrefix = "fab".asInstanceOf[IconPrefix]
  val fak: IconPrefix = "fak".asInstanceOf[IconPrefix]
  val fass: IconPrefix = "fass".asInstanceOf[IconPrefix]
}

package ddm.ui.facades.fontawesome.commontypes

import scala.scalajs.js

// In theory I could make this non-native so that I can create instances for
// use with fontawesome's look-up methods, but then I wouldn't be able to
// take advantage of tree-shaking.
@js.native
trait IconLookup extends js.Object {
  val prefix: IconPrefix = js.native
  val iconName: IconName = js.native
}

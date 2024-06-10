package ddm.ui.facades.opfs

import scala.scalajs.js

@js.native
trait FileSystemHandle extends js.Object {
  def kind: FileSystemHandleKind = js.native
  def name: String = js.native
}

package ddm.ui.facades.opfs

import org.scalajs.dom

import scala.scalajs.js

@js.native
trait WorkerNavigator extends dom.WorkerNavigator {
  def storage: StorageManager = js.native
}

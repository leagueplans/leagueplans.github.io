package ddm.ui.wrappers.workers

import ddm.ui.facades.opfs.WorkerNavigator

trait WorkerScope {
  def navigator: WorkerNavigator
}

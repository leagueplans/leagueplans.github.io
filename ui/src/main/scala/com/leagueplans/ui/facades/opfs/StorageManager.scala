package com.leagueplans.ui.facades.opfs

import org.scalajs.dom

import scala.scalajs.js

@js.native
trait StorageManager extends dom.StorageManager {
  def getDirectory(): js.Promise[FileSystemDirectoryHandle] = js.native
}

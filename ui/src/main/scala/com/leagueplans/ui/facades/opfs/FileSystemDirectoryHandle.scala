package com.leagueplans.ui.facades.opfs

import com.leagueplans.ui.facades.js.AsyncIterator

import scala.scalajs.js

@js.native
trait FileSystemDirectoryHandle extends FileSystemHandle {
  def values(): AsyncIterator[FileSystemHandle] = js.native

  def getDirectoryHandle(
    name: String,
    options: js.UndefOr[FileSystemGetDirectoryOptions] = js.native
  ): js.Promise[FileSystemDirectoryHandle] = js.native
  
  def getFileHandle(
    name: String,
    options: js.UndefOr[FileSystemGetFileOptions] = js.native
  ): js.Promise[FileSystemFileHandle] = js.native
  
  def removeEntry(
    name: String,
    options: js.UndefOr[FileSystemRemoveOptions] = js.native
  ): js.Promise[Unit] = js.native
}

package com.leagueplans.ui.facades.opfs

import org.scalajs.dom.BufferSource

import scala.scalajs.js

@js.native
trait FileSystemSyncAccessHandle extends js.Object {
  def close(): Unit = js.native
  
  def flush(): Unit = js.native

  def getSize(): Double = js.native
  
  def read(
    buffer: BufferSource, 
    options: js.UndefOr[FileSystemReadWriteOptions] = js.native
  ): Double = js.native
  
  def truncate(newSize: Double): Unit = js.native
  
  def write(
    buffer: BufferSource, 
    options: js.UndefOr[FileSystemReadWriteOptions] = js.native
  ): Double = js.native
}

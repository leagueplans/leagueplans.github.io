package com.leagueplans.ui.facades.opfs

opaque type FileSystemHandleKind <: String = String

object FileSystemHandleKind {
  val file: FileSystemHandleKind = "file"
  val directory: FileSystemHandleKind = "directory"
}

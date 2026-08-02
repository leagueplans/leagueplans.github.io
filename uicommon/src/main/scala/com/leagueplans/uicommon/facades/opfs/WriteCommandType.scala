package com.leagueplans.uicommon.facades.opfs

// https://fs.spec.whatwg.org/#api-filesystemwritablefilestream
opaque type WriteCommandType <: String = String

object WriteCommandType {
  val write: WriteCommandType = "write"
  val seek: WriteCommandType = "seek"
  val truncate: WriteCommandType = "truncate"
}

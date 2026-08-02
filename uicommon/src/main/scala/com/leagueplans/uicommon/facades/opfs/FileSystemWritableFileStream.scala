package com.leagueplans.uicommon.facades.opfs

import scala.scalajs.js

@js.native
// https://fs.spec.whatwg.org/#api-filesystemwritablefilestream
trait FileSystemWritableFileStream extends js.Object {
  def write(data: FileSystemWriteChunkType): js.Promise[Unit] = js.native

  def seek(position: Long): js.Promise[Unit] = js.native

  def truncate(size: Long): js.Promise[Unit] = js.native

  def close(): js.Promise[Unit] = js.native
}

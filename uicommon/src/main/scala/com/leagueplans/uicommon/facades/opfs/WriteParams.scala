package com.leagueplans.uicommon.facades.opfs

import org.scalajs.dom.{Blob, BufferSource}

import scala.scalajs.js

// https://fs.spec.whatwg.org/#api-filesystemwritablefilestream
trait WriteParams extends js.Object {
  var `type`: WriteCommandType
  var size: js.UndefOr[Long] = js.undefined
  var position: js.UndefOr[Long] = js.undefined
  var data: js.UndefOr[BufferSource | Blob | String] = js.undefined
}

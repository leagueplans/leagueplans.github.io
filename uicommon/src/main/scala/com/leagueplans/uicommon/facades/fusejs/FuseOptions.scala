package com.leagueplans.uicommon.facades.fusejs

import scala.scalajs.js

trait FuseOptions extends js.Object {
  var threshold: js.UndefOr[Double] = js.undefined
  var keys: js.UndefOr[js.Array[String]] = js.undefined
  /** Whether a match far from the start of the text counts as much as one near it. */
  var ignoreLocation: js.UndefOr[Boolean] = js.undefined
}

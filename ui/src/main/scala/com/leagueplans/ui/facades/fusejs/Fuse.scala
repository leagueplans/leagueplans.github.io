package com.leagueplans.ui.facades.fusejs

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@nowarn("msg=unused explicit parameter") @js.native @JSImport("fuse.js", JSImport.Default)
final class Fuse(list: js.Array[js.Any], options: FuseOptions) extends js.Object {
  def search(
    pattern: String,
    options: SearchOptions = SearchOptions(Int.MaxValue)
  ): js.Array[Result] = js.native
}

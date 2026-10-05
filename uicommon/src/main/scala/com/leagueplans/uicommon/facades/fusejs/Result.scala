package com.leagueplans.uicommon.facades.fusejs

import scala.scalajs.js

final class Result(
  val item: js.Any,
  val refIndex: Int,
  /** Only with [[FuseOptions.includeScore]]. From 0 for a perfect match to 1 for none. */
  val score: js.UndefOr[Double],
  /** Only with [[FuseOptions.includeMatches]] */
  val matches: js.UndefOr[js.Array[Result.Match]]
) extends js.Object

object Result {
  @js.native
  trait Match extends js.Object {
    /** Start and end positions, both inclusive, of each matching run of characters */
    val indices: js.Array[js.Tuple2[Int, Int]] = js.native
    /** The key that matched */
    val key: js.UndefOr[String] = js.native
    /** For a key holding a list, the position in it of the text that matched */
    val refIndex: js.UndefOr[Int] = js.native
    /** The text that matched */
    val value: js.UndefOr[String] = js.native
  }
}

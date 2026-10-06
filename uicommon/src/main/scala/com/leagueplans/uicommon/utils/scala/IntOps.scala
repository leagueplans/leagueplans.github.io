package com.leagueplans.uicommon.utils.scala

object IntOps {
  extension (self: Int) {
    /** The number with thousands separators, such as 12,345 */
    def withCommas: String =
      String.format("%,d", self)
  }
}

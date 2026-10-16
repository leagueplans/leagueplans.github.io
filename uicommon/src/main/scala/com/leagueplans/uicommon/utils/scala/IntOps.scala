package com.leagueplans.uicommon.utils.scala

object IntOps {
  extension (self: Int) {
    /** The number with thousands separators, such as 12,345 */
    def withCommas: String =
      String.format("%,d", self)

    /** The number as a position, such as 1st, 2nd, 13th or 1,002nd */
    def ordinal: String = {
      val suffix =
        if (self % 100 >= 11 && self % 100 <= 13) "th"
        else self % 10 match {
          case 1 => "st"
          case 2 => "nd"
          case 3 => "rd"
          case _ => "th"
        }
      s"${self.withCommas}$suffix"
    }
  }
}

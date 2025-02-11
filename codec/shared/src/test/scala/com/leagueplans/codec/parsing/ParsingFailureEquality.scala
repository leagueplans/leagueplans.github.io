package com.leagueplans.codec.parsing

import org.scalactic.Equality

object ParsingFailureEquality {
  given equality: Equality[ParsingFailure] = {
    case (a: ParsingFailure, b: ParsingFailure) =>
      (a.cause == b.cause) &&
        (a.position == b.position) &&
        Equality.default[Array[Byte]].areEqual(a.bytes, b.bytes)
    case _ =>
      false
  }
}

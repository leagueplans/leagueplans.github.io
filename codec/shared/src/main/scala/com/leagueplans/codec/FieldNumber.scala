package com.leagueplans.codec

opaque type FieldNumber <: Int = Int

object FieldNumber {
  inline def apply(i: Int): FieldNumber = i
}

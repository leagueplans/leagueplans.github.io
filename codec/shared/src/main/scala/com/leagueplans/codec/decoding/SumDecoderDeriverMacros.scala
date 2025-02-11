package com.leagueplans.codec.decoding

import scala.annotation.tailrec
import scala.compiletime.summonFrom
import scala.deriving.Mirror
import scala.quoted.{Expr, Quotes, Type}

private[decoding] object SumDecoderDeriverMacros {
  // The tailrec annotation can't be used for typical inline functions. If you use regular
  // recursion instead, then for large ADTs (>100 subtypes) the compiler will fail with a
  // stack overflow, even if you increase the maximum number of inlines.
  def summonOrDeriveDecoders[Types <: Tuple : Type](using quotes: Quotes): Expr[List[Decoder[?]]] = {
    import quotes.reflect.{AppliedType, TypeRepr, TypeReprMethods}
    val nonEmptyTupleType = TypeRepr.of[*:]
    val emptyTupleType = TypeRepr.of[EmptyTuple]

    @tailrec
    def recurse(tupleType: TypeRepr, acc: List[Expr[Decoder[?]]]): List[Expr[Decoder[?]]] =
      tupleType match {
        case AppliedType(tpe, List(head, tail)) if tpe =:= nonEmptyTupleType =>
          val decoder = head.asType match { case '[t] => '{ summonOrDeriveDecoder[t] } }
          recurse(tail, acc :+ decoder)
          
        case tpe if tpe <:< emptyTupleType =>
          acc
      }

    Expr.ofList(recurse(TypeRepr.of[Types].dealias, List.empty))
  }

  private inline def summonOrDeriveDecoder[T]: Decoder[T] =
    summonFrom {
      case decoder: Decoder[T] => decoder
      case given Mirror.Of[T] => Decoder.derived
    }
}

package com.leagueplans.codec.encoding

import com.leagueplans.codec.Encoding

import scala.annotation.tailrec
import scala.deriving.Mirror
import scala.quoted.{Expr, Quotes, Type}

private[encoding] object SumEncoderDeriverMacros {
  // The tailrec annotation can't be used for typical inline functions. If you use regular
  // recursion instead, then for large ADTs (>100 subtypes) the compiler will fail with a
  // stack overflow, even if you increase the maximum number of inlines.
  def encode[Types <: Tuple : Type](value: Expr[Any])(using quotes: Quotes): Expr[Encoding] = {
    import quotes.reflect.*
    val nonEmptyTupleType = TypeRepr.of[*:]
    val emptyTupleType = TypeRepr.of[EmptyTuple]

    @tailrec
    def recurse(tupleType: TypeRepr, acc: List[CaseDef]): List[CaseDef] =
      tupleType match {
        case AppliedType(tpe, List(head, tail)) if tpe =:= nonEmptyTupleType =>
          recurse(tail, acc :+ defineCase(head))
        case tpe if tpe <:< emptyTupleType =>
          acc
      }

    def defineCase(tpe: TypeRepr): CaseDef = {
      val variableName = Symbol.newBind(
        parent = Symbol.spliceOwner,
        name = "subtype",
        Flags.EmptyFlags,
        tpe
      )
      val matcher = Bind(variableName, Typed(Wildcard(), Inferred(tpe)))

      val rhs = tpe.asType match { case '[t] =>
        val patternRef = Ref(variableName).asExprOf[t]
        '{ Encoder.encode($patternRef)(using ${summonOrDeriveEncoder[t]}) }
      }

      CaseDef(matcher, guard = None, rhs.asTerm)
    }

    // Can't use `scala.compiletime.summonFrom` here as it results in a compiler
    // crash when summoning fails. Doing it this way provides a compilation
    // failure with a nicer error message.
    def summonOrDeriveEncoder[T : Type]: Expr[Encoder[T]] =
      Implicits.search(TypeRepr.of[Encoder[T]]) match {
        case success: ImplicitSearchSuccess =>
          success.tree.asExprOf[Encoder[T]]

        case _ =>
          Implicits.search(TypeRepr.of[Mirror.Of[T]]) match {
            case success: ImplicitSearchSuccess =>
              val mirror = success.tree.asExprOf[Mirror.Of[T]]
              '{ Encoder.derived[T](using $mirror) }

            case failure: ImplicitSearchFailure =>
              report.errorAndAbort(failure.explanation)
          }
      }

    Match(
      value.asTerm,
      recurse(TypeRepr.of[Types].dealias, List.empty)
    ).asExprOf[Encoding]
  }
}

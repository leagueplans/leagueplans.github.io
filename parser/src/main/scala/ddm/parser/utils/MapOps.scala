package ddm.parser.utils

import shapeless.labelled.FieldType
import shapeless.ops.coproduct.LiftAll
import shapeless.ops.hlist.{LeftFolder, Zip}
import shapeless.ops.maps.FromMap
import shapeless.{::, Coproduct, Generic, HList, HNil, Id, Poly2, Typeable, Witness}

object MapOps {
  implicit final class RichMap[T](val self: Map[T, _]) extends AnyVal {
    /** Suppose you have
      * - a sealed trait T, whose implementations are singletons
      * - a TypeMapper linking each implementation with another type
      * - a Map, pairing the implementations of T with a value of
      *   their linked type
      *
      * Then this method will return a shapeless record built from the
      * Map. For example:
      * {{{
      * sealed trait T
      * case object A extends T
      * case object B extends T
      *
      * implicit val aToInt    = new TypeMapper[A.type] { type Out = Int }
      * implicit val bToString = new TypeMapper[B.type] { type Out = String }
      *
      * val map: Map[T, _] = Map(A -> 5, B -> "four")
      * val record = map.toRecord.get // Unwrapping the option
      *
      * import shapeless.record.recordOps
      * val a: Int = record.get(A) // Passes type checking
      * val b: Int = record.get(B) // Fails type checking
      * }}}
      */
    def toRecord: Curried[Id, T] =
      new Curried[Id, T](self)

    /** A variant of [[toRecord]] which expects the Map to have shape
      * Map[T, F[_]].
      */
    def toRecordF[F[_]]: Curried[F, T] =
      new Curried[F, T](self)
  }

  final class Curried[F[_], T](val map: Map[T, _]) extends AnyVal {
    def apply[
      TRepr <: Coproduct,
      WitnessesRepr <: HList,
      TypeablesRepr <: HList,
      ZippedRepr <: HList,
      FoldedRepr,
      Record <: HList,
    ]()(
      implicit gen: Generic.Aux[T, TRepr],
      witnesses: LiftAll.Aux[Witness.Aux, TRepr, WitnessesRepr],
      typeables: MappedLiftAll.Aux[Lambda[A => Typeable[F[A]]], TRepr, TypeablesRepr],
      zipper: Zip.Aux[WitnessesRepr :: TypeablesRepr :: HNil, ZippedRepr],
      folder: LeftFolder.Aux[ZippedRepr, FromMap[HNil], fromMapBuilder.type, FoldedRepr],
      ev: FoldedRepr =:= FromMap[Record]
    ): Option[Record] =
      zipper(witnesses.instances :: (typeables.instances :: HNil))
        .foldLeft(FromMap.hnilFromMap)(fromMapBuilder)
        .apply(map)
  }

  object fromMapBuilder extends Poly2 {
    implicit def default[K, V, T <: HList]: Case.Aux[
      FromMap[T],
      (Witness.Aux[K], Typeable[V]),
      FromMap[FieldType[K, V] :: T]
    ] =
      at { case (fmt, (wk, tv)) => FromMap.hlistFromMap(wk, tv, fmt) }
  }
}

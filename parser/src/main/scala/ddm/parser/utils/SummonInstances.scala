package ddm.parser.utils

import shapeless.{Coproduct, Generic, HList, Poly1}
import shapeless.ops.coproduct.LiftAll
import shapeless.ops.hlist.{Mapper, SubtypeUnifier, ToTraversable}

object SummonInstances {
  /** Summons all implementations of a given sealed trait T,
    * assuming all implementations are singletons. Note that
    * in order to infer the appropriate types, the method
    * should be called via
    *
    * SummonInstances[T]()
    */
  def apply[T]: Curried[T] =
    new Curried[T]

  final class Curried[T](val dummy: Boolean = true) extends AnyVal {
    def apply[
      TRepr <: Coproduct,
      ValueOfsRepr <: HList,
      MappersRepr <: HList,
      UnifierRepr <: HList
    ]()(
      implicit gen: Generic.Aux[T, TRepr],
      lift: LiftAll.Aux[ValueOf, TRepr, ValueOfsRepr],
      mapper: Mapper.Aux[toValue.type, ValueOfsRepr, MappersRepr],
      unifier: SubtypeUnifier.Aux[MappersRepr, T, UnifierRepr],
      toTraversable: ToTraversable.Aux[UnifierRepr, List, T]
    ): List[T] =
      lift
        .instances
        .map(toValue)
        .unifySubtypes
        .toList
  }

  object toValue extends Poly1 {
    implicit def default[T]: Case.Aux[ValueOf[T], T] =
      at(_.value)
  }
}

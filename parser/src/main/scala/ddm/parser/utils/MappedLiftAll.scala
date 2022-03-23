package ddm.parser.utils

import shapeless.{:+:, ::, CNil, Coproduct, HList, HNil}

/** Similar to shapeless' LiftAll type, but for summoning
  * instances of type classes for mapped types
  */
sealed trait MappedLiftAll[F[_], In <: Coproduct] {
  type Out <: HList
  def instances: Out
}

object MappedLiftAll {
  type Aux[F[_], In0 <: Coproduct, Out0 <: HList] =
    MappedLiftAll[F, In0] { type Out = Out0 }

  implicit def cnil[F[_]]: MappedLiftAll.Aux[F, CNil, HNil] =
    new MappedLiftAll[F, CNil] {
      type Out = HNil
      val instances: Out = HNil
    }

  implicit def ccons[F[_], H, HOut, T <: Coproduct, TI <: HList](
    implicit typeMapper: TypeMapper.Aux[H, HOut],
    headInstance: F[HOut],
    tailInstances: Aux[F, T, TI]
  ): MappedLiftAll.Aux[F, H :+: T, F[HOut] :: TI] =
    new MappedLiftAll[F, H :+: T] {
      type Out = F[HOut] :: TI
      val instances: Out = headInstance :: tailInstances.instances
    }
}

package ddm.parser.utils

import ddm.parser.utils.MapOps.{RichMap, fromMapBuilder}
import shapeless.ops.coproduct.LiftAll
import shapeless.ops.hlist.{LeftFolder, Mapper, SubtypeUnifier, ToTraversable, Zip}
import shapeless.ops.maps.FromMap
import shapeless.ops.record.Selector
import shapeless.record.recordOps
import shapeless.{::, Coproduct, Generic, HList, HNil, Typeable, Witness}

import scala.util.chaining.scalaUtilChainingOps

object RecordBuilder {
  def apply[K <: RecordBuilder.Key]: Curried[K] =
    new Curried[K]

  final class Curried[K <: RecordBuilder.Key](val dummy: Boolean = true) extends AnyVal {
    def apply[
      KRepr <: Coproduct,
      ValueOfsRepr <: HList,
      MapperRepr <: HList,
      UnifierRepr <: HList,
      Record <: HList
    ]()(
      implicit gen: Generic.Aux[K, KRepr],
      lift: LiftAll.Aux[ValueOf, KRepr, ValueOfsRepr],
      mapper: Mapper.Aux[SummonInstances.toValue.type, ValueOfsRepr, MapperRepr],
      unifier: SubtypeUnifier.Aux[MapperRepr, K, UnifierRepr],
      toTraversable: ToTraversable.Aux[UnifierRepr, List, K]
    ): RecordBuilder[K] =
      new RecordBuilder(SummonInstances[K]().map(_ -> None).toMap)
  }

  sealed trait Implicits[K, Record <: HList] {
    type KRepr <: Coproduct
    type WitnessesRepr <: HList
    type TypeablesRepr <: HList
    type ZippedRepr <: HList
    type FoldedRepr

    implicit def gen: Generic.Aux[K, KRepr]
    implicit def witnesses: LiftAll.Aux[Witness.Aux, KRepr, WitnessesRepr]
    implicit def typeables: MappedLiftAll.Aux[Lambda[A => Typeable[Option[A]]], KRepr, TypeablesRepr]
    implicit def zipper: Zip.Aux[WitnessesRepr :: TypeablesRepr :: HNil, ZippedRepr]
    implicit def folder: LeftFolder.Aux[ZippedRepr, FromMap[HNil], fromMapBuilder.type, FoldedRepr]
    implicit def ev: FoldedRepr =:= FromMap[Record]
  }

  object Implicits {
    implicit def generate[
      K,
      KRepr0 <: Coproduct,
      WitnessesRepr0 <: HList,
      TypeablesRepr0 <: HList,
      ZippedRepr0 <: HList,
      FoldedRepr0,
      Record <: HList
    ](
      implicit gen0: Generic.Aux[K, KRepr0],
      witnesses0: LiftAll.Aux[Witness.Aux, KRepr0, WitnessesRepr0],
      typeables0: MappedLiftAll.Aux[Lambda[A => Typeable[Option[A]]], KRepr0, TypeablesRepr0],
      zipper0: Zip.Aux[WitnessesRepr0 :: TypeablesRepr0 :: HNil, ZippedRepr0],
      folder0: LeftFolder.Aux[ZippedRepr0, FromMap[HNil], fromMapBuilder.type, FoldedRepr0],
      ev0: FoldedRepr0 =:= FromMap[Record]
    ): Implicits[K, Record] =
      new Implicits[K, Record] {
        type KRepr = KRepr0
        type WitnessesRepr = WitnessesRepr0
        type TypeablesRepr = TypeablesRepr0
        type ZippedRepr = ZippedRepr0
        type FoldedRepr = FoldedRepr0

        implicit def gen: Generic.Aux[K, KRepr0] = gen0
        implicit def witnesses: LiftAll.Aux[Witness.Aux, KRepr0, WitnessesRepr0] = witnesses0
        implicit def typeables: MappedLiftAll.Aux[Lambda[A => Typeable[Option[A]]], KRepr0, TypeablesRepr0] = typeables0
        implicit def zipper: Zip.Aux[WitnessesRepr0 :: TypeablesRepr0 :: HNil, ZippedRepr0] = zipper0
        implicit def folder: LeftFolder.Aux[ZippedRepr0, FromMap[HNil], MapOps.fromMapBuilder.type, FoldedRepr0] = folder0
        implicit def ev: =:=[FoldedRepr0, FromMap[Record]] = ev0
      }
  }

  trait Key { self =>
    type Value

    implicit final lazy val typeMapper: TypeMapper.Aux[self.type, self.Value] =
      new TypeMapper[self.type] { type Out = self.Value }
  }
}

final class RecordBuilder[K <: RecordBuilder.Key] private (underlying: Map[K, Option[_]]) {
  def set[K1 <: K](k: K1)(v: k.Value): RecordBuilder[K] =
    new RecordBuilder(underlying + (k -> Some(v)))

  def modify[K1 <: K, Record <: HList](k: K1)(f: Option[k.Value] => k.Value)(
    implicit implicits: RecordBuilder.Implicits[K, Record],
    witness: Witness.Aux[K1],
    selector: Selector.Aux[Record, K1, Option[k.Value]]
  ): RecordBuilder[K] =
    asRecord
      .get(witness)
      .pipe(maybeV => set(k)(f(maybeV)))

  def asRecord[Record <: HList](implicit implicits: RecordBuilder.Implicits[K, Record]): Record = {
    import implicits._
    underlying
      .toRecordF[Option]()
      .getOrElse(presumedImpossible())
  }

  private def presumedImpossible(): Nothing =
    throw new RuntimeException(
      s"""This should be impossible.
         |
         |Creation of instances of RecordBuilder[T] is strictly controlled, such
         |that all initial creations go through RecordBuilder.apply[T]. The Map
         |created in this method inserts a key (with value None) for each possible
         |instance of T.
         |
         |The only other way that RecordBuilder[T]s are then created is through
         |the RecordBuilder#set method. It shouldn't be possible to add new keys
         |into the underlying Map through this method, and it's definitely not
         |possible to remove any. As such, the keys should remain constant after
         |the initial RecordBuilder[T] is created.
         |
         |The FromMap instance we create as part of initialising asRecord expects
         |the set of keys in the underlying Map to be exactly equal to the set of
         |all possible instances of T. As discussed, that is always true, and
         |therefore the Option[Record] returned is always a Some[Record]. This is
         |why .get should be safe to call.
         |""".stripMargin
    )

  override def toString: String = {
    val filtered = underlying.collect { case (k, Some(v)) => k -> v }
    s"RecordBuilder(${filtered.mkString(", ")})"
  }
}

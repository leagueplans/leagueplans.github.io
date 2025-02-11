package ddm.codec

import org.scalactic.Equality

final class MapEquality[K, V : Equality as vEquality] extends Equality[Map[K, V]] {
  def areEqual(a: Map[K, V], other: Any): Boolean =
    other match {
      case b: Map[K @unchecked, ?] =>
        (a.size == b.size) &&
          a.forall((k, v) => b.get(k).exists(vEquality.areEqual(v, _)))

      case _ => false
    }
}

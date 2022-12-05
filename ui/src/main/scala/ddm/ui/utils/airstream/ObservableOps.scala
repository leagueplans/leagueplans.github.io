package ddm.ui.utils.airstream

import com.raquo.airstream.core.{BaseObservable, Observable}

object ObservableOps {
  implicit final class RichObserverTuple[+F[+_] <: Observable[_], S, T](val self: BaseObservable[F, (S, T)]) extends AnyVal {
    def unzip: (F[S], F[T]) = {
      val s: F[S] = self.map[S] { case (s, _) => s }
      val t: F[T] = self.map[T] { case (_, t) => t }
      (s, t)
    }
  }
}

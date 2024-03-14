package ddm.ui.utils.airstream

import com.raquo.airstream.core.{BaseObservable, Observable}

object ObservableOps {
  extension [F[+_] <: Observable[?], S, T](self: BaseObservable[F, (S, T)]) {
    def unzip: (F[S], F[T]) = {
      val s: F[S] = self.map[S]((s, _) => s)
      val t: F[T] = self.map[T]((_, t) => t)
      (s, t)
    }
  }
}

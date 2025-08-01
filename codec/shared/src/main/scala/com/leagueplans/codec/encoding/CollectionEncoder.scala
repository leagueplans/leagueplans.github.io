package com.leagueplans.codec.encoding

import com.leagueplans.codec.Encoding

trait CollectionEncoder[T] {
  extension [S <: T](s: S) def encoded: List[Encoding]
  
  final def contramap[S](f: S => T): CollectionEncoder[S] =
    CollectionEncoder(f(_).encoded)
}

object CollectionEncoder {
  def apply[T : CollectionEncoder as encoder]: encoder.type =
    encoder

  def apply[T](f: T => List[Encoding]): CollectionEncoder[T] =
    new CollectionEncoder[T] {
      extension [S <: T](s: S) def encoded: List[Encoding] = f(s)
    }

  def encode[T : CollectionEncoder](t: T): List[Encoding] =
    t.encoded

  given iterableOnceEncoder[F[X] <: IterableOnce[X], T : Encoder]: CollectionEncoder[F[T]] =
    CollectionEncoder(_.iterator.toList.map(_.encoded))
    
  given mapEncoder[K, V](using Encoder[(K, V)]): CollectionEncoder[Map[K, V]] =
    iterableOnceEncoder[List, (K, V)].contramap(_.toList)
}

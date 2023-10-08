package ddm.ui.model.common.forest

import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}

object Tree {
  implicit def codec[T : Encoder : Decoder]: Codec[Tree[T]] =
    deriveCodec[Tree[T]]
}

final case class Tree[T](data: T, children: List[Tree[T]])

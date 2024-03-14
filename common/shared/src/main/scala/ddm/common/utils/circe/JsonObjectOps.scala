package ddm.common.utils.circe

import io.circe.{CursorOp, Decoder, DecodingFailure, JsonObject}

object JsonObjectOps {
  extension (self: JsonObject) {
    def decodeField[T : Decoder](key: String, ops: => List[CursorOp]): Decoder.Result[T] =
      decodeOptField[T](key).flatMap(
        _.toRight(left =
          DecodingFailure(s"Missing key: [$key]", ops :+ CursorOp.Field(key))
        )
      )

    def decodeOptField[T : Decoder](key: String): Decoder.Result[Option[T]] =
      self(key) match {
        case Some(json) => json.as[T].map(Some(_))
        case None => Right(None)
      }

    def decodeNestedField[T : Decoder](key1: String, keys: String*)(ops: => List[CursorOp]): Decoder.Result[T] =
      keys.toList match {
        case key2 :: rest =>
          decodeField[JsonObject](key1, ops).flatMap(obj =>
            obj.decodeNestedField[T](key2, rest*)(ops :+ CursorOp.Field(key1))
          )
        case Nil =>
          decodeField[T](key1, ops)
      }
  }
}

package ddm.ui.model.common

import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, JsonObject}

import scala.annotation.tailrec

object Tree {
  implicit def encoder[T : Encoder]: Encoder[Tree[T]] =
    Encoder[JsonObject].contramap(tree =>
      JsonObject(
        "root" -> tree.root.asJson,
        "children" -> tree.children.asJson
      )
    )

  implicit def decoder[T : Decoder]: Decoder[Tree[T]] =
    Decoder[JsonObject].emap(obj =>
      for {
        root <- decodeField[T](obj, "root")
        children <- decodeField[List[Tree[T]]](obj, "children")
      } yield Tree(root, children)
    )

  private def decodeField[T : Decoder](obj: JsonObject, key: String): Either[String, T] =
    obj(key)
      .toRight(left = s"Missing key: [$key]")
      .flatMap(_.as[T].left.map(_.message))
}

final case class Tree[T](root: T, children: List[Tree[T]]) {
  def map[S](f: T => S): Tree[S] =
    Tree(f(root), children.map(_.map(f)))

  def flatten: List[T] =
    flattenHelper(acc = List(root), remaining = children)

  @tailrec
  private def flattenHelper(acc: List[T], remaining: List[Tree[T]]): List[T] =
    remaining match {
      case Nil => acc
      case h :: t => flattenHelper(acc = acc :+ h.root, remaining = h.children ++ t)
    }
}

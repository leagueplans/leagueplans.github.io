package ddm.ui.model.common

import cats.{Functor, Monoid}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, JsonObject}

import scala.annotation.tailrec

object Tree {
  def updateRoot[T](
    updated: Tree[T],
    ancestors: List[Tree[T]]
  ): Tree[T] =
    ancestors.foldLeft(updated) { (descendant, ancestor) =>
      val node = descendant.node
      ancestor.copy(children = ancestor.children.map {
        case Tree(`node`, _) => descendant
        case other => other
      })
    }

  implicit val functor: Functor[Tree] =
    new Functor[Tree] {
      def map[A, B](fa: Tree[A])(f: A => B): Tree[B] =
        fa.map(f)
    }

  implicit def encoder[T : Encoder]: Encoder[Tree[T]] =
    Encoder[JsonObject].contramap(tree =>
      JsonObject(
        "node" -> tree.node.asJson,
        "children" -> tree.children.asJson
      )
    )

  implicit def decoder[T : Decoder]: Decoder[Tree[T]] =
    Decoder[JsonObject].emap(obj =>
      for {
        node <- decodeField[T](obj, "node")
        children <- decodeField[List[Tree[T]]](obj, "children")
      } yield Tree(node, children)
    )

  private def decodeField[T : Decoder](obj: JsonObject, key: String): Either[String, T] =
    obj(key)
      .toRight(left = s"Missing key: [$key]")
      .flatMap(_.as[T].left.map(_.message))
}

final case class Tree[T](node: T, children: List[Tree[T]]) {
  lazy val nodeToTree: Map[T, Tree[T]] =
    recurse(tree => List(tree.node -> tree)).toMap

  lazy val childNodeToParentNode: Map[T, T] =
    recurse(tree => tree.children.map(_.node -> tree.node)).toMap

  def map[S](f: T => S): Tree[S] =
    new Tree(f(node), children.map(_.map(f)))

  def addChild(tree: Tree[T]): Tree[T] =
    copy(children = children :+ tree)

  def removeChild(tree: Tree[T]): Tree[T] =
    copy(children = children.filterNot(_ == tree))

  def update[K](updatedDescendant: Tree[T])(toKey: T => K): Tree[T] = {
    if (toKey(node) == toKey(updatedDescendant.node))
      updatedDescendant
    else
      ancestors(updatedDescendant.node)(toKey) match {
        case Nil => this
        case ancestors =>
          ancestors.foldLeft(updatedDescendant) { (child, parent) =>
            parent.copy(children = parent.children.map {
              case Tree(node, _) if toKey(node) == toKey(child.node) => child
              case other => other
            })
          }
      }
  }

  private def ancestors[K](child: T)(toKey: T => K): List[Tree[T]] = {
    val keyToParentNode =
      childNodeToParentNode.map { case (k, v) => toKey(k) -> v }

    List.unfold(child)(node =>
      keyToParentNode
        .get(toKey(node))
        .map(parent => (parent, parent))
    ).map(nodeToTree)
  }

  def recurse[Acc : Monoid](f: Tree[T] => Acc): Acc =
    recursionHelper(acc = Monoid[Acc].empty, remaining = List(this))(f)

  @tailrec
  private def recursionHelper[Acc : Monoid](
    acc: Acc,
    remaining: List[Tree[T]]
  )(f: Tree[T] => Acc): Acc =
    remaining match {
      case Nil => acc
      case h :: t =>
        recursionHelper(
          acc = Monoid[Acc].combine(acc, f(h)),
          remaining = h.children ++ t
        )(f)
    }

  override def toString: String =
    toStringHelper(indentSize = 0)

  private def toStringHelper(indentSize: Int): String = {
    val indent = List.fill(indentSize)(' ').mkString
    val childrenString = children.map(_.toStringHelper(indentSize + 2)).mkString("\n", "", s"")
    s"$indent$node$childrenString"
  }
}

package ddm.ui.model.common

import cats.{Functor, Monoid}
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}

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

  implicit def codec[T : Encoder : Decoder]: Codec[Tree[T]] =
    deriveCodec[Tree[T]]
}

final case class Tree[T](node: T, children: List[Tree[T]]) {
  lazy val nodeToTree: Map[T, Tree[T]] =
    recurse(tree => List(tree.node -> tree)).toMap

  lazy val childNodeToParentNode: Map[T, T] =
    recurse(tree => tree.children.map(_.node -> tree.node)).toMap

  def mapNode(f: T => T): Tree[T] =
    copy(f(node))

  def map[S](f: T => S): Tree[S] =
    Tree(f(node), children.map(_.map(f)))

  def addChild(tree: Tree[T]): Tree[T] =
    copy(children = children :+ tree)

  def removeChild(tree: Tree[T]): Tree[T] =
    copy(children = children.filterNot(_ == tree))

  def modifyChild[K](key: K)(toKey: T => K)(f: Tree[T] => Tree[T]): Tree[T] =
    copy(children = children.map {
      case child @ Tree(node, _) if toKey(node) == key => f(child)
      case other => other
    })

  def update[K](updatedDescendant: Tree[T])(toKey: T => K): Tree[T] =
    modify(toKey(updatedDescendant.node))(toKey)(_ => updatedDescendant)

  def modify[K](key: K)(toKey: T => K)(f: Tree[T] => Tree[T]): Tree[T] =
    if (toKey(node) == key)
      f(this)
    else
      ancestors(key)(toKey) match {
        case Nil => this
        case parentOfTarget :: ancestors =>
          val updatedParent = parentOfTarget.modifyChild(key)(toKey)(f)
          ancestors.foldLeft(updatedParent)((updatedChild, parent) =>
            parent.modifyChild(toKey(updatedChild.node))(toKey)(_ => updatedChild)
          )
      }

  private def ancestors[K](key: K)(toKey: T => K): List[Tree[T]] = {
    val keyToParentNode =
      childNodeToParentNode.map { case (child, parent) => toKey(child) -> parent }

    List.unfold(key)(k =>
      keyToParentNode
        .get(k)
        .map(parent => (parent, toKey(parent)))
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

package ddm.ui.model.common

import scala.annotation.tailrec

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

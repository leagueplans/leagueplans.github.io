package ddm.ui.component.common

import ddm.ui.component.common.TreeComponent.Props
import ddm.ui.model.common.Tree
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, Key, ScalaComponent}

import scala.util.chaining.scalaUtilChainingOps

object TreeComponent {
  final case class Props[T](
    tree: Tree[T],
    toKey: Tree[T] => Key,
    editTree: Tree[T] => Callback,
    editingEnabled: Boolean,
    renderT: (T, VdomNode) => VdomNode
  )
}

final class TreeComponent[T] {
  val build: Component[Props[T], Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props[T]]
      .render_P(render)
      .build

  private val subtreeReorderingComponent = new DragSortableListComponent[Key].build

  private def render(props: Props[T]): VdomNode = {
    val keyToChild = props.tree.children.map(t => props.toKey(t) -> t).toMap

    val subTrees =
      subtreeReorderingComponent(DragSortableListComponent.Props(
        props.tree.children.map(props.toKey),
        _.map(keyToChild).pipe(newOrder =>
          props.editTree(props.tree.copy(children = newOrder))
        ),
        renderChildren(props, keyToChild, _)
      ))

    props.renderT(props.tree.node, subTrees)
  }

  private def renderChildren(
    props: Props[T],
    keyToTree: Map[Key, Tree[T]],
    keyTagPairs: List[(Key, TagMod)],
  ): VdomNode =
    <.ol(
      ^.className := "tree-children",
      keyTagPairs.toTagMod { case (key, dragControlTag) =>
        val tree = keyToTree(key)

        <.li(
          ^.key := key,
          Option.when(props.editingEnabled)(dragControlTag).whenDefined,
          build(Props(
            tree,
            props.toKey,
            Tree.updateRoot(_, List(props.tree)).pipe(props.editTree),
            props.editingEnabled,
            props.renderT
          ))
        )
      }
    )
}

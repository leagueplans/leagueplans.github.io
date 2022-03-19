package ddm.ui.component.common

import ddm.ui.component.Render
import ddm.ui.model.common.Tree
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, Key, ScalaComponent}

import scala.util.chaining.scalaUtilChainingOps

object TreeComponent {
  def build[T]: ScalaComponent[Props[T], Unit, Backend[T], CtorType.Props] =
    ScalaComponent
      .builder[Props[T]]
      .renderBackend[Backend[T]]
      .build

  final case class Props[T](
    tree: Tree[T],
    toKey: Tree[T] => Key,
    editTree: Tree[T] => Callback,
    editingEnabled: Boolean,
    render: Render[T]
  )

  final class Backend[T](scope: BackendScope[Props[T], Unit]) {
    private val dragSortableListComponent = DragSortableListComponent.build[Key]

    def render(props: Props[T]): VdomNode = {
      val keyToChild = props.tree.children.map(t => props.toKey(t) -> t).toMap

      val subTrees =
        dragSortableListComponent(DragSortableListComponent.Props(
          props.tree.children.map(props.toKey),
          _.map(keyToChild).pipe(newOrder =>
            props.editTree(props.tree.copy(children = newOrder))
          ),
          renderChildren(props, keyToChild, _)
        ))

      props.render(props.tree.node, subTrees)
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
            dragControlTag.when(props.editingEnabled),
            render(Props(
              tree,
              props.toKey,
              Tree.updateRoot(_, List(props.tree)).pipe(props.editTree),
              props.editingEnabled,
              props.render
            ))
          )
        }
      )
  }
}

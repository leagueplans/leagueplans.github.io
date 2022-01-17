package ddm.ui.component.common

import cats.data.NonEmptyList
import ddm.ui.component.common.DragSortableTreeComponent.Props
import ddm.ui.component.plan.editing.EditingManagementComponent.EditingMode
import ddm.ui.model.common.Tree
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, Key, ScalaComponent}
import scala.util.chaining.scalaUtilChainingOps

object DragSortableTreeComponent {
  final case class Props[T](
    tree: Tree[T],
    toKey: Tree[T] => Key,
    editTree: Tree[T] => Callback,
    editingMode: EditingMode,
    renderT: (T, VdomNode) => VdomNode
  )
}

final class DragSortableTreeComponent[T] {
  val build: Component[Props[T], Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props[T]]
      .render_P(render)
      .build

  private val sortComponent = DragSortableComponent.build[Tree, T]
  private val treeComponent = new TreeComponent[(T, TagMod)].build

  private def render(props: Props[T]): VdomNode =
    sortComponent(DragSortableComponent.Props(
      props.tree,
      showPreview = false,
      isViableTarget(_, props.tree.childNodeToParentNode),
      transform,
      props.editTree,
      render(_, props)
    ))

  private def render(tree: Tree[(T, TagMod)], props: Props[T]): VdomNode =
    treeComponent(TreeComponent.Props[(T, TagMod)](
      tree,
      props.toKey.compose(_.map { case (t, _) => t }),
      props.editTree.compose(_.map { case (t, _) => t }),
      props.editingMode == EditingMode.ModifyOrder,
      { case ((t, dragTags), substeps) =>
        <.div(
          dragTags.when(props.editingMode == EditingMode.ModifyHierarchy),
          props.renderT(t, substeps)
        )
      }
    ))

  private def isViableTarget(
    hover: DragSortableComponent.Hover[T],
    childNodeToParentNode: Map[T, T]
  ): Boolean = {
    // Doesn't actually change the parent
    lazy val notParent = !childNodeToParentNode.get(hover.dragged).contains(hover.hovered)
    // Should avoid loops
    lazy val hoveredAncestryLine = ancestryLine(hover.hovered, childNodeToParentNode).toList.toSet
    lazy val draggedNotAncestorOfHovered = !hoveredAncestryLine.contains(hover.dragged)

    notParent && draggedNotAncestorOfHovered
  }

  private def ancestryLine(firstNode: T, childNodeToParentNode: Map[T, T]): NonEmptyList[T] =
    NonEmptyList(
      firstNode,
      List.unfold(firstNode)(node =>
        childNodeToParentNode
          .get(node)
          .map(parent => (parent, parent))
      )
    )

  private def transform(hover: DragSortableComponent.Hover[T], tree: Tree[T]): Tree[T] = {
    val dragged =
      tree.nodeToTree(hover.dragged)

    val parentOfDragged =
      tree.childNodeToParentNode(hover.dragged)
        .pipe(tree.nodeToTree)

    val treeWithDraggedRemoved =
      tree.update(
        parentOfDragged.removeChild(dragged)
      )(toKey = identity)

    val hovered =
      treeWithDraggedRemoved.nodeToTree(hover.hovered)

    treeWithDraggedRemoved.update(
      hovered.addChild(dragged)
    )(toKey = identity)
  }
}

package ddm.ui.component.common

import cats.data.NonEmptyList
import ddm.ui.component.common.DragSortableTreeComponent.{EditingMode, Props}
import ddm.ui.model.common.Tree
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, Key, ScalaComponent}

object DragSortableTreeComponent {
  final case class Props[T](
    tree: Tree[T],
    toKey: Tree[T] => Key,
    editTree: Tree[T] => Callback,
    editingMode: EditingMode,
    renderT: (T, VdomNode) => VdomNode
  )

  sealed trait EditingMode

  object EditingMode {
    case object Locked extends EditingMode
    case object ModifyHierarchy extends EditingMode
    case object ModifyOrder extends EditingMode
  }
}

final class DragSortableTreeComponent[T] {
  val build: Component[Props[T], Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props[T]]
      .render_P(render)
      .build

  private val sortComponent = DragSortableComponent.build[Tree, T]
  private val treeComponent = new TreeComponent[(T, TagMod)].build

  private def render(props: Props[T]): VdomNode = {
    val nodeToTree =
      props.tree.recurse(tree =>
        List(tree.node -> tree)
      ).toMap

    val childNodeToParentNode =
      props.tree.recurse(tree =>
        tree.children.map(_.node -> tree.node)
      ).toMap

    sortComponent(DragSortableComponent.Props(
      props.tree,
      showPreview = false,
      isViableTarget(_, childNodeToParentNode),
      // Disabled previews, so we're only ever transforming the initial state,
      // for which we've pre-generated some maps for above
      (hover, _) => transform(hover, nodeToTree, childNodeToParentNode),
      props.editTree,
      render(_, props)
    ))
  }

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

  private def transform(
    hover: DragSortableComponent.Hover[T],
    nodeToTree: Map[T, Tree[T]],
    childNodeToParentNode: Map[T, T]
  ): Tree[T] = {
    val nodeHoveredAncestryLine @ NonEmptyList(nodeHovered, _ ) =
      ancestryLine(hover.hovered, childNodeToParentNode)

    // Suppose nodeDraggedAncestors was empty
    // therefore nodeDragged is the root node
    // but a root node cannot be dragged anywhere, since all nodes are its children which would cause a loop
    // therefore nodeDraggedAncestors cannot be empty
    val NonEmptyList(nodeDragged, nodeDraggedAncestors @ nodeParentOfDragged :: _) =
      ancestryLine(hover.dragged, childNodeToParentNode)

    // nodesDraggedAncestors cannot be empty as above
    // neither can the nodeHoveredAncestryLine
    // therefore there's at least one common node
    val (nodeFirstCommonAncestor :: nodeSubsequentCommonAncestors, _) =
      nodeDraggedAncestors
        .reverse
        .zip(nodeHoveredAncestryLine.toList.reverse)
        .takeWhile { case (a, b) => a == b }
        .reverse
        .unzip

    // the hover target may be the firstCommonAncestor, so prune it afterwards
    val nodeHoveredUniqueAncestors =
      nodeHoveredAncestryLine
        .toList
        .takeWhile(_ != nodeFirstCommonAncestor)
        .dropWhile(_ == nodeHoveredAncestryLine.head)

    // the parentOfDragged may be the firstCommonAncestor, so prune it afterwards
    val nodeParentOfDraggedUniqueAncestors =
      nodeDraggedAncestors
        .takeWhile(_ != nodeFirstCommonAncestor)
        .dropWhile(_ == nodeParentOfDragged)

    val treeDragged = nodeToTree(nodeDragged)
    val treeUpdatedParentOfDragged = nodeToTree(nodeParentOfDragged).removeChild(treeDragged)
    val treeUpdatedHovered = nodeToTree(nodeHovered).addChild(treeDragged)

    val treeFirstCommonAncestorWithBothUpdates =
      nodeFirstCommonAncestor match {
        case `nodeHovered` =>
          Tree.updateRoot(
            treeUpdatedParentOfDragged,
            nodeParentOfDraggedUniqueAncestors.map(nodeToTree) :+ treeUpdatedHovered
          )

        case `nodeParentOfDragged` =>
          Tree.updateRoot(
            treeUpdatedHovered,
            nodeHoveredUniqueAncestors.map(nodeToTree) :+ treeUpdatedParentOfDragged
          )

        case _ =>
          val treeFirstCommonAncestorWithFirstUpdate =
            Tree.updateRoot(
              treeUpdatedParentOfDragged,
              (nodeParentOfDraggedUniqueAncestors :+ nodeFirstCommonAncestor).map(nodeToTree)
            )

          Tree.updateRoot(
            treeUpdatedHovered,
            nodeHoveredUniqueAncestors.map(nodeToTree) :+ treeFirstCommonAncestorWithFirstUpdate
          )
      }

    // with both updates applied, we can now safely update the common ancestors
    Tree.updateRoot(
      treeFirstCommonAncestorWithBothUpdates,
      nodeSubsequentCommonAncestors.map(nodeToTree)
    )
  }
}

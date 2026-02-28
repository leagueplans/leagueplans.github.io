package com.leagueplans.ui.facades.floatingui

// https://github.com/floating-ui/floating-ui/blob/d729054f1ebcf1d7c3e85c0967d455f816985671/packages/utils/src/index.ts#L8
type Placement =
  Placement.top.type |
    Placement.topStart.type |
    Placement.topEnd.type |
    Placement.right.type |
    Placement.rightStart.type |
    Placement.rightEnd.type |
    Placement.bottom.type |
    Placement.bottomStart.type |
    Placement.bottomEnd.type |
    Placement.left.type |
    Placement.leftStart.type |
    Placement.leftEnd.type

object Placement {
  val top = "top"
  val topStart = "top-start"
  val topEnd = "top-end"
  val right = "right"
  val rightStart = "right-start"
  val rightEnd = "right-end"
  val bottom = "bottom"
  val bottomStart = "bottom-start"
  val bottomEnd = "bottom-end"
  val left = "left"
  val leftStart = "left-start"
  val leftEnd = "left-end"
  
  type AlongTheBottom = Placement.bottom.type | Placement.bottomStart.type | Placement.bottomEnd.type
  type AlongTheRight = Placement.right.type | Placement.rightStart.type | Placement.rightEnd.type
  type AlongTheTop = Placement.top.type | Placement.topStart.type | Placement.topEnd.type
  type AlongTheLeft = Placement.left.type | Placement.leftStart.type | Placement.leftEnd.type
}

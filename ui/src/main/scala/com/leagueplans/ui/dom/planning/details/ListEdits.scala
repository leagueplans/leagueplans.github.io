package com.leagueplans.ui.dom.planning.details

/** Edits to a step's effects or requirements by position. Positions outside the list leave it
  * unchanged, since a list can change between a row being chosen and the edit arriving. */
object ListEdits {
  def delete[T](list: List[T], index: Int): List[T] =
    if (list.indices.contains(index)) list.patch(index, Nil, 1) else list

  def replace[T](list: List[T], index: Int, item: T): List[T] =
    if (list.indices.contains(index)) list.updated(index, item) else list
}

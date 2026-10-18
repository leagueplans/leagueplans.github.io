package com.leagueplans.uicommon.dom.form

/** The parts of a dropdown's keyboard handling that don't need the page: which entry the arrow
  * keys move to, and which entry typing jumps to. Entries are counted across the dropdown's
  * groups, in the order they're shown.
  */
private[form] object SelectKeys {
  /** How far Page Up and Page Down move */
  val pageSize: Int = 10

  /** The entry a key moves to from `from`, out of `size` entries, or None for a key that doesn't
    * move. Moving stops at the ends rather than wrapping, as a native select's does. Nothing
    * marked, which is -1, moves to the first entry going down and the last going up.
    */
  def move(from: Int, size: Int, key: String): Option[Int] =
    if (size <= 0) None
    else {
      val target =
        key match {
          case "ArrowDown" => Some(if (from < 0) 0 else from + 1)
          case "ArrowUp" => Some(if (from < 0) size - 1 else from - 1)
          case "PageDown" => Some(if (from < 0) 0 else from + pageSize)
          case "PageUp" => Some(if (from < 0) size - 1 else from - pageSize)
          case "Home" => Some(0)
          case "End" => Some(size - 1)
          case _ => None
        }
      target.map(_.max(0).min(size - 1))
    }

  /** The entry typing `query` jumps to, when `from` is marked: the next entry that starts with
    * what's been typed, ignoring case. A single letter looks past the marked entry, so that
    * typing it again moves through the entries that start with it. A longer query looks from the
    * marked entry itself, so that typing more of its name keeps it. Typing the same letter
    * repeatedly moves through the entries that start with that letter, unless an entry starts
    * with the repeated letters.
    */
  def typeAhead(labels: IndexedSeq[String], from: Int, query: String): Option[Int] =
    if (labels.isEmpty || query.isEmpty) None
    else {
      val needle = query.toLowerCase
      def search(prefix: String, start: Int): Option[Int] = {
        val first = Math.floorMod(start, labels.size)
        (0 until labels.size).iterator
          .map(offset => (first + offset) % labels.size)
          .find(labels(_).toLowerCase.startsWith(prefix))
      }

      if (needle.length == 1) search(needle, from + 1)
      else search(needle, from.max(0)).orElse(
        Option.when(needle.forall(_ == needle.head))(needle.take(1)).flatMap(search(_, from + 1))
      )
    }

  /** Numbers the entries of each group, counting on across the groups */
  def numbered[A](groups: List[List[A]]): List[List[(A, Int)]] =
    groups
      .scanLeft((List.empty[(A, Int)], 0)) { case ((_, start), group) =>
        (group.zipWithIndex.map((entry, index) => entry -> (start + index)), start + group.size)
      }
      .drop(1)
      .map(_._1)

  /** Whether a key types a character, rather than being a named key such as Tab or a shortcut */
  def isPrintable(key: String, ctrl: Boolean, meta: Boolean, alt: Boolean): Boolean =
    key.length == 1 && !ctrl && !meta && !alt
}

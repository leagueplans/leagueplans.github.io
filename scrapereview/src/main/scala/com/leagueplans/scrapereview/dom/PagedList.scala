package com.leagueplans.scrapereview.dom

import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor, textToTextNode}

/** Renders a long list a page at a time.
  *
  * A scrape after a long gap produces thousands of entries in a single bucket, and the
  * cards here are not cheap — several carry their own reactive bindings, a search box, or
  * an image read off disk. Rendering them all at once locks the page up for seconds.
  */
private[scrapereview] object PagedList {
  private val pageSize = 25

  def apply[A](items: List[A])(render: A => L.Node): L.Div = {
    val shown = Var(pageSize)

    L.div(
      L.cls(Styles.list),
      L.children <-- shown.signal.map(items.take(_).map(render)),
      L.child <-- shown.signal.map(limit =>
        if (limit >= items.size) L.emptyNode
        else pager(items.size, limit, shown)
      )
    )
  }

  private def pager(total: Int, shown: Int, limit: Var[Int]): L.Div =
    L.div(
      L.cls(Styles.pager),
      L.button(
        L.cls(Styles.button),
        L.tpe("button"),
        s"Show ${math.min(pageSize, total - shown)} more",
        L.onClick --> (_ => limit.update(_ + pageSize))
      ),
      L.button(
        L.cls(Styles.button),
        L.tpe("button"),
        "Show all",
        L.onClick --> (_ => limit.set(total)),
        // The reviewer may well want the lot; they should just know it will not be quick.
        L.title(s"Renders all $total at once, which may take a moment")
      ),
      L.span(L.cls(Styles.pagerStatus), s"showing $shown of $total")
    )
}

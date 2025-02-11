package com.leagueplans.ui.facades.js

import scala.scalajs.js

trait AsyncIterator[+T] extends js.Object {
  def next(): js.Promise[js.Iterator.Entry[T]]
}

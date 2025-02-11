package com.leagueplans.scraper.wiki.scraper.leagues

import zio.{Ref, Trace, UIO}

opaque type TaskIndexer = Ref[Int]

object TaskIndexer {
  def make(using Trace): UIO[TaskIndexer] = Ref.make(1)
}

extension (self: TaskIndexer) {
  def next(using Trace): UIO[Int] = self.getAndIncrement
}

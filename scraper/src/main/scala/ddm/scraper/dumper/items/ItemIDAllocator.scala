package ddm.scraper.dumper.items

import ddm.common.model.Item
import zio.{Ref, Trace, UIO, ZIO}

private[items] object ItemIDAllocator {
  def make(occupied: Map[WikiKey, Item.ID])(using Trace): UIO[ItemIDAllocator] =
    Ref.make[Int](0).map(ItemIDAllocator(_, occupied))
}

private[items] final class ItemIDAllocator(last: Ref[Int], occupied: Map[WikiKey, Item.ID]) {
  private val ids = occupied.values.toSet
  
  def get(key: WikiKey)(using Trace): UIO[Item.ID] =
    occupied.get(key) match {
      case Some(id) => ZIO.succeed(id)
      case None => next
    }
    
  private def next(using Trace): UIO[Item.ID] =
    last.incrementAndGet.flatMap {
      case id if ids.exists(_ == id) => next
      case id => ZIO.succeed(Item.ID(id))
    }
}

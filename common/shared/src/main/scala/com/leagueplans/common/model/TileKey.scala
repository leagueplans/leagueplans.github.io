package com.leagueplans.common.model

object TileKey {
  /** A tile's width and height in pixels */
  val size = 256
  /** The zoom the regions are rendered at */
  val nativeZoom = 2
  /** The furthest zoom out that has tiles */
  val minZoom = -3

  /** The tile at the native zoom that shows exactly the given region */
  def ofRegion(regionX: Int, regionY: Int, plane: Int): TileKey =
    TileKey(plane, nativeZoom, regionX, -(regionY + 1))
}

/** A map tile, addressed the way Leaflet asks for it.
  *
  * The app's map uses Leaflet's simple coordinate system with one map unit per game tile, and
  * names its tile files in Leaflet's own scheme, so that it needs no URL logic of its own. At
  * [[TileKey.nativeZoom]] one tile is one 64 × 64 game region at 4 px per game tile. Each zoom
  * below halves the scale. Leaflet's y grows southwards while the game's grows northwards, so a
  * tile's y is negative for real game coordinates.
  *
  * Tile addresses depend only on game coordinates, so a new render replaces images in place and
  * never moves anything a plan has stored.
  */
final case class TileKey(plane: Int, zoom: Int, x: Int, y: Int) {
  /** Where the tile is published, relative to the output directory */
  def path: String =
    s"$plane/$zoom/${x}_$y.png"

  /** The tile at the next zoom out that covers this one */
  def parent: TileKey =
    TileKey(plane, zoom - 1, Math.floorDiv(x, 2), Math.floorDiv(y, 2))

  /** Where this tile is drawn within its parent, in pixels from the parent's top left */
  def offsetInParent: (Int, Int) = {
    val half = TileKey.size / 2
    ((x - 2 * parent.x) * half, (y - 2 * parent.y) * half)
  }
}

package com.leagueplans.maprender

import java.awt.image.BufferedImage

object Pixels {
  /** Gives the ground its alpha.
    *
    * RuneLite's map dumper writes ground colours as plain RGB, while objects and icons include
    * full alpha. On the transparent image the dumper draws onto, the ground would be invisible.
    * Pixels nothing was drawn on are exactly 0, so they stay transparent, which the sparse upper
    * planes rely on.
    */
  def fixAlpha(image: BufferedImage): BufferedImage = {
    val pixels = argb(image)
    var i = 0
    while (i < pixels.length) {
      val p = pixels(i)
      if ((p >>> 24) == 0 && (p & 0xFFFFFF) != 0) pixels(i) = p | 0xFF000000
      i += 1
    }
    image.setRGB(0, 0, image.getWidth, image.getHeight, pixels, 0, image.getWidth)
    image
  }

  /** Whether every pixel is fully transparent */
  def isEmpty(image: BufferedImage): Boolean =
    argb(image).forall(p => (p >>> 24) == 0)

  private def argb(image: BufferedImage): Array[Int] =
    image.getRGB(0, 0, image.getWidth, image.getHeight, null, 0, image.getWidth)
}

package com.leagueplans.maprender

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.awt.image.BufferedImage

final class PixelsTest extends AnyFreeSpec with Matchers {
  private def image(pixels: Int*): BufferedImage = {
    val result = BufferedImage(pixels.size, 1, BufferedImage.TYPE_INT_ARGB)
    result.setRGB(0, 0, pixels.size, 1, pixels.toArray, 0, pixels.size)
    result
  }

  private def pixels(image: BufferedImage): List[Int] =
    image.getRGB(0, 0, image.getWidth, 1, null, 0, image.getWidth).toList

  "Pixels" - {
    "fixAlpha" - {
      "makes a colour with no alpha opaque" in {
        pixels(Pixels.fixAlpha(image(0x00336699))) shouldBe List(0xFF336699)
      }

      "leaves pixels nothing was drawn on transparent" in {
        pixels(Pixels.fixAlpha(image(0))) shouldBe List(0)
      }

      "leaves colours that already have alpha alone" in {
        pixels(Pixels.fixAlpha(image(0xFF112233, 0x80112233))) shouldBe List(0xFF112233, 0x80112233)
      }
    }

    "isEmpty" - {
      "is true when every pixel is transparent" in {
        Pixels.isEmpty(image(0, 0x00FFFFFF)) shouldBe true
      }

      "is false when any pixel has alpha" in {
        Pixels.isEmpty(image(0, 0x01000000)) shouldBe false
      }
    }
  }
}

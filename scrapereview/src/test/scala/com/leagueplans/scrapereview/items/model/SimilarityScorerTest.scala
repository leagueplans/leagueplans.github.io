package com.leagueplans.scrapereview.items.model

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class SimilarityScorerTest extends AnyFreeSpec with Matchers {
  private def item(name: String, examine: String): SimilarityScorer.Text =
    SimilarityScorer.Text(name, examine)

  "SimilarityScorer" - {
    "score" - {
      "is 1 for identical items" in {
        SimilarityScorer.score(
          item("Dragon scimitar", "A vicious looking sword."),
          item("Dragon scimitar", "A vicious looking sword.")
        ) shouldBe 1d
      }

      "is 1 for two items with nothing to compare" in {
        SimilarityScorer.score(item("", ""), item("", "")) shouldBe 1d
      }

      "ignores case" in {
        SimilarityScorer.score(
          item("Dragon scimitar", "A vicious looking sword."),
          item("DRAGON SCIMITAR", "a vicious looking sword.")
        ) shouldBe 1d
      }

      "is near 0 for items with nothing in common" in {
        SimilarityScorer.score(
          item("Bucket", "It's a wooden bucket."),
          item("Zamorakian hasta", "A powerful weapon.")
        ) should be < 0.3
      }

      "ranks a renamed item above an unrelated one" in {
        val removed = item("Trailblazer axe", "A woodcutting axe.")

        val renamed =
          SimilarityScorer.score(removed, item("Trailblazer axe (charged)", "A woodcutting axe."))
        val unrelated =
          SimilarityScorer.score(removed, item("Bucket of sand", "It's full of sand."))

        renamed should be > unrelated
      }

      // Names carry most of the weight, so a shared examine cannot pull two plainly
      // different items together.
      "weighs the name above the examine" in {
        val removed = item("Dragon scimitar", "A vicious looking sword.")

        val sameName =
          SimilarityScorer.score(removed, item("Dragon scimitar", "Something else entirely."))
        val sameExamine =
          SimilarityScorer.score(removed, item("Something else entirely", "A vicious looking sword."))

        sameName should be > sameExamine
      }

      "is symmetric" in {
        val left = item("Rune platebody", "Provides excellent protection.")
        val right = item("Rune platelegs", "Provides excellent protection.")

        SimilarityScorer.score(left, right) shouldBe SimilarityScorer.score(right, left)
      }

      "stays within 0 and 1" in {
        val pairs = List(
          (item("", ""), item("Bucket", "It's a bucket.")),
          (item("a", "b"), item("aaaaaaaaaa", "bbbbbbbbbb")),
          (item("Bucket", "It's a bucket."), item("Bucket", "It's a bucket."))
        )

        pairs.foreach((left, right) =>
          withClue(s"${left.name} vs ${right.name}") {
            val score = SimilarityScorer.score(left, right)
            score should be >= 0d
            score should be <= 1d
          }
        )
      }
    }

    // The page skips the full score whenever this falls below the threshold, so it must
    // never be lower than the score it stands in for, or a real match would be hidden.
    "upperBound" - {
      "is never below the score" in {
        val names = List("", "a", "Bucket", "Bucket of sand", "DRAGON SCIMITAR", "Dragon scimitar (or)", "İstanbul rope")
        val examines = List("", "It's a bucket.", "A vicious looking sword.", "Short.")
        val items = for (n <- names; e <- examines) yield item(n, e)

        for (left <- items; right <- items)
          withClue(s"${left.name} / ${left.examine} vs ${right.name} / ${right.examine}")(
            SimilarityScorer.upperBound(left, right) should be >= SimilarityScorer.score(left, right)
          )
      }

      "agrees with the score for every pair it keeps, and keeps every pair that qualifies" in {
        val names = List("", "a", "Bucket", "Bucket of sand", "Bucket (empty)", "DRAGON SCIMITAR", "Dragon scimitar (or)")
        val examines = List("", "It's a bucket.", "It's an empty bucket.", "A vicious looking sword.")
        val items = for (n <- names; e <- examines) yield item(n, e)

        for (left <- items; right <- items; threshold <- List(0d, 0.5, 0.7, 0.9, 1d)) {
          val score = SimilarityScorer.score(left, right)
          withClue(s"${left.name} / ${left.examine} vs ${right.name} / ${right.examine} at $threshold")(
            SimilarityScorer.scoreIfAtLeast(left, right, threshold) shouldBe Option.when(score >= threshold)(score)
          )
        }
      }

      "is exact for identical items" in {
        val bucket = item("Bucket", "It's a bucket.")
        SimilarityScorer.upperBound(bucket, bucket) shouldBe 1d
      }
    }
  }
}

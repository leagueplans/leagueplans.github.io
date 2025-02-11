package com.leagueplans.ui.model.player.diary

import io.circe.{Decoder, Encoder}

object DiaryTier {
  private val nameToTier: Map[String, DiaryTier] =
    values.map(s => s.toString -> s).toMap

  given Encoder[DiaryTier] = Encoder[String].contramap(_.toString)
  given Decoder[DiaryTier] = Decoder[String].emap(s =>
    nameToTier
      .get(s)
      .toRight(left = s"Unknown diary tier: [$s]")
  )

  given Ordering[DiaryTier] = Ordering.by(_.ordinal)
}

enum DiaryTier {
  case Easy, Medium, Hard, Elite
}

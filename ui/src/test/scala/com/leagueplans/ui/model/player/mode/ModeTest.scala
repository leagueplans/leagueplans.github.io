package com.leagueplans.ui.model.player.mode

import com.leagueplans.codec.CharacterEncodings as CE
import com.leagueplans.codec.CharacterEncodings.*
import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.{Decoder, DecodingFailure}
import org.scalatest.Assertion

final class ModeTest extends CodecSpec {
  "Mode" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(mode: Mode, expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation(mode, Decoder.decodeLen, expectedEncoding)

      "MainGame" in test(
        MainGame,
        Array(`m`, CE.`a`, `i`, `n`, `-`, `g`, CE.`a`, `m`, `e`)
      )

      "LeaguesI" in test(
        LeaguesI,
        Array(`l`, `e`, CE.`a`, `g`, `u`, `e`, `s`, `-`, `1`)
      )

      "LeaguesII" in test(
        LeaguesII,
        Array(`l`, `e`, CE.`a`, `g`, `u`, `e`, `s`, `-`, `2`)
      )

      "LeaguesIII" in test(
        LeaguesIII,
        Array(`l`, `e`, CE.`a`, `g`, `u`, `e`, `s`, `-`, `3`)
      )

      "LeaguesIV" in test(
        LeaguesIV,
        Array(`l`, `e`, CE.`a`, `g`, `u`, `e`, `s`, `-`, `4`)
      )

      "LeaguesV" in test(
        LeaguesV,
        Array(`l`, `e`, CE.`a`, `g`, `u`, `e`, `s`, `-`, `5`)
      )

      "GridMaster" in test(
        GridMaster,
        Array(`g`, `r`, `i`, `d`, `-`, `m`, CE.`a`, `s`, `t`, `e`, `r`)
      )
      
      "Armageddon" in test(
        Armageddon,
        Array(`d`, `e`, CE.`a`, `d`, `m`, CE.`a`, `n`, `-`, CE.`a`, `r`, `m`, CE.`a`, `g`, `e`, `d`, `d`, `o`, `n`)
      )
    }

    "decoding should fail for an unexpected encoding" in (
      Decoder.decodeLen[Mode](
        Array(`l`, `e`, CE.`a`, `g`, `u`, `e`, `s`, `-`, `6`)
      ).left.value shouldBe a[DecodingFailure]
    )
    
    "League" - {
      "encoding values to and decoding values from an expected encoding" - {
        def test(league: Mode.League, expectedEncoding: Array[Byte]): Assertion =
          testRoundTripSerialisation(league, Decoder.decodeLen, expectedEncoding)

        "LeaguesI" in test(
          LeaguesI,
          Array(`l`, `e`, CE.`a`, `g`, `u`, `e`, `s`, `-`, `1`)
        )

        "LeaguesII" in test(
          LeaguesII,
          Array(`l`, `e`, CE.`a`, `g`, `u`, `e`, `s`, `-`, `2`)
        )

        "LeaguesIII" in test(
          LeaguesIII,
          Array(`l`, `e`, CE.`a`, `g`, `u`, `e`, `s`, `-`, `3`)
        )

        "LeaguesIV" in test(
          LeaguesIV,
          Array(`l`, `e`, CE.`a`, `g`, `u`, `e`, `s`, `-`, `4`)
        )

        "LeaguesV" in test(
          LeaguesV,
          Array(`l`, `e`, CE.`a`, `g`, `u`, `e`, `s`, `-`, `5`)
        )
      }

      "decoding should fail for an unexpected encoding" in(
        Decoder.decodeLen[Mode.League](
          Array(`m`, CE.`a`, `i`, `n`, `-`, `g`, CE.`a`, `m`, `e`)
        ).left.value shouldBe a[DecodingFailure]
      )
    }
  }
}

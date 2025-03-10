package com.leagueplans.scraper.wiki.http.response

import com.leagueplans.common.utils.circe.JsonObjectOps.*
import com.leagueplans.scraper.wiki.model.{Page, PageDescriptor}
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{CursorOp, Decoder, HCursor, JsonObject}
import zio.Chunk
import zio.http.QueryParams

private[http] enum WikiResponse {
  case Failure(error: Error)

  case Success(
    continueParams: Option[QueryParams],
    warnings: Map[String, List[String]],
    pages: List[Page[JsonObject]]
  )
}

private[http] object WikiResponse {
  given Decoder[WikiResponse] =
    Decoder[Failure].map(f => f: WikiResponse)
      .or(Decoder[Success].map(s => s: WikiResponse))

  object Failure {
    given Decoder[Failure] = deriveDecoder[Failure]
  }

  object Success {
    given Decoder[Success] = {
      given Decoder[Page[JsonObject]] =
        (c: HCursor) =>
          Decoder[JsonObject]
            .apply(c)
            .flatMap(json =>
              for {
                id <- json.decodeField[PageDescriptor.ID]("pageid", c.history)
                title <- json.decodeField[PageDescriptor.Name]("title", c.history)
              } yield (PageDescriptor(id, title), json.remove("pageid").remove("title"))
            )

      (c: HCursor) =>
        Decoder[JsonObject]
          .apply(c)
          .flatMap(json =>
            for {
              continueParams <- decodeContinueParams(json)
              warnings <- decodeWarnings(json, c.history)
              pages <- json.decodeNestedField[List[Page[JsonObject]]]("query", "pages")(c.history)
            } yield Success(continueParams, warnings, pages)
          )
    }

    private def decodeContinueParams(json: JsonObject): Decoder.Result[Option[QueryParams]] =
      json
        .decodeOptField[JsonObject]("continue")
        .flatMap {
          case Some(paramsJson) =>
            val (decodingFailures, params) =
              paramsJson.toList.partitionMap((key, value) =>
                value.as[String].map(v => key -> Chunk(v))
              )

            decodingFailures match {
              case Nil =>
                Right(Some(QueryParams(params.toMap)))
              case h :: _ =>
                Left(h)
            }

          case None =>
            Right(None)
        }

    /* This is ridiculous. Yes, the format really does look like this:
     * "warnings": {
     *   "key1": {
     *     "warnings": "<warning1>\n<warning2>\n...<warningn>"
     *   },
     *   "key2": { ... }
     * }
     *
     * It could have been
     * "warnings": {
     *   "key1": [ "<warning1>", "<warning2>", ..., "warningn" ],
     *   "key2": [ ... ]
     * }
     *
     * but then people might have actually used this API
     */
    private def decodeWarnings(
      json: JsonObject,
      ops: => List[CursorOp]
    ): Decoder.Result[Map[String, List[String]]] =
      json
        .decodeOptField[JsonObject]("warnings")
        .flatMap {
          case Some(warningsJson) =>
            val (decodingFailures, warnings) =
              warningsJson.toList.partitionMap((key, warningContentJson) =>
                warningContentJson
                  .as[JsonObject]
                  .flatMap(warningContentObj =>
                    warningContentObj.decodeField[String](
                      "warnings",
                      ops :+ CursorOp.Field("warnings") :+ CursorOp.Field(key)
                    )
                  )
                  .map(allWarnings => key -> allWarnings.split("\n").toList)
              )

            decodingFailures match {
              case Nil => Right(warnings.toMap)
              case h :: _ => Left(h)
            }

          case None =>
            Right(Map.empty)
        }
  }
}

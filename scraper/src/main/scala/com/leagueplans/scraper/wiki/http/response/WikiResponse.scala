package com.leagueplans.scraper.wiki.http.response

import com.leagueplans.common.utils.circe.JsonObjectOps.*
import com.leagueplans.scraper.wiki.model.{Page, PageDescriptor}
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{CursorOp, Decoder, HCursor, JsonObject}
import zio.Chunk
import zio.http.QueryParams

/** A response to a query, with its pages of type `P`.
  *
  * Every query shares the same envelope - errors, continuation, warnings, and how the wiki
  * resolved the titles it was asked about - but what each page holds depends on what was
  * asked for, so the pages are decoded by whichever decoder suits the query.
  */
private[http] enum WikiResponse[+P] {
  case Failure(error: Error) extends WikiResponse[Nothing]

  /** @param normalised the titles the wiki respelled, from the spelling asked for to its own
    * @param redirects the titles that redirect elsewhere, to the title they redirect to
    */
  case Success(
    continueParams: Option[QueryParams],
    warnings: Map[String, List[String]],
    normalised: Map[String, String],
    redirects: Map[String, String],
    pages: List[P]
  )
}

private[http] object WikiResponse {
  def decoder[P : Decoder]: Decoder[WikiResponse[P]] =
    Decoder[Failure].map(f => f: WikiResponse[P])
      .or(Success.decoder[P].map(s => s: WikiResponse[P]))

  /** A page that exists, with its content left to be interpreted by whoever asked for it. The
    * queries that use it only ever describe pages that exist, so a page without an ID is an
    * error.
    */
  val existingPage: Decoder[Page[JsonObject]] =
    (c: HCursor) =>
      Decoder[JsonObject]
        .apply(c)
        .flatMap(json =>
          for {
            id <- json.decodeField[PageDescriptor.ID]("pageid", c.history)
            title <- json.decodeField[PageDescriptor.Name]("title", c.history)
          } yield (PageDescriptor(id, title), json.remove("pageid").remove("title"))
        )

  object Failure {
    given Decoder[Failure] = deriveDecoder[Failure]
  }

  object Success {
    extension (self: Success[?]) {
      /** The title the wiki answered under, for a title that was asked about. */
      def resolve(title: String): String = {
        val normalised = self.normalised.getOrElse(title, title)
        self.redirects.getOrElse(normalised, normalised)
      }
    }

    private final case class TitleMapping(from: String, to: String)
    private given Decoder[TitleMapping] = deriveDecoder

    def decoder[P : Decoder]: Decoder[Success[P]] =
      (c: HCursor) =>
        Decoder[JsonObject]
          .apply(c)
          .flatMap(json =>
            for {
              continueParams <- decodeContinueParams(json)
              warnings <- decodeWarnings(json, c.history)
              query <- json.decodeField[JsonObject]("query", c.history)
              normalised <- decodeTitleMappings(query, "normalized")
              redirects <- decodeTitleMappings(query, "redirects")
              pages <- query.decodeField[List[P]]("pages", c.history :+ CursorOp.Field("query"))
            } yield Success(continueParams, warnings, normalised, redirects, pages)
          )

    private def decodeTitleMappings(query: JsonObject, key: String): Decoder.Result[Map[String, String]] =
      query
        .decodeOptField[List[TitleMapping]](key)
        .map(_.toList.flatten.map(mapping => mapping.from -> mapping.to).toMap)

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

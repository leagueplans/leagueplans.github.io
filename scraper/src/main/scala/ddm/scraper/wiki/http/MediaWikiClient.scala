package ddm.scraper.wiki.http

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.`User-Agent`
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.stream.scaladsl.{Flow, Source}
import cats.data.NonEmptyList
import ddm.common.utils.circe.JsonObjectOps.*
import ddm.scraper.http.ThrottledHttpClient
import ddm.scraper.wiki.http.response.MediaWikiResponse
import ddm.scraper.wiki.model.Page
import io.circe.Decoder.Result
import io.circe.{CursorOp, Decoder, JsonObject, parser}
import org.log4s.getLogger

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

final class MediaWikiClient(
  httpClient: ThrottledHttpClient,
  userAgent: `User-Agent`,
  baseURL: String
)(using ec: ExecutionContext) {
  private val apiUrl = Uri(s"$baseURL/api.php")
  private val logger = getLogger

  def fetch(
    selector: MediaWikiSelector,
    maybeContent: Option[MediaWikiContent]
  ): Source[(Page, Decoder.Result[String]), ?] =
    toParams(selector)
      .map(_ ++ maybeContent.fold(Map.empty[String, String])(toParams))
      .flatMapConcat(params => fetch(params))
      .via(maybeContent.fold[DecodingFlow](noContentDecodingFlow)(decodingFlow))

  def fetchImage(fileName: Page.Name.File): Future[Array[Byte]] = {
    val encoded = URLEncoder.encode(
      s"${fileName.raw}.${fileName.extension}".replace(' ', '_'),
      StandardCharsets.UTF_8
    )

    httpClient
      .queue(buildRequest(s"$baseURL/images/$encoded"))
      .flatMap {
        case (status, data) if status.isSuccess() => Future.successful(data)
        case _ => fetchImageUsingRest(encoded)
      }
  }

  def fetchAllMembers(
    category: Page.Name.Category,
    maybeContent: Option[MediaWikiContent]
  ): Source[(Page, Decoder.Result[String]), ?] =
    fetch(MediaWikiSelector.Members(category), maybeContent).flatMapConcat {
      case (Page(_, subCategory: Page.Name.Category), _) =>
        fetchAllMembers(subCategory, maybeContent)
      case (page, data) =>
        Source.single((page, data))
    }

  private def toParams(selector: MediaWikiSelector): Source[Map[String, String], ?] =
    selector match {
      case MediaWikiSelector.Members(category) =>
        Source.single(Map(
          "generator" -> "categorymembers",
          "gcmlimit" -> "50",
          "gcmtitle" -> category.wikiName,
          "gcmnamespace" -> "0|6|14"
        ))

      case MediaWikiSelector.Pages(names) =>
        Source(names.sliding(size = 50, step = 50).toList)
          .map(names => Map("titles" -> names.map(_.wikiName).mkString("|")))

      case MediaWikiSelector.PagesThatTransclude(template) =>
        Source.single(Map(
          "generator" -> "embeddedin",
          "geilimit" -> "50",
          "geititle" -> template.wikiName,
          "geinamespace" -> "0"
        ))
    }

  private def toParams(content: MediaWikiContent): Map[String, String] =
    (content match {
      case MediaWikiContent.Revisions => Map("rvslots" -> "main", "rvprop" -> "content")
    }) + ("prop" -> toProp(content))

  private def toProp(content: MediaWikiContent): String =
    content match {
      case MediaWikiContent.Revisions => "revisions"
    }

  private def fetch(params: Map[String, String]): Source[(Page, JsonObject), ?] =
    Source
      .unfoldAsync(Option(params)) {
        case None =>
          Future.successful(None)
        case Some(params) =>
          execute(buildQuery(params))
            .flatMap(data => Future.fromTry(decodeResponse(data)))
            .map(response => Some((
              response.continueParams.map(params ++ _),
              response.pages
            )))
      }
      .mapConcat(identity)

  private def execute(request: HttpRequest): Future[Array[Byte]] =
    httpClient
      .queue(request)
      .flatMap {
        case (statusCode, data) if statusCode.isSuccess() =>
          Future.successful(data)
        case (statusCode, body) =>
          Future.failed(RuntimeException(s"Bad response [${statusCode.value}], body: [${String(body)}]"))
      }

  private def decodeResponse(response: Array[Byte]): Try[MediaWikiResponse.Success] =
    parser
      .decode[MediaWikiResponse](String(response))
      .flatMap {
        case f: MediaWikiResponse.Failure =>
          Left(RuntimeException(s"Error response from wiki: [${f.error.code}] - [${f.error.info}]"))
        case s: MediaWikiResponse.Success =>
          Right(s)
      }
      .toTry

  private def buildQuery(params: Map[String, String]): HttpRequest =
    buildRequest(
      apiUrl.withQuery(Query(
        params +
          ("action" -> "query") +
          ("format" -> "json") +
          ("formatversion" -> "2")
      ))
    )

  private def buildRequest(uri: Uri): HttpRequest =
    HttpRequest(uri = uri, headers = List(userAgent))

  private type DecodingFlow = Flow[(Page, JsonObject), (Page, Decoder.Result[String]), ?]

  private val noContentDecodingFlow: DecodingFlow =
    Flow[(Page, JsonObject)].map((page, _) => (page, Right("")))

  private def decodingFlow(content: MediaWikiContent): DecodingFlow =
    Flow[(Page, JsonObject)].collect(Function.unlift { (page, rawData) =>
      val prop = toProp(content)

      rawData.decodeOptField[NonEmptyList[JsonObject]](prop) match {
        case Right(Some(data)) =>
          Some((page, decode(content, data.head, CursorOp.Field(prop))))

        case Left(error) =>
          Some((page, Left(error)))

        case Right(None) =>
          logger.debug(s"Ignoring empty result for page [${page.name}], id = [${page.id}]")
          None
      }
    })

  private def decode(content: MediaWikiContent, json: JsonObject, cursorOp: CursorOp): Result[String] =
    content match {
      case MediaWikiContent.Revisions =>
        json.decodeNestedField[String]("slots", "main", "content")(List(cursorOp))
    }

  def fetchImageUsingRest(encodedFileName: String): Future[Array[Byte]] =
    for {
      metadata <- execute(buildRequest(s"$baseURL/rest.php/v1/file/$encodedFileName"))
      json <- Future.fromTry(parser.decode[JsonObject](String(metadata)).toTry)
      actualURL <- Future.fromTry(json.decodeNestedField[String]("preferred", "url")(List.empty).toTry)
      image <- execute(buildRequest(actualURL))
    } yield image
}

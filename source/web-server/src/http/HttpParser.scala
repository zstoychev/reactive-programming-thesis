package http

import java.nio.ByteBuffer

import functional.Monad
import http.Http.Headers._
import http.Http._
import parser.{LineParser, ParseException, Parser}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

// HTTP парсър с основна валидация
trait HttpParser[FL, M <: HttpMessage] extends Parser[ByteBuffer, Try[M]] {
  protected case class Data(initialLine: Option[FL], headersStrings: List[String], headers: Map[String, String],
                            bodyBuffer: Array[Byte], bodyPosition: Int, body: Option[HttpBody])

  private val EmptyData = Data(None, Nil, Map.empty, Array.empty, 0, None)
  private val HeaderKeyValueSplit = ": ".r

  private val lineParser = new LineParser

  protected def parseInitialLine(line: String): Option[FL]
  protected def build(data: Data): M

  private def finish(data: Data): Option[Try[M]] = {
    become(readInitialLine(EmptyData) _)
    Some(Success(build(data)))
  }

  private def parseHeader(header: String) : Option[(String, String)] =
    HeaderKeyValueSplit.pattern.split(header, 2).toList match {
      case List(key, value) => Some(key, value)
      case _ => None
    }

  private def buildHeaders(headersStrings: List[String]) =
    Monad.sequence(headersStrings.map(parseHeader)) map { _.toMap }

  private def readInitialLine(data: Data)(input: ByteBuffer): Option[Try[M]] =
    lineParser.receive(input) flatMap { line => parseInitialLine(line) match {
        case Some(initialLine) => becomeAndReceive(input)(readHeaders(data.copy(initialLine = Some(initialLine))) _)
        case None => Some(Failure(new ParseException("Invalid initial line")))
      }
    }

  private def readHeaders(data: Data)(input: ByteBuffer): Option[Try[M]] = lineParser.receive(input) match {
    case Some("") => buildHeaders(data.headersStrings) match {
      case Some(headers) =>
        if (headers.contains(ContentLengthHeader)) {
          val bodyBuffer = new Array[Byte](headers(ContentLengthHeader).toInt)
          becomeAndReceive(input)(readBody(data.copy(headers = headers, bodyBuffer = bodyBuffer, bodyPosition = 0)) _)
        }
        else finish(data.copy(headers = headers, body = Some(HttpBody.Empty)))
      case None => Some(Failure(new ParseException("Invalid headers")))
    }
    case Some(header) =>
      becomeAndReceive(input)(readHeaders(data.copy(headersStrings = header :: data.headersStrings)) _)
    case None => None
  }

  private def readBody(data: Data)(input: ByteBuffer): Option[Try[M]] = {
    val length = math.min(data.bodyBuffer.length - data.bodyPosition, input.remaining)
    input.get(data.bodyBuffer, data.bodyPosition, length)
    val newBodyPosition = data.bodyPosition + length

    if (newBodyPosition == data.bodyBuffer.length)
      finish(data.copy(body = Some(HttpBody.Custom(data.headers.get(ContentTypeHeader), data.bodyBuffer))))
    else None
  }

  protected def process(input: ByteBuffer): Option[Try[M]] = becomeAndReceive(input)(readInitialLine(EmptyData) _)
}

class HttpRequestParser extends HttpParser[(String, String), HttpRequest] {
  private val RequestLineFormat = "^(\\S+) (\\S+) HTTP/1.1$".r

  protected def parseInitialLine(requestLine: String): Option[(String, String)] = requestLine match {
    case RequestLineFormat(method, path) => Some((method, path))
    case _ => None
  }

  protected def build(data: Data): HttpRequest = {
    val (method, path) = data.initialLine.get
    HttpRequest(method, path, data.headers, data.body.get)
  }
}

class HttpResponseParser extends HttpParser[String, HttpResponse] {
  private val StatusLineFormat = "^HTTP/1.1 (\\d* .*)".r

  protected def parseInitialLine(statusLine: String): Option[String] = statusLine match {
    case StatusLineFormat(statusCode) => Some(statusCode)
    case _ => None
  }

  protected def build(data: Data): HttpResponse = HttpResponse(data.initialLine.get, data.headers, data.body.get)
}

package http

import functional.json.{JsonSerializable, ToJson}

object Http {
  type HttpHeaders = Map[String, String]

  sealed trait HttpMessage {
    def headers: HttpHeaders
    def body: HttpBody

    def initialLine: String

    val toBytes: Array[Byte] = {
      val headersText = (body.toHeaders ++ headers).map { case(key, value) => s"$key: $value" } mkString "\r\n"

      Array.concat(s"$initialLine\r\n$headersText\r\n\r\n".getBytes, body.data)
    }
  }
  case class HttpRequest(method: String, path: String, headers: HttpHeaders, body: HttpBody) extends HttpMessage {
    def initialLine = s"$method $path HTTP/1.1"
  }

  case class HttpResponse(statusCode: String, headers: HttpHeaders, body: HttpBody) extends HttpMessage {
    def initialLine = s"HTTP/1.1 $statusCode"
  }

  object HttpMethods {
    val GET = "GET"
    val POST = "POST"
    val PUT = "PUT"
    val PATCH = "PATCH"
    val DELETE = "DELETE"
    val HEAD = "HEAD"
    val OPTIONS = "OPTIONS"
  }

  object Headers {
    val ContentLengthHeader = "Content-Length"
    val ContentTypeHeader = "Content-Type"
    val Host = "Host"
  }

  object StatusCodes {
    val ok = "200 OK"
    val notFound = "404 NotFound"
    val serviceUnavailable = "503 Service Unavailable"
  }

  trait HttpBody {
    import Headers._

    def contentType: Option[String]
    def data: Array[Byte]

    def toHeaders: HttpHeaders = contentType.map(ContentTypeHeader -> _).toMap ++
      Map(ContentLengthHeader -> data.size.toString)
  }

  object HttpBody {
    case class Text(value: String) extends HttpBody {
      val contentType = Some("text/plain; charset=UTF-8")
      lazy val data = value.getBytes("UTF-8")
    }

    case class Json[T](value: T)(implicit js: JsonSerializable[T]) extends HttpBody {
      def contentType = Some("application/json")
      lazy val data = value.toJsonString.getBytes("UTF-8")
    }

    case object Empty extends HttpBody {
      def contentType = None
      val data = Array.empty[Byte]
    }

    case class Custom(contentType: Option[String], data: Array[Byte]) extends HttpBody
  }
}

package reactivethesis.util

import play.api.libs.json._
import play.api.mvc.WebSocket.FrameFormatter

object Formatters {
  implicit val tuple2Format = new Format[(String, String)] {
    def reads(json: JsValue): JsResult[(String, String)] = json match {
      case JsArray(Seq(a: JsString, b: JsString)) => JsSuccess((a.value, b.value))
      case _ => JsError()
    }
    def writes(o: (String, String)): JsValue = JsArray(Seq(JsString(o._1), JsString(o._2)))
  }
  implicit val tuple2FrameFormatter = FrameFormatter.jsonFrame[(String, String)]
}

package functional

package object json {
  def transformJsonToString(value: JsonValue): String = value match {
    case JsonNumber(value) => value.toString
    case JsonString(value) => value
    case JsonBoolean(value) => value.toString
    case JsonArray(elements) => "[" + elements.map(transformJsonToString).mkString(", ") + "]"
    case JsonObject(members) =>
      val membersStrings = members.map { case (key, value) =>
        s"""\"${key}\": ${transformJsonToString(value)}"""
      }
      "{" + membersStrings.mkString(", ") + "}"
    case JsonNull => "null"
  }

  def serializeToJson[T](value: T)(implicit js: JsonSerializable[T]): String = transformJsonToString(js.toJson(value))

  implicit class ToJson[T](val value: T) extends AnyVal {
    def toJson(implicit js: JsonSerializable[T]) = js.toJson(value)
    def toJsonString(implicit js: JsonSerializable[T]) = serializeToJson(value)
  }
}

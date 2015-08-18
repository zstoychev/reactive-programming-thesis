package functional.json

sealed trait JsonValue
case class JsonNumber(value: BigDecimal) extends JsonValue
case class JsonString(value: String) extends JsonValue
case class JsonBoolean(value: Boolean) extends JsonValue
case class JsonArray(value: Seq[JsonValue]) extends JsonValue
case class JsonObject(value: Map[String, JsonValue]) extends JsonValue
case object JsonNull extends JsonValue

trait JsonSerializable[T] {
  def toJson(value: T): JsonValue
}

object JsonSerializable {
  implicit val booleanJsonSerializable = new JsonSerializable[Boolean] {
    def toJson(value: Boolean) = JsonBoolean(value)
  }
  implicit val intJsonSerializable = new JsonSerializable[Int] { def toJson(value: Int) = JsonNumber(value) }
  implicit val longJsonSerializable = new JsonSerializable[Long] { def toJson(value: Long) = JsonNumber(value) }
  implicit val doubleJsonSerializable = new JsonSerializable[Double] { def toJson(value: Double) = JsonNumber(value) }

  implicit val stringJsonSerializable = new JsonSerializable[String] { def toJson(value: String) = JsonString(value) }

  implicit def seqJsonSerializable[T](implicit js: JsonSerializable[T]) = new JsonSerializable[List[T]] {
    def toJson(xs: List[T]) = JsonArray(xs map js.toJson)
  }

  implicit def optionJsonSerializable[T](implicit js: JsonSerializable[T]) = new JsonSerializable[Option[T]] {
    def toJson(o: Option[T]) = o match {
      case Some(value) => js.toJson(value)
      case None => JsonNull
    }
  }
}

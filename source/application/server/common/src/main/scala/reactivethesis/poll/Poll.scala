package reactivethesis.poll

import play.api.libs.json.Json
import play.api.mvc.WebSocket.FrameFormatter

case class PollAnswer(id: Int, name: String, optionsAnswers: List[Boolean])
case class Poll(id: String, options: List[String], answers: List[PollAnswer])

object PollAnswer {
  implicit val pollAnswerFormat = Json.format[PollAnswer]
  implicit val pollAnswerFrameFormat = FrameFormatter.jsonFrame[PollAnswer]
}

object Poll {
  implicit val pollFormat = Json.format[Poll]
  implicit val pollFrameFormat = FrameFormatter.jsonFrame[Poll]
}
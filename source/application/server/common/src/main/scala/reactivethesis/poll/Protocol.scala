package reactivethesis.poll

import play.api.libs.json.Json

object Protocol {
  sealed trait PollCommand
  case class StartPoll(options: List[String]) extends PollCommand
  case class AnswerPoll(name: String, optionsAnswers: List[Boolean]) extends PollCommand
  case class UpdatePollAnswer(id: Int, optionsAnswers: List[Boolean]) extends PollCommand
  case class RemovePollAnswer(id: Int) extends PollCommand

  case class StartPollAck(id: String)
  case class AnswerPollAck(id: Int)
  case object UpdatePollAnswerAck
  case object RemovePollAnswerAck

  sealed trait PollChatCommand
  case object InitializePollChat extends PollChatCommand
  case class PostChatMessage(name: String, message: String) extends PollChatCommand

  case object InitializePollChatAck
  case object PostChatMessageAck

  case object InvalidState

  implicit val startPollFormat = Json.format[StartPoll]
  implicit val answerPollFormat = Json.format[AnswerPoll]
  implicit val updatePollAnswerFormat = Json.format[UpdatePollAnswer]
  implicit val removePollAnswerFormat = Json.format[RemovePollAnswer]
}

package reactivethesis.poll

case class PollState(poll: Poll, totalAnswers: Int = 0)

object PollState {
  sealed trait PollEvent
  case class PollStarted(id: String, options: List[String]) extends PollEvent
  case class PollAnswered(id: Int, name: String, optionsAnswers: List[Boolean]) extends PollEvent
  case class PollAnswerUpdated(id: Int, optionsAnswers: List[Boolean]) extends PollEvent
  case class PollAnswerRemoved(id: Int) extends PollEvent

  def updateState(state: Option[PollState], event: PollEvent) = (state, event) match {
    case (None, PollStarted(id, options)) => Some(PollState(Poll(id, options, List.empty)))
    case (Some(ps@PollState(poll, totalAnswers)), PollAnswered(id, name, optionsAnswers)) =>
      val answers = PollAnswer(id, name, optionsAnswers) :: poll.answers
      Some(ps.copy(poll = poll.copy(answers = answers), totalAnswers = totalAnswers + 1))
    case (Some(ps@PollState(poll, _)), PollAnswerUpdated(id, optionsAnswers)) =>
      poll.answers.find(_.id == id) match {
        case Some(answer) =>
          val answers = PollAnswer(id, answer.name, optionsAnswers) :: poll.answers.filter(_.id != id)
          Some(ps.copy(poll = poll.copy(answers = answers)))
        case None => Some(ps)
      }
    case (Some(ps@PollState(poll, _)), PollAnswerRemoved(id)) =>
      val answers = poll.answers.filter(_.id != id)
      Some(ps.copy(poll = poll.copy(answers = answers)))
    case (_, _) => state
  }
}

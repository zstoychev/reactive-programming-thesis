package reactivethesis.poll.actors

import akka.persistence.PersistentActor
import reactivethesis.poll.actors.PollChatActor.ChatMessagePosted

object TotalChatMessagesView {
  case object GetTotalMessages
  case class TotalMessages(total: Int)
}

class TotalChatMessagesView extends PersistentActor {
  import TotalChatMessagesView._

  def persistenceId: String = "total-chat-messages-view"

  var totalMessages = 0

  def receiveCommand: Receive = {
    case cm: ChatMessagePosted => persist(cm) { _ =>
      totalMessages += 1
    }
    case GetTotalMessages => sender() ! TotalMessages(totalMessages)
  }

  def receiveRecover: Receive = {
    case cm: ChatMessagePosted => totalMessages += 1
  }
}

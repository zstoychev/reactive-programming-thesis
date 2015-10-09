package proactor.reactor

import ProactorProtocol._

trait CompletionDispatcher[CH] {
  def dispatch(message: Message, completionHandler: CH)
}

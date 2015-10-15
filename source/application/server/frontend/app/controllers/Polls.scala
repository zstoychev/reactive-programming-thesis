package controllers

import java.util.UUID
import javax.inject.{Inject, Named}

import actors.{PollChatQueryActor, PollQueryActor}
import akka.actor.{ActorRef, Props}
import akka.pattern.{AskTimeoutException, ask}
import akka.util.Timeout
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.mvc.{Action, Controller, WebSocket}
import reactivethesis.poll.Poll
import reactivethesis.poll.Protocol._
import reactivethesis.poll.actors.PollViewActor.QueryPoll
import reactivethesis.util.Formatters._

import scala.concurrent.Future
import scala.concurrent.duration._

class Polls @Inject() (@Named("polls") polls: ActorRef, @Named("pollsChats") pollsChats: ActorRef,
                       @Named("pollsViews") pollsViews: ActorRef, @Named("pollsChatViews") pollsChatViews: ActorRef)
  extends Controller {
  implicit val timeout = Timeout(4.seconds)

  def handlePollAck(ack: Future[Any]) = {
    ack
      .map {
        case InvalidState => Conflict
        case StartPollAck(id) => Ok(JsString(id))
        case AnswerPollAck(id) => Ok(JsNumber(id))
        case UpdatePollAnswerAck => Ok
        case RemovePollAnswerAck => Ok
      }
      .recover { case _: AskTimeoutException => ServiceUnavailable }
  }

  def startPoll = Action.async(parse.json[StartPoll]) { request =>
    val id = UUID.randomUUID.toString

    Future.sequence(List(
      polls ? (id, request.body),
      pollsChats ? (id, InitializePollChat)
    )) flatMap { case List(pollAck, _) => handlePollAck(Future.successful(pollAck)) }
  }

  def answerPoll(id: String) =  Action.async(parse.json[AnswerPoll]) { request =>
    handlePollAck(polls ? (id, request.body))
  }

  def updatePollAnswer(id: String, answerId: Int) = Action.async(parse.json[UpdatePollAnswer]) { request =>
    handlePollAck(polls ? (id, request.body))
  }

  def removePollAnswer(id: String, answerId: Int) = Action.async(parse.json[RemovePollAnswer]) { request =>
    handlePollAck(polls ? (id, request.body))
  }

  def getPoll(id: String) = Action.async { request =>
    pollsViews ? (id, QueryPoll()) map {
      case Some(poll: Poll) => Ok(Json.toJson(poll))
      case _ => Conflict
    }
  }

  def queryPoll(id: String) = WebSocket.acceptWithActor[String, Poll] { request => out =>
    Props(new PollQueryActor(id, pollsViews, out))
  }

  def queryPollChat(id: String) = WebSocket.acceptWithActor[String, (String, String)] { request => out =>
    Props(new PollChatQueryActor(id, pollsChatViews, out))
  }
}

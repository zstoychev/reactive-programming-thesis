package controllers

import javax.inject.{Named, Inject}

import actors.{ParallelismListenerActor, CodeProcessorActor}
import akka.actor.{ActorRef, Props}
import play.api.Play.current
import play.api.mvc.{Controller, WebSocket}

class CodeProcessor @Inject() (@Named("codeProcessingRouter") codeProcessingRouter: ActorRef) extends Controller {
  def processCode = WebSocket.acceptWithActor[String, String] { request => out =>
    Props(new CodeProcessorActor(codeProcessingRouter, out))
  }

  def parallelism = WebSocket.acceptWithActor[String, String] { request => out =>
    Props(new ParallelismListenerActor(out))
  }
}

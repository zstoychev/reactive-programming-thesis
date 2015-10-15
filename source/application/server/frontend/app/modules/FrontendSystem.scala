package modules

import javax.inject.{Inject, Singleton}

import akka.actor.{Props, ActorSystem}
import akka.cluster.sharding.{ClusterShardingSettings, ClusterSharding}
import akka.routing.FromConfig
import reactivethesis.poll.actors.{PollChatActor, PollChatViewActor, PollActor, PollViewActor}
import reactivethesis.sharding.ShardedEntityWithBackoff

@Singleton
class FrontendSystem @Inject() (system: ActorSystem) {
  val codeProcessingRouter = system.actorOf(FromConfig.props(), "codeProcessingRouter")

  val pollsViews = ClusterSharding(system).startProxy(
    typeName = PollViewActor.shardName,
    role = Some("backend"),
    extractEntityId = PollViewActor.extractEntityId,
    extractShardId = PollViewActor.extractShardId)

  val polls = ClusterSharding(system).startProxy(
    typeName = PollActor.shardName,
    role = Some("backend"),
    extractEntityId = PollActor.extractEntityId,
    extractShardId = PollActor.extractShardId)

  val pollsChatViews = ClusterSharding(system).startProxy(
    typeName = PollChatViewActor.shardName,
    role = Some("backend"),
    extractEntityId = PollChatViewActor.extractEntityId,
    extractShardId = PollChatViewActor.extractShardId)

  val pollsChats = ClusterSharding(system).startProxy(
    typeName = PollChatActor.shardName,
    role = Some("backend"),
    extractEntityId = PollChatActor.extractEntityId,
    extractShardId = PollChatActor.extractShardId)
}

package reactivethesis.backend

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import reactivethesis.poll.actors._
import reactivethesis.sharding.ShardedEntityWithBackoff

class Backend(system: ActorSystem) {
  def start() = {
    val totalChatMessagesView = system.actorOf(ClusterSingletonManager.props(
      singletonProps = Props[TotalChatMessagesView],
      terminationMessage = PoisonPill,
      settings = ClusterSingletonManagerSettings(system).withRole("backend")),
      name = "totalChatMessagesView")

    val pollsViews = ClusterSharding(system).start(
      typeName = PollViewActor.shardName,
      entityProps = Props(new ShardedEntityWithBackoff((id, passivator) =>
        Props(new PollViewActor(id, passivator))
      )),
      settings = ClusterShardingSettings(system),
      extractEntityId = PollViewActor.extractEntityId,
      extractShardId = PollViewActor.extractShardId)

    val polls = ClusterSharding(system).start(
      typeName = PollActor.shardName,
      entityProps = Props(new ShardedEntityWithBackoff((id, passivator) =>
        Props(new PollActor(id, passivator, pollsViews))
      )),
      settings = ClusterShardingSettings(system),
      extractEntityId = PollActor.extractEntityId,
      extractShardId = PollActor.extractShardId)

    val pollsChatViews = ClusterSharding(system).start(
      typeName = PollChatViewActor.shardName,
      entityProps = Props(new ShardedEntityWithBackoff((id, passivator) =>
        Props(new PollChatViewActor(id, passivator, totalChatMessagesView))
      )),
      settings = ClusterShardingSettings(system),
      extractEntityId = PollChatViewActor.extractEntityId,
      extractShardId = PollChatViewActor.extractShardId)

    val pollsChats = ClusterSharding(system).start(
      typeName = PollChatActor.shardName,
      entityProps = Props(new ShardedEntityWithBackoff((id, passivator) =>
        Props(new PollChatActor(id, passivator, pollsChatViews))
      )),
      settings = ClusterShardingSettings(system),
      extractEntityId = PollChatActor.extractEntityId,
      extractShardId = PollChatActor.extractShardId)
  }
}

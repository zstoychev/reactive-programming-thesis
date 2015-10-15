package modules

import javax.inject.Named

import com.google.inject.{AbstractModule, Provides}

class FrontendSystemModule extends AbstractModule {
  def configure(): Unit = {
    bind(classOf[FrontendSystem]).asEagerSingleton
  }

  @Provides
  @Named("codeProcessingRouter")
  def productsStore(frontendSystem: FrontendSystem) = frontendSystem.codeProcessingRouter

  @Provides
  @Named("polls")
  def polls(frontendSystem: FrontendSystem) = frontendSystem.polls

  @Provides
  @Named("pollsViews")
  def pollsViews(frontendSystem: FrontendSystem) = frontendSystem.pollsViews

  @Provides
  @Named("pollsChats")
  def pollsChats(frontendSystem: FrontendSystem) = frontendSystem.pollsChats

  @Provides
  @Named("pollsChatViews")
  def pollsChatViews(frontendSystem: FrontendSystem) = frontendSystem.pollsChatViews
}

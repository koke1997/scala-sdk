package modules

import com.google.inject.{AbstractModule, Provides}
import javax.inject.Singleton
import org.apache.pekko.actor.typed.{ActorSystem, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import play.api.Configuration
import scala.concurrent.ExecutionContext

class PekkoModule extends AbstractModule {
  // Define a root command type for our actor system
  sealed trait RootCommand
  case object Initialize extends RootCommand
  
  override def configure(): Unit = {
    // No additional configuration needed
  }
  
  @Provides
  @Singleton
  def provideActorSystem(
    configuration: Configuration,
    executionContext: ExecutionContext
  ): ActorSystem[RootCommand] = {
    // Define a root behavior with our specific type
    val rootBehavior: Behavior[RootCommand] = Behaviors.empty[RootCommand]
    
    // Create actor system with the behavior
    ActorSystem(rootBehavior, "mcp-server-system")
  }
}
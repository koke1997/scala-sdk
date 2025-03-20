package modules

import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}
import core.http.HttpClient
import services.ollama.OllamaClient
import services.data.DataService
import services.streaming.{OllamaStreamingClient, StreamingService}

/**
 * Module for HTTP-related services
 */
class HttpModule(environment: Environment, configuration: Configuration) extends AbstractModule {
  
  override def configure(): Unit = {
    // Bind interfaces to implementations
    bind(classOf[HttpClient]).to(classOf[OllamaClient])
    
    // Bind service instances
    bind(classOf[DataService])
    bind(classOf[OllamaStreamingClient])
    bind(classOf[StreamingService])
  }
}


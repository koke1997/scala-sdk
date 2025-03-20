package core.config

import javax.inject._
import play.api.Configuration

/**
 * Type-safe configuration for Ollama
 */
@Singleton
class OllamaConfig @Inject()(config: Configuration) {
  val baseUrl: String = config.getOptional[String]("ollama.baseUrl")
    .getOrElse("http://localhost:11434/api")
    
  val defaultModel: String = config.getOptional[String]("ollama.defaultModel")
    .getOrElse("phi4-mini")
    
  val timeout: Int = config.getOptional[Int]("ollama.timeout")
    .getOrElse(300) // 5 minutes default
    
  val streamingEnabled: Boolean = config.getOptional[Boolean]("ollama.streaming")
    .getOrElse(true)
}


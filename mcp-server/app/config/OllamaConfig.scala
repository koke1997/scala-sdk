package config

import javax.inject._
import play.api.{Configuration, Logger}

@Singleton
class OllamaConfig @Inject()(config: Configuration) {
  private val logger = Logger(this.getClass)

  val baseUrl: String = config.getOptional[String]("ollama.baseUrl")
    .getOrElse("http://localhost:11434/api")
  
  val defaultModel: String = config.getOptional[String]("ollama.defaultModel")
    .getOrElse("phi4-mini")
  
  val requestTimeout: java.time.Duration = java.time.Duration.ofSeconds(
    config.getOptional[Long]("ollama.timeout.seconds").getOrElse(300)
  )
  
  logger.info(s"Ollama configured with baseUrl: $baseUrl, defaultModel: $defaultModel")
}


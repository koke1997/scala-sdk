package services.ollama

import javax.inject._
import play.api.Logger
import play.api.libs.json._
import core.config.OllamaConfig

import scala.concurrent.{ExecutionContext, Future}

/**
 * Service for model-related operations
 */
@Singleton
class ModelService @Inject()(
  client: OllamaClient,
  config: OllamaConfig
)(implicit ec: ExecutionContext) {
  
  private val logger = Logger(getClass)
  
  /**
   * List available models
   */
  def listModels(): Future[JsValue] = {
    logger.info("Listing available models")
    client.get(s"${config.baseUrl}/tags")
  }
  
  /**
   * Get model details
   */
  def getModel(name: String): Future[Option[JsValue]] = {
    listModels().map { response =>
      (response \ "models").asOpt[JsArray].flatMap { modelsArray =>
        modelsArray.value.find { model =>
          (model \ "name").asOpt[String].contains(name)
        }
      }
    }
  }
}


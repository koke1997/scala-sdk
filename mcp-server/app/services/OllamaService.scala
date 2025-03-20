package services

import javax.inject._
import play.api.libs.json._
import play.api.libs.ws._
import models._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}
import zio._
import zio.interop.catz._

/**
 * Service that handles communication with Ollama API
 */
@Singleton
class OllamaService @Inject()(ws: WSClient)(implicit ec: ExecutionContext) {
  
  private val ollamaBaseUrl = "http://localhost:11434/api"
  
  def generateCompletion(request: OllamaRequest): Task[OllamaResponse] = {
    ZIO.fromFuture { implicit ec =>
      ws.url(s"$ollamaBaseUrl/generate")
        .withHttpHeaders("Content-Type" -> "application/json")
        .post(Json.toJson(request))
        .map { response =>
          response.status match {
            case 200 => response.json.as[OllamaResponse]
            case status => 
              throw new RuntimeException(s"Ollama API error: $status - ${response.body}")
          }
        }
    }
  }
  
  def listModels(): Task[Seq[String]] = {
    ZIO.fromFuture { implicit ec =>
      ws.url(s"$ollamaBaseUrl/tags")
        .get()
        .map { response =>
          response.status match {
            case 200 => 
              val models = (response.json \ "models").as[Seq[JsObject]]
              models.map(model => (model \ "name").as[String])
            case status =>
              throw new RuntimeException(s"Ollama API error: $status - ${response.body}")
          }
        }
    }
  }
  
  // Convert between context formats
  def convertToOllamaRequest(contextRequest: ContextRequest, modelName: String = "llama3"): OllamaRequest = {
    OllamaRequest(
      prompt = contextRequest.text,
      model = modelName,
      stream = Some(false),
      options = contextRequest.settings.map(settings => 
        Json.fromJson[OllamaOptions](settings).getOrElse(OllamaOptions())
      )
    )
  }
  
  def convertFromOllamaResponse(ollamaResponse: OllamaResponse, requestId: String): ContextResponse = {
    ContextResponse(
      id = requestId,
      status = if (ollamaResponse.done) "completed" else "processing",
      summary = Some(ollamaResponse.response.take(100) + "..."),
      metadata = Some(Json.obj(
        "model" -> ollamaResponse.model,
        "complete" -> ollamaResponse.done,
        "contextSize" -> JsNumber(BigDecimal(ollamaResponse.context.map(_.size).getOrElse(0)))
      ))
    )
  }
}
package models.ollama

import play.api.libs.json._

/**
 * Model data structures for Ollama API responses
 */
case class ModelInfo(
  name: String,
  model: String,
  modified_at: Option[String] = None,
  size: Option[Long] = None,
  digest: Option[String] = None,
  details: Option[JsObject] = None
)

case class ModelsResponse(
  models: Seq[ModelInfo]
)

object ModelInfo {
  implicit val format: Format[ModelInfo] = Json.format[ModelInfo]
}

object ModelsResponse {
  implicit val format: Format[ModelsResponse] = Json.format[ModelsResponse] 
}

/**
 * Request models
 */
case class GenerateRequest(
  model: String,
  prompt: String,
  stream: Option[Boolean] = None,
  options: Option[JsObject] = None
)

object GenerateRequest {
  implicit val format: Format[GenerateRequest] = Json.format[GenerateRequest]
}

case class GenerateResponse(
  model: String,
  created_at: String,
  response: String,
  done: Boolean
)

object GenerateResponse {
  implicit val format: Format[GenerateResponse] = Json.format[GenerateResponse]
}


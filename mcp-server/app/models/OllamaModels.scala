package models

import play.api.libs.json._

// Request models
case class GenerateRequest(
  model: String,
  prompt: String,
  stream: Option[Boolean] = None,
  options: Option[Map[String, JsValue]] = None
)

object GenerateRequest {
  implicit val format: Format[GenerateRequest] = Json.format[GenerateRequest]
  
  def fromText(text: String, model: String): GenerateRequest = 
    GenerateRequest(model = model, prompt = text)
    
  def withDefaults(model: String, prompt: String, stream: Boolean = false): GenerateRequest =
    GenerateRequest(model = model, prompt = prompt, stream = Some(stream))
}

// Response models for better type safety
case class ModelInfo(
  name: String,
  model: String,
  modified_at: Option[String] = None,
  size: Option[Long] = None
)

object ModelInfo {
  implicit val format: Format[ModelInfo] = Json.format[ModelInfo]
}

case class ModelsResponse(models: Seq[ModelInfo])

object ModelsResponse {
  implicit val format: Format[ModelsResponse] = Json.format[ModelsResponse]
}

case class GenerateResponse(
  model: String,
  created_at: Option[String] = None,
  response: String,
  done: Boolean
)

object GenerateResponse {
  implicit val format: Format[GenerateResponse] = Json.format[GenerateResponse]
}

// Error model
case class ApiError(
  message: String,
  code: Option[String] = None,
  details: Option[JsValue] = None
)

object ApiError {
  implicit val format: Format[ApiError] = Json.format[ApiError]
  
  def fromException(e: Throwable): ApiError = 
    ApiError(message = e.getMessage, code = Some("INTERNAL_ERROR"))
}


package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import services.{MCPService, OllamaService}
import models._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Controller that handles Model Context Protocol (MCP) API endpoints.
 * Provides endpoints for context processing and retrieval to support Claude Desktop and Cursor.
 */
@Singleton
class MCPController @Inject()(
  cc: ControllerComponents,
  mcpService: MCPService,
  ollamaService: OllamaService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  /**
   * Action to process a new context request (create) - Claude Desktop compatible
   */
  def process = Action.async(parse.json) { implicit request =>
    withJsonBody[ContextRequest] { contextRequest =>
      mcpService.runTask(mcpService.processContext(contextRequest)).map { response =>
        Created(Json.toJson(response))
      }
    }
  }

  /**
   * Action to update an existing context
   */
  def updateContext(id: String) = Action.async(parse.json) { implicit request =>
    withJsonBody[ContextRequest] { contextRequest =>
      mcpService.runTask(mcpService.updateContext(id, contextRequest)).map { response =>
        Ok(Json.toJson(response))
      }.recover {
        case e: RuntimeException => NotFound(Json.toJson(ErrorResponse(
          error = e.getMessage,
          code = Some("CONTEXT_NOT_FOUND")
        )))
      }
    }
  }
  
  /**
   * Action to get a context by ID
   */
  def getContext(id: String) = Action.async { implicit request =>
    mcpService.runTask(mcpService.getContext(id)).map { response =>
      Ok(Json.toJson(response))
    }.recover {
      case e: RuntimeException => NotFound(Json.toJson(ErrorResponse(
        error = e.getMessage,
        code = Some("CONTEXT_NOT_FOUND")
      )))
    }
  }
  
  /**
   * Ollama API compatibility - generate completions
   */
  def ollamaGenerate = Action.async(parse.json) { implicit request =>
    withJsonBody[OllamaRequest] { ollamaRequest =>
      mcpService.runTask(ollamaService.generateCompletion(ollamaRequest)).map { response =>
        Ok(Json.toJson(response))
      }.recover {
        case e: Exception => BadRequest(Json.toJson(ErrorResponse(
          error = e.getMessage,
          code = Some("OLLAMA_API_ERROR")
        )))
      }
    }
  }
  
  /**
   * List available Ollama models
   */
  def ollamaModels = Action.async { implicit request =>
    mcpService.runTask(ollamaService.listModels()).map { models =>
      Ok(Json.obj("models" -> models))
    }.recover {
      case e: Exception => InternalServerError(Json.toJson(ErrorResponse(
        error = e.getMessage,
        code = Some("OLLAMA_API_ERROR")
      )))
    }
  }
  
  /**
   * Process a context with Ollama
   */
  def processWithOllama(id: String) = Action.async(parse.json) { implicit request =>
    val modelName = (request.body \ "model").asOpt[String].getOrElse("llama3")
    mcpService.runTask(mcpService.processWithOllama(id, modelName)).map { response =>
      Ok(Json.toJson(response))
    }.recover {
      case e: RuntimeException => NotFound(Json.toJson(ErrorResponse(
        error = e.getMessage,
        code = Some("CONTEXT_OR_MODEL_NOT_FOUND")
      )))
    }
  }

  /**
   * Helper for handling JSON body
   */
  private def withJsonBody[A](f: A => Future[Result])(implicit request: Request[JsValue], reads: Reads[A]): Future[Result] = {
    request.body.validate[A].fold(
      errors => {
        Future.successful(BadRequest(Json.toJson(ErrorResponse(
          error = "Invalid request format",
          code = Some("INVALID_FORMAT"),
          details = Some(JsError.toJson(errors))
        ))))
      },
      model => f(model)
    )
  }
}

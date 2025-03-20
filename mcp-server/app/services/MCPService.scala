package services

import javax.inject._
import models._
import play.api.libs.json._
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import zio._
import zio.interop.catz._

/**
 * Service that handles Model Context Protocol (MCP) operations
 * that can be used by Claude Desktop or Cursor.
 */
@Singleton
class MCPService @Inject()(ollamaService: OllamaService)(implicit ec: ExecutionContext) {

  // In-memory storage for contexts
  private val contexts = collection.mutable.Map[String, ContextRequest]()
  
  /**
   * Process a new context request
   */
  def processContext(request: ContextRequest): Task[ContextResponse] = {
    // Generate a unique ID for the context
    val id = UUID.randomUUID().toString
    
    // Store the context
    contexts.put(id, request)
    
    // For Claude Desktop compatibility, we'll just acknowledge receipt
    // For actual processing, we'd use Ollama or other backends
    ZIO.succeed(ContextResponse(
      id = id,
      status = "processed",
      summary = Some(s"Processed context: ${request.text.take(50)}..."),
      metadata = request.metadata
    ))
  }
  
  /**
   * Update an existing context
   */
  def updateContext(id: String, request: ContextRequest): Task[ContextResponse] = {
    ZIO.fromOption(contexts.get(id))
      .mapError(_ => new RuntimeException(s"Context with ID $id not found"))
      .flatMap { existingContext =>
        // Update the context
        val updatedContext = request
        contexts.put(id, updatedContext)
        
        ZIO.succeed(ContextResponse(
          id = id,
          status = "updated",
          summary = Some(s"Updated context: ${request.text.take(50)}..."),
          metadata = request.metadata
        ))
      }
  }
  
  /**
   * Get a context by ID
   */
  def getContext(id: String): Task[ContextResponse] = {
    ZIO.fromOption(contexts.get(id))
      .mapError(_ => new RuntimeException(s"Context with ID $id not found"))
      .map { context =>
        ContextResponse(
          id = id,
          status = "available",
          summary = Some(s"Retrieved context: ${context.text.take(50)}..."),
          metadata = context.metadata
        )
      }
  }
  
  /**
   * Process a context through Ollama
   */
  def processWithOllama(id: String, modelName: String): Task[ContextResponse] = {
    ZIO.fromOption(contexts.get(id))
      .mapError(_ => new RuntimeException(s"Context with ID $id not found"))
      .flatMap { context =>
        val ollamaRequest = ollamaService.convertToOllamaRequest(context, modelName)
        ollamaService.generateCompletion(ollamaRequest)
          .map(response => ollamaService.convertFromOllamaResponse(response, id))
      }
  }
  
  /**
   * Convert ZIO Task to Future for Play compatibility
   */
  def runTask[A](effect: Task[A]): Future[A] = {
    val runtime = Runtime.default
    
    Unsafe.unsafe { implicit unsafe =>
      runtime.unsafe.runToFuture(effect)
    }
  }
}

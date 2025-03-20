package filters

import javax.inject._
import org.apache.pekko.util.ByteString
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source}
import org.apache.pekko.stream.Materializer
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.streams.Accumulator
import play.api.mvc.request.RequestAttrKey
import scala.concurrent.{ExecutionContext, Future}
import services.RequestLogger

/**
 * Filter that logs API requests and responses for the dashboard.
 * Uses Apache Pekko instead of Akka.
 */
@Singleton
class RequestLoggingFilter @Inject()(
  implicit ec: ExecutionContext, 
  mat: Materializer
) extends EssentialFilter {
  
  def apply(next: EssentialAction) = new EssentialAction {
    def apply(requestHeader: RequestHeader) = {
      
      // Only log API requests, not assets or dashboard pages
      val shouldLog = requestHeader.path.startsWith("/api/")

      if (!shouldLog) {
        // Don't log non-API requests
        next(requestHeader)
      } else {
        // For API requests, capture request and response
        val method = requestHeader.method
        val path = requestHeader.path
        
        // For POST/PUT/PATCH requests, we need to extract the body
        if (Set("POST", "PUT", "PATCH").contains(method)) {
          // Parse the body, then pass it along with the original request
          val bodyParser = BodyParser { _ =>
            val sink = Sink.fold[ByteString, ByteString](ByteString.empty)(_ ++ _)
            
            Accumulator(sink).mapFuture { bytes =>
              if (bytes.size > 100 * 1024) { // 100KB limit
                Future.successful(Left(Results.EntityTooLarge("Request body exceeds maximum size")))
              } else {
                Future.successful(Right(bytes))
              }
            }
          }
          
          bodyParser(requestHeader).mapFuture {
            case Left(result) => 
              // Body was too large or invalid
              Future.successful(result)
            case Right(bytes) =>
              val bodyString = bytes.utf8String
              val bodyJson = if (bodyString.nonEmpty) {
                try {
                  Some(Json.parse(bodyString))
                } catch {
                  case _: Exception => Some(JsString(bodyString))
                }
              } else None

              // Create a fake request with the same body to pass to the next filter
              val fakeRequest = requestHeader.withBody(bytes)
              
              // Call the next action and process its result
              next(fakeRequest).run().map { result =>
                // Extract content type
                val contentType = result.header.headers.get("Content-Type")
                val isJson = contentType.exists(_.contains("application/json"))
                
                // For JSON responses, try to extract the body
                val responseJson = if (isJson) {
                  result match {
                    case r: Results.Status => r.body match {
                      case body: play.api.http.HttpEntity.Strict =>
                        try {
                          Json.parse(body.data.utf8String)
                        } catch {
                          case _: Exception => JsNull
                        }
                      case _ => JsNull
                    }
                    case _ => JsNull
                  }
                } else JsNull
                
                // Log the request
                RequestLogger.logRequest(method, path, bodyJson, result.header.status, responseJson)
                
                result
              }
          }
        } else {
          // For GET/DELETE requests without bodies
          next(requestHeader).mapFuture { result =>
            // Extract content type
            val contentType = result.header.headers.get("Content-Type")
            val isJson = contentType.exists(_.contains("application/json"))
            
            // For JSON responses, try to extract the body
            val responseJson = if (isJson) {
              result match {
                case r: Results.Status => r.body match {
                  case body: play.api.http.HttpEntity.Strict =>
                    try {
                      Json.parse(body.data.utf8String)
                    } catch {
                      case _: Exception => JsNull
                    }
                  case _ => JsNull
                }
                case _ => JsNull
              }
            } else JsNull
            
            // Log the request
            RequestLogger.logRequest(method, path, None, result.header.status, responseJson)
            
            Future.successful(result)
          }
        }
      }
    }
  }
}


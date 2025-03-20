package controllers.api

import javax.inject._
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import services.data.DataService

import scala.concurrent.{ExecutionContext, Future}

/**
 * Controller for data processing operations
 */
@Singleton
class DataController @Inject()(
  cc: ControllerComponents,
  dataService: DataService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  
  private val logger = Logger(getClass)
  
  /**
   * Load data from CSV file
   */
  def loadCsv() = Action.async(parse.json) { request =>
    val path = (request.body \ "path").asOpt[String]
    val hasHeader = (request.body \ "header").asOpt[Boolean].getOrElse(true)
    
    if (path.isEmpty) {
      Future.successful(BadRequest(Json.obj("error" -> "Path is required")))
    } else {
      dataService.readCsv(path.get, hasHeader)
        .map(data => Ok(data))
        .recover {
          case e: Exception =>
            logger.error(s"Error reading CSV: ${e.getMessage}", e)
            InternalServerError(Json.obj("error" -> e.getMessage))
        }
    }
  }
  
  /**
   * Extract keywords from text
   */
  def extractKeywords() = Action.async(parse.json) { request =>
    val text = (request.body \ "text").asOpt[String]
    val topN = (request.body \ "topN").asOpt[Int].getOrElse(5)
    
    if (text.isEmpty) {
      Future.successful(BadRequest(Json.obj("error" -> "Text is required")))
    } else {
      dataService.extractKeywords(text.get, topN)
        .map(keywords => Ok(Json.obj("keywords" -> keywords)))
        .recover {
          case e: Exception =>
            logger.error(s"Error extracting keywords: ${e.getMessage}", e)
            InternalServerError(Json.obj("error" -> e.getMessage))
        }
    }
  }
}


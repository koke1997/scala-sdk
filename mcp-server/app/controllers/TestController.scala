package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._

@Singleton
class TestController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  
  // Simple GET endpoint
  def hello = Action { implicit request =>
    Ok(Json.obj("message" -> "Hello world!"))
  }
  
  // Simple POST endpoint with tolerantJson parser
  def echo = Action(parse.tolerantJson) { implicit request =>
    // Debug print
    println(s"Received body: ${request.body}")
    Ok(request.body)
  }
}
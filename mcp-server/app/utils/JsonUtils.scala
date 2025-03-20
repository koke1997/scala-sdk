package utils

import play.api.libs.json._

/**
 * Utility functions for working with JSON.
 */
object JsonUtils {
  /**
   * Transforms JSON by applying a function to specified fields.
   */
  def transformJson[T](json: JsValue, fieldNames: Seq[String], f: JsValue => JsValue)
    (implicit reads: Reads[T], writes: Writes[T]): JsValue = {
    
    json.validate[T].fold(
      _ => json,
      valid => {
        val obj = json.as[JsObject]
        val transformed = fieldNames.foldLeft(obj) { (res, field) =>
          (res \ field).toOption match {
            case Some(value) => res + (field -> f(value))
            case None => res
          }
        }
        transformed
      }
    )
  }
}

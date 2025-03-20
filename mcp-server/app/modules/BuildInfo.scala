package modules

/**
 * Build information for the application.
 * This information can be provided to clients for diagnostics and versioning.
 */
object BuildInfo {
  val name = "mcp-server"
  val version = "1.0.0"
  val scalaVersion = "2.13.10"
  val buildTime = System.currentTimeMillis()
  
  /**
   * Application environment (development, test, production)
   */
  def environment: String = sys.env.getOrElse("APP_ENV", "development")
  
  /**
   * Get all build information as a map
   */
  def toMap: Map[String, String] = Map(
    "name" -> name,
    "version" -> version,
    "scalaVersion" -> scalaVersion,
    "buildTime" -> buildTime.toString,
    "environment" -> environment
  )
}


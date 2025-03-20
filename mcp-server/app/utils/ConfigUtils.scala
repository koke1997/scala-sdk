package utils

import com.typesafe.config.{Config, ConfigFactory}
import javax.inject._

/**
 * Utility for working with configuration.
 */
@Singleton
class ConfigUtils @Inject()() {
  private val config: Config = ConfigFactory.load()

  /**
   * Gets a string configuration value.
   */
  def getString(path: String, default: String = ""): String = {
    if (config.hasPath(path)) config.getString(path) else default
  }

  /**
   * Gets an integer configuration value.
   */
  def getInt(path: String, default: Int = 0): Int = {
    if (config.hasPath(path)) config.getInt(path) else default
  }

  /**
   * Gets a boolean configuration value.
   */
  def getBoolean(path: String, default: Boolean = false): Boolean = {
    if (config.hasPath(path)) config.getBoolean(path) else default
  }
}

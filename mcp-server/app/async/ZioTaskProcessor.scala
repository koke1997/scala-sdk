package async

import zio._

/**
 * Handles asynchronous processing of tasks using ZIO.
 */
object ZioTaskProcessor {
  /**
   * Processes a task asynchronously.
   *
   * @param task The task to be processed.
   * @return A ZIO effect that represents the asynchronous processing.
   */
  def processTask[A, B](task: A)(processFunction: A => B): Task[B] = {
    ZIO.attempt(processFunction(task))
  }
}

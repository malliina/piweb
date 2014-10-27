package controllers

import com.mle.logbackrx.{BasicBoundedReplayRxAppender, LogbackUtils}
import com.mle.play.controllers.LogStreaming
import play.api.mvc.{Action, Call}

/**
 * @author Michael
 */
object Logs extends PiController with LogStreaming {
  def logs = Action(implicit req => {
    Ok(views.html.logs())
  })

  lazy val appender = LogbackUtils.getAppender[BasicBoundedReplayRxAppender]("RX")

  override def openSocketCall: Call = routes.Logs.openSocket

  override def onMessage(msg: Logs.Message, client: Logs.Client): Unit = {
    try {
      super.onMessage(msg, client)
    } catch {
      case e: Exception => log.warn(s"Failure", e)
    }
  }
}

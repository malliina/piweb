package controllers

import com.mle.pi.PiRevB2
import com.mle.play.controllers.{AuthResult, Streaming}
import com.mle.play.ws.SyncAuth
import com.pi4j.io.gpio.PinState
import play.api.libs.json.{JsError, JsResult, JsValue, Json}
import play.api.mvc._
import rx.lang.scala.Observable

import scala.util.Try

/**
 *
 * @author mle
 */
object Home extends Controller with Streaming with SyncAuth {
  val MSG = "msg"
  val PWM = "pwm"
  val ON = "on"
  val OFF = "off"
  val VALUE = "value"
  val NUMBER = "number"

  val board = Try(new PiRevB2).toOption
  //  def snapshot = board.ppins.map(prov => PinInfo(prov.number, prov.outPin.getState, prov.enableState))
  val testSnapshot = Seq(PinInfo(1, PinState.HIGH, PinState.HIGH), PinInfo(2, PinState.LOW, PinState.HIGH))

  def snapshot = board.fold(testSnapshot)(b => b.ppins.map(prov => PinInfo(prov.number, prov.outPin.getState, prov.enableState)))

  def index = Action(implicit req => {
    Ok(views.html.index(snapshot))
  })

  override def jsonEvents: Observable[JsValue] = Observable.empty

  override def authenticate(implicit req: RequestHeader): Option[AuthSuccess] = Some(AuthResult("test"))

  override def openSocketCall: Call = routes.Home.openSocket

  override def onMessage(msg: Message, client: Client): Unit = {
    super.onMessage(msg, client)
    parseMessage(msg) map handleMessage
  }

  def parseMessage(json: JsValue): JsResult[PiMessage] = (json \ MSG).validate[String].flatMap {
    case PWM => (json \ VALUE).validate[String].map(_.toInt).map(Pwm.apply)
    case ON => json.validate[DigitalOn]
    case OFF => json.validate[DigitalOff]
    case other => JsError(s"Unknown JSON message type: $other")
  }

  def handleMessage(msg: PiMessage) = msg match {
    case Pwm(value) =>
      log info s"PWM: $value, not yet implemented"
    case DigitalOn(number) =>
      findPin(number).foreach(_.enable())
      log info s"On: $number"
    case DigitalOff(number) =>
      findPin(number).foreach(_.disable())
      log info s"Off: $number"
  }

  def findPin(number: Int) = board.flatMap(_.ppins.find(_.number == number))

  sealed trait PiMessage

  case class Pwm(value: Int) extends PiMessage

  case class DigitalOn(number: Int) extends PiMessage

  case class DigitalOff(number: Int) extends PiMessage

  implicit val offJson = Json.format[DigitalOff]
  implicit val onJson = Json.format[DigitalOn]
  implicit val pwmJson = Json.format[Pwm]
}


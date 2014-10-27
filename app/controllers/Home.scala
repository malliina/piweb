package controllers

import com.mle.pi.{PiRevB2, ProvisionedDigitalPin, ProvisionedPin, ProvisionedPwmPin}
import com.mle.piweb.Snapshot
import com.mle.play.controllers.Streaming
import com.pi4j.io.gpio.{GpioPin, GpioPinDigitalOutput, GpioPinPwmOutput, PinState}
import play.api.libs.json.{JsError, JsResult, JsValue, Json}
import play.api.mvc._
import rx.lang.scala.Observable

/**
 *
 * @author mle
 */
object Home extends PiController with Streaming {
  val MSG = "msg"
  val PWM = "pwm"
  val ON = "on"
  val OFF = "off"
  val VALUE = "value"
  val NUMBER = "number"

  val board = try {
    Some(new PiRevB2)
  } catch {
    case e: Exception =>
      log.warn(s"Unable to initialize Raspberry Pi.", e)
      None
    case ule: UnsatisfiedLinkError =>
      log.warn(s"Link error.", ule)
      None
    case err: Error =>
      log.warn("Error.", err)
      None
  }
  //  def snapshot = board.ppins.map(prov => PinInfo(prov.number, prov.outPin.getState, prov.enableState))
  val testSnapshot = Snapshot(
    Seq(PinInfo(1, PinState.HIGH, PinState.HIGH), PinInfo(2, PinState.LOW, PinState.HIGH)),
    Seq(PwmInfo(12, 128)))


  def snapshot = board.fold(testSnapshot)(b => Snapshot(
    b.ppins.map(prov => PinInfo(prov.number, prov.pin.getState, prov.enableState)),
    b.ppwms.map(pwm => PwmInfo(pwm.number, pwm.pwm))))

  def index = Action(implicit req => {
    Ok(views.html.index(snapshot))
  })

  override def jsonEvents: Observable[JsValue] = Observable.empty


  override def openSocketCall: Call = routes.Home.openSocket

  override def onMessage(msg: Message, client: Client): Unit = {
    super.onMessage(msg, client)
    parseMessage(msg) map handleMessage //recoverTotal (err => log.warn(s"Invalid JSON: $msg", err))
  }

  def parseMessage(json: JsValue): JsResult[PiMessage] = (json \ MSG).validate[String].flatMap {
    case PWM => json.validate[Pwm]
    case ON => json.validate[DigitalOn]
    case OFF => json.validate[DigitalOff]
    case other => JsError(s"Unknown JSON message type: $other")
  }

  def handleMessage(msg: PiMessage) = msg match {
    case Pwm(number, value) =>
      findPwm(number).foreach(_.pwm = value)
      log info s"PWM: $value to PIN: $number"
    case DigitalOn(number) =>
      findDigital(number).foreach(_.enable())
      log info s"On: $number"
    case DigitalOff(number) =>
      findDigital(number).foreach(_.disable())
      log info s"Off: $number"
  }

  def findPwm(number: Int) = find[GpioPinPwmOutput, ProvisionedPwmPin](number, _.ppwms) // board.flatMap(_.ppwms.find(_.number == number))

  def findDigital(number: Int) = find[GpioPinDigitalOutput, ProvisionedDigitalPin](number, _.ppins) // board.flatMap(_.ppins.find(_.number == number))

  def find[T <: GpioPin, U <: ProvisionedPin[T]](number: Int, f: PiRevB2 => Seq[U]): Option[U] =
    board.flatMap(b => f(b).find(_.number == number))

  sealed trait PiMessage

  case class Pwm(number: Int, value: Int) extends PiMessage

  case class DigitalOn(number: Int) extends PiMessage

  case class DigitalOff(number: Int) extends PiMessage

  implicit val offJson = Json.format[DigitalOff]
  implicit val onJson = Json.format[DigitalOn]
  implicit val pwmJson = Json.format[Pwm]
}


package controllers

import com.mle.pi.PinEvents.{DigitalStateChanged, PwmChanged, Released}
import com.mle.pi._
import com.mle.piweb.Snapshot
import com.mle.play.controllers.Streaming
import com.mle.play.json.JsonStrings
import com.mle.util.TryImplicits.RichTry
import com.pi4j.io.gpio.{GpioPin, GpioPinDigitalOutput, GpioPinPwmOutput, PinState}
import play.api.libs.json.Json.{obj, toJson}
import play.api.libs.json._
import play.api.mvc._
import rx.lang.scala.Observable

import scala.util.Try
import scala.util.control.NonFatal

/**
 *
 * @author mle
 */
object Home extends PiController with Streaming {

  import com.mle.play.json.JsonStrings.EVENT

  val MSG = "msg"
  val PWM = "pwm"
  val HW_PWM = "hw_pwm"
  val BLAST_PWM = "blast_pwm"
  val ON = "on"
  val OFF = "off"
  val VALUE = "value"
  val NUMBER = "number"
  val BLAST = "blast"
  val RELEASE = "release"
  val RELEASE_ALL = "release_all"
  val RELEASED = "released"
  val OPEN = "open"
  val CLOSE = "close"
  val OPENED = "opened"
  val CLOSED = "closed"
  val STATUS = "status"
  val FEEDBACK = "feedback"
  val PIN = "pin"
  val ERROR = "error"
  val MESSAGE = "message"
  val DIGITAL = "digital"
  val STATE = "state"
  val HIGH = "high"
  val LOW = "low"
  val BOARD = "board"
  val GPIO = "gpio"

  implicit val pinJson = new Writes[PinPlan] {
    override def writes(o: PinPlan): JsValue = obj(BOARD -> o.boardNumber, GPIO -> o.gpioNumber)
  }

  val blaster = new Blaster
  val blasterPins = PiRevB2.pins
  val blasterJson = blaster.events.map {
    case Released(gpio) =>
      findPinGPIO(gpio)
        .map(pin => obj(EVENT -> RELEASED, PIN -> Json.toJson(pin)))
        .getOrElse(gpioNotFound(gpio))
    case PinEvents.Pwm(gpio, value) =>
      findPinGPIO(gpio)
        .map(pin => obj(EVENT -> BLAST_PWM, PIN -> toJson(pin), VALUE -> value))
        .getOrElse(gpioNotFound(gpio))
    case PwmChanged(pin, value) =>
      obj(EVENT -> HW_PWM, PIN -> toJson(pin.backing.plan), VALUE -> value)
    case DigitalStateChanged(pin, state) =>
      obj(EVENT -> DIGITAL, PIN -> toJson(pin.backing.plan), STATE -> (if (state.isHigh) HIGH else LOW))
  }

  def gpioNotFound(gpio: Int) = obj(EVENT -> ERROR, MESSAGE -> s"Unable to find PIN with GPIO number: $gpio")

  var board: Option[PiRevB2] = None

  override def jsonEvents: Observable[JsValue] = blasterJson

  def tryOpen(): Either[(String, Throwable), PiRevB2] =
    try Right(new PiRevB2)
    catch errorMessage.andThen(pair => Left(pair))

  def close(sender: Client) = {
    board.foreach(_.close())
    board = None
    broadcast(statusEvent)
  }

  def reOpen(sender: Client): Unit = {
    close(sender)
    tryOpen().fold(
      pair => sender.channel push feedback(pair._1),
      b => board = Option(b))
    broadcast(statusEvent)
  }

  def statusEvent = event(board.fold(CLOSED)(_ => OPENED))

  def errorMessage: PartialFunction[Throwable, (String, Throwable)] = {
    case e: Exception =>
      ("Unable to initialize Raspberry Pi.", e)
    case ule: UnsatisfiedLinkError =>
      ("Link error.", ule)
    case NonFatal(ex) =>
      ("Error.", ex)
  }

  //  def snapshot = board.ppins.map(prov => PinInfo(prov.number, prov.outPin.getState, prov.enableState))
  val testSnapshot = Snapshot(
    Seq(PinInfo(1, PinState.HIGH, PinState.HIGH), PinInfo(2, PinState.LOW, PinState.HIGH)),
    Seq(PwmInfo(12, 128)))

  def snapshot = board.fold(testSnapshot)(b => Snapshot(
    b.ppins.map(prov => PinInfo(prov.boardNumber, prov.pin.getState, prov.enableState)),
    b.ppwms.map(pwm => PwmInfo(pwm.boardNumber, pwm.pwm))))

  def index = Action(implicit req => {
    Ok(views.html.digital(snapshot))
  })

  def blast = Action(implicit req => {
    Ok(views.html.blaster(blasterPins))
  })

  override def openSocketCall: Call = routes.Home.openSocket

  override def onMessage(msg: Message, client: Client): Unit = {
    super.onMessage(msg, client)
    log info s"Message: $msg"
    parseMessage(msg) map (msg => handleMessage(msg, client)) recoverTotal (err => log.warn(s"Invalid JSON: $msg", err))
  }

  def parseMessage(json: JsValue): JsResult[PiMessage] = (json \ MSG).validate[String].flatMap {
    case STATUS => JsSuccess(GetStatus)
    case BLAST => json.validate[BlastPwm]
    case RELEASE => json.validate[Release]
    case RELEASE_ALL => JsSuccess(ReleaseAll)
    case PWM => json.validate[Pwm]
    case ON => json.validate[DigitalOn]
    case OFF => json.validate[DigitalOff]
    case OPEN => JsSuccess(Open)
    case CLOSE => JsSuccess(Close)
    case other => JsError(s"Unknown JSON message type: $other")
  }

  def handleMessage(msg: PiMessage, sender: Client) = msg match {
    case GetStatus =>
      sender.channel push statusEvent
    case Pwm(number, value) =>
      findPwm(number).foreach(_.pwm = value)
      log info s"PIN: $number to PWM: $value."
    case Release(number) =>
      release(number, sender)
    case ReleaseAll =>
      blasterPins.foreach(p => release(p.boardNumber, sender))
    case BlastPwm(number, value) =>
      blast(number, sender)(p => {
        blaster.write(p, value).map(_ => log info s"Blasted PIN: $number to PWM: $value.")
      })
    case DigitalOn(number) =>
      findDigital(number).foreach(_.enable())
      log info s"On: $number"
    case DigitalOff(number) =>
      findDigital(number).foreach(_.disable())
      log info s"Off: $number"
    case Close =>
      close(sender)
    case Open =>
      reOpen(sender)
  }

  def release(boardNumber: Int, sender: Client) = blast(boardNumber, sender)(p => {
    blaster.release(p).map(_ => log info s"Released PIN: $boardNumber.")
  })

  def blast(number: Int, client: Client)(f: PinPlan => Try[Unit]) = findPinBoard(number)
    .fold(log.warn(s"Unable to find PIN with board number: $number."))(pin => {
    f(pin).recoverAll(t => {
      val msg = "Unable to blast."
      log.warn(msg, t)
      client.channel push feedback(msg)
    })
  })

  def findPinGPIO(gpioNumber: Int) = findPin(gpioNumber, _.gpioNumber)

  def findPinBoard(boardNumber: Int): Option[PinPlan] = findPin(boardNumber, _.boardNumber)

  def findPin(number: Int, comparator: PinPlan => Int): Option[PinPlan] =
    PiRevB2.pins.find(p => comparator(p) == number)

  def findPwm(number: Int) = find[GpioPinPwmOutput, ProvisionedPwmPin](number, _.ppwms)

  def findDigital(number: Int) = find[GpioPinDigitalOutput, ProvisionedDigitalPin](number, _.ppins)

  def find[T <: GpioPin, U <: ProvisionedPin[T, _]](number: Int, f: PiRevB2 => Seq[U]): Option[U] =
    board.flatMap(b => f(b).find(_.boardNumber == number))

  def event(e: String) = obj(JsonStrings.EVENT -> e)

  def feedback(fb: String) = event(FEEDBACK) ++ obj(FEEDBACK -> fb)

  def broadcastEvent(e: String) = broadcast(event(e))

  sealed trait PiMessage

  case class BlastPwm(number: Int, value: Int) extends PiMessage

  case class Release(number: Int) extends PiMessage

  case object ReleaseAll extends PiMessage

  case class Pwm(number: Int, value: Int) extends PiMessage

  case class DigitalOn(number: Int) extends PiMessage

  case class DigitalOff(number: Int) extends PiMessage

  case object Open extends PiMessage

  case object Close extends PiMessage

  case object GetStatus extends PiMessage

  implicit val offJson = Json.format[DigitalOff]
  implicit val onJson = Json.format[DigitalOn]
  implicit val pwmJson = Json.format[Pwm]
  implicit val blastJson = Json.format[BlastPwm]
  implicit val relJson = Json.format[Release]

}


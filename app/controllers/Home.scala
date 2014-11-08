package controllers

import com.mle.pi.PinEvents.{DigitalStateChanged, PwmChanged, Released}
import com.mle.pi._
import com.mle.piweb.PiMessages._
import com.mle.piweb.PiStrings._
import com.mle.piweb.Snapshot
import com.mle.play.controllers.Streaming
import com.mle.play.json.JsonStrings.EVENT
import com.mle.util.TryImplicits.RichTry
import com.pi4j.io.gpio.{GpioPin, GpioPinDigitalOutput, GpioPinPwmOutput, PinState}
import play.api.libs.json.Json.{obj, toJson}
import play.api.libs.json._
import play.api.mvc._
import play.twirl.api.Html
import rx.lang.scala.Observable
import rx.lang.scala.subjects.BehaviorSubject

import scala.util.Try
import scala.util.control.NonFatal

/**
 *
 * @author mle
 */
object Home extends PiController with Streaming {
  implicit val pinJson = new Writes[PinPlan] {
    override def writes(o: PinPlan): JsValue = obj(BOARD -> o.boardNumber, GPIO -> o.gpioNumber)
  }
  var board: Option[PiRevB2] = None
  val defaultBlaster = new LEDBlaster(PiRevB2.PIN18, PiRevB2.PIN16, PiRevB2.PIN11)
  val ledBlaster = BehaviorSubject[LEDBlaster](defaultBlaster)
  val blaster = defaultBlaster
  val blasterPins = PiRevB2.pins
  val brightnessEvents = blaster.brighty.map(b => obj(EVENT -> BRIGHTNESS, VALUE -> b.value))
  val colorEvents = blaster.colory.map(c => obj(EVENT -> COLOR, VALUE -> toJson(c)))
  val blasterEvents = blaster.events.map {
    case Released(gpio) =>
      tryFindGPIO(gpio)(pin => obj(EVENT -> RELEASED, PIN -> toJson(pin)))
    case PinEvents.Pwm(gpio, value) =>
      tryFindGPIO(gpio)(pin => obj(EVENT -> BLAST_PWM, PIN -> toJson(pin), VALUE -> value))
    case PwmChanged(pin, value) =>
      obj(EVENT -> HW_PWM, PIN -> toJson(pin.backing.plan), VALUE -> value)
    case DigitalStateChanged(pin, state) =>
      obj(EVENT -> DIGITAL, PIN -> toJson(pin.backing.plan), STATE -> (if (state.isHigh) HIGH else LOW))
  }

  def tryFindGPIO(gpioNumber: Int)(f: PinPlan => JsValue) =
    findPinGPIO(gpioNumber).map(f) getOrElse gpioNotFound(gpioNumber)

  def gpioNotFound(gpio: Int) = obj(EVENT -> ERROR, MESSAGE -> s"Unable to find PIN with GPIO number: $gpio")

  override def jsonEvents: Observable[JsValue] = blasterEvents merge brightnessEvents merge colorEvents

  def currentBlaster = ledBlaster.head.toBlocking.head

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

  def statusEvent = obj(
    EVENT -> STATUS,
    BOARD -> board.fold(CLOSED)(_ => OPENED),
    BLASTER -> toJson(blaster.status.map(kv => kv._1.boardNumber.toString -> kv._2)),
    BRIGHTNESS -> blaster.currentBrightness,
    COLOR -> blaster.currentColor)

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

  def digital = GoTo(implicit req => views.html.digital(snapshot))

  def blast = GoTo(implicit req => views.html.blaster(blasterPins))

  def color = GoTo(implicit req => views.html.color())

  def GoTo(page: RequestHeader => Html) = Action(implicit req => Ok(page(req)))

  override def openSocketCall: Call = routes.Home.openSocket

  override def onMessage(msg: Message, client: Client): Boolean = {
    val handled = super.onMessage(msg, client)
    if (!handled) {
      log info s"Message: $msg"
      parseMessage(msg) map (msg => handleMessage(msg, client)) recoverTotal (err => log.warn(s"Invalid JSON: $msg", err))
    }
    true
  }

  def parseMessage(json: JsValue): JsResult[PiMessage] = (json \ MSG).validate[String].flatMap {
    case STATUS => JsSuccess(GetStatus)
    case RGB => json.validate[Rgb]
    case BRIGHTNESS => json.validate[Bright]
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
    case rgb: Rgb =>
      withRecovery(blaster color rgb, sender)
    case Bright(b) =>
      withRecovery(blaster brightness b, sender)
    case Release(number) =>
      release(number, sender)
    case ReleaseAll =>
      blasterPins.foreach(p => release(p.boardNumber, sender))
    case BlastPwm(number, value) =>
      blast(number, sender)(p => blaster.write(p, value))
    case DigitalOn(number) =>
      findDigital(number).foreach(_.enable())
    case DigitalOff(number) =>
      findDigital(number).foreach(_.disable())
    case Close =>
      close(sender)
    case Open =>
      reOpen(sender)
  }

  def release(boardNumber: Int, sender: Client) = blast(boardNumber, sender)(p => {
    blaster.release(p).map(_ => log info s"Released PIN: $boardNumber.")
  })

  def blast(number: Int, client: Client)(f: PinPlan => Try[Unit]) = findPinBoard(number)
    .fold(log.warn(s"Unable to find PIN with board number: $number."))(pin => withRecovery(f(pin), client))

  def withRecovery(blast: Try[Unit], client: Client) = blast.recoverAll(t => {
    val error = Option(t.getMessage).getOrElse("")
    val msg = s"Unable to blast."
    log.warn(msg, t)
    client.channel push feedback(s"$msg $error")
  })

  def findPinGPIO(gpioNumber: Int) = findPin(gpioNumber, _.gpioNumber)

  def findPinBoard(boardNumber: Int): Option[PinPlan] = findPin(boardNumber, _.boardNumber)

  def findPin(number: Int, comparator: PinPlan => Int): Option[PinPlan] =
    PiRevB2.pins.find(p => comparator(p) == number)

  def findPwm(number: Int) = find[GpioPinPwmOutput, ProvisionedPwmPin](number, _.ppwms)

  def findDigital(number: Int) = find[GpioPinDigitalOutput, ProvisionedDigitalPin](number, _.ppins)

  def find[T <: GpioPin, U <: ProvisionedPin[T, _]](number: Int, f: PiRevB2 => Seq[U]): Option[U] =
    board.flatMap(b => f(b).find(_.boardNumber == number))

  def event(e: String) = obj(EVENT -> e)

  def feedback(fb: String) = event(FEEDBACK) ++ obj(FEEDBACK -> fb)

  def broadcastEvent(e: String) = broadcast(event(e))
}


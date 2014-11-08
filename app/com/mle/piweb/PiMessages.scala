package com.mle.piweb

import com.mle.pi.{PwmValue, Brightness, ColorValue, IRGBColor}
import play.api.libs.json.Json

/**
 * @author Michael
 */
object PiMessages {

  sealed trait PiMessage

  case class Rgb(red: ColorValue, green: ColorValue, blue: ColorValue) extends PiMessage with IRGBColor

  case class Bright(value: Brightness) extends PiMessage

  case class BlastPwm(number: Int, value: PwmValue) extends PiMessage

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
  implicit val rgbJson = Json.format[Rgb]
  implicit val brightJson = Json.format[Bright]
}

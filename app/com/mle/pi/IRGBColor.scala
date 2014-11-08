package com.mle.pi

import play.api.libs.json.{Format, JsValue, Json, Writes}

/**
 * @author Michael
 */
trait IRGBColor {
  def red: ColorValue

  def green: ColorValue

  def blue: ColorValue
}

object IRGBColor {

  import com.mle.piweb.PiStrings.{BLUE, GREEN, RED}

  implicit val j: Format[ColorValue] = ColorValue.json
  implicit val json = new Writes[IRGBColor] {
    override def writes(o: IRGBColor): JsValue = Json.obj(
      RED -> o.red,
      GREEN -> o.green,
      BLUE -> o.blue
    )
  }
}
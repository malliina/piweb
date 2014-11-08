package com.mle.pi

/**
 * @author Michael
 */
case class RGBColor(red: ColorValue, green: ColorValue, blue: ColorValue) extends IRGBColor

object RGBColor {
  val empty = RGBColor(ColorValue.empty, ColorValue.empty, ColorValue.empty)
  val off = empty
  val red = RGBColor(ColorValue.MaxValue, ColorValue.empty, ColorValue.empty)
}
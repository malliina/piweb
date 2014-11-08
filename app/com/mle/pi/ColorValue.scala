package com.mle.pi

import com.mle.values.{RangedInt, WrappedValue}

/**
 * @author Michael
 */
case class ColorValue private(value: Int) extends WrappedValue[Int]

object ColorValue extends RangedInt[ColorValue](0, 255) {
  override protected def build(t: Int): ColorValue = ColorValue(t)
}

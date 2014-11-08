package com.mle.pi

import com.mle.values.{RangedInt, WrappedValue}

/**
 * @author Michael
 */
case class Brightness private(value: Int) extends WrappedValue[Int]

object Brightness extends RangedInt[Brightness](0, 1000) {
  val full = Brightness(Max)

  override protected def build(t: Int): Brightness = Brightness(t)
}
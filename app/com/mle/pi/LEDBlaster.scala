package com.mle.pi

import com.mle.piweb.MemoizingBlaster
import com.mle.rx.RxExtensions.RichBehaviorSubject
import com.mle.util.Log
import rx.lang.scala.subjects.BehaviorSubject

import scala.util.{Failure, Try}


/**
 * @author Michael
 */
class LEDBlaster(redPin: PinPlan, greenPin: PinPlan, bluePin: PinPlan) extends MemoizingBlaster with Log {
  val colory = BehaviorSubject[IRGBColor](RGBColor.red)
  val brighty = BehaviorSubject[Brightness](Brightness.empty)

  def currentColor = colory.firstItem

  def currentBrightness = brighty.firstItem

  def color(color: IRGBColor) = {
    for {
      _ <- writeColor(redPin, color.red)
      _ <- writeColor(greenPin, color.green)
      r <- writeColor(bluePin, color.blue)
    } yield {
      colory onNext color
      log info s"Blasted color: $color."
      r
    }
  }

  def release(): Try[Unit] = {
    for {
      _ <- release(redPin)
      _ <- release(greenPin)
      r <- release(bluePin)
    } yield {
      colory onNext RGBColor.empty
      r
    }
  }

  def writeColor(pin: PinPlan, color: ColorValue): Try[Unit] = {
    colorToPwm(color)
      .fold[Try[Unit]](Failure(new Exception(s"Unable to construct PWM value from color: $color.")))(v => write(pin, v))
  }

  def brightness(b: Brightness) = {
    brighty onNext b
    color(currentColor)
  }

  private def colorToPwm(color: ColorValue): Option[PwmValue] =
    PwmValue from (1.0f * color.value / ColorValue.Max * currentBrightness.value / Brightness.Max * PwmValue.Max).toInt
}




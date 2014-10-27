package controllers

import com.pi4j.io.gpio.PinState

/**
 * @author Michael
 */
case class PinInfo(number: Int, pinState: PinState, enabledState: PinState) {
  val isEnabled = pinState == enabledState
}

case class PwmInfo(number: Int, pwm: Int)
package com.mle.piweb

import com.mle.pi.{Blaster, PinPlan, PwmValue}

import scala.collection.concurrent.TrieMap
import scala.util.Try

/**
 * @author Michael
 */
class MemoizingBlaster extends Blaster {
  protected val memory = TrieMap.empty[PinPlan, PwmValue]

  override def write(pin: PinPlan, value: PwmValue): Try[Unit] = super.write(pin, value).map(_ => memory.put(pin, value))

  def status = memory.toMap
}

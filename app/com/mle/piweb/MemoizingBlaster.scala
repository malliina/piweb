package com.mle.piweb

import com.mle.pi.Blaster

import scala.collection.concurrent.TrieMap
import scala.util.Try

/**
 * @author Michael
 */
class MemoizingBlaster extends Blaster {
  protected val memory = TrieMap.empty[Int, Int]

  override def write(pin: Int, value: Int): Try[Unit] = super.write(pin, value).map(_ => memory.put(pin, value))

  override def release(pin: Int): Try[Unit] = super.release(pin).map(_ => memory.put(pin, 0))

  def status = memory.toMap
}

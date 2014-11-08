package com.mle.rx

import rx.lang.scala.subjects.BehaviorSubject

/**
 * @author Michael
 */
object RxExtensions {

  implicit class RichBehaviorSubject[T](subject: BehaviorSubject[T]) {
    def firstItem: T = subject.head.toBlocking.head
  }

}

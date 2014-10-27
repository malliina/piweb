package com.mle.piweb

import controllers.{PinInfo, PwmInfo}

/**
 * @author Michael
 */
case class Snapshot(digitals: Seq[PinInfo], pwms: Seq[PwmInfo])

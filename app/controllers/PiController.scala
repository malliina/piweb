package controllers

import com.mle.play.controllers.AuthResult
import com.mle.play.ws.SyncAuth
import play.api.mvc.{Controller, RequestHeader}

/**
 * @author Michael
 */
trait PiController extends Controller with SyncAuth {
  type AuthSuccess = AuthResult

  override def authenticate(implicit req: RequestHeader): Option[AuthSuccess] = Some(AuthResult("test"))
}

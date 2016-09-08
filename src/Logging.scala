package dotty.website

import org.http4s._
import scalaz._
import scalaz.concurrent.Task

object Logging {

  def log(service: HttpService): HttpService = Service.lift { req =>
    service(req).attempt.flatMap {
      case -\/(error) =>
        System.err.println(s"An internal server error ocurred: $error")
        error.printStackTrace()
        Task.fail(error)
      case \/-(value) =>
        Task.now(value)
    }
  }

}

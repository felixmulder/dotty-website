package testy

import org.http4s._
import org.http4s.util._
import org.http4s.dsl._
import org.http4s.server.blaze._
import org.http4s.server.syntax._
import org.http4s.server.{Server, ServerApp}
import scalaz.concurrent.Task

object Conductor extends ServerApp
                    with GithubService
{
  val services = htmlService orElse githubApiService

  override def server(args: List[String]): Task[Server] =
    BlazeBuilder
      .bindHttp(8080, "localhost")
      .mountService(services, "/")
      .start
}

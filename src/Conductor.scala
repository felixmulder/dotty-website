package dotty.website

import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.ServerApp
import org.http4s.server.syntax._

object Conductor extends ServerApp
                    with SiteService
                    with GithubService
{
  val services = siteService || githubApiService

  override def server(args: List[String]) =
    BlazeBuilder
      .bindHttp(8080, "localhost")
      .mountService(services, "/")
      .start
}

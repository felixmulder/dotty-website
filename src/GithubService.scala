package testy

import org.http4s._
import org.http4s.dsl._
import org.http4s.headers.`Content-Type`
import org.http4s.MediaType._

import scalaz._
import scalaz.concurrent.Task

trait GithubService extends CirceCoder {
  import testy.util._
  import testy.util.repo._
  import testy.util.store._
  import testy.util.template._

  import testy.github._
  import testy.github.decoders._

  val githubApiService = HttpService {
    case req @ POST -> Root / "github" =>
      req.decode[GithubEvent] {
        case push: Push =>

          Config.projects.foreach { project =>
            val stat = for {
              _      <- SetStatus(push.after, project, Building)
              outDir <- Clone(project, push.after)
              status <- RunTests(project, outDir)
              _      <- SetStatus(push.after, project, status)
            } yield status

            Task.fork(stat).map {
              case Built(_, _, e) if e > 0 =>
                s"Failed to build ${project.name}"
              case Built(_, w, _) if w > 0 =>
                s"Could build ${project.name}, with warnings"
              case _: Built =>
                s"Successfully built ${project.name} against SNAPSHOT dotty"
            } runAsync {
              case \/-(status) => println(status)
              case -\/(e) => println(s"Error: $e")
            }
          }

          Ok(s"Building projects for new head ${push.after}")
        case _ =>
          Ok("Unsupported operation, backend only handles pushes to master")
      }
  }

  val htmlService = HttpService {
    case req @ GET -> "static" /: path =>
      StaticFile
        .fromResource(path.toString, Some(req))
        .fold(NotFound())(Task.now)

    case GET -> Root / "blog" =>
      for {
        posts <- GetPosts()
        res <- Ok(RenderHTML(twirl.html.BlogPost(posts.head).toString :: Nil)).withContentType(Some(`Content-Type`(`text/html`)))
      } yield res

    case GET -> Root / "thismonth" =>
      for {
        frag <- GetThisMonthInDotty()
        html <- RenderThisMonthInDotty(frag)
        res  <- Ok(html).withContentType(Some(`Content-Type`(`text/html`)))
      } yield res

    case GET -> Root =>
      for {
        builds <- GetBuildInfo
        tpl    <- RenderIndex(builds)
        res    <- Ok(tpl).withContentType(Some(`Content-Type`(`text/html`)))
      } yield res
  }
}

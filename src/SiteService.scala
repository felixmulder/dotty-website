package dotty.website

import org.http4s._
import org.http4s.dsl._
import org.http4s.headers.`Content-Type`
import org.http4s.MediaType._

import scalaz.concurrent.Task

trait SiteService {
  import dotty.website.util.store._
  import dotty.website.util.template._

  val siteService = Logging log HttpService {
    case req @ GET -> "static" /: path =>
      StaticFile
        .fromResource(path.toString, Some(req))
        .fold(NotFound())(Task.now)

    case GET -> Root / "blog" =>
      for {
        posts <- GetPosts()
        html  <- RenderBlog(posts)
        res   <- Ok(html).withContentType(Some(`Content-Type`(`text/html`)))
      } yield res

    case GET -> Root / "blog" / year / month / day / title =>
      for {
        posts <- GetPosts()
        Some(post) = posts.find(_.fileName == s"$year-$month-$day-$title.md")
        html  <- RenderBlogPost(post)
        res   <- Ok(html).withContentType(Some(`Content-Type`(`text/html`)))
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

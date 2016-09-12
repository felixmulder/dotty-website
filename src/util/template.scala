package dotty.website.util

import store.{Build, BlogPost}
import twirl.html._
import scalaz.concurrent.Task

object template {
  private[this] def indexFrags(builds: List[Build]) =
    LogoPage() :: FAQ() :: Features() :: BuildStatus(builds) ::
    GettingStarted() :: Nil

  def RenderHTML(fragments: List[String]): Task[String] =
    Task.delay(Index(fragments).toString)

  def RenderIndex(builds: List[Build]): Task[String] =
    RenderHTML(indexFrags(builds).map(_.toString))

  def RenderThisMonthInDotty(text: String): Task[String] =
    RenderHTML(ThisMonthInDotty(text).toString :: Nil)

  def RenderBlogPost(focused: BlogPost, archive: List[BlogPost]): Task[String] =
    RenderHTML(BlogIndex(focused, archive).toString :: Nil)

  def RenderBlog(posts: List[BlogPost]): Task[String] =
    RenderBlogPost(posts.head, posts)
}

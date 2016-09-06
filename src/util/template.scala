package testy.util

import store.Build
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
}

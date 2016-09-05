package testy.util

object template {
  import scalaz.concurrent.Task
  import store.Build

  def RenderBuildStatus(packs: List[Build]): Task[String] = Task.delay {
    try {
      twirl.html.BuildStatus(packs).toString
    } catch { case x: Throwable => println(x); throw x }
  }
}

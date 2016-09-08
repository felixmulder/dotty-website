package dotty.website.util

import io.circe._
import io.circe.parser._
import io.circe.generic.auto._
import repo.Project

case object Config {
  lazy val projects = {
    val is = getClass.getResourceAsStream("/projects.json")
    val source = scala.io.Source.fromInputStream(is)
    decode[List[Project]](source.mkString).getOrElse(Nil)
  }

  private val dottyId = 7035651
  private val dottyFullName = "lampepfl/dotty"

  val (repoId, repoFullName): (Int, String) =
    (sys.env.get("TESTID"), sys.env.get("TESTNAME")) match {
      case (Some(id), Some(name)) => (id.toInt, name)
      case _ => (dottyId, dottyFullName)
    }

  val secret = sys.env("SECRET")
}

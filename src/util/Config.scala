package testy.util

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
}

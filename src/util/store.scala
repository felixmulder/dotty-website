package testy.util

import scala.collection.immutable.SortedSet
import scalaz.concurrent.Task
import repo.{Project, Status}
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._

object store {

  case class Build(commitHash: String, projects: Map[String, ProjectAndStatus]) {
    override def equals(other: Any) = other match {
      case Build(hash,_) => hash == commitHash
      case _ => false
    }
  }

  case class ProjectAndStatus(project: Project, status: Status)

  implicit object BuildOrdering extends Ordering[Build] {
    def compare(a: Build, b: Build) = a.commitHash compare b.commitHash
  }

  sealed trait AccessInstruction
  case object Get extends AccessInstruction
  case class GetAndSet(getAndSet: Set[Build] => Set[Build]) extends AccessInstruction

  private[this] def accessStats(instr: AccessInstruction): Set[Build] = store.synchronized {
    import better.files._

    def jsonFile = file"builds.json"

    def get: Set[Build] = {
      val json = jsonFile.createIfNotExists().lines.mkString("")
      decode[List[Build]](json).getOrElse(Nil).toSet
    }

    def set(s: Set[Build]) = {
      jsonFile
        .createIfNotExists()
        .overwrite(s.toList.asJson.noSpaces)
      s
    }

    instr match {
      case Get => get
      case GetAndSet(getAndSet) => set(getAndSet(get))
    }
  }

  def SetStatus(hash: String, proj: Project, newStat: Status): Task[Unit] =
    Task.delay {
      accessStats(GetAndSet { set =>
        val build = set
          .find(_.commitHash == hash)
          .getOrElse {
            Build(hash, Map(proj.name -> ProjectAndStatus(proj, newStat)))
          }

        Set(build.copy(
          projects = build.projects + (proj.name -> ProjectAndStatus(proj, newStat))
        )) ++ set
      })
    }

  def GetStatus(proj: Project): Task[Option[Status]] = Task.delay {
    accessStats(Get).find(_.projects.contains(proj.name)).map(_.projects(proj.name).status)
  }

  def GetBuildInfo: Task[List[Build]] = Task.delay {
    accessStats(Get).toList
  }
}

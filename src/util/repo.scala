package dotty.website.util

import java.util.concurrent.{TimeoutException, ScheduledExecutorService}
import scala.concurrent.duration._
import scalaz.concurrent.Task
import org.eclipse.jgit.api._
import better.files._
import scalaz._
import scalaz.syntax.id._

object repo {
  case class Project(
    name: String,
    repo: String,
    branch: Option[String],
    testDir: Option[String],
    testCmd: String
  )

  sealed trait Status { def output: List[Line] }
  case object Building extends Status { val output = Nil }
  case object TimedOut extends Status { val output = Nil }
  case class Built(output: List[Line], warnings: Int, errors: Int) extends Status

  sealed trait Line { def line: String }
  case class Normal(line: String) extends Line
  case class Warning(line: String) extends Line
  case class Error(line: String) extends Line

  def Clone(proj: Project, hash: String): Task[File] = Task.delay {
    println(s"Cloning: $proj")

    val outDir = "cloned" / s"${proj.name}-$hash"

    // TODO: must be deleted if already exists, just for testing
    if (!outDir.notExists) outDir.delete()

    Git.cloneRepository()
      .setURI(proj.repo)
      .setBranch(proj.branch.getOrElse("master"))
      .setDirectory(outDir toJava)
      .call()

    println(s"Done cloning: $proj")
    outDir
  }

  def RunTests(proj: Project, dir: File): Task[Status] = Task.delay {
    import scala.io.Source

    println(s"Running tests for: $proj")
    var stdout: List[Line] = Nil
    var warnings = 0
    var errors = 0

    val testDir = proj.testDir.fold(dir)(dir / _)

    val pb =
      new java.lang.ProcessBuilder(proj.testCmd.split(" "): _*)
      .directory(testDir.toJava)
      .redirectErrorStream(true)


    val process = pb.start()

    Source.fromInputStream(process.getInputStream)
      .getLines
      .foreach{ orig =>
        orig.toLowerCase |> { out =>
          val line =
            if (out.contains("warning")) {
              warnings = warnings + 1
              Warning(orig)
            } else if (out.contains("error")) {
              errors = errors + 1
              Error(orig)
            } else Normal(orig)

          stdout = line :: stdout
        }
      }

    process.waitFor()
    println(s"Ran tests for: $proj")

    Built(stdout, warnings, errors)
  }
  .timed(10.minutes)
  .handleWith {
    case _: TimeoutException => Task.now(TimedOut)
  }
}

package dotty.website.util

import org.http4s.client.blaze._
//import org.pegdown.{PegDownProcessor, Extensions}
import scalaz.concurrent.Task
import repo.{Project, Status}
import scala.collection.immutable.SortedSet
import scala.concurrent.duration._
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import com.github.rjeschke.txtmark.{Processor, Configuration}

object store {

  sealed trait Cached[+I] { self =>
    def getOrElse[R >: I](default: => Task[R]): Task[R] = self match {
      case CacheItem(i, deadline) if deadline.hasTimeLeft => Task.now(i)
      case _ => default
    }
  }
  case object EmptyCache extends Cached[Nothing]
  case class CacheItem[I](item: I, validUntil: Deadline) extends Cached[I]

  case class Build(commitHash: String,
                   projects: Map[String, ProjectAndStatus],
                   time: Long = System.currentTimeMillis) {
    override def equals(other: Any) = other match {
      case Build(hash, _, _) => hash == commitHash
      case _ => false
    }
  }

  private[this] val markdownConfig =
    Configuration.builder.forceExtentedProfile.build

  private[this] def parseMarkdown(md: String): String =
    Processor.process(md, markdownConfig)

  case class BlogPost(title: String,
                      subTitle: Option[String],
                      author: String,
                      authorImg: Option[String],
                      date: String,
                      text: String,
                      fileName: String) {
    lazy val html = parseMarkdown(text)
  }

  case class ProjectAndStatus(project: Project, status: Status)

  implicit object BuildOrdering extends Ordering[Build] {
    def compare(a: Build, b: Build) = b.time compare a.time
  }

  sealed trait AccessInstruction
  case object Get extends AccessInstruction
  case class GetAndSet(getAndSet: Set[Build] => Set[Build]) extends AccessInstruction

  private[this] var builds: Set[Build] = _

  private[this] def accessStats(instr: AccessInstruction): Set[Build] =
    store.synchronized {
      import better.files._

      def jsonFile = file"builds.json"

      def get: Set[Build] = {
        if (builds == null) {
          val json = jsonFile.createIfNotExists().lines.mkString("")
          builds = decode[List[Build]](json).getOrElse(Nil).toSet
        }

        builds
      }

      def set(s: Set[Build]) = {
        jsonFile
          .createIfNotExists()
          .overwrite(s.toList.asJson.noSpaces)

        builds = s
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

  def GetBuildInfo: Task[List[Build]] =
    Task.delay(accessStats(Get).toList)

  private[this] val client = PooledHttp1Client()
  private[this] var thisMonthInDottyHtml: Cached[String] = EmptyCache

  def GetThisMonthInDotty(): Task[String] = store.synchronized {
    thisMonthInDottyHtml.getOrElse {
      val endpoint =
        "https://raw.githubusercontent.com/wiki/lampepfl/dotty/This-Month-in-Dotty.md"

      client.expect[String](endpoint).map { content =>
        val html = parseMarkdown(content)
        thisMonthInDottyHtml = CacheItem(html, 1.hour.fromNow)
        html
      }
    }
  }

  var posts: Cached[List[BlogPost]] = EmptyCache
  def GetPosts(): Task[List[BlogPost]] = store.synchronized {
    posts getOrElse Task.delay {
      import better.files._
      import org.yaml.snakeyaml.Yaml

      val File.Type.Directory(mdposts) = file"blog/"

      def parseYaml(str: String): Map[String, String] = try {
        import scala.collection.JavaConverters._
        new Yaml()
          .loadAll(str)
          .iterator.next
          .asInstanceOf[java.util.Map[String,String]].asScala
          .toMap
      } catch {
        case ex: Throwable =>
          System.err.println(ex)
          System.err.println(ex.getMessage)
          ex.printStackTrace()
          Map.empty
      }

      val newPosts = for {
        post <- mdposts.toList
        if post.name.endsWith(".md")
        date = post.name.substring(0, 10)
        text = post.contentAsString(scala.io.Codec.UTF8)
        postInfo = parseYaml(text)
      } yield BlogPost(
        postInfo("title"),
        postInfo.get("subTitle"),
        postInfo("author"),
        postInfo.get("authorImg").map("/static" + _),
        date,
        text.lines
          .dropWhile(_.trim != "---")
          .drop(1)
          .dropWhile(_.trim != "---")
          .drop(1)
          .mkString("\n"),
        post.name
      )
      val sortedPosts = newPosts.sortBy(_.date).reverse
      posts = CacheItem(sortedPosts, 1.hour.fromNow)
      sortedPosts
    }
  }
}

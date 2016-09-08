package testy

import org.http4s._
import org.http4s.dsl._

import scalaz._
import scalaz.concurrent.Task

trait GithubService extends CirceCoder {
  import testy.util.repo._
  import testy.util.store._
  import testy.util.Config

  import testy.github._
  import testy.github.decoders._

  def verifySecret(push: Push, request: Request)(onValid: Push => Task[Response]): Task[Response] = {
    import com.roundeights.hasher.{Hasher, Algo}

    val correctIdAndRepo =
      push.repository.id == Config.repoId &&
      push.repository.fullName == Config.repoFullName

    val signature =
      request
      .headers.get("X-Hub-Signature".ci)
      .map(_.value.split("=").last).getOrElse("")

    request.body.runLast.flatMap {
      case None => throw new InvalidBodyException("Couldn't parse body bytes")
      case Some(bodyBytes) =>
        val digest = Algo.hmac(Config.secret).sha1(bodyBytes.toArray)

        if (digest hash= signature) onValid(push)
        else throw new IllegalArgumentException("Incorrect signature for push request")
    }
  }

  val githubApiService = HttpService {
    case req @ POST -> Root / "github" =>
      req.decode[GithubEvent] {
        case p: Push =>
          verifySecret(p, req) { push =>
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
          }
        case _ =>
          Ok("Unsupported operation, backend only handles pushes to master")
      }
  }

}

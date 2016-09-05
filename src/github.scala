package testy

object github {
  import io.circe._
  import io.circe.generic.semiauto._

  sealed trait GithubEvent

  case class Push(
    ref:        String,
    before:     String,
    after:      String,
    created:    Boolean,
    deleted:    Boolean,
    forced:     Boolean,
    baseRef:    Option[String],
    compare:    String,
    commits:    List[Commit],
    headCommit: Option[Commit],
    repository: Repository,
    pusher:     Author,
    sender:     User
  ) extends GithubEvent

  case class PullRequestAction(
    action: String,
    number: Long,
    pullRequest: PullRequest,
    repository: Repository,
    sender: User
  ) extends GithubEvent

  case class PullRequest(
    url:            String,
    id:             Long,
    number:         Long,
    state:          String,
    locked:         Boolean,
    title:          String,
    user:           User,
    body:           String,
    assignee:       Option[User],
    head:           Head,
    base:           Head,
    merged:         Option[Boolean],
    mergeable:      Option[Boolean],
    mergeableState: Option[String],
    mergedBy:       Option[User]
  )

  case class Commit(
    id:        String,
    distinct:  Boolean,
    message:   String,
    timestamp: String,
    url:       String,
    author:    Author,
    committer: Author,
    added:     List[String],
    removed:   List[String],
    modified:  List[String]
  )

  case class Author(
    name:     String,
    email:    String,
    username: Option[String] = None,
    date:     Option[String] = None
  )

  case class Repository(
    id:            Long,
    name:          String,
    fullName:      String,
    owner:         User,
    isPrivate:     Boolean,
    description:   Option[String],
    isFork:        Boolean,
    homepage:      Option[String],
    language:      Option[String],
    defaultBranch: String,
    organization:  Option[User]
  )

  case class Head(
    label: String,
    ref:   String,
    sha:   String,
    user:  User,
    repo:  Option[Repository]
  )

  case class User(
    name:              Option[String],
    email:             Option[String],
    login:             Option[String],
    id:                Option[Long],
    avatarUrl:         Option[String],
    url:               Option[String],
    htmlUrl:           Option[String],
    userType:          Option[String],
    siteAdmin:         Option[Boolean]
  )

  object decoders {
    implicit lazy val authorEncoder = deriveEncoder[Author]
    implicit lazy val authorDecoder = deriveDecoder[Author]
    implicit lazy val userDecoder = Decoder.instance { cursor =>
      for {
        name              <- cursor.downField("name").as[Option[String]]
        email             <- cursor.downField("email").as[Option[String]]
        login             <- cursor.downField("login").as[Option[String]]
        id                <- cursor.downField("id").as[Option[Long]]
        avatarUrl         <- cursor.downField("avatar_url").as[Option[String]]
        url               <- cursor.downField("url").as[Option[String]]
        htmlUrl           <- cursor.downField("html_url").as[Option[String]]
        userType          <- cursor.downField("type").as[Option[String]]
        siteAdmin         <- cursor.downField("site_admin").as[Option[Boolean]]
      } yield User(
        name, email, login, id, avatarUrl, url, htmlUrl, userType, siteAdmin
      )
    }
    implicit lazy val repositoryDecoder = Decoder.instance { cursor =>
      for {
        id            <- cursor.downField("id").as[Long]
        name          <- cursor.downField("name").as[String]
        fullName      <- cursor.downField("full_name").as[String]
        owner         <- cursor.downField("owner").as[User]
        isPrivate     <- cursor.downField("private").as[Boolean]
        description   <- cursor.downField("description").as[Option[String]]
        isFork        <- cursor.downField("fork").as[Boolean]
        homePage      <- cursor.downField("homepage").as[Option[String]]
        language      <- cursor.downField("language").as[Option[String]]
        defaultBranch <- cursor.downField("default_branch").as[String]
        organization  <- cursor.downField("organization").as[Option[User]]
      } yield Repository(
        id, name, fullName, owner, isPrivate, description, isFork, homePage,
        language, defaultBranch, organization
      )
    }
    implicit lazy val headDecoder = deriveDecoder[Head]
    implicit lazy val commitDecoder = deriveDecoder[Commit]
    implicit lazy val pushDecoder = Decoder.instance { cursor =>
      for {
        ref        <- cursor.downField("ref").as[String]
        before     <- cursor.downField("before").as[String]
        after      <- cursor.downField("after").as[String]
        created    <- cursor.downField("created").as[Boolean]
        deleted    <- cursor.downField("deleted").as[Boolean]
        forced     <- cursor.downField("forced").as[Boolean]
        baseRef    <- cursor.downField("base_ref").as[Option[String]]
        compare    <- cursor.downField("compare").as[String]
        commits    <- cursor.downField("commits").as[List[Commit]]
        headCommit <- cursor.downField("head_commit").as[Option[Commit]]
        repository <- cursor.downField("repository").as[Repository]
        pusher     <- cursor.downField("pusher").as[Author]
        sender     <- cursor.downField("sender").as[User]
      } yield Push(
        ref, before, after, created, deleted, forced, baseRef, compare,
        commits, headCommit, repository, pusher, sender
      )
    }
    implicit lazy val pullRequestDecoder = Decoder.instance { cursor =>
      for {
        url             <- cursor.downField("url").as[String]
        id              <- cursor.downField("id").as[Long]
        number          <- cursor.downField("number").as[Long]
        state           <- cursor.downField("state").as[String]
        locked          <- cursor.downField("locked").as[Boolean]
        title           <- cursor.downField("title").as[String]
        user            <- cursor.downField("user").as[User]
        body            <- cursor.downField("body").as[String]
        assignee        <- cursor.downField("assignee").as[Option[User]]
        head            <- cursor.downField("head").as[Head]
        base            <- cursor.downField("base").as[Head]
        merged          <- cursor.downField("merged").as[Option[Boolean]]
        mergeable       <- cursor.downField("mergeable").as[Option[Boolean]]
        mergeableState  <- cursor.downField("mergeable_state").as[Option[String]]
        mergedBy        <- cursor.downField("merged_by").as[Option[User]]
      } yield PullRequest(
        url, id, number, state, locked, title, user, body, assignee, head,
        base, merged, mergeable, mergeableState, mergedBy
      )
    }
    implicit lazy val pullRequestActionDecoder = Decoder.instance { cursor =>
      for {
        action      <- cursor.downField("action").as[String]
        number      <- cursor.downField("number").as[Long]
        pullRequest <- cursor.downField("pull_request").as[PullRequest]
        repository  <- cursor.downField("repository").as[Repository]
        sender      <- cursor.downField("sender").as[User]
      } yield PullRequestAction(action, number, pullRequest, repository, sender)
    }
    implicit lazy val githubHook: Decoder[GithubEvent] = Decoder.instance { cursor =>
      cursor.as[PullRequestAction] orElse cursor.as[Push]
    }
  }
}

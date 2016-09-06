lazy val buildSettings = Seq(
  organization := "ch.epfl.scala",
  version :=  "0.1.0-SNAPSHOT",
  scalaVersion :=  "2.11.8",
  updateOptions := updateOptions.value.withCachedResolution(true)
)

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture",
  "-Xlint"
)

lazy val commonSettings = Seq(
  triggeredMessage in ThisBuild := Watched.clearWhenTriggered,
  scalacOptions in (Compile, console) := compilerOptions :+ "-Yrepl-class-based",
  testOptions in Test += Tests.Argument("-oD")
)

lazy val allSettings = buildSettings ++ commonSettings

lazy val testy = project.in(file("."))
  .settings(allSettings)
  .settings(
    resourceDirectory in Compile := baseDirectory.value / "resources",
    scalaSource       in Compile := baseDirectory.value / "src",
    scalaSource       in Test    := baseDirectory.value / "test",
    sourceDirectories in (Compile, TwirlKeys.compileTemplates) :=
      (unmanagedSourceDirectories in Compile).value,


    resolvers ++= Seq(
      "jgit-repo" at "http://download.eclipse.org/jgit/maven"
    ),

    libraryDependencies ++= {
      val http4sVersion = "0.14.4a"
      val finchVersion  = "0.11.0-M2"
      val circeVersion  = "0.4.1"

      val scalaDeps = Seq(
        "com.github.pathikrit" %% "better-files"        % "2.16.0",
        "io.circe"             %% "circe-core"          % circeVersion,
        "io.circe"             %% "circe-generic"       % circeVersion,
        "io.circe"             %% "circe-parser"        % circeVersion,
        "org.http4s"           %% "http4s-dsl"          % http4sVersion,
        "org.http4s"           %% "http4s-circe"        % http4sVersion,
        "org.http4s"           %% "http4s-blaze-server" % http4sVersion,
        "org.http4s"           %% "http4s-blaze-client" % http4sVersion
      )

      val javaDeps = Seq(
        "org.pegdown"      % "pegdown"          % "1.6.0",
        "org.eclipse.jgit" % "org.eclipse.jgit" % "4.4.1.201607150455-r",
        "org.slf4j"        % "slf4j-nop"        % "1.6.4",
        "org.yaml"         % "snakeyaml"        % "1.17"
      )

      scalaDeps ++ javaDeps
    }
  )
  .enablePlugins(SbtTwirl)

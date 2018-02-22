/*
scalafmt: {
  style = defaultWithAlign
  maxColumn = 150
  align.tokens = [
    { code = "=>", owner = "Case" }
    { code = "?", owner = "Case" }
    { code = "extends", owner = "Defn.(Class|Trait|Object)" }
    { code = "//", owner = ".*" }
    { code = "{", owner = "Template" }
    { code = "}", owner = "Template" }
    { code = ":=", owner = "Term.ApplyInfix" }
    { code = "++=", owner = "Term.ApplyInfix" }
    { code = "+=", owner = "Term.ApplyInfix" }
    { code = "%", owner = "Term.ApplyInfix" }
    { code = "%%", owner = "Term.ApplyInfix" }
    { code = "%%%", owner = "Term.ApplyInfix" }
    { code = "->", owner = "Term.ApplyInfix" }
    { code = "?", owner = "Term.ApplyInfix" }
    { code = "<-", owner = "Enumerator.Generator" }
    { code = "?", owner = "Enumerator.Generator" }
    { code = "=", owner = "(Enumerator.Val|Defn.(Va(l|r)|Def|Type))" }
  ]
}
 */

// Dependency versions
val akkaVersion      = "2.5.9"
val akkaHttpVersion  = "10.0.11"
val catsVersion      = "1.0.1"
val circeVersion     = "0.9.1"
val scalaTestVersion = "3.0.5"
val shapelessVersion = "2.3.3"
val jenaVersion      = "3.6.0"

// Nexus dependency versions
val serviceVersion = "0.10.0"

// Dependency modules
lazy val akkaStream   = "com.typesafe.akka" %% "akka-stream"    % akkaVersion
lazy val akkaHttpCore = "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion
lazy val akkaHttp     = "com.typesafe.akka" %% "akka-http"      % akkaHttpVersion
lazy val catsCore     = "org.typelevel"     %% "cats-core"      % catsVersion
lazy val circeCore    = "io.circe"          %% "circe-core"     % circeVersion
lazy val jenaArq      = "org.apache.jena"   % "jena-arq"        % jenaVersion
lazy val shapeless    = "com.chuusai"       %% "shapeless"      % shapelessVersion
lazy val scalaTest    = "org.scalatest"     %% "scalatest"      % scalaTestVersion

// Nexus dependency modules
lazy val kamon = "ch.epfl.bluebrain.nexus" %% "service-kamon" % serviceVersion

// Projects
lazy val core = project
  .in(file("modules/core"))
  .settings(
    name       := "admin-core",
    moduleName := "admin-core",
    libraryDependencies ++= Seq(
      akkaStream,
      akkaHttpCore,
      catsCore,
      circeCore,
      jenaArq,
      shapeless,
      scalaTest % Test,
    )
  )

lazy val service = project
  .in(file("modules/service"))
  .enablePlugins(InstrumentationPlugin)
  .dependsOn(core)
  .settings(
    name       := "admin-service",
    moduleName := "admin-service",
    libraryDependencies ++= Seq(
      kamon,
      akkaHttp,
      scalaTest % Test,
    )
  )

lazy val root = project
  .in(file("."))
  .settings(noPublish)
  .settings(
    name       := "admin",
    moduleName := "admin"
  )
  .aggregate(core, service)

/* ********************************************************
 ******************** Grouped Settings ********************
 **********************************************************/

lazy val noPublish = Seq(
  publishLocal    := {},
  publish         := {},
  publishArtifact := false,
)

inThisBuild(
  List(
    homepage := Some(url("https://github.com/BlueBrain/nexus-admin")),
    licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    scmInfo  := Some(ScmInfo(url("https://github.com/BlueBrain/nexus-admin"), "scm:git:git@github.com:BlueBrain/nexus-admin.git")),
    developers := List(
      Developer("bogdanromanx", "Bogdan Roman", "noreply@epfl.ch", url("https://bluebrain.epfl.ch/")),
      Developer("hygt", "Henry Genet", "noreply@epfl.ch", url("https://bluebrain.epfl.ch/")),
      Developer("umbreak", "Didac Montero Mendez", "noreply@epfl.ch", url("https://bluebrain.epfl.ch/")),
      Developer("wwajerowicz", "Wojtek Wajerowicz", "noreply@epfl.ch", url("https://bluebrain.epfl.ch/")),
    ),
    // These are the sbt-release-early settings to configure
    releaseEarlyWith              := BintrayPublisher,
    releaseEarlyNoGpg             := true,
    releaseEarlyEnableSyncToMaven := false,
  ))

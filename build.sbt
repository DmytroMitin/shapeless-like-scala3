ThisBuild / scalaVersion := "3.9.0"

ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-release:17",
  "-unchecked"
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "shapeless-like-scala3",
    libraryDependencies += "org.scalameta" %% "munit" % "1.2.4" % Test,
    Test / unmanagedSourceDirectories += baseDirectory.value / "examples"
  )

package ch.epfl.bluebrain.nexus.admin.core.projects

import java.time.Clock

import cats.instances.try_._
import ch.epfl.bluebrain.nexus.admin.core.CallerCtx._
import ch.epfl.bluebrain.nexus.admin.core.Fault.{CommandRejected, Unexpected}
import ch.epfl.bluebrain.nexus.admin.core.config.AppConfig.ProjectsConfig
import ch.epfl.bluebrain.nexus.admin.core.projects.Project.{Config, LoosePrefixMapping, ProjectValue}
import ch.epfl.bluebrain.nexus.admin.core.resources.ResourceRejection._
import ch.epfl.bluebrain.nexus.admin.core.resources.ResourceState._
import ch.epfl.bluebrain.nexus.admin.core.types.Ref._
import ch.epfl.bluebrain.nexus.admin.core.types.RefVersioned
import ch.epfl.bluebrain.nexus.admin.ld.Const.{nxv, rdf}
import ch.epfl.bluebrain.nexus.admin.refined.permissions._
import ch.epfl.bluebrain.nexus.admin.refined.project._
import ch.epfl.bluebrain.nexus.commons.iam.acls.Permission._
import ch.epfl.bluebrain.nexus.commons.iam.acls.{Permission, Permissions}
import ch.epfl.bluebrain.nexus.commons.iam.identity.Caller.AnonymousCaller
import ch.epfl.bluebrain.nexus.commons.iam.identity.Identity.Anonymous
import ch.epfl.bluebrain.nexus.commons.test.Randomness
import ch.epfl.bluebrain.nexus.sourcing.mem.MemoryAggregate
import ch.epfl.bluebrain.nexus.sourcing.mem.MemoryAggregate._
import eu.timepit.refined.api.RefType.{applyRef, refinedRefType}
import eu.timepit.refined.auto._
import io.circe.Json
import org.scalatest.{Matchers, TryValues, WordSpecLike}
import ch.epfl.bluebrain.nexus.admin.ld.PrefixMapping._
import io.circe.syntax._

import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

class ProjectsSpec extends WordSpecLike with Matchers with TryValues with Randomness {

  private implicit val caller: AnonymousCaller = AnonymousCaller(Anonymous())
  private implicit val clock: Clock            = Clock.systemUTC
  private implicit val config: ProjectsConfig  = ProjectsConfig(3 seconds, "https://nexus.example.ch/v1/projects/", 100000L)
  private val aggProject                       = MemoryAggregate("projects")(Initial, next, eval).toF[Try]
  private val projects                         = Projects(aggProject)

  def genJson(): Json = Json.obj("key" -> Json.fromString(genString()))

  def genReference(length: Int = 9): ProjectReference =
    refinedRefType.unsafeWrap(genString(length = length, Vector.range('a', 'z') ++ Vector.range('0', '9')))

  def genProjectValue(): ProjectValue = {
    val prefixMappings = List(
      LoosePrefixMapping(randomPrefix(), refinedRefType.unsafeRewrap(nxv.namespaceBuilder)),
      LoosePrefixMapping(randomPrefix(), refinedRefType.unsafeRewrap(rdf.namespaceBuilder))
    )
    ProjectValue(Some(genString()), None, prefixMappings, Config(genInt().toLong))
  }
  trait Context {
    val id: ProjectReference = genReference()
    val prefixMappings = List(
      LoosePrefixMapping(randomPrefix(), refinedRefType.unsafeRewrap(nxv.namespaceBuilder)),
      LoosePrefixMapping(randomPrefix(), refinedRefType.unsafeRewrap(rdf.namespaceBuilder))
    )
    val value: ProjectValue = genProjectValue()
  }

  private implicit class EitherSyntax[A](either: Either[String, A]) {
    def toPermTry: Try[A] =
      either.fold(str => Failure(Unexpected(str)), Success(_))
  }

  "A Project bundle" should {
    implicit val hasWrite: HasWriteProjects =
      applyRef[HasWriteProjects](Permissions(Read, Permission("projects/write"))).toPermTry.success.value
    implicit val hasOwn: HasOwnProjects =
      applyRef[HasOwnProjects](Permissions(Read, Permission("projects/own"))).toPermTry.success.value
    implicit val hasRead: HasReadProjects =
      applyRef[HasReadProjects](Permissions(Read, Permission("projects/read"))).toPermTry.success.value

    "create a new project" in new Context {
      projects.create(id, value.asJson).success.value shouldEqual RefVersioned(id, 1L)
      projects.fetch(id).success.value shouldEqual Some(Project(id, 1L, value, value.asJson, deprecated = false))
    }

    "update a resource" in new Context {
      val updatedValue: ProjectValue = genProjectValue()
      projects.create(id, value.asJson).success.value shouldEqual RefVersioned(id, 1L)
      projects.update(id, 1L, updatedValue.asJson).success.value shouldEqual RefVersioned(id, 2L)
      projects.fetch(id).success.value shouldEqual Some(
        Project(id, 2L, updatedValue, updatedValue.asJson, deprecated = false))
    }

    "deprecate a resource" in new Context {
      projects.create(id, value.asJson).success.value shouldEqual RefVersioned(id, 1L)
      projects.deprecate(id, 1L).success.value shouldEqual RefVersioned(id, 2L)
      projects.fetch(id).success.value shouldEqual Some(Project(id, 2L, value, value.asJson, deprecated = true))
    }

    "fetch old revision of a resource" in new Context {
      val updatedValue: ProjectValue = genProjectValue()
      projects.create(id, value.asJson).success.value shouldEqual RefVersioned(id, 1L)
      projects.update(id, 1L, updatedValue.asJson).success.value shouldEqual RefVersioned(id, 2L)

      projects.fetch(id, 2L).success.value shouldEqual Some(
        Project(id, 2L, updatedValue, updatedValue.asJson, deprecated = false))
      projects.fetch(id, 1L).success.value shouldEqual Some(Project(id, 1L, value, value.asJson, deprecated = false))

    }

    "return None when fetching a revision that does not exist" in new Context {
      projects.create(id, value.asJson).success.value shouldEqual RefVersioned(id, 1L)
      projects.fetch(id, 10L).success.value shouldEqual None
    }

    "prevent double deprecations" in new Context {
      projects.create(id, value.asJson).success.value shouldEqual RefVersioned(id, 1L)
      projects.deprecate(id, 1L).success.value shouldEqual RefVersioned(id, 2L)
      projects.deprecate(id, 2L).failure.exception shouldEqual CommandRejected(ResourceIsDeprecated)
    }

    "prevent updating when deprecated" in new Context {
      val updatedValue: ProjectValue = genProjectValue()
      projects.create(id, value.asJson).success.value shouldEqual RefVersioned(id, 1L)
      projects.deprecate(id, 1L).success.value shouldEqual RefVersioned(id, 2L)
      projects.update(id, 2L, updatedValue.asJson).failure.exception shouldEqual CommandRejected(ResourceIsDeprecated)
    }

    "prevent updating to non existing project" in new Context {
      projects.create(id, value.asJson).success.value shouldEqual RefVersioned(id, 1L)
      projects.update(genReference(), 2L, value.asJson).failure.exception shouldEqual CommandRejected(
        ResourceDoesNotExists)
    }

    "prevent updating with incorrect rev" in new Context {
      val updatedValue: ProjectValue = genProjectValue()
      projects.create(id, value.asJson).success.value shouldEqual RefVersioned(id, 1L)
      projects.update(id, 2L, updatedValue.asJson).failure.exception shouldEqual CommandRejected(
        IncorrectRevisionProvided)
    }

    "prevent deprecation with incorrect rev" in new Context {
      projects.create(id, value.asJson).success.value shouldEqual RefVersioned(id, 1L)
      projects.deprecate(id, 2L).failure.exception shouldEqual CommandRejected(IncorrectRevisionProvided)
    }

    "prevent deprecation to non existing project incorrect rev" in new Context {
      projects.create(id, value.asJson).success.value shouldEqual RefVersioned(id, 1L)
      projects.deprecate(genReference(), 1L).failure.exception shouldEqual CommandRejected(ResourceDoesNotExists)
    }

    "project cannot be used from a child resource when does not exist" in new Context {
      projects.validateUnlocked(genReference()).failure.exception shouldEqual CommandRejected(
        ParentResourceDoesNotExists)
    }

    "project cannot be used from a child resource when it is already deprecated" in new Context {
      projects.create(id, value.asJson).success.value shouldEqual RefVersioned(id, 1L)
      projects.deprecate(id, 1L).success.value shouldEqual RefVersioned(id, 2L)
      projects.validateUnlocked(id).failure.exception shouldEqual CommandRejected(ResourceIsDeprecated)
    }

    "project can be used from a child resource" in new Context {
      projects.create(id, value.asJson).success.value shouldEqual RefVersioned(id, 1L)
      projects.validateUnlocked(id).success.value shouldEqual (())
    }
  }
}

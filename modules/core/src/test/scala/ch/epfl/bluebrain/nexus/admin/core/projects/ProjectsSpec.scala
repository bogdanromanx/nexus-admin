package ch.epfl.bluebrain.nexus.admin.core.projects

import java.time.Clock

import cats.instances.try_._
import ch.epfl.bluebrain.nexus.admin.core.TestHeplers
import ch.epfl.bluebrain.nexus.admin.core.projects.Project.Config
import ch.epfl.bluebrain.nexus.admin.core.resources.Ref
import ch.epfl.bluebrain.nexus.admin.core.resources.ResourceState._
import ch.epfl.bluebrain.nexus.commons.iam.identity.Caller.AnonymousCaller
import ch.epfl.bluebrain.nexus.admin.core.CallerCtx._
import ch.epfl.bluebrain.nexus.admin.core.Fault.CommandRejected
import ch.epfl.bluebrain.nexus.admin.core.resources.ResourceRejection._
import ch.epfl.bluebrain.nexus.commons.iam.identity.Identity.Anonymous
import ch.epfl.bluebrain.nexus.sourcing.mem.MemoryAggregate
import ch.epfl.bluebrain.nexus.sourcing.mem.MemoryAggregate._
import org.scalatest.{Matchers, TryValues, WordSpecLike}

import scala.util.Try

class ProjectsSpec extends WordSpecLike with Matchers with TryValues with TestHeplers {

  private implicit val caller: AnonymousCaller = AnonymousCaller(Anonymous())
  private implicit val clock: Clock            = Clock.systemUTC
  private val aggProject                       = MemoryAggregate("projects")(Initial, next, eval).toF[Try]
  private val projects                         = Projects(aggProject)

  trait Context {
    val id: Label = Label(genName()).get
    val value     = Project.Value(genJson(), Config(10L))
  }

  "A Project bundle" should {

    "create a new project" in new Context {
      projects.create(id, value).success.value shouldEqual Ref(id, 1L)
      projects.fetch(id).success.value shouldEqual Some(Project(Ref(id, 1L), value, deprecated = false))

    }

    "update a resource" in new Context {
      val updatedValue: Project.Value = value.copy(`@context` = genJson(), config = Config(20L))
      projects.create(id, value).success.value shouldEqual Ref(id, 1L)
      projects.update(id, 1L, updatedValue).success.value shouldEqual Ref(id, 2L)
      projects.fetch(id).success.value shouldEqual Some(Project(Ref(id, 2L), updatedValue, deprecated = false))
    }

    "deprecate a resource" in new Context {
      projects.create(id, value).success.value shouldEqual Ref(id, 1L)
      projects.deprecate(id, 1L).success.value shouldEqual Ref(id, 2L)
      projects.fetch(id).success.value shouldEqual Some(Project(Ref(id, 2L), value, deprecated = true))
    }

    "fetch old revision of a resource" in new Context {
      val updatedValue: Project.Value = value.copy(`@context` = genJson(), config = Config(20L))
      projects.create(id, value).success.value shouldEqual Ref(id, 1L)
      projects.update(id, 1L, updatedValue).success.value shouldEqual Ref(id, 2L)

      projects.fetch(id, 2L).success.value shouldEqual Some(Project(Ref(id, 2L), updatedValue, deprecated = false))
      projects.fetch(id, 1L).success.value shouldEqual Some(Project(Ref(id, 1L), value, deprecated = false))

    }

    "return None when fetching a revision that does not exist" in new Context {
      projects.create(id, value).success.value shouldEqual Ref(id, 1L)
      projects.fetch(id, 10L).success.value shouldEqual None
    }

    "prevent double deprecations" in new Context {
      projects.create(id, value).success.value shouldEqual Ref(id, 1L)
      projects.deprecate(id, 1L).success.value shouldEqual Ref(id, 2L)
      projects.deprecate(id, 2L).failure.exception shouldEqual CommandRejected(ResourceIsDeprecated)
    }

    "prevent updating when deprecated" in new Context {
      val updatedValue: Project.Value = value.copy(`@context` = genJson(), config = Config(20L))
      projects.create(id, value).success.value shouldEqual Ref(id, 1L)
      projects.deprecate(id, 1L).success.value shouldEqual Ref(id, 2L)
      projects.update(id, 2L, updatedValue).failure.exception shouldEqual CommandRejected(ResourceIsDeprecated)
    }

    "prevent updating to non existing project" in new Context {
      val label: Label = Label(genName()).get
      projects.create(id, value).success.value shouldEqual Ref(id, 1L)
      projects.update(label, 2L, value).failure.exception shouldEqual CommandRejected(ResourceDoesNotExists)

    }

    "prevent updating with incorrect rev" in new Context {
      val updatedValue: Project.Value = value.copy(`@context` = genJson(), config = Config(20L))
      projects.create(id, value).success.value shouldEqual Ref(id, 1L)
      projects.update(id, 2L, updatedValue).failure.exception shouldEqual CommandRejected(IncorrectRevisionProvided)
    }

    "prevent deprecation with incorrect rev" in new Context {
      projects.create(id, value).success.value shouldEqual Ref(id, 1L)
      projects.deprecate(id, 2L).failure.exception shouldEqual CommandRejected(IncorrectRevisionProvided)
    }

    "prevent deprecation to non existing project incorrect rev" in new Context {
      val label: Label = Label(genName()).get
      projects.create(id, value).success.value shouldEqual Ref(id, 1L)
      projects.deprecate(label, 1L).failure.exception shouldEqual CommandRejected(ResourceDoesNotExists)
    }
  }
}

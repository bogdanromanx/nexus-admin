package ch.epfl.bluebrain.nexus.admin.core.resources

import ch.epfl.bluebrain.nexus.admin.core.resources.ResourceCommand._
import ch.epfl.bluebrain.nexus.admin.core.resources.ResourceEvent.{ResourceCreated, ResourceDeprecated, ResourceUpdated}
import ch.epfl.bluebrain.nexus.admin.core.resources.ResourceRejection._
import ch.epfl.bluebrain.nexus.commons.iam.acls.Meta
import io.circe.Json

sealed trait ResourceState extends Product with Serializable

object ResourceState {

  final case object Initial extends ResourceState
  final case class Current(ref: UriRef, meta: Meta, value: Json, deprecated: Boolean)
      extends ResourceState

  def next(state: ResourceState, event: ResourceEvent): ResourceState = {

    (state, event: ResourceEvent) match {
      case (Initial, ResourceCreated(id, meta, _, value)) =>
        Current(id, meta, value, deprecated = false)
      // $COVERAGE-OFF$
      case (Initial, _) => Initial
      // $COVERAGE-ON$
      case (c @ Current(_, _, _, true), _) => c
      case (c, _: ResourceCreated)            => c
      case (c: Current, ResourceUpdated(id, meta, _, value)) =>
        c.copy(ref = c.ref.copy(rev = id.rev), meta = meta, value = value)
      case (c: Current, ResourceDeprecated(id, meta, _)) =>
        c.copy(ref = c.ref.copy(rev = id.rev), meta = meta, deprecated = true)
    }
  }

  def eval(state: ResourceState, cmd: ResourceCommand): Either[ResourceRejection, ResourceEvent] = {

    def createResource(c: CreateResource): Either[ResourceRejection, ResourceEvent] =
      state match {
        case Initial => Right(ResourceCreated(Ref(c.id, 1L), c.meta, c.tags, c.value))
        case _       => Left(ResourceAlreadyExists)
      }

    def updateResource(c: UpdateResource): Either[ResourceRejection, ResourceEvent] = state match {
      case Initial                                    => Left(ResourceDoesNotExists)
      case Current(id, _, _, _) if id.rev != c.rev => Left(IncorrectRevisionProvided)
      case Current(_, _, _, true)                  => Left(ResourceIsDeprecated)
      case s: Current                                 => Right(ResourceUpdated(s.ref.copy(rev = s.ref.rev + 1), c.meta, c.tags, c.value))
    }

    def deprecateResource(c: DeprecateResource): Either[ResourceRejection, ResourceEvent] = state match {
      case Initial                                    => Left(ResourceDoesNotExists)
      case Current(id, _, _, _) if id.rev != c.rev => Left(IncorrectRevisionProvided)
      case Current(_, _, _, true)                  => Left(ResourceIsDeprecated)
      case s: Current                                 => Right(ResourceDeprecated(s.ref.copy(rev = s.ref.rev + 1), c.meta, c.tags))
    }

    cmd match {
      case c: CreateResource    => createResource(c)
      case c: UpdateResource    => updateResource(c)
      case c: DeprecateResource => deprecateResource(c)
    }
  }
}

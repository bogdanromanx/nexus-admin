package ch.epfl.bluebrain.nexus.admin.core.resources

import akka.http.scaladsl.model.Uri
import cats.syntax.all._
import cats.{MonadError, Show}
import ch.epfl.bluebrain.nexus.admin.core.CallerCtx
import ch.epfl.bluebrain.nexus.admin.core.Fault.{CommandRejected, Unexpected}
import ch.epfl.bluebrain.nexus.admin.ld.Id._
import ch.epfl.bluebrain.nexus.admin.core.resources.ResourceCommand._
import ch.epfl.bluebrain.nexus.admin.core.resources.ResourceRejection._
import ch.epfl.bluebrain.nexus.admin.core.resources.ResourceState._
import ch.epfl.bluebrain.nexus.admin.core.resources.Resources.Agg
import ch.epfl.bluebrain.nexus.admin.ld.Id
import ch.epfl.bluebrain.nexus.sourcing.Aggregate
import com.github.ghik.silencer.silent
import io.circe.Json
import journal.Logger
import shapeless.Typeable

abstract class Resources[F[_], A: Id: Show](agg: Agg[F])(implicit
                                                         F: MonadError[F, Throwable],
                                                         A: Typeable[A]) {

  private val logger = Logger[this.type]

  def toPersistedId(a: A): F[String] = toPersistedId(a.uri)

  def toPersistedId(uri: Uri): F[String]

  private implicit def tupleToString(tuple: (String, String)): String =
    tuple match { case (id, parent) => s"${id}_$parent" }

  @silent
  def validate(id: A, value: Json): F[Unit] =
    F.pure(())

  def validateUnlocked(id: A, parent: A): F[Unit] =
    agg.currentState(id.show -> parent.show) flatMap {
      case Initial                => F.raiseError(CommandRejected(ParentResourceDoesNotExists))
      case Current(_, _, _, true) => F.raiseError(CommandRejected(ResourceIsDeprecated))
      case _                      => F.pure(())
    }

  def create(id: A, value: Json, tags: Set[String] = Set.empty)(implicit ctx: CallerCtx): F[UriRef] =
    for {
      pId <- toPersistedId(id)
      _   <- validate(id, value)
      r   <- evaluate(CreateResource(id.uri, ctx.meta, tags + pId, value), pId, s"Create res '$id'")
    } yield r.ref

  def update(refId: Ref[A], value: Json, tags: Set[String] = Set.empty)(implicit ctx: CallerCtx): F[UriRef] =
    for {
      pId <- toPersistedId(refId.id)
      _   <- validate(refId.id, value)
      r   <- evaluate(UpdateResource(refId.id.uri, refId.rev, ctx.meta, tags + pId, value), pId, s"Update res '$refId'")
    } yield r.ref

  def deprecate(refId: Ref[A], tags: Set[String] = Set.empty)(implicit ctx: CallerCtx): F[UriRef] =
    for {
      pId <- toPersistedId(refId.id)
      r   <- evaluate(DeprecateResource(refId.id.uri, refId.rev, ctx.meta, tags + pId), pId, s"Deprecate res '$refId'")
    } yield r.ref

  def fetch(id: A): F[Option[UriResource]] =
    for {
      pId   <- toPersistedId(id)
      state <- agg.currentState(pId)
    } yield {
      state match {
        case Initial    => None
        case c: Current => Some(Resource(c.ref, c.value, c.deprecated))
      }
    }

  def fetch(refId: Ref[A]): F[Option[UriResource]] =
    for {
      pId   <- toPersistedId(refId.id)
      state <- stateAt(pId, refId.rev)
    } yield {
      state match {
        case c: Current if c.ref.rev == refId.rev => Some(Resource(c.ref, c.value, c.deprecated))
        case _                                    => None
      }
    }

  private def stateAt(pId: String, rev: Long): F[ResourceState] =
    agg.foldLeft[ResourceState](pId, Initial) {
      case (state, ev) if ev.ref.rev <= rev => next(state, ev)
      case (state, _)                       => state
    }

  private def evaluate(cmd: ResourceCommand, pId: String, intent: => String): F[Current] =
    F.pure {
      logger.debug(s"$intent: evaluating command '$cmd' for resource type '${A.describe}'")
    } flatMap { _ =>
      agg.eval(pId, cmd)
    } flatMap {
      case Left(rejection) =>
        logger.debug(s"$intent: command '$cmd' was rejected due to '$rejection' for resource type '${A.describe}'")
        F.raiseError(CommandRejected(rejection))
      // $COVERAGE-OFF$
      case Right(s @ Initial) =>
        logger.error(
          s"$intent: command '$cmd' evaluation failed, received an '$s' state for resource type '${A.describe}'")
        F.raiseError(
          Unexpected(
            s"Unexpected Initial state as outcome of evaluating command '$cmd' for resource type '${A.describe}'"))
      // $COVERAGE-ON$
      case Right(state: Current) =>
        logger.debug(
          s"$intent: command '$cmd' evaluation succeeded, generated state: '$state' for resource type '${A.describe}'")
        F.pure(state)
    }
}

object Resources {

  type Agg[F[_]] = Aggregate[F] {
    type Identifier = String
    type Event      = ResourceEvent
    type State      = ResourceState
    type Command    = ResourceCommand
    type Rejection  = ResourceRejection
  }
}
//  def create[A: Id](id: A): F[Ref[A]] = ???
//
//
//  def fetch[A: Id](id: A): F[Resource[A]] = {
//    val x: F[Resource[Uri]] = _
//
//    x.flatMap(withTypedId[A, Uri])
//  }
//
//  private def withTypedId[A: Id, B](resource: Resource[B]): F[Resource[A]] =
//    resource.ref.toId[A] match {
//      case Some(refa) => F.pure(resource.copy(ref = refa))
//      case _ => F.raiseError(new IllegalStateException("unknown id"))
//    }

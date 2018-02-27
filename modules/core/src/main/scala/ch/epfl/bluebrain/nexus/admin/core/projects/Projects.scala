package ch.epfl.bluebrain.nexus.admin.core.projects

import akka.http.scaladsl.model.Uri
import cats.MonadError
import cats.syntax.flatMap._
import cats.syntax.show._
import ch.epfl.bluebrain.nexus.admin.core.CallerCtx
import ch.epfl.bluebrain.nexus.admin.core.Fault.Unexpected
import ch.epfl.bluebrain.nexus.admin.core.projects.Label._
import ch.epfl.bluebrain.nexus.admin.core.projects.Project.Value
import ch.epfl.bluebrain.nexus.admin.core.resources.Resources.Agg
import ch.epfl.bluebrain.nexus.admin.core.resources.{Ref, Resource, Resources}
import io.circe.syntax._
import journal.Logger

class Projects[F[_]](resources: Resources[F, Label])(implicit F: MonadError[F, Throwable]) {

  private val logger = Logger[this.type]
  private type LabelRef = Ref[Label]
  private def tags = Set("project")

  def create(lb: Label, value: Value)(implicit ctx: CallerCtx): F[LabelRef] = resources.create(lb, value.asJson, tags)

  def update(lb: Label, rev: Long, value: Value)(implicit ctx: CallerCtx): F[LabelRef] =
    resources.update(Ref(lb, rev), value.asJson, tags)

  def deprecate(lb: Label, rev: Long)(implicit ctx: CallerCtx): F[LabelRef] = resources.deprecate(Ref(lb, rev), tags)

  def fetch(lb: Label): F[Option[Project]] = resources.fetch(lb)

  def fetch(lb: Label, rev: Long): F[Option[Project]] = resources.fetch(Ref(lb, rev))

  private implicit def resourceToProject(resource: F[Option[Resource[Uri]]]): F[Option[Project]] =
    resource.flatMap {
      case Some(res) =>
        res.ref.toId[Label] match {
          case Some(labelRef) =>
            res.value.as[Value] match {
              case Right(value) => F.pure(Some(Project(labelRef, value, res.deprecated)))
              case Left(err) =>
                logger.error(s"Could not convert json value '${res.value}' to Value", err)
                F.raiseError(Unexpected(s"Could not convert json value '${res.value}' to Value"))
            }
          case None => F.raiseError(Unexpected(s"Could not convert ref '${res.ref}' to label"))
        }
      case None => F.pure(None)
    }

  private implicit def uriRefToLabelRef(uriRef: F[Ref[Uri]]): F[LabelRef] =
    uriRef.flatMap { ref =>
      ref.toId[Label] match {
        case Some(labelRef) => F.pure(labelRef)
        case None           => F.raiseError(Unexpected(s"Could not convert ref '$ref' to label"))
      }
    }
}

object Projects {
  final def apply[F[_]](agg: Agg[F])(implicit F: MonadError[F, Throwable]): Projects[F] = {
    val resources = new Resources[F, Label](agg) {
      override def toPersistedId(uri: Uri): F[String] =
        labelId.fromUri(uri) match {
          case Some(label) => F.pure(s"${label.show}-${uri.authority.host.address().hashCode.abs.toString.take(4)}")
          case None        => F.raiseError(Unexpected(s"Could not convert the uri '$uri' to a Label"))
        }
    }
    new Projects(resources)
  }

}

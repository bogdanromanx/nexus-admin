package ch.epfl.bluebrain.nexus.admin.core.projects

import cats.MonadError
import ch.epfl.bluebrain.nexus.admin.core.resources.Ref

class Projects[F[_]](implicit F: MonadError[F, Throwable]) {

  def create(project: Project): F[Ref[Label]] = ???

}

package ch.epfl.bluebrain.nexus.admin.core.resources

import cats.MonadError

class Resources[F[_]](implicit F: MonadError[F, Throwable]) {


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

}

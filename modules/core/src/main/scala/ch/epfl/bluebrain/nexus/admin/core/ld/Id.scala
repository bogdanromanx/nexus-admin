package ch.epfl.bluebrain.nexus.admin.core.ld

import akka.http.scaladsl.model.Uri

trait Id[A] {
  def uri(a: A): Uri
  def fromUri(uri: Uri): Option[A]
}

object Id {

  @inline
  final def apply[A](implicit instance: Id[A]): Id[A] = instance

  final def id[A](f: A => Uri, g: Uri => Option[A]): Id[A] = new Id[A] {
    override def uri(a: A): Uri = f(a)
    override def fromUri(uri: Uri): Option[A] = g(uri)
  }

  final implicit val uriId: Id[Uri] =
    id(identity, Option.apply)

}
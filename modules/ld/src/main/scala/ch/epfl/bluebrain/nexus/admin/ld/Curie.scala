package ch.epfl.bluebrain.nexus.admin.ld

import java.util.UUID

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Query
import cats.Show
import ch.epfl.bluebrain.nexus.commons.iam.acls.Path

/**
  * nxv:rev
  *
  * a:rev
  *
  * base = https://bbp.epfl.ch/nexus/vocab/
  * prefix = nxv
  * value = rev
  *
  * @param base
  * @param prefix
  * @param reference
  */
final case class Curie private[ld] (base: Uri, prefix: String, reference: String) {
  def uri: Uri = Uri(s"$base$reference")

  def withPrefix(prefix: String): Curie = copy(prefix = prefix)
}

object Curie {

  final implicit val curieShow: Show[Curie] =
    Show.show(c => s"${c.prefix}:${c.reference}")

  def apply(base: Uri, prefix: String, reference: String): Option[Curie] =
    if (reference.isEmpty) None
    else if (validBase(base)) Some(new Curie(base, prefix, reference))
    else None

  def apply(base: Uri, reference: String): Option[Curie] = apply(base, prefixGen, reference)

  def apply(uri: Uri): Option[Curie] =
    uri.query() match {
      case Query.Empty =>
        uri.fragment match {
          case Some(fragment) if !fragment.isEmpty =>
            Some(new Curie(s"${uri.withoutFragment}#", prefixGen, fragment))
          case Some(_) =>
            None
          case _ =>
            (uri.path: Path) match {
              case Path.Segment(head, tail) if !head.isEmpty =>
                Some(new Curie(uri.copy(path = tail), prefixGen, head))
              case _ => None
            }
        }
      case _ => None
    }

  private[ld] def prefixGen: String = UUID.randomUUID().toString.toLowerCase.take(5)

  private[ld] def validBase(base: Uri): Boolean =
    base.query() match {
      case Query.Empty =>
        base.fragment match {
          case Some(fragment) => fragment.isEmpty
          case None           => base.path.endsWithSlash
        }
      case _ => false
    }

  final implicit val curieId: Id[Curie] =
    Id.id(_.uri, apply)
}

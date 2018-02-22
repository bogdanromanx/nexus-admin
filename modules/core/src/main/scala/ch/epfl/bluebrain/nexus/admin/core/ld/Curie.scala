package ch.epfl.bluebrain.nexus.admin.core.ld

import java.util.UUID

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.Uri.Path.{Segment, Slash}
import cats.Show

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
  * @param value
  */
final case class Curie(base: Uri, prefix: String, value: String) {
  def uri: Uri =
    Uri(s"$base$value")

  def withPrefix(prefix: String): Curie =
    copy(prefix = prefix)
}

object Curie {

  final implicit val curieShow: Show[Curie] =
    Show.show(c => s"${c.prefix}:${c.value}")

  final implicit val curieId: Id[Curie] =
    Id.id(_.uri, uri =>
      {
        val str = uri.toString()
        val hashIdx = str.lastIndexOf("#")
        if (hashIdx != -1) {
          val base = str.substring(0, hashIdx + 1)
          val value = str.substring(hashIdx + 1)
          val prefix = UUID.randomUUID().toString.toLowerCase
          Some(Curie(base, prefix, value))
        } else {
          uri.path match {
            case Path.Empty => None
            case Slash(Path.Empty) => None
            case Segment(head, tail) =>
              val value = head
              val base = uri.copy(path = tail)
              val prefix = UUID.randomUUID().toString.toLowerCase
              Some(Curie(base, prefix, value))
          }
        }
      }
    )
}
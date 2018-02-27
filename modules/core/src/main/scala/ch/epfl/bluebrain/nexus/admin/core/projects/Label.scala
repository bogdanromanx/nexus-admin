package ch.epfl.bluebrain.nexus.admin.core.projects

import cats.Show
import ch.epfl.bluebrain.nexus.admin.ld.Id
import ch.epfl.bluebrain.nexus.admin.ld.Prefixes.projects
import io.circe.{Decoder, Encoder}

final case class Label private (value: String)

object Label {
  private val regex = "[a-zA-Z0-9-_]{3,16}".r

  final def apply(value: String): Option[Label] = value match {
    case regex() => Some(new Label(value))
    case _       => None
  }

  final implicit val labelId: Id[Label] = Id.id(
    l => projects.reference(l.value)uri,
    uri => projects.wrap(uri).flatMap(c => Label(c.reference))
  )

  final implicit val labelShow: Show[Label] = Show.show(_.value)

  final implicit val labelEnc: Encoder[Label] = Encoder.encodeString.contramap(_.value)

  final implicit val labelDec: Decoder[Label] =
    Decoder.decodeString.emap(l => apply(l).toRight(s"Unable to decode value '$l' into a Label"))
}

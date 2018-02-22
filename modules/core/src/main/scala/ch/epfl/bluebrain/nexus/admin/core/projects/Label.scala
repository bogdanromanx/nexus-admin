package ch.epfl.bluebrain.nexus.admin.core.projects

import ch.epfl.bluebrain.nexus.admin.core.ld.Id
import ch.epfl.bluebrain.nexus.admin.core.ld.Prefixes._

final case class Label private(value: String)

object Label {

  final def apply(value: String): Option[Label] =
    if (value matches "[0-9a-zA-Z-_]{3,16}") Some(new Label(value))
    else None

  final implicit val labelId: Id[Label] = Id.id(
    l => projects.point(l.value).uri,
    uri => projects.wrap(uri).flatMap(c => Label(c.value))
  )
}
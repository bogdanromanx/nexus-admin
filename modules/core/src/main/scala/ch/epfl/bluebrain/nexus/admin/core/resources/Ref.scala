package ch.epfl.bluebrain.nexus.admin.core.resources

import ch.epfl.bluebrain.nexus.admin.core.ld.Id

final case class Ref[A: Id](id: A, rev: Long) {
  def toId[B: Id]: Option[Ref[B]] =
    Id[B].fromUri(Id[A].uri(id)).map(idB => Ref(idB, rev))
}

package ch.epfl.bluebrain.nexus.admin.core.resources

import ch.epfl.bluebrain.nexus.admin.ld.Id
import io.circe.Json

final case class Resource[A: Id](ref: Ref[A], value: Json, deprecated: Boolean)
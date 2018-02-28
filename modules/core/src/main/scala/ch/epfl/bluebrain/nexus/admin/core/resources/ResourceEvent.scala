package ch.epfl.bluebrain.nexus.admin.core.resources

import ch.epfl.bluebrain.nexus.commons.iam.acls.Meta
import io.circe.Json

sealed trait ResourceEvent extends Product with Serializable {
  def ref: UriRef
  def meta: Meta
  def tags: Set[String]
}

object ResourceEvent {

  final case class ResourceCreated(ref: UriRef, meta: Meta, tags: Set[String], value: Json) extends ResourceEvent
  final case class ResourceUpdated(ref: UriRef, meta: Meta, tags: Set[String], value: Json) extends ResourceEvent
  final case class ResourceDeprecated(ref: UriRef, meta: Meta, tags: Set[String])           extends ResourceEvent

}

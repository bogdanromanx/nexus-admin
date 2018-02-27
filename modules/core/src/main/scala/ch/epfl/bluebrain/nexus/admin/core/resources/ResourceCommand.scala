package ch.epfl.bluebrain.nexus.admin.core.resources

import akka.http.scaladsl.model.Uri
import ch.epfl.bluebrain.nexus.commons.iam.acls.Meta
import io.circe.Json

sealed trait ResourceCommand extends Product with Serializable {
  def id: Uri
  def meta: Meta
  def tags: Set[String]
}

object ResourceCommand {

  final case class CreateResource(id: Uri, meta: Meta, tags: Set[String], value: Json) extends ResourceCommand
  final case class UpdateResource(id: Uri, rev: Long, meta: Meta, tags: Set[String], value: Json)
      extends ResourceCommand
  final case class DeprecateResource(id: Uri, rev: Long, meta: Meta, tags: Set[String]) extends ResourceCommand
}

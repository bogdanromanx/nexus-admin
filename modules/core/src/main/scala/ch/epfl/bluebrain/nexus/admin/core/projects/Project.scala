package ch.epfl.bluebrain.nexus.admin.core.projects

import ch.epfl.bluebrain.nexus.admin.core.ld.Prefixes._
import ch.epfl.bluebrain.nexus.admin.core.resources.{Ref, Resource}
import io.circe.Json

case class Project(ref: Ref[Label], attachmentSize: Int, value: Json)

object Project {

  implicit class FromResource[A](resource: Resource[A]) {
    def asProject: Option[Project] = for {
      ref            <- resource.ref.toId[Label]
      attachmentSize <- resource.predicate[Int](projects.maxAttachmentSize)
    } yield Project(ref, attachmentSize, resource.value)
  }

}
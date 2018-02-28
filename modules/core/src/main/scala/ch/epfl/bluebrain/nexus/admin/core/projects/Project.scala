package ch.epfl.bluebrain.nexus.admin.core.projects

import ch.epfl.bluebrain.nexus.admin.core.projects.Project.Value
import ch.epfl.bluebrain.nexus.admin.core.resources.Ref
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Json}
final case class Project(ref: Ref[Label], value: Value, deprecated: Boolean)

object Project {

  implicit def valueEncoder(implicit E: Encoder[Config] = deriveEncoder[Config]): Encoder[Value] = deriveEncoder[Value]
  implicit def valueDecoder(implicit D: Decoder[Config] = deriveDecoder[Config]): Decoder[Value] = deriveDecoder[Value]

  final case class Value(`@context`: Json, config: Config)
  final case class Config(maxAttachmentSize: Long)

  //  implicit class FromResource[A](resource: Resource[A]) {
  //    resource.
  //    def asProject: Option[Project] = for {
  //      context <- resource.value.hcursor.get[Json]("@context").toOption
  //      ref            <- resource.ref.toId[Label]
  //      attachmentSize <- resource.predicate[Int](projects.maxAttachmentSize)
  //      deprecated <- resource.predicate[Boolean](projects.deprecated)
  //    } yield Project(ref, attachmentSize, resource.value, deprecated)
  //  }
}

package ch.epfl.bluebrain.nexus.admin.ld

import akka.http.scaladsl.model.Uri
import ch.epfl.bluebrain.nexus.admin.ld.JsonLD.IdType
import io.circe.Json
import shapeless.Typeable

trait JsonLD {
  def json: Json
  def id: Option[IdType]
  def tpe: Option[Curie]
  def predicate[T: Typeable](uri: Uri): Option[T]
  def predicate[T: Typeable](curie: Curie): Option[T] = predicate(curie.uri)
  def deepMerge(other: Json): JsonLD
  def deepMerge(otherLD: JsonLD): JsonLD = deepMerge(otherLD.json)
}

object JsonLD {

  final def apply(json: Json): JsonLD = JenaJsonLD(json)

  implicit def toJson(value: JsonLD): Json = value.json

  sealed trait IdType extends Product with Serializable

  object IdType {
    final case class IdTypeUri(value: Uri)        extends IdType
    final case class IdTypeBlank(value: String) extends IdType
  }

}

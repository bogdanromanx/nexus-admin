package ch.epfl.bluebrain.nexus.admin.ld

import akka.http.scaladsl.model.Uri

class CurieBuilder private[ld] (val base: Uri, val prefix: String) {

  protected lazy val baseAsString: String = base.toString()

  final def reference(value: String): Curie =
    new Curie(base, prefix, value)

  final def wrap(uri: Uri): Option[Curie] = {
    val str = uri.toString()
    if (!str.startsWith(baseAsString)) None
    else {
      val delta = str.substring(baseAsString.length)
      if (delta.length > 0) Some(reference(delta))
      else None
    }
  }
}

object CurieBuilder {
  def apply(base: Uri, prefix: String): Option[CurieBuilder] =
    if (Curie.validBase(base)) Some(new CurieBuilder(base, prefix))
    else None

  def apply(base: Uri): Option[CurieBuilder] = apply(base, Curie.prefixGen)

}

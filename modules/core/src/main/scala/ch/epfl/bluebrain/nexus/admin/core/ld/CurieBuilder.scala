package ch.epfl.bluebrain.nexus.admin.core.ld

import akka.http.scaladsl.model.Uri

abstract class CurieBuilder(val base: Uri, val prefix: String) {

  protected lazy val baseAsString: String = base.toString()

  final def point(value: String): Curie =
    Curie(base, prefix, value)

  final def wrap(uri: Uri): Option[Curie] = {
    val str = uri.toString()
    if (!str.startsWith(baseAsString)) None
    if (uri.rawQueryString.isDefined) None
    else {
      val delta = str.substring(baseAsString.length)
      if (delta.length > 0) Some(Curie(base, prefix, delta))
      else None
    }
  }
}

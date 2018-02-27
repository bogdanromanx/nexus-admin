package ch.epfl.bluebrain.nexus.admin.core

import akka.http.scaladsl.model.Uri

package object resources {
  private[resources] type UriRef      = Ref[Uri]
  private[resources] type UriResource = Resource[Uri]
}

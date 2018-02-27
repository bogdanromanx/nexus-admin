package ch.epfl.bluebrain.nexus.admin.core

import akka.http.scaladsl.model.Uri
import io.circe.{Decoder, Encoder}

import scala.util.Try

  trait UriSerialization {
  implicit final val uriEncoder: Encoder[Uri] = Encoder.encodeString.contramap(_.toString())
  implicit final val uriDecoder: Decoder[Uri] = Decoder.decodeString.emapTry(s => Try(Uri(s)))

}
object UriSerialization extends UriSerialization

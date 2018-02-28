package ch.epfl.bluebrain.nexus.admin.core.resources

import ch.epfl.bluebrain.nexus.admin.ld.Id
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class Ref[A: Id](id: A, rev: Long) {
  def toId[B: Id]: Option[Ref[B]] =
    Id[B].fromUri(Id[A].uri(id)).map(idB => Ref(idB, rev))
}

object Ref {

  implicit def refEncoder[A: Id: Encoder]: Encoder[Ref[A]] = deriveEncoder[Ref[A]]
  implicit def refDecoder[A: Id: Decoder]: Decoder[Ref[A]] = deriveDecoder[Ref[A]]

//  implicit def refEncoder[A: Encoder: Id]: Encoder[Ref[A]] =
//    Encoder.encodeJson.contramap(ref => Json.obj(`@id` -> Json.fromString(ref.id.uri.toString()), nxv.rev.show -> Json.fromLong(ref.rev)))
//
//  implicit def refDecoder[A: Decoder](implicit Id: Id[A], T: Typeable[A]): Decoder[Ref[A]] = Decoder.apply { hc =>
//    for {
//      uri  <- hc.get[String](`@id`)
//      id <- Id.fromUri(uri).toRight(DecodingFailure(s"could not convert form uri '$uri' to ${T.describe}", hc.history))
//      rev <- hc.get[Long](nxv.rev.show)
//    } yield (Ref(id, rev))
//  }
}

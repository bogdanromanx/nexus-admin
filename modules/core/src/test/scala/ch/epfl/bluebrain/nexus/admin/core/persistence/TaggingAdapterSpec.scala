package ch.epfl.bluebrain.nexus.admin.core.persistence

import java.time.Clock

import akka.http.scaladsl.model.Uri
import akka.persistence.journal.Tagged
import ch.epfl.bluebrain.nexus.admin.core.TestHeplers
import ch.epfl.bluebrain.nexus.admin.core.persistence.TaggingAdapterSpec.OtherEvent
import ch.epfl.bluebrain.nexus.admin.core.resources.Ref
import ch.epfl.bluebrain.nexus.admin.core.resources.ResourceEvent.{ResourceCreated, ResourceDeprecated, ResourceUpdated}
import ch.epfl.bluebrain.nexus.commons.iam.acls.Meta
import ch.epfl.bluebrain.nexus.commons.iam.identity.Identity.UserRef
import ch.epfl.bluebrain.nexus.commons.test.Randomness
import org.scalatest.{Inspectors, Matchers, WordSpecLike}

class TaggingAdapterSpec extends WordSpecLike with Matchers with Inspectors with Randomness with TestHeplers {

  "A TaggingAdapter" should {

    val adapter = new TaggingAdapter()

    val ref  = Ref(Uri("http://localhost"), 1L)
    val meta = Meta(UserRef("realm", "sub:1234"), Clock.systemUTC.instant())

    val mapping = Map(
      ResourceCreated(ref, meta, Set("project", "other"), genJson()) -> Set("project", "other"),
      ResourceUpdated(ref, meta, Set("project", "one"), genJson())   -> Set("project", "one"),
      ResourceDeprecated(ref, meta, Set("project"))                  -> Set("project")
    )

    "set the appropriate tags" in {
      forAll(mapping.toList) {
        case (ev, tags) => adapter.toJournal(ev) shouldEqual Tagged(ev, tags)
      }
    }

    "return an empty manifest" in {
      adapter.manifest(OtherEvent(genString())) shouldEqual ""
    }

  }
}

object TaggingAdapterSpec {
  private[persistence] case class OtherEvent(some: String)
}

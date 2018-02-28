package ch.epfl.bluebrain.nexus.admin.ld

import akka.http.scaladsl.model.Uri
import ch.epfl.bluebrain.nexus.admin.ld.JsonLD.IdType._
import ch.epfl.bluebrain.nexus.commons.test.Resources
import org.scalatest.{Matchers, OptionValues, WordSpecLike}

class JsonLDSpec extends WordSpecLike with Matchers with Resources with OptionValues {

  "A JsonLD" should {
    val jsonLD      = JsonLD(jsonContentOf("/no_id.json"))
    val typedJsonLD = JsonLD(jsonContentOf("/id_and_type.json"))
    val aliasedTypeJsonLD = JsonLD(jsonContentOf("/id_and_type_alias.json"))
    val schemaOrg   = new CurieBuilder("http://schema.org/", "schema")

    "find a string from a given a predicate" in {
      jsonLD.predicate[String](schemaOrg.reference("name")).value shouldEqual "The Empire State Building"
    }

    "find a uri from a given a predicate" in {
      jsonLD.predicate[Uri](schemaOrg.reference("image")).value shouldEqual Uri(
        "http://www.civil.usherbrooke.ca/cours/gci215a/empire-state-building.jpg")
    }

    "find a float from a given a predicate" in {
      jsonLD.predicate[Float](schemaOrg.reference("latitude")).value shouldEqual 40.75f
    }

    "find the @id" in {
      jsonLD.id.value shouldBe a[IdTypeBlank]
      typedJsonLD.id.value shouldEqual IdTypeUri("http://example.org/cars/for-sale#tesla")
      aliasedTypeJsonLD.id.value shouldEqual IdTypeUri("https://bbp-nexus.epfl.ch/dev/v0/contexts/nexus/core/distribution/v0.1.0")
    }

    "find the @type" in {
      jsonLD.tpe shouldEqual None
      typedJsonLD.tpe.value shouldEqual Curie("http://purl.org/goodrelations/v1#", "gr", "Offering").value
      aliasedTypeJsonLD.tpe.value shouldEqual Curie("https://bbp-nexus.epfl.ch/vocabs/nexus/core/terms/v0.1.0/", "nxv", "Some").value
    }
  }
}

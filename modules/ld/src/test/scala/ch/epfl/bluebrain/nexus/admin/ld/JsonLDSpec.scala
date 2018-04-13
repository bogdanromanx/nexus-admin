package ch.epfl.bluebrain.nexus.admin.ld

import akka.http.scaladsl.model.Uri
import ch.epfl.bluebrain.nexus.admin.ld.Const._
import ch.epfl.bluebrain.nexus.admin.ld.JsonLD.{EmptyCursor, EmptyEffectBuilder, GraphCursor}
import ch.epfl.bluebrain.nexus.admin.refined.ld._
import ch.epfl.bluebrain.nexus.commons.test.Resources
import eu.timepit.refined.auto._
import io.circe.Json
import org.scalatest.{Inspectors, Matchers, OptionValues, WordSpecLike}
import ch.epfl.bluebrain.nexus.commons.http.JsonOps._
class JsonLDSpec extends WordSpecLike with Matchers with Resources with OptionValues with Inspectors {

  "A JsonLD" should {
    val jsonLD       = JsonLD(jsonContentOf("/no_id.json"))
    val typedJsonLD  = JsonLD(jsonContentOf("/id_and_types.json"))
    val typed2JsonLD = JsonLD(jsonContentOf("/id_and_type.json"))
    val jsonLDSimple = JsonLD(jsonContentOf("/id_simple.json"))
    val schemaOrg    = new IdRefBuilder("schema", "http://schema.org/")

    "find a string from a given a predicate" in {
      jsonLD.value[String](schemaOrg.build("name")).value shouldEqual "empire"
    }

    "find a prefix from a given a predicate using refined" in {
      jsonLD.value[Prefix](schemaOrg.build("name")).value shouldEqual ("empire": Prefix)
    }

    "find a uri from a given a predicate" in {
      jsonLD.value[Uri](schemaOrg.build("image")).value shouldEqual Uri(
        "http://www.civil.usherbrooke.ca/cours/gci215a/empire-state-building.jpg")
    }

    "find a uri from a given a predicate using refined" in {
      jsonLD
        .value[Id](schemaOrg.build("image"))
        .value shouldEqual ("http://www.civil.usherbrooke.ca/cours/gci215a/empire-state-building.jpg": Id)
    }

    "find another type of uri from a given a predicate using refined" in {
      jsonLD
        .value[AliasOrNamespace](schemaOrg.build("image"))
        .value shouldEqual ("http://www.civil.usherbrooke.ca/cours/gci215a/empire-state-building.jpg": AliasOrNamespace)
    }

    "find a float from a given a predicate on a nested field" in {
      jsonLD.downFirst(schemaOrg.build("geo")).value[Float](schemaOrg.build("latitude")).value shouldEqual 40.75f
    }

    "find a list of elements from a given a predicate on a nested field" in {
      jsonLD.down(schemaOrg.build("geo")).foldLeft(List.empty[(Float, Float)]) {
        case (acc, c) =>
          val tuple = for {
            lat  <- c.value[Float](schemaOrg.build("latitude"))
            long <- c.value[Float](schemaOrg.build("longitude"))
          } yield lat -> long
          tuple.map(_ :: acc).getOrElse(acc)
      } should contain theSameElementsAs List(40.75f -> 73.98f, 10.0f -> 12.0f)
    }

    "return None when attempting to fetch the value from a predicate on an unexisting parent" in {
      jsonLD.downFirst(schemaOrg.build("nonExisting")).value[Float](schemaOrg.build("latitude")) shouldEqual None
    }

    "return empty list of cursors when attempting to navigate down an unexisting parent" in {
      jsonLD.down(schemaOrg.build("nonExisting")) shouldEqual List.empty[GraphCursor]
      jsonLD.down(schemaOrg.build("nonExisting")) shouldEqual List.empty[GraphCursor]

    }

    "return empty cursor when attempting to navigate down the first unexisting parent" in {
      jsonLD.downFirst(schemaOrg.build("nonExisting")) shouldEqual EmptyCursor(jsonLD)
      jsonLD.downFirst(schemaOrg.build("nonExisting")).down(schemaOrg.build("nonExisting2")) shouldEqual List
        .empty[GraphCursor]
      jsonLD
        .downFirst(schemaOrg.build("nonExisting"))
        .downFirst(schemaOrg.build("nonExisting2")) shouldEqual EmptyCursor(jsonLD)
    }

    "return None  when attempting to fetch the type of a down navigation on an unexisting parent" in {
      jsonLD.downFirst(schemaOrg.build("nonExisting")).tpe shouldEqual Set.empty
    }

    "find the @id" in {
      jsonLD.id shouldBe a[IdTypeBlank]
      typedJsonLD.id shouldEqual IdTypeUri("http://example.org/cars/for-sale#tesla")
      typed2JsonLD.id shouldEqual IdTypeUri("https://bbp-nexus.epfl.ch/dev/v0/contexts/nexus/core/distribution/v0.1.0")
    }

    "find the @type" in {
      jsonLD.tpe shouldEqual Set.empty
      typedJsonLD.tpe shouldEqual Set(IdRef("gr", "http://purl.org/goodrelations/v1#", "Offering"),
                                      IdRef("owl", "http://www.w3.org/2002/07/owl#", "Ontology"))
      val tpe = typed2JsonLD.tpe
      tpe.size shouldEqual 1
      tpe.head.reference shouldEqual ("Some": Reference)
      tpe.head.namespace shouldEqual ("http://example.com/": Namespace)
    }

    "convert to Json" in {
      val json: Json = jsonLD
      json shouldEqual jsonLD.json
    }

    "deep merge" in {
      val mergedJson   = jsonLD.json deepMerge typedJsonLD.json
      val mergedJsonLD = jsonLD deepMerge typedJsonLD
      mergedJsonLD.json shouldEqual mergedJson
    }

    "return the namespace given a prefix" in {
      jsonLD.namespaceOf("xsd").value shouldEqual ("http://www.w3.org/2001/XMLSchema#": Namespace)
    }

    "return None attempting to fetch a namespace when the namespace in the json @context is wrong" in {
      jsonLD.namespaceOf("invalidNamespace") shouldEqual None
    }

    "return None attempting to fetch a namespace when the prefix does not exist in the json @context" in {
      jsonLD.namespaceOf("nonExisting") shouldEqual None
    }

    "return the prefix given a namespace" in {
      jsonLD.prefixOf("http://www.w3.org/2001/XMLSchema#").value shouldEqual ("xsd": Prefix)
    }

    "return None attempting to fetch a prefix when the namespace does not exist in the json @context" in {
      jsonLD.prefixOf("http://non-existinf.com/something/") shouldEqual None
    }

    "expand a value" in {
      jsonLD.expand("xsd:name").value shouldEqual ("http://www.w3.org/2001/XMLSchema#name": Id)
    }

    "expand a value already expanded returns the original value" in {
      jsonLD
        .expand("http://www.w3.org/2001/XMLSchema#name")
        .value shouldEqual ("http://www.w3.org/2001/XMLSchema#name": Id)
    }

    "fail to expand a value" in {
      forAll(List("", "xsd2:name", "something")) { value =>
        jsonLD.expand(value) shouldEqual None
      }
    }

    val noIdAddedContext = jsonContentOf("/no_id_added_context.json")

    "append a new context" in {
      val ctxToAdd = jsonContentOf("/added_context.json")
      jsonLD.appendContext(ctxToAdd).json shouldEqual noIdAddedContext
      jsonLD.appendContext(JsonLD(ctxToAdd)).json shouldEqual noIdAddedContext
    }
    "append nothing when the passed value does not contain @context" in {
      val ctxToAdd = Json.obj("one" -> Json.fromString("two"))
      jsonLD.appendContext(ctxToAdd).json shouldEqual jsonLD.json
    }

    "add triples to the jsonLD" in {
      val expected = jsonLDSimple.json.removeKeys("@context") deepMerge Json.obj(
        "@type" -> Json.fromString("nxv:Project"))
      jsonLDSimple.add(rdf.tpe, nxv.Project).apply().value.json.removeKeys("@context") shouldEqual expected
    }

    "add same triples twice the jsonLD" in {
      val expected = jsonLDSimple.json.removeKeys("@context") deepMerge Json.obj(
        "@type" -> Json.fromString("nxv:Project"))
      jsonLDSimple
        .add(rdf.tpe, nxv.Project)
        .add(rdf.tpe, nxv.Project)
        .apply()
        .value
        .json
        .removeKeys("@context") shouldEqual expected
    }

    "add several triples to the jsonLD" in {
      val expected = jsonLDSimple.json.removeKeys("@context") deepMerge Json.obj(
        "@type"    -> Json.fromString("nxv:Project"),
        "nxv:_rev" -> Json.fromInt(2))
      val json = jsonLDSimple.add(rdf.tpe, nxv.Project).add(nxv.rev, 2).apply().value.json
      json.removeKeys("@context") shouldEqual expected

      val json2 = jsonLDSimple.add(rdf.tpe, nxv.Project).apply().value.add(nxv.rev, 2L).apply().value.json
      json2.removeKeys("@context") shouldEqual expected

      val json3 = jsonLDSimple.add(nxv.rev, 2.0).apply().value.json
      json3.removeKeys("@context") shouldEqual (jsonLDSimple.json.removeKeys("@context") deepMerge Json.obj(
        "nxv:_rev" -> Json.fromDoubleOrNull(2.0)))

    }

    "add triple to an empty cursor returns the original json" in {
      val updateBuilder = typedJsonLD.downFirst(nxv.build("non-exists")).add(nxv.deprecated, false)
      updateBuilder shouldEqual EmptyEffectBuilder(typedJsonLD)
      updateBuilder.apply().value.json shouldEqual typedJsonLD.json
    }

    "add nothing " in {
      val updateBuilder = typedJsonLD.downFirst(nxv.build("non-exists")).add(nxv.deprecated, false)
      updateBuilder shouldEqual EmptyEffectBuilder(typedJsonLD)
      updateBuilder.apply().value.json shouldEqual typedJsonLD.json
    }

    "add several triples to the jsonLD cursor" in {
      val expected = jsonContentOf("/id_and_type_updated.json")

      typed2JsonLD
        .downFirst(nxv.build("_links"))
        .add(nxv.deprecated, false)
        .add(nxv.name, "something")
        .apply()
        .value
        .json
        .removeKeys("@context") shouldEqual expected

      typed2JsonLD
        .downFirst(nxv.build("_links"))
        .add(nxv.deprecated, false)
        .apply()
        .value
        .downFirst(nxv.build("_links"))
        .add(nxv.name, "something")
        .apply()
        .value
        .json
        .removeKeys("@context") shouldEqual expected

    }
  }
}

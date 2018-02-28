package ch.epfl.bluebrain.nexus.admin.ld

import cats.syntax.show._
import akka.http.scaladsl.model.Uri
import ch.epfl.bluebrain.nexus.commons.test.Randomness
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{Matchers, OptionValues, WordSpecLike}

class CurieSpec extends WordSpecLike with Matchers with TableDrivenPropertyChecks with OptionValues with Randomness {

  "A Curie" should {
    val curies = Table(
      ("uri", "base", "prefix", "reference"),
      (Uri("http://schema.org/name"), Uri("http://schema.org/"), "schema", "name"),
      (Uri("http://www.w3.org/2001/XMLSchema#float"), Uri("http://www.w3.org/2001/XMLSchema#"), "xsd", "float")
    )

    "be generated form a uri" in {
      forAll(curies) { (uri, base, _, reference) =>
        val curie = Curie(uri).value
        curie shouldEqual Curie(base, curie.prefix, reference).value
        curie.uri shouldEqual uri
        CurieBuilder(base, curie.prefix).value.reference(reference) shouldEqual curie
        curie.uri shouldEqual uri
      }
    }

    "print its short form" in {
      forAll(curies) { (_, base, prefix, reference) =>
        val curie = Curie(base, prefix, reference).value
        curie.show shouldEqual s"$prefix:$reference"
      }
    }

    "be generated from a builder with prefix" in {
      val list = Table(
        ("uri", "base", "prefix", "reference"),
        (Uri("http://schema.org/name"), Uri("http://schema.org/"), "schema", "name"),
        (Uri("http://www.w3.org/2001/XMLSchema#float"), Uri("http://www.w3.org/2001/XMLSchema#"), "xsd", "float")
      )
      forAll(list) { (uri, base, prefix, reference) =>
        val curie = CurieBuilder(base, prefix).value.reference(reference)
        curie shouldEqual Curie(base, prefix, reference).value
        curie.uri shouldEqual uri
      }
    }

    "return None when attempting to create a curie using a builder and the base isn't valid" in {
      val incorrect = Table("base", "http://schema.org", "http://schema.org?q=some", "http://schema.org/one")
      forAll(incorrect) { base =>
        CurieBuilder(base, genString()) shouldEqual None
      }
    }

    "return None when attempting to create a curie and the uri isn't valid" in {
      val incorrect = Table("uri", "http://schema.org", "http://schema.org/", "http://www.w3.org/2001/XMLSchema#", "http://schema.org?q=some")
      forAll(incorrect) { uri =>
        Curie(uri) shouldEqual None
      }
    }
  }

}

package ch.epfl.bluebrain.nexus.admin.refined

import ch.epfl.bluebrain.nexus.admin.refined.ld._
import ch.epfl.bluebrain.nexus.commons.test.Randomness
import eu.timepit.refined.api.RefType._
import org.scalatest.{EitherValues, Inspectors, Matchers, WordSpecLike}
class PrefixSpec extends WordSpecLike with Matchers with Inspectors with Randomness with EitherValues {

  "A Prefix" should {
    val startPool = Vector.range('a', 'z') ++ Vector.range('A', 'Z') ++ Vector('_')
    val pool      = Vector.range('a', 'z') ++ Vector.range('A', 'Z') ++ Vector.range('0', '9') :+ '_' :+ '-' :+ '.'

    "be constructed correctly" in {
      val list = List.fill(100)(genString(1, startPool) + genString(genInt(5), pool))
      forAll(list) { el =>
        val name = applyRef[Prefix](el)
        name.right.value.value shouldEqual el
      }
    }

    "fail to construct" in {
      val incorrect = List("", "0ab", "-ab", ".a", "aksjd*&^%")
      forAll(incorrect) { el =>
        val name = applyRef[Prefix](el)
        name.left.value
      }
    }
  }
}
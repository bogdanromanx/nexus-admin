package ch.epfl.bluebrain.nexus.admin.core

import ch.epfl.bluebrain.nexus.commons.test.Randomness
import io.circe.Json

trait TestHeplers extends Randomness {

  def genJson(): Json = Json.obj("key" -> Json.fromString(genString()))

  def genName(length: Int = 9): String = genString(length = length, Vector.range('a', 'z') ++ Vector.range('0', '9'))
}

object TestHeplers extends TestHeplers

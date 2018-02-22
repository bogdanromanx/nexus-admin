package ch.epfl.bluebrain.nexus.admin.core.ld

object Prefixes {

  object rdf extends CurieBuilder("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf") {
    val tpe: Curie = point("type")
  }

  object nxv extends CurieBuilder("https://bbp.epfl.ch/nexus/vocab/", "nxv") {
    val rev: Curie = point("rev")
    val deprecated: Curie = point("deprecated")
  }

  object projects extends CurieBuilder("https://bbp.epfl.ch/nexus/projects/", "projects") {
    val maxAttachmentSize: Curie = point("maxAttachmentSize")
  }

}

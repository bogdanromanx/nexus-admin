package ch.epfl.bluebrain.nexus.admin.ld

object Prefixes {

  val `@id`      = "@id"
  val `@type`    = "@type"
  val `@context` = "@context"

  //noinspection TypeAnnotation
  object rdf extends CurieBuilder("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf") {
    val tpe = reference("type")
  }

  //noinspection TypeAnnotation
  object nxv extends CurieBuilder("https://bbp-nexus.epfl.ch/vocabs/nexus/core/terms/v0.1.0/", "nxv") {
    val rev        = reference("rev")
    val deprecated = reference("deprecated")
    val self = reference("self")

    val Project = reference("Project")

    val maxAttachmentSize = reference("maxAttachmentSize")
    val config            = reference("config")
  }

  object projects extends CurieBuilder("https://bbp.epfl.ch/nexus/projects/", "projects")

}

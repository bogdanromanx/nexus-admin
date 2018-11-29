package ch.epfl.bluebrain.nexus.admin.projects

import ch.epfl.bluebrain.nexus.admin.config.AppConfig.HttpConfig
import ch.epfl.bluebrain.nexus.rdf.Iri.AbsoluteIri

/**
  * Type that represents the URI segments to access a project.
  *
  * @param organization the parent organization label
  * @param value        the project label
  */
final case class ProjectLabel(organization: String, value: String) {

  def toIri(implicit httpConfig: HttpConfig): AbsoluteIri = httpConfig.baseIri + organization + value
}
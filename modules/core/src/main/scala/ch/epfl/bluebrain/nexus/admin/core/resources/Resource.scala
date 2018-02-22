package ch.epfl.bluebrain.nexus.admin.core.resources

import java.io.ByteArrayInputStream

import ch.epfl.bluebrain.nexus.admin.core.ld.{Curie, Id}
import io.circe.Json
import org.apache.jena.graph.{Node, NodeFactory}
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.{Lang, RDFDataMgr}
import shapeless.Typeable

final case class Resource[A: Id](ref: Ref[A], deprecated: Boolean, value: Json) {

  private lazy val rootId = NodeFactory.createURI(Id[A].uri(ref.id).toString())

  private lazy val graph = {
    val model = ModelFactory.createDefaultModel()
    RDFDataMgr.read(model, new ByteArrayInputStream(value.noSpaces.getBytes), Lang.JSONLD)
    model.getGraph
  }

  def predicate[T: Typeable](curie: Curie): Option[T] = {
    import scala.collection.JavaConverters._
    val pred = NodeFactory.createURI(curie.uri.toString())
    graph
      .find(rootId, pred, Node.ANY)
      .asScala.map(_.getObject)
      .find(_.isLiteral)
      .flatMap { node =>
        Typeable[T].cast(node.getLiteral.getValue)
      }
  }
}

object Resource {

  // {
  //   "config": {
  //     "max": 32,
  //     "value": {
  //        "min": "a",
  //        "truth": true
  //     }
  //   }
  //
  // }

}
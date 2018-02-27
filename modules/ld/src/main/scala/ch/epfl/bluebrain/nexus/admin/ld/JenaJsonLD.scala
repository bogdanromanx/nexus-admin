package ch.epfl.bluebrain.nexus.admin.ld

import java.io.ByteArrayInputStream

import akka.http.scaladsl.model.Uri
import ch.epfl.bluebrain.nexus.admin.ld.JsonLD.IdType
import ch.epfl.bluebrain.nexus.admin.ld.JsonLD.IdType._
import ch.epfl.bluebrain.nexus.admin.ld.Prefixes.{nxv, rdf}
import io.circe.Json
import org.apache.jena.graph._
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.{Lang, RDFDataMgr}
import shapeless.Typeable

import scala.collection.JavaConverters._

private[ld] final case class JenaJsonLD(json: Json) extends JsonLD {

  private lazy val graph = {
    val model = ModelFactory.createDefaultModel()
    RDFDataMgr.read(model, new ByteArrayInputStream(json.noSpaces.getBytes), Lang.JSONLD)
    model.getGraph
  }

  private lazy val idNode: Option[Node] = {
    val (subs, objs) = graph
      .find(Node.ANY, Node.ANY, Node.ANY)
      .asScala
      .filter(triple => triple.getSubject.isBlankOrUri && triple.getObject.isBlankOrUri)
      .foldLeft(Set.empty[Node] -> Set.empty[Node]) {
        case ((s, o), c) if isSelfPredicate(c.getPredicate) => s                  -> o
        case ((s, o), c)                                    => (s + c.getSubject) -> (o + c.getObject)
      }
    val result = subs -- objs
    if (result.size == 1) Some(result.head)
    else None
  }

  private def isSelfPredicate(node: Node): Boolean =
    node.toCurie match {
      case Some(curie) => curie == nxv.self
      case _           => false
    }

  override lazy val id: Option[IdType] = idNode.flatMap {
    case node: Node_URI   => Some(IdTypeUri(node.getURI))
    case node: Node_Blank => Some(IdTypeBlank(node.getBlankNodeId.getLabelString))
    case _                => None
  }

  override lazy val tpe: Option[Curie] =
    for {
      node  <- idNode
      obj   <- graph.find(node, rdf.tpe, Node.ANY).asScala.collectFirst { case t if t.getObject.isURI => t.getObject }
      curie <- obj.toCurie
    } yield curie

  override def deepMerge(other: Json): JsonLD = JenaJsonLD(json deepMerge other)

  override def predicate[T: Typeable](uri: Uri): Option[T] = {
    val p = NodeFactory.createURI(uri.toString())
    graph.find(Node.ANY, p, Node.ANY).asScala.map(_.getObject).find(node => node.isLiteral || node.isURI).flatMap {
      case node: Node_URI     => Typeable[T].cast(Uri(node.getURI)) orElse Typeable[T].cast(node.getURI)
      case node: Node_Literal => Typeable[T].cast(node.getLiteral.getValue)
      case _                  => None
    }
  }

  private implicit def curieToNode(curie: Curie): Node = NodeFactory.createURI(curie.uri.toString())

  private implicit class NodeSyntax(node: Node) {
    def isBlankOrUri: Boolean = node.isBlank || node.isURI

    def toCurie: Option[Curie] = {
      if (node.isURI) {
        val uri   = node.getURI
        val short = graph.getPrefixMapping.shortForm(node.getURI)
        short.split(":", 2).toList match {
          case prefix :: value :: Nil => CurieBuilder(uri.replaceFirst(value, ""), prefix).map(_.reference(value))
          case _                      => Curie(uri)
        }
      } else None
    }
  }
}

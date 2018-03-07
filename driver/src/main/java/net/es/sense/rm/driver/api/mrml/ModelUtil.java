package net.es.sense.rm.driver.api.mrml;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Arrays;
import net.es.sense.rm.driver.schema.Nml;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;

/**
 *
 * @author hacksaw
 */
public class ModelUtil {

  public static final String[] supported = {
    "turtle",
    "ttl"
  };

  public static boolean isSupported(String type) {
    return Arrays.stream(supported).anyMatch(s -> s.equalsIgnoreCase(type));
  }

  public static OntModel unmarshalOntModel(String ttl) throws Exception {
    OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    //$$ TODO: add ontology schema and namespace handling code
    try {
      model.read(new ByteArrayInputStream(ttl.getBytes()), null, "TURTLE");
    } catch (Exception e) {
      throw new Exception(String.format("failure to unmarshall ontology model, due to %s", e.getMessage()));
    }
    return model;
  }

  public static String marshalOntModel(OntModel model) throws Exception {
    //$$ TODO: add namespace handling code
    StringWriter out = new StringWriter();
    try {
      model.write(out, "TURTLE");
    } catch (Exception e) {
      throw new Exception(String.format("failure to marshall ontology model, due to %s", e.getMessage()));
    }
    String ttl = out.toString();
    return ttl;
  }

  public static OntModel unmarshalOntModelJson(String json) throws Exception {
    OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    //$$ TODO: add ontology schema and namespace handling code
    try {
      model.read(new ByteArrayInputStream(json.getBytes()), null, "RDF/JSON");
    } catch (Exception e) {
      throw new Exception(String.format("failure to unmarshall ontology model, due to %s", e.getMessage()));
    }
    return model;
  }

  public static String marshalOntModelJson(OntModel model) throws Exception {
    //$$ TODO: add namespace handling code
    StringWriter out = new StringWriter();
    try {
      model.write(out, "RDF/JSON");
    } catch (Exception e) {
      throw new Exception(String.format("failure to marshall ontology model, due to %s", e.getMessage()));
    }
    String ttl = out.toString();
    return ttl;
  }

  public static Model unmarshalModel(String ttl) {
    Model model = ModelFactory.createDefaultModel();
    //$$ TODO: add ontology schema and namespace handling code
    model.read(new ByteArrayInputStream(ttl.getBytes()), null, "TURTLE");
    return model;
  }

  public static String marshalModel(Model model) throws Exception {
    //$$ TODO: add namespace handling code
    StringWriter out = new StringWriter();
    try {
      model.write(out, "TURTLE");
    } catch (Exception e) {
      throw new Exception(String.format("failure to marshall ontology model, due to %s", e.getMessage()));
    }
    String ttl = out.toString();
    return ttl;
  }

  public static Model unmarshalModelJson(String ttl) throws Exception {
    Model model = ModelFactory.createDefaultModel();
    //$$ TODO: add ontology schema and namespace handling code
    try {
      model.read(new ByteArrayInputStream(ttl.getBytes()), null, "RDF/JSON");
    } catch (Exception e) {
      throw new Exception(String.format("failure to unmarshall ontology model, due to %s", e.getMessage()));
    }
    return model;
  }

  public static String marshalModelJson(Model model) throws Exception {
    //$$ TODO: add namespace handling code
    StringWriter out = new StringWriter();
    try {
      model.write(out, "RDF/JSON");
    } catch (Exception e) {
      throw new Exception(String.format("failure to marshall ontology model, due to %s", e.getMessage()));
    }
    String ttl = out.toString();
    return ttl;
  }

  public static OntModel cloneOntModel(OntModel model) {
    OntModel cloned = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    cloned.add(model.getBaseModel());
    return cloned;
  }

  public static boolean isEmptyModel(Model model) {
    if (model == null) {
      return true;
    }
    StmtIterator stmts = model.listStatements();
    while (stmts.hasNext()) {
      Statement stmt = stmts.next();
      // check subject will be enough
      if (stmt.getSubject().isResource() && stmt.getPredicate().toString().contains("ogf")) {
        return false;
      }
    }
    return true;
  }

  public static Model getOddModel(Model model) {
    Model odd = ModelFactory.createDefaultModel();
    if (model == null) {
      return odd;
    }
    StmtIterator stmts = model.listStatements();
    while (stmts.hasNext()) {
      Statement stmt = stmts.next();
      // check subject will be enough
      if (stmt.getSubject().isResource() && stmt.getPredicate().toString().contains("ogf")) {
        odd.add(stmt);
      }
    }
    return odd;
  }

  public static boolean isResourceOfType(Model model, Resource res, Resource resType) {
    String sparql = String.format("SELECT $s WHERE {$s a $t. FILTER($s = <%s> && $t = <%s>)}", res, resType);
    ResultSet r = ModelUtil.sparqlQuery(model, sparql);
    return r.hasNext();
  }

  public static ResultSet getResourcesOfType(Model model, Resource resType) {
    String sparql = String.format("SELECT ?resource WHERE { ?resource a <%s> }", resType);
    ResultSet r = ModelUtil.sparqlQuery(model, sparql);
    return r;
  }

  public static Resource getResourceOfType(Model model, Resource res, Resource resType) {
    String sparql = String.format("SELECT ?resource WHERE {?resource a ?type. FILTER($resource = <%s> && $type = <%s>)}", res, resType);
    ResultSet r = ModelUtil.sparqlQuery(model, sparql);

    if (r.hasNext()) {
      return r.next().get("resource").asResource();
    } else {
      return null;
    }
  }

  public static Resource getParentBidirectionalPort(Model model, Resource res) {
    String sparql = String.format("SELECT ?resource WHERE { ?resource a <" + Nml.BidirectionalPort + ">. ?resource <" + Nml.hasBidirectionalPort + "> <%s> }", res);
    ResultSet r = ModelUtil.sparqlQuery(model, sparql);

    if (r.hasNext()) {
      return r.next().get("resource").asResource();
    } else {
      return null;
    }
  }

  public static ResultSet sparqlQuery(Model model, String sparqlStringWithoutPrefix) {
    String sparqlString
            = "prefix sd:    <http://schemas.ogf.org/nsi/2013/12/services/definition#>\n"
            + "prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "prefix owl:   <http://www.w3.org/2002/07/owl#>\n"
            + "prefix xsd:   <http://www.w3.org/2001/XMLSchema#>\n"
            + "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "prefix nml:   <http://schemas.ogf.org/nml/2013/03/base#>\n"
            + "prefix mrs:   <http://schemas.ogf.org/mrs/2013/12/topology#>\n"
            + sparqlStringWithoutPrefix;

    Query query = QueryFactory.create(sparqlString);
    QueryExecution qexec = QueryExecutionFactory.create(query, model);
    ResultSet rs = (ResultSet) qexec.execSelect();
    return rs;
  }

  public static Model applyDeltaAddition(Model original, Model addition) {
    if (original == null) {
      throw new IllegalArgumentException("applyDeltaAddition encountered null original model");
    }

    if (addition == null) {
      throw new IllegalArgumentException("applyDeltaAddition encountered null addition model");
    }

    original.add(addition);

    return original;
  }

  public static Model applyDeltaReduction(Model original, Model reduction) {
    if (original == null) {
      throw new IllegalArgumentException("applyDeltaAddition encountered null original model");
    }

    if (reduction == null) {
      throw new IllegalArgumentException("applyDeltaAddition encountered null addition model");
    }

    original.remove(reduction);

    return original;
  }
}

package net.es.sense.rm.driver.api.mrml;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.*;

/**
 *
 * @author hacksaw
 */
public class ModelUtil {

  static public OntModel unmarshalOntModel(String ttl) throws Exception {
    OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    //$$ TODO: add ontology schema and namespace handling code
    try {
      model.read(new ByteArrayInputStream(ttl.getBytes()), null, "TURTLE");
    } catch (Exception e) {
      throw new Exception(String.format("failure to unmarshall ontology model, due to %s", e.getMessage()));
    }
    return model;
  }

  static public String marshalOntModel(OntModel model) throws Exception {
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

  static public OntModel unmarshalOntModelJson(String json) throws Exception {
    OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    //$$ TODO: add ontology schema and namespace handling code
    try {
      model.read(new ByteArrayInputStream(json.getBytes()), null, "RDF/JSON");
    } catch (Exception e) {
      throw new Exception(String.format("failure to unmarshall ontology model, due to %s", e.getMessage()));
    }
    return model;
  }

  static public String marshalOntModelJson(OntModel model) throws Exception {
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

  static public Model unmarshalModel(String ttl) throws Exception {
    Model model = ModelFactory.createDefaultModel();
    //$$ TODO: add ontology schema and namespace handling code
    try {
      model.read(new ByteArrayInputStream(ttl.getBytes()), null, "TURTLE");
    } catch (Exception e) {
      throw new Exception(String.format("failure to unmarshall ontology model, due to %s", e.getMessage()));
    }
    return model;
  }

  static public String marshalModel(Model model) throws Exception {
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

  static public Model unmarshalModelJson(String ttl) throws Exception {
    Model model = ModelFactory.createDefaultModel();
    //$$ TODO: add ontology schema and namespace handling code
    try {
      model.read(new ByteArrayInputStream(ttl.getBytes()), null, "RDF/JSON");
    } catch (Exception e) {
      throw new Exception(String.format("failure to unmarshall ontology model, due to %s", e.getMessage()));
    }
    return model;
  }

  static public String marshalModelJson(Model model) throws Exception {
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

  static public OntModel cloneOntModel(OntModel model) {
    OntModel cloned = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    cloned.add(model.getBaseModel());
    return cloned;
  }

  static public boolean isEmptyModel(Model model) {
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

  static public Model getOddModel(Model model) {
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
}

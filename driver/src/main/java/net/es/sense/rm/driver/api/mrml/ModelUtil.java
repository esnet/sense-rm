package net.es.sense.rm.driver.api.mrml;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.schema.*;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RiotException;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This utility class provides a set of methods for manipulating Jena Ontologies.
 *
 * @author hacksaw
 */
@Slf4j
public class ModelUtil {
  public static final PrefixMapping PREFIXES = PrefixMapping.Factory.create()
      .setNsPrefix("rdfs", RDFS.getURI())
      .setNsPrefix("rdf", RDF.getURI())
      .setNsPrefix("dc", DC_11.getURI())
      .setNsPrefix("owl", OWL.getURI())
      .setNsPrefix("xsd", XSD.getURI())
      .setNsPrefix("nml", Nml.getURI())
      .setNsPrefix("mrs", Mrs.getURI())
      .setNsPrefix("spa", Spa.getURI())
      .lock();

  // The types of model serialization encodings we support in this class.
  public static final Lang[] SUPPORTED_ENCODINGS = {
      Lang.TURTLE,
      Lang.TTL,
      Lang.RDFJSON,
      Lang.RDFXML,
  };

  // The list of ontology imports we need for our base MRML model.
  public static final List<Schema> SCHEMA_IMPORTS = List.of(
      new Schema(Rdf.getURI(), "/schema/rdf.ttl", Lang.TURTLE),
      new Schema(Rdfs.getURI(), "/schema/rdfs.ttl", Lang.TURTLE),
      new Schema(Owl.getURI(), "/schema/owl.ttl", Lang.TURTLE),
      new Schema(Nml.getURI(), "/schema/nml.ttl", Lang.TURTLE),
      new Schema(Spa.getURI(), "/schema/spa.owl", Lang.RDFXML),
      new Schema(Mrs.getURI(), "/schema/mrs.ttl", Lang.TURTLE));

  /**
   * Create a new empty MRML OntModel with OWL reasoning turned on.
   *
   * @return OntModel initialized with import models for MRML.
   * @throws IOException If there are issues loading dependent models.
   */
  public static OntModel newMrmlModel() throws IOException {
    // Create a new ontology model with reasoning enabled.
    final OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    model.setNsPrefixes(PREFIXES);

    // Configure the document manager with imported schema models.
    OntDocumentManager dm = model.getDocumentManager();
    for (Schema schema : SCHEMA_IMPORTS) {
      log.debug("[newSchemaModel] loading import {} {} {}", schema.getUri(), schema.getPath(), schema.getType());
      try (InputStream file = ModelUtil.class.getResourceAsStream(schema.getPath())) {
        OntModel impModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        impModel.read(file, null, schema.getType().getName());
        dm.addModel(schema.getUri(), impModel);
      } catch (IOException e) {
        throw new IOException(String.format("failure to load schema ontology models, %s", e.getMessage()));
      }
    }
    return model;
  }

  /**
   * Determine if the specified model encoding is supported by this class.
   *
   * @param type The requested encoding type.
   * @return True if the requested encoding type is supported, false otherwise.
   */
  public static boolean isSupported(String type) {
    return Arrays.stream(SUPPORTED_ENCODINGS).anyMatch(s -> s.getName().equalsIgnoreCase(type));
  }

  /**
   * Unmarshal the serialized ontology using the specified encoding.
   *
   * @param serialized A string containing the serialized model.
   * @param encoding The encoding used on the serialized model.
   * @return An OntModel containing the unmarshalled model.
   * @throws IOException If there was an issue unmarshalling the serialized model.
   */
  public static OntModel unmarshalOntModel(String serialized, String encoding) throws IOException, RiotException {
    // Get a new MRML ontology model pre-populated with imported schema.
    OntModel model = newMrmlModel();
    model.read(new ByteArrayInputStream(serialized.getBytes()), null, encoding);
    return model;
  }

  /**
   * Unmarshal the supplied turtle model into an OntModel representation.
   *
   * @param ttl A string containing the turtle encoded model.
   * @return An OntModel containing the unmarshalled model.
   * @throws IOException If there was an issue unmarshalling the serialized model.
   */
  public static OntModel unmarshalOntModelTurtle(String ttl) throws IOException, RiotException {
    return unmarshalOntModel(ttl, Lang.TURTLE.getLabel());
  }

  /**
   * Unmarshal the supplied RDF/JSON model into an OntModel representation.
   *
   * @param json A string containing the RDF/JSON encoded model.
   * @return An OntModel containing the unmarshalled model.
   * @throws IOException If there was an issue unmarshalling the serialized model.
   */
  public static OntModel unmarshalOntModelJson(String json) throws IOException, RiotException {
    return unmarshalOntModel(json, Lang.RDFJSON.getLabel());
  }

  /**
   * Unmarshal the supplied RDF/XML model into an OntModel representation.
   *
   * @param json A string containing the RDF/XML encoded model.
   * @return An OntModel containing the unmarshalled model.
   * @throws IOException If there was an issue unmarshalling the serialized model.
   */
  public static OntModel unmarshalOntModelXml(String json) throws IOException, RiotException {
    return unmarshalOntModel(json, Lang.RDFXML.getLabel());
  }

  /**
   * Unmarshal an MRML model contained in filename using the specified encoding.
   *
   * @param filename The path to the file containing the model to unmarshal.
   * @param encoding The encoding of the model in the file.
   * @return A fully initialized MRML model containing the contents of the file.
   * @throws IOException If there is an issue reading the model file.
   */
  public static OntModel unmarshalOntModelFromFile(String filename, String encoding)
      throws IOException, RiotException {
    try {
      OntModel model = newMrmlModel();
      model.read(new ByteArrayInputStream(Files.readAllBytes(Paths.get(filename))), null, encoding);
      return model;
    } catch (IOException io) {
      throw new IOException(String.format("failure to unmarshall ontology model, due to %s \"%s\"",
          filename, io.getMessage()));
    }
  }

  /**
   * Marshal into a string the specified model using the specified encoding.
   *
   * @param model The model to serialize.
   * @param encoding The encoding to use for serialization.
   * @return A string containing the serialized model.
   */
  public static String marshalOntModel(OntModel model, String encoding) {
    StringWriter out = new StringWriter();
    model.write(out, encoding);
    return out.toString();
  }

  /**
   * Marshal into a string the specified model using the specified encoding.
   *
   * @param model The model to serialize.
   * @param encoding The encoding to use for serialization.
   * @return A string containing the serialized model.
   */
  public static String marshalModel(Model model, String encoding) {
    StringWriter out = new StringWriter();
    model.write(out, encoding);
    return out.toString();
  }

  /**
   * We like the Turtle encoding so much we make it the default.
   *
   * @param model The model to serialize into Turtle.
   * @return The OntModel serialized into a string.
   */
  public static String marshalOntModel(OntModel model) {
    return marshalOntModelTurtle(model);
  }

  /**
   * Marshal the specified model into RDF/XML serialization.
   *
   * @param model The model to serialize into RDF/XML.
   * @return The OntModel serialized into a string.
   */
  public static String marshalOntModelXml(OntModel model) {
    return marshalOntModel(model, Lang.RDFXML.getLabel());
  }

  /**
   * Marshal the specified model into RDF/JSON serialization.
   *
   * @param model The model to serialize into RDF/JSON.
   * @return The OntModel serialized into a string.
   */
  public static String marshalOntModelJson(OntModel model) {
    return marshalOntModel(model, Lang.RDFJSON.getLabel());
  }

  /**
   * Marshal the specified model into a Turtle serialization.
   *
   * @param model The model to serialize into Turtle.
   * @return The OntModel serialized into a string.
   */
  public static String marshalOntModelTurtle(OntModel model) {
    return marshalOntModel(model, Lang.TURTLE.getLabel());
  }

  /**
   * Clone an MRML model.
   *
   * @param model The model to clone.
   * @return The new cloned copy of the input model.
   * @throws IOException Thrown if there is an issue cloning the model.
   */
  public static OntModel cloneOntModel(OntModel model) throws IOException {
    OntModel cloned = newMrmlModel();
    cloned.add(model.getBaseModel());
    return cloned;
  }

  /**
   * Determine if the MRML model is empty of MRML related resources.
   *
   * @param model The model to interrogate.
   * @return True if the model is empty, and false otherwise.
   */
  public static boolean isEmptyModel(OntModel model) {
    if (model == null) {
      return true;
    }
    StmtIterator stmts = model.getBaseModel().listStatements();
    while (stmts.hasNext()) {
      Statement stmt = stmts.next();
      // check subject will be enough
      if (stmt.getPredicate().toString().contains("http://schemas.ogf.org/")) {
        return false;
      }
    }
    return true;
  }

  /**
   * Determine if a named resource is of a specific type.
   *
   * @param model The model to query.
   * @param object The named resources to search for.
   * @param predicate The resource type it must match.
   * @return True if the named resource is in the model and of the specified type.
   */
  public static boolean isResourceOfType(OntModel model, Resource predicate, Resource object) {
    return getResourceOfSubjectAndType(model, predicate, object) != null;
  }

  /**
   * Search the provided model for the set of resources of provided resType.
   *
   * @param model The model to search.
   * @param resType The resource type to match.
   * @return The set of matching resources.
   */
  public static List<Resource> getResourcesOfType(OntModel model, Resource resType) {
    return model.getBaseModel()
        .listStatements(null, Rdf.type, resType) // StmtIterator
        .toList().stream() // Convert StmtIterator to a stream.
        .map(Statement::getSubject) // Extract the subject from each statement.
        .distinct() // Optional: remove duplicates if you expect subjects to repeat.
        .map(s -> model.getResource(s.getURI())) // Map each subject URI to the corresponding resource in the model.
        .collect(Collectors.toList());// Collect all results to a list.
  }

  /**
   * Search the provided model for a resource of a specific subject and type.
   *
   * @param model The model to search.
   * @param type The Rdf.type object to match.
   * @param subject The resource id to match.
   * @return The resource matching the id and type, or null if no match.
   */
  public static Resource getResourceOfSubjectAndType(OntModel model, Resource type, Resource subject) {
    if (model.contains(subject, RDF.type, type)) {
      return subject;
    }

    return null;
  }

  /**
   * Search the provided model for a resource of a specific type.
   *
   * @param model The model to search.
   * @param type The Rdf.type object to match.
   * @param subject The resource id to match.
   * @return The resource matching the id and type, or null if no match.
   */
  public static Resource getResourceOfSubjectAndType(OntModel model, Resource type, String subject) {
    Resource resource = model.getBaseModel().getResource(subject);
    if (model.contains(resource, RDF.type, type)) {
      return resource;
    }

    return null;
  }

  /**
   * Return the mrs:tag property from the specified resource.
   *
   * @param model The model to search.
   * @param resource The resource to return the mrs:tag property.
   * @return The mrs:tag property if present, null otherwise.
   */
  public static String getMrsTag(OntModel model, Resource resource) {
    if (resource != null) {
      Statement property = model.getProperty(resource, Mrs.tag);
      if (property != null && property.getObject().isLiteral()) {
        return property.getObject().asLiteral().getString();
      }
    }
    return null;
  }

  /**
   * Return the Nml.existsDuring property from the specified resource.
   *
   * @param model The model to search.
   * @param resource The resource to return the Nml.existsDuring property.
   * @return The Nml.existsDuring property if present, null otherwise.
   */
  public static Resource getNmlExistsDuring(OntModel model, Resource resource) {
    if (resource != null) {
      Statement property = model.getProperty(resource, Nml.existsDuring);
      if (property != null && property.getObject().isResource()) {
        return property.getObject().asResource();
      }
    }
    return null;
  }

  /**
   * Return the nml:belongsTo property from the specified resource.
   *
   * @param model The model to search.
   * @param resource The resource to return the nml:belongsTo property.
   * @return The nml:belongsTo property if present, null otherwise.
   */
  public static Resource getNmlBelongsTo(OntModel model, Resource resource) {
    if (resource != null) {
      Statement property = model.getProperty(resource, Nml.belongsTo);
      if (property != null && property.getObject().isResource()) {
        return property.getObject().asResource();
      }
    }
    return null;
  }

  /**
   * Return the rdf:type property from the specified resource.
   *
   * @param resource The resource to return the rdf:type property.
   * @return The rdf:type property if present, null otherwise.
   */
  public static List<Resource> getRdfType(OntModel model, Resource resource) {
    return getObjectsOfPredicateRelationship(model, resource, Rdf.type.asObjectProperty());
  }

  /**
   * Get the list of subjects within the model that has a predicate property to the specified resource.
   *
   * @param model The model to search.
   * @param predicate The relationship predicate to filter.
   * @param object The object resource of the has statement.

   * @return The list of matching resources.
   */
  public static List<Resource> getSubjectsOfPredicateRelationship(OntModel model,
                                                                  ObjectProperty predicate, Resource object) {
    StmtIterator stmtIterator = model.getBaseModel().listStatements(null, predicate, object);
    return stmtIterator.toList().stream() // Convert StmtIterator to a stream.
        .map(Statement::getSubject) // Extract the subject from each statement.
        .distinct() // Optional: remove duplicates if you expect subjects to repeat.
        .map(s -> model.getResource(s.getURI())) // Map each subject URI to the corresponding resource in the model.
        .toList(); // Return list of results.
  }

  /**
   * Get the list of objects within the model that has a subject and predicate property to the specified object.
   *
   * @param model The model to search.
   * @param subject The subject resource of the statement.
   * @param predicate The relationship predicate to filter.
   *
   * @return The list of matching resources.
   */
  public static List<Resource> getObjectsOfPredicateRelationship(OntModel model,
                                                                 Resource subject, ObjectProperty predicate) {
    StmtIterator stmtIterator = model.getBaseModel().listStatements(subject, predicate, (String) null);
    return stmtIterator.toList().stream() // Convert StmtIterator to a stream.
        .map(Statement::getObject) // Extract the object from each statement.
        .map(RDFNode::asResource) // Get the object resource.
        .toList(); // Return list of results.
  }

  /**
   * Get a list of all Mrs.SwitchingSubnet in the model.
   *
   * @param model
   * @return List of resources of Mrs.SwitchingSubnet type.
   */
  public static List<Resource> getAllSwitchingSubnet(OntModel model) {
    return getRdfType(model, Mrs.SwitchingSubnet);
  }

  public static List<Resource> getAllWithExistsDuring(OntModel model, Resource existsDuring) {
    //return model.listStatements(null, Nml.existsDuring, existsDuring)
    //    .toList().stream().map(Statement::getSubject)
    //    .map(s -> model.getResource(s.getURI())).toList();

    return model.listStatements(null, Nml.existsDuring, existsDuring)
        .toList().stream().map(Statement::getSubject).toList();
  }

  public static List<Resource> getAllSwitchingSubnetWithExistsDuring(OntModel model, Resource existsDuring) {
    return getAllWithExistsDuring(model, existsDuring).stream()
        .filter(s -> ModelUtil.isSwitchingSubnet(model, s)).toList();
  }

  /**
   * Return the BidirectionalPort resource of the provided resource identifier.
   *
   * @param model The model to search.
   * @param subject The BidirectionalPort resource identifier.
   * @return The parent resource referring to the BidirectionalPort.
   */
  public static Resource getBidirectionalPort(OntModel model, Resource subject) {
    return getResourceOfSubjectAndType(model, Nml.BidirectionalPort, subject);
  }

  /**
   * Return the parent BidirectionalPort resource of the provided BidirectionalPort resource.
   *
   * @param model The model to search.
   * @param subject The resource for witch we want to find the parent BidirectionalPort resource.
   * @return The parent resource referring to the BidirectionalPort.
   */
  public static Resource getParentBidirectionalPort(OntModel model, Resource subject) {
    List<Resource> subjects = getSubjectsOfPredicateRelationship(model, Nml.hasBidirectionalPort, subject);
    for (Resource s : subjects) {
      if (s.hasProperty(Rdf.type, Nml.BidirectionalPort)) {
        return s;
      }
    }
    return null;
  }

  /**
   * Merge model1 and model2 creating a new distinct model.
   *
   * @param model1 First model to merge.
   * @param model2 Second model to merge.
   * @return
   */
  public static OntModel mergeModels(OntModel model1, OntModel model2) {
    if (model1 == null) {
      throw new IllegalArgumentException("applyDeltaAddition encountered null original model");
    }

    if (model2 == null) {
      throw new IllegalArgumentException("applyDeltaAddition encountered null addition model");
    }

    OntModel result = ModelFactory.createOntologyModel(model1.getSpecification(), model1);
    result.add(model2);

    return result;
  }

  public static void applyDeltaAddition(OntModel original, OntModel addition) {
    if (original == null) {
      throw new IllegalArgumentException("applyDeltaAddition encountered null original model");
    }

    if (addition == null) {
      throw new IllegalArgumentException("applyDeltaAddition encountered null addition model");
    }

    original.add(addition);
  }

  public static void applyDeltaReduction(OntModel original, OntModel reduction) {
    if (original == null) {
      throw new IllegalArgumentException("applyDeltaAddition encountered null original model");
    }

    if (reduction == null) {
      throw new IllegalArgumentException("applyDeltaAddition encountered null addition model");
    }

    original.remove(reduction);
  }

  /**
   * Return a list of subjects contained in the specified model.
   *
   * @param m The model to search for subjects.
   * @return A list of subjects.
   */
  public static List<Resource> getSubjects(OntModel m) {
    return m.getBaseModel().listSubjects()
        .filterDrop(s -> Strings.isNullOrEmpty(s.getURI()))
        .filterDrop(s -> s.getURI().startsWith("http://www.w3.org"))
        .toList();
  }

  /**
   * For the given resolved resource return the Rdf.type URI as a string.
   *
   * @param res The resource for which to return the Rdf.type URI.
   * @return A string containing the URI of the Rdf.type object, null otherwise.
   */
  public static String getResourceTypeUri(Resource res) {
    Statement property = res.getProperty(Rdf.type);
    if (property != null && property.getObject().isURIResource()) {
      return property.getObject().asResource().getURI();
    }

    return null;
  }

  /**
   * Determine if the given resources is a Nml.BidirectionalPort.
   *
   * @param model The model containing the resource to test.
   * @param subject The resource to test.
   * @return True if this resource is of type Nml.BidirectionalPort, false otherwise.
   */
  public static boolean isBidirectionalPort(OntModel model, Resource subject) {
    return model.contains(subject, RDF.type, Nml.BidirectionalPort);
  }

  /**
   * Determine if the given resources is a Nml.SwitchingService.
   *
   * @param model The model containing the resource to test.
   * @param subject The resource to test.
   * @return True if this resource is of type Nml.SwitchingService, false otherwise.
   */
  public static boolean isSwitchingService(OntModel model, Resource subject) {
    return model.contains(subject, RDF.type, Nml.SwitchingService);
  }

  /**
   * Determine if the given resources is a Mrs.SwitchingSubnet.
   *
   * @param model The model containing the resource to test.
   * @param subject The resource to test.
   * @return True if this resource is of type Mrs.SwitchingSubnet, false otherwise.
   */
  public static boolean isSwitchingSubnet(OntModel model, Resource subject) {
    return model.contains(subject, RDF.type, Mrs.SwitchingSubnet);
  }

  /**
   * Determine if the given resources is a Mrs.BandwidthService.
   *
   * @param model The model containing the resource to test.
   * @param subject The resource to test.
   * @return True if this resource is of type Mrs.BandwidthService, false otherwise.
   */
  public static boolean isBandwidthService(OntModel model, Resource subject) {
    return model.contains(subject, RDF.type, Mrs.BandwidthService);
  }

  /**
   * Determine if the given resources is a Nml.existsDuring.
   *
   * @param model The model containing the resource to test.
   * @param subject The resource to test.
   * @return True if this resource is of type Nml.existsDuring, false otherwise.
   */
  public static boolean isExistsDuring(OntModel model, Resource subject) {
    return model.contains(subject, RDF.type, Nml.existsDuring);
  }
}

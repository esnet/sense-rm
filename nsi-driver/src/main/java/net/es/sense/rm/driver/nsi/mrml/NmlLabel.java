package net.es.sense.rm.driver.nsi.mrml;

import net.es.nsi.cs.lib.SimpleLabel;
import net.es.sense.rm.driver.schema.Nml;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

/**
 * Helper class that creates an nml:Label from equivalent RDF Resource.
 *
 * @author hacksaw
 */
public class NmlLabel {

  private final Resource label;
  private final Resource labelType;
  private final Statement labelValue;

  /**
   * Constructor parses RDF Resource representing an NML Label.
   *
   * @param label The nml:Label as an RDF resource.
   */
  public NmlLabel(Resource label) {
    this.label = label;
    labelType = label.getProperty(Nml.labeltype).getResource();
    labelValue = label.getProperty(Nml.value);
  }

  /**
   * Get the URI identifier for the label.
   * @return A string representing the URI identifier.
   */
  public String getId() {
    return label.getURI();
  }

  /**
   * Get the label type of the nml:Label (i.e. vlan).
   *
   * @return the labelType of the nml:Label.
   */
  public Resource getLabelType() {
    return labelType;
  }

  /**
   * Get the labelValue of the nml:Label (i.e. 1917).
   *
   * @return the labelValue.
   */
  public Statement getLabelValue() {
    return labelValue;
  }

  /**
   * Get the current nml:Label as a simple label string.
   * @return A simple label.
   */
  public SimpleLabel getSimpleLabel() {
    return new SimpleLabel(SimpleLabel.strip(labelType.getURI()), labelValue.getString());
  }

}

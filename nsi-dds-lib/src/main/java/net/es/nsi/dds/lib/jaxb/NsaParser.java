package net.es.nsi.dds.lib.jaxb;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import net.es.nsi.common.jaxb.JaxbParser;
import net.es.nsi.dds.lib.jaxb.nsa.NsaType;
import net.es.nsi.dds.lib.jaxb.nsa.ObjectFactory;

import java.io.IOException;

/**
 * A singleton to load the very expensive NMWG JAXBContext once.
 *
 * @author hacksaw
 */
public class NsaParser extends JaxbParser {

  private static final String PACKAGES = "net.es.nsi.dds.lib.jaxb.nsa";
  private static final ObjectFactory FACTORY = new ObjectFactory();

  private NsaParser() {
    super(PACKAGES);
  }

  /**
   * An internal static class that invokes our private constructor on object creation.
   */
  private static class ParserHolder {

    public static final NsaParser INSTANCE = new NsaParser();
  }

  /**
   * Returns an instance of this singleton class.
   *
   * @return An object of the NmwgParser.
   */
  public static NsaParser getInstance() {
    return ParserHolder.INSTANCE;
  }

  public NsaType readTopology(String filename) throws JAXBException, IOException {
    return this.parseFile(NsaType.class, filename);
  }

  public void writeTopology(String file, NsaType nsa) throws JAXBException, IOException {
    // Parse the specified file.
    JAXBElement<NsaType> element = FACTORY.createNsa(nsa);
    this.writeFile(element, file);
  }
}

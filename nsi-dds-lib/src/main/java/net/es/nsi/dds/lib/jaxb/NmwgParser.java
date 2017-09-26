package net.es.nsi.dds.lib.jaxb;

/**
 * A singleton to load the very expensive NMWG JAXBContext once.
 *
 * @author hacksaw
 */
public class NmwgParser extends JaxbParser {
    private static final String PACKAGES = "net.es.nsi.topology.translator.jaxb.nmwg";

    private NmwgParser() {
        super(PACKAGES);
    }

    /**
     * An internal static class that invokes our private constructor on object
     * creation.
     */
    private static class ParserHolder {
        public static final NmwgParser INSTANCE = new NmwgParser();
    }

    /**
     * Returns an instance of this singleton class.
     *
     * @return An object of the NmwgParser.
     */
    public static NmwgParser getInstance() {
            return ParserHolder.INSTANCE;
    }
}

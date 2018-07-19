package net.es.nsi.common.constants;

/**
 *
 * @author hacksaw
 */
public class Nsi {

  public static final String APPLICATION_XML = "application/xml";
  public static final String NSI_DDS_V1_XML = "application/vnd.ogf.nsi.dds.v1+xml";
  public static final String NSI_DDS_V1_JSON = "application/vnd.ogf.nsi.dds.v1+json";
  public static final String NSI_NSA_V1 = "application/vnd.ogf.nsi.nsa.v1+xml";
  public static final String NSI_TOPOLOGY_V1 = "application/vnd.ogf.nsi.topology.v1+xml";
  public static final String NSI_TOPOLOGY_V2 = "application/vnd.ogf.nsi.topology.v2+xml";
  public static final String NSI_CS_V2 = "application/vnd.org.ogf.nsi.cs.v2+soap";

  public static final String NSI_CS_REQUESTER_V2 = "application/vnd.ogf.nsi.cs.v2.requester+soap";
  public static final String NSI_CS_PROVIDER_V2 = "application/vnd.ogf.nsi.cs.v2.provider+soap";

  public static final String NSI_CS_PROVIDER_V1_1 = "application/vnd.ogf.nsi.cs.v1-1.provider+soap";
  public static final String NSI_CS_REQUESTER_V1_1 = "application/vnd.ogf.nsi.cs.v1-1.requester+soap";

  public static final String NSI_CS_PROVIDER_V1 = "application/vnd.ogf.nsi.cs.v1.provider+soap";
  public static final String NSI_CS_REQUESTER_V1 = "application/vnd.ogf.nsi.cs.v1.requester+soap";

  public static final String NSI_DOC_TYPE_NSA_V1 = "vnd.ogf.nsi.nsa.v1+xml";
  public static final String NSI_DOC_TYPE_TOPOLOGY_V1 = "vnd.ogf.nsi.topology.v1+xml";
  public static final String NSI_DOC_TYPE_TOPOLOGY_V2 = "vnd.ogf.nsi.topology.v2+xml";

  public static final String NSI_CS_URA = "vnd.ogf.nsi.cs.v2.role.uRA";
  public static final String NSI_CS_UPA = "vnd.ogf.nsi.cs.v2.role.uPA";
  public static final String NSI_CS_AGG = "vnd.ogf.nsi.cs.v2.role.aggregator";

  public static final String NML_PEERSWITH_RELATION = "http://schemas.ogf.org/nsi/2013/09/topology#peersWith";

  // Point-to-point service definitions.
  public static final String NSI_SERVICETYPE_EVTS = "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE";
  public static final String NSI_SERVICETYPE_EVTS_OPENNSA = "{http://schemas.ogf.org/nsi/2013/12/services/point2point}p2ps";
  public static final String NSI_SERVICETYPE_EVTS_OSCARS = "http://services.ogf.org/nsi/2013/07/definitions/EVTS.A-GOLE";
  public static final String NSI_SERVICETYPE_L2_LB_ES = "http://services.ogf.org/nsi/2018/06/descriptions/l2-lb-es";

  // Multi-point service definition.
  public static final String NSI_SERVICETYPE_L2_MP_ES = "http://services.ogf.org/nsi/2018/06/descriptions/l2-mp-es";
}

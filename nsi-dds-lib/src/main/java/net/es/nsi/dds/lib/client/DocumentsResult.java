package net.es.nsi.dds.lib.client;

import java.util.List;
import lombok.Data;
import net.es.nsi.dds.lib.jaxb.dds.DocumentType;

/**
 *
 * @author hacksaw
 */
@Data
public class DocumentsResult extends Result {
  List<DocumentType> documents;
}
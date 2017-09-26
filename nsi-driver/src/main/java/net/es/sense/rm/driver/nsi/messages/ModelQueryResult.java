package net.es.sense.rm.driver.nsi.messages;

import java.io.Serializable;
import java.util.Collection;
import net.es.sense.rm.driver.nsi.db.Model;

/**
 *
 * @author hacksaw
 */
@lombok.Data
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class ModelQueryResult implements Serializable {
    private static final long serialVersionUID = 1L;

    ModelQueryType type;
    Collection<Model> models;
}

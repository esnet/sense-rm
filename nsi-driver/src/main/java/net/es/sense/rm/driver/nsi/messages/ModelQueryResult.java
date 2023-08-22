package net.es.sense.rm.driver.nsi.messages;

import akka.actor.ActorPath;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.es.sense.rm.driver.nsi.db.Model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;

/**
 * Message for exchange of topology model results.
 *
 * @author hacksaw
 */
@lombok.Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper=true)
public class ModelQueryResult extends Message implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    ModelQueryType type;
    Collection<Model> models;

    public ModelQueryResult() {
        super();
    }

    public ModelQueryResult(String initiator) {
        super(initiator);
    }

    public ModelQueryResult(String initiator, ActorPath path) {
        super(initiator, path);
    }

    public ModelQueryResult(String initiator, ActorPath path, ModelQueryType type, Collection<Model> models) {
        super(initiator, path);
        this.type = type;
        this.models = models;
    }
}

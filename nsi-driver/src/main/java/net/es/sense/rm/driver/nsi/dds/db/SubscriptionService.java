package net.es.sense.rm.driver.nsi.dds.db;

import java.util.Collection;
import java.util.Set;

/**
 *
 * @author hacksaw
 */
public interface SubscriptionService {

  Subscription create(Subscription subscription);

  void delete(Subscription subscription);

  void delete(String ddsUrl);

  Collection<Subscription> getAll();

  Subscription get(String ddsurl);

  Subscription getByHref(String href);

  Set<String> keySet();

  Subscription update(Subscription subscription);

}

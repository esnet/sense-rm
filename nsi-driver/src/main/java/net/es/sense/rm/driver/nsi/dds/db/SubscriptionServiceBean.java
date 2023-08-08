package net.es.sense.rm.driver.nsi.dds.db;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;

/**
 *
 * @author hacksaw
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class SubscriptionServiceBean implements SubscriptionService {

  @Autowired
  private SubscriptionRepository subscriptionRepository;

  @Override
  public Set<String> keySet() {
    return subscriptionRepository.keySet();
  }

  @Override
  public Collection<Subscription> getAll() {
    return Lists.newArrayList(subscriptionRepository.findAll());
  }

  @Override
  public Subscription get(String ddsurl) {
    if (Strings.isNullOrEmpty(ddsurl)) {
      return null;
    }
    return subscriptionRepository.findOneByDdsURL(ddsurl);
  }

  @Override
  public Subscription getByHref(String href) {
    if (Strings.isNullOrEmpty(href)) {
      return null;
    }
    return subscriptionRepository.findByHref(href);
  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
  @Override
  public Subscription create(Subscription subscription) {
    if (Strings.isNullOrEmpty(subscription.getDdsURL())) {
      return null;
    }
    return subscriptionRepository.save(subscription);
  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
  @Override
  public Subscription update(Subscription subscription) {
    Subscription findOne = subscriptionRepository.findOneByDdsURL(subscription.getDdsURL());
    if (findOne == null) {
      return null;
    }
    return subscriptionRepository.save(subscription);
  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
  @Override
  public void delete(Subscription subscription) {
    subscriptionRepository.delete(subscription);
  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
  @Override
  public void delete(String ddsUrl) {
    subscriptionRepository.deleteByddsURL(ddsUrl);
  }
}

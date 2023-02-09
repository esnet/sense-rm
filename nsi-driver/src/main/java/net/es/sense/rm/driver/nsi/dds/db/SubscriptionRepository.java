package net.es.sense.rm.driver.nsi.dds.db;

import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author hacksaw
 */
@Repository
public interface SubscriptionRepository extends CrudRepository<Subscription, String> {

    public Subscription findOneByDdsURL(@Param("ddsURL") String ddsurl);

    public void deleteByddsURL(@Param("ddsURL") String ddsurl);

    @Query("select s.ddsURL from #{#entityName} s")
    public Set<String> keySet();

    public Subscription findByHref(String href);
}

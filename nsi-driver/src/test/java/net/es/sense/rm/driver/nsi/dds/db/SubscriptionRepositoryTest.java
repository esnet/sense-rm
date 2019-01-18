/*
 * SENSE Resource Manager (SENSE-RM) Copyright (c) 2016, The Regents
 * of the University of California, through Lawrence Berkeley National
 * Laboratory (subject to receipt of any required approvals from the
 * U.S. Dept. of Energy).  All rights reserved.
 *
 * If you have questions about your rights to use or distribute this
 * software, please contact Berkeley Lab's Innovation & Partnerships
 * Office at IPO@lbl.gov.
 *
 * NOTICE.  This Software was developed under funding from the
 * U.S. Department of Energy and the U.S. Government consequently retains
 * certain rights. As such, the U.S. Government has been granted for
 * itself and others acting on its behalf a paid-up, nonexclusive,
 * irrevocable, worldwide license in the Software to reproduce,
 * distribute copies to the public, prepare derivative works, and perform
 * publicly and display publicly, and to permit other to do so.
 *
 */
package net.es.sense.rm.driver.nsi.dds.db;

import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * @author hacksaw
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { SubscriptionRepository.class, Subscription.class, DbUnitTestConfiguration.class })
@DataJpaTest
@ActiveProfiles("test")
public class SubscriptionRepositoryTest {
  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private SubscriptionRepository subscriptions;

  private static final long LASTAUDIT = System.currentTimeMillis();
  private static final long CREATEDATE_1 = 651509795000L;
  private static final long CREATEDATE_2 = 967128995000L;

  private static final String DDSURL_1 = "https://nsi-am-sl.northwestern.edu/dds";
  private static final String HREF_1 = "https://nsi-am-sl.northwestern.edu/dds/notifications/2bb63849-1628-4b10-9465-cf768d8849c4";

  private static final String DDSURL_2 = "https://nsi-aggr-west.es.net/discovery";
  private static final String HREF_2 = "https://nsi-aggr-west.es.net/discovery/notifications/62b7dca8-06f5-4dac-9608-9dd5eccc28c6";

  private static long initDate(long value) {
    Date date = new Date();
    date.setTime(value);
    return date.getTime();
  }

  @Before
  public void buildDatabase() {
    Subscription s1 = new Subscription();
    s1.setDdsURL(DDSURL_1);
    s1.setHref(HREF_1);
    s1.setCreated(CREATEDATE_1);
    s1.setLastAudit(LASTAUDIT);
    subscriptions.save(s1);

    Subscription s2 = new Subscription();
    s2.setDdsURL(DDSURL_2);
    s2.setHref(HREF_2);
    s2.setCreated(CREATEDATE_2);
    s2.setLastAudit(LASTAUDIT);
    subscriptions.save(s2);
  }

  @Test
  public void testKeySet() {
    Assert.assertEquals(2, subscriptions.keySet().size());

    for (String key : subscriptions.keySet()) {
      if (!key.contentEquals(DDSURL_1) && !key.contentEquals(DDSURL_2)) {
        Assert.fail(key + " - Not expected value");
      }
    }
  }

  @Test
  public void testValues() {
    Subscription findOne = subscriptions.findOne(DDSURL_1);
    Assert.assertEquals(HREF_1, findOne.getHref());
    Assert.assertEquals(CREATEDATE_1, findOne.getCreated());
    Assert.assertEquals(LASTAUDIT, findOne.getLastAudit());

    findOne = subscriptions.findOne(DDSURL_2);
    Assert.assertEquals(HREF_2, findOne.getHref());
    Assert.assertEquals(CREATEDATE_2, findOne.getCreated());
    Assert.assertEquals(LASTAUDIT, findOne.getLastAudit());
  }

  @Test
  public void testFindByHref() {
    Subscription find = subscriptions.findByHref(HREF_1);
    Assert.assertEquals(DDSURL_1, find.getDdsURL());

    find = subscriptions.findByHref(HREF_2);
    Assert.assertEquals(DDSURL_2, find.getDdsURL());
  }
}

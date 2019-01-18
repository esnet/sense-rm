/*
 * SENSE Resource Manager (SENSE-RM) Copyright (c) 2016, The Regents
 * of the University of California, through Lawrence Berkeley National
 * Laboratory (subject to receipt of any required approvals from the
 * U.S. Dept. of Energy).  All rights reserved.
 *
 * If you have questions about your rights to use or distribute this
 * software, please contact Berkeley Lab's Innovation & Partnerships
// * Office at IPO@lbl.gov.
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
package net.es.sense.rm.measurements.db;

import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Some unit tests for the ReservationRepository class.
 *
 * @author hacksaw
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { MeasurementRepository.class, MeasurementResource.class, DbUnitTestConfiguration.class })
@DataJpaTest
@ActiveProfiles("test")
public class MeasurementRepositoryTest {
  @Autowired
  private MeasurementRepository measurements;

  /**
   * Build a small database for the unit tests.
   */
  public void buildSmallDatabase() {
    MeasurementResource m1 = new MeasurementResource();
    m1.setGenerated(1000);
    m1.setMeasurement(MeasurementType.DELTA_RESERVE);
    m1.setResource("742e978a-586c-4332-84ae-693762c6a202");
    m1.setMtype(MetricType.DURATION);
    m1.setMvalue("10000");
    measurements.save(m1);

    MeasurementResource m2 = new MeasurementResource();
    m2.setGenerated(2000);
    m2.setMeasurement(MeasurementType.MODEL_AUDIT);
    m2.setResource("ef815a68-a758-4d65-948b-b280be3df614");
    m2.setMtype(MetricType.DURATION);
    m2.setMvalue("20000");
    measurements.save(m2);
  }

  /**
   * Do a set of empty database tests.
   */
  @Test
  public void emptyTest() {
    assertEquals(measurements.count(), 0);

    Collection<MeasurementResource> m = measurements.findAllNewer(0);
    assertNotNull(m);
    assertEquals(m.size(), 0);
  }

  /**
   * Do a set of tests over a small number of database entries.
   */
  @Test
  public void basicTest() {
    buildSmallDatabase();

    assertEquals(measurements.count(), 2);

    Collection<MeasurementResource> m = measurements.findAllNewer(0);
    assertNotNull(m);
    assertEquals(m.size(), 2);

    m = measurements.findAllByOrderByGeneratedAsc();
    assertNotNull(m);
    assertEquals(m.size(), 2);

    m = measurements.findAllNewer(1000);
    assertNotNull(m);
    assertEquals(m.size(), 1);

    measurements.deleteAll();
    assertEquals(measurements.count(), 0);
  }
}

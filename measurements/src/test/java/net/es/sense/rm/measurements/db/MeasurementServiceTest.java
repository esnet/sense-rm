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
package net.es.sense.rm.measurements.db;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 *
 * @author hacksaw
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { MeasurementService.class, MeasurementServiceBean.class, MeasurementRepository.class, MeasurementResource.class, DbUnitTestConfiguration.class })
@AutoConfigureTestDatabase
@ActiveProfiles("test")
public class MeasurementServiceTest {
  @Autowired
  private MeasurementService measurementService;

  private final static List<MeasurementResource> DATA = new ArrayList<>();

  static {
    DATA.add(new MeasurementResource(
            "ef815a68-a758-4d65-948b-b280be3df614",
            MeasurementType.DELTA_RESERVE,
            "/path/to/resource/model/fa501f43-a8a7-4c8e-a81f-6954d095baf1",
            20000,
            MetricType.DURATION,
            "20000"));
    DATA.add(new MeasurementResource(
            "cad8bf46-007b-45fb-8af7-667f57f8b93e",
            MeasurementType.DELTA_COMMIT,
            "/path/to/resource/model/0ae8e898-95ed-4199-bedb-ae5a46202223",
            30000,
            MetricType.DURATION,
            "30000"));
    DATA.add(new MeasurementResource(
            "742e978a-586c-4332-84ae-693762c6a202",
            MeasurementType.MODEL_AUDIT,
            "/path/to/resource/model/0695722d-509e-422a-be41-626a49124b8d",
            10000,
            MetricType.DURATION,
            "10000"));
    DATA.add(new MeasurementResource(
            "9bee3f58-e031-4f7b-a1c4-91c6c82cb128",
            MeasurementType.MODEL_AUDIT,
            "/path/to/resource/model/aebe2b4f-50fa-4bc5-897a-9ccc09564926",
            40000,
            MetricType.DURATION,
            "40000"));
  }

  @Test
  public void emptyTest() {
    // Check all the API work with an empty database.
    Collection<MeasurementResource> m = measurementService.get();
    assertNotNull(m);
    assertEquals(m.size(), 0);

    assertNull(measurementService.first());
    assertNull(measurementService.last());

    // Delete of non-existent id should now be silent.
    measurementService.delete("12345");

    assertEquals(measurementService.prune().size(), 0);

    // This should not error.
    measurementService.delete();
  }

@Test
  public void makeOneTest() {
    // Add one entry to the database.
    MeasurementResource m1 = new MeasurementResource();
    m1.setMeasurement(MeasurementType.DELTA_RESERVE);
    m1.setResource("742e978a-586c-4332-84ae-693762c6a202");
    m1.setMtype(MetricType.DURATION);
    m1.setMvalue("10000");
    MeasurementResource stored = measurementService.store(m1);
    assertNotNull(stored);

    // Test to see if we have the correct first record.
    assertEquals(measurementService.first().getId(), stored.getId());

    // Test to see if we have the correct last record.
    assertEquals(measurementService.last().getId(), stored.getId());

    Collection<MeasurementResource> m = measurementService.get();
    assertNotNull(m);
    assertEquals(m.size(), 1);
    assertEquals(measurementService.size(), 1);
    Optional<MeasurementResource> findFirst = m.stream().findFirst();
    assertEquals("742e978a-586c-4332-84ae-693762c6a202", findFirst.get().getResource());

    // This should not error.
    measurementService.delete(findFirst.get());

    // Should be empty.
    m = measurementService.get();
    assertNotNull(m);
    assertEquals(m.size(), 0);

    // This should not error.
    measurementService.delete();
  }

@Test
  public void makeSomeTest() {
    // Add a list of entries to the database.
    DATA.forEach(m -> measurementService.store(m));
    assertEquals(measurementService.size(), DATA.size());

    Collection<MeasurementResource> m = measurementService.get();
    assertNotNull(m);
    assertEquals(m.size(), DATA.size());

    // Test to see if we have the correct first record.
    assertEquals("742e978a-586c-4332-84ae-693762c6a202", measurementService.first().getId());

    // Test to see if we have the correct last record.
    assertEquals("9bee3f58-e031-4f7b-a1c4-91c6c82cb128", measurementService.last().getId());

    m = measurementService.get(20000);
    assertNotNull(m);
    assertEquals(m.size(), 2);

    // This should not error.
    measurementService.delete();
  }
}

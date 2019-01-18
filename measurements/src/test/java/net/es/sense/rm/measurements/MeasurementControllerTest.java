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
package net.es.sense.rm.measurements;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.measurements.db.MeasurementResource;
import net.es.sense.rm.measurements.db.MeasurementType;
import net.es.sense.rm.measurements.db.MetricType;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * @author hacksaw
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { MeasurementController.class, DbUnitTestConfiguration.class })
@ActiveProfiles("test")
@DataJpaTest
public class MeasurementControllerTest {
  @Autowired
  private MeasurementController measurementController;

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
    DATA.add(new MeasurementResource(
            "d6013e19-7316-4c88-bac7-c2043fc897a5",
            MeasurementType.MODEL_AUDIT,
            "/path/to/resource/model/03f7bc43-e36b-4f07-ae69-cd5fb5281bee",
            50000,
            MetricType.DURATION,
            "50000"));
    DATA.add(new MeasurementResource(
            "08a6e407-89bc-499e-b8b4-1b42f0b23ed8",
            MeasurementType.MODEL_AUDIT,
            "/path/to/resource/model/0602a12e-0d9d-44fc-a076-a4c7e6262677",
            60000,
            MetricType.DURATION,
            "60000"));
    DATA.add(new MeasurementResource(
            "b417188a-1eee-4933-9083-8002d383e0f9",
            MeasurementType.MODEL_AUDIT,
            "/path/to/resource/model/9e5ffbde-8c7f-4e06-b069-072d7f2b52b6",
            70000,
            MetricType.DURATION,
            "70000"));
  }

  /**
   * Test of add method, of class MeasurementQueue.
   */
  @Test
  public void test() {
    // Add a list of entries to the database.
    DATA.stream().forEach(m -> measurementController.add(m));
    assertEquals(DATA.size(), measurementController.size());
  }
}

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
package net.es.sense.rm.api;

import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.measurements.MeasurementController;
import net.es.sense.rm.model.DeltaRequest;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 *
 * @author hacksaw
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = WebEnvironment.RANDOM_PORT,
  classes = {
    SenseRmController.class,
    TestDriver.class,
    ApiTestConfiguration.class
  })
@EntityScan(basePackages = {"net.es.sense.rm.measurements"})
@EnableJpaRepositories("net.es.sense.rm.measurements")
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SenseRmControllerTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  ApplicationContext context;

  private static final JsonProxy proxy = new JsonProxy();

  @Test
  public void testRoot() throws Exception {
    log.info("Testing SENSE-N-RM / resource");

    MeasurementController userRepoFromContext = context.getBean(MeasurementController.class);

    // We expect an OK for root listing of available resources.
    mvc.perform(get("/api/sense/v1").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(2)));

    // We expect a NOT_FOUND for a bad resource reference.
    mvc.perform(get("/api/sense/v1/modelz").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());

  }

  @Test
  public void testModels() throws Exception {
    log.info("Testing SENSE-N-RM /models resource");

    // Query the current topology model.
    mvc.perform(get("/api/sense/v1/models?current=true").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is("5027e105-ea2f-4452-b843-7a21f3b65bc8")));

    // Query all available topology models.
    mvc.perform(get("/api/sense/v1/models").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(52)));

    // We expect an error for an unsupported model type.
    mvc.perform(get("/api/sense/v1/models?model=poop").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

    // We expect an OK for a turtle model type.
    mvc.perform(get("/api/sense/v1/models?model=turtle").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

    // We expect an OK for a ttl model type.
    mvc.perform(get("/api/sense/v1/models?model=ttl").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

    // We expect an OK for a encode type.
    mvc.perform(get("/api/sense/v1/models?encode=true").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

    // Test the If-Modified-Since query - should return two resources.
    mvc.perform(
              get("/api/sense/v1/models")
                      .contentType(MediaType.APPLICATION_JSON)
                      .header("If-Modified-Since", "Fri, 27 Jan 2018 11:15:12 GMT")
            )
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(header().string("Last-Modified", "Mon, 29 Jan 2018 11:15:13 GMT"));

    // Test the If-Modified-Since query - should return three resources.
    mvc.perform(
              get("/api/sense/v1/models")
                      .contentType(MediaType.APPLICATION_JSON)
                      .header("If-Modified-Since", "Fri, 27 Jan 2018 11:15:11 GMT")
            )
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(header().string("Last-Modified", "Mon, 29 Jan 2018 11:15:13 GMT"));

    // Test the If-Modified-Since query - should return NOT_MODIFIED.
    mvc.perform(
              get("/api/sense/v1/models")
                      .contentType(MediaType.APPLICATION_JSON)
                      .header("If-Modified-Since", "Mon, 29 Jan 2018 11:15:13 GMT")
            )
            .andExpect(status().isNotModified());

    // Test the If-Modified-Since query - should return NOT_MODIFIED.
    mvc.perform(
              get("/api/sense/v1/models?current=true")
                      .contentType(MediaType.APPLICATION_JSON)
                      .header("If-Modified-Since", "Mon, 29 Jan 2018 11:15:13 GMT")
            )
            .andExpect(status().isNotModified());
  }

  @Test
  public void testModel() throws Exception {
    log.info("Testing SENSE-N-RM /models/{id} resource");

    // We expect an OK for this resource query.
    mvc.perform(
              get("/api/sense/v1/models/5027e105-ea2f-4452-b843-7a21f3b65bc8")
                      .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is("5027e105-ea2f-4452-b843-7a21f3b65bc8")))
            .andExpect(header().string("Last-Modified", "Mon, 29 Jan 2018 11:15:13 GMT"));

    // We expect a NOT_FOUND for this query.
    mvc.perform(
              get("/api/sense/v1/models/thisisabadid")
                      .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isNotFound());

    // We expect a NOT_MODIFIED for this resource query.
    mvc.perform(
              get("/api/sense/v1/models/5027e105-ea2f-4452-b843-7a21f3b65bc8")
                      .contentType(MediaType.APPLICATION_JSON)
                      .header("If-Modified-Since", "Mon, 29 Jan 2018 11:15:13 GMT")
            )
            .andExpect(status().isNotModified());

    // We expect an OK for this resource query.
    mvc.perform(
              get("/api/sense/v1/models/5027e105-ea2f-4452-b843-7a21f3b65bc8")
                      .contentType(MediaType.APPLICATION_JSON)
                      .header("If-Modified-Since", "Mon, 29 Jan 2018 11:15:12 GMT")
            )
            .andExpect(status().isOk())
            .andExpect(header().string("Last-Modified", "Mon, 29 Jan 2018 11:15:13 GMT"));
  }

  @Test
  public void testDeltas() throws Exception {
    log.info("Testing SENSE-N-RM /deltas resource");

    // Query the list of delta resources.
    mvc.perform(get("/api/sense/v1/deltas").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(header().string("Last-Modified", "Mon, 29 Jan 2018 11:15:13 GMT"));

    // Query a resource newer than specific date - returns one resource.
    mvc.perform(
              get("/api/sense/v1/deltas")
                      .contentType(MediaType.APPLICATION_JSON)
                      .header("If-Modified-Since", "Mon, 29 Jan 2018 11:15:12 GMT")
            )
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is("922f4388-c8f6-4014-b6ce-5482289b0200")))
            .andExpect(header().string("Last-Modified", "Mon, 29 Jan 2018 11:15:13 GMT"));

    // Query a resource newer than specific date - returns two resource.
    mvc.perform(
              get("/api/sense/v1/deltas")
                      .contentType(MediaType.APPLICATION_JSON)
                      .header("If-Modified-Since", "Thu, 25 Jan 2018 11:15:12 GMT")
            )
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(header().string("Last-Modified", "Mon, 29 Jan 2018 11:15:13 GMT"));
  }

  @Test
  public void testDelta() throws Exception {
    log.info("Testing SENSE-N-RM /deltas/{id} resource");

    // We expect an OK for this resource query.
    mvc.perform(
              get("/api/sense/v1/deltas/922f4388-c8f6-4014-b6ce-5482289b0200")
                      .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id", is("922f4388-c8f6-4014-b6ce-5482289b0200")))
            .andExpect(header().string("Last-Modified", "Mon, 29 Jan 2018 11:15:13 GMT"));

    // We expect a NOT_FOUND for this query.
    mvc.perform(
              get("/api/sense/v1/deltas/thisisabadid")
                      .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isNotFound());

    // We expect a NOT_MODIFIED for this resource query.
    mvc.perform(
              get("/api/sense/v1/deltas/922f4388-c8f6-4014-b6ce-5482289b0200")
                      .contentType(MediaType.APPLICATION_JSON)
                      .header("If-Modified-Since", "Mon, 29 Jan 2018 11:15:13 GMT")
            )
            .andExpect(status().isNotModified());

    // We expect an OK for this resource query.
    mvc.perform(
              get("/api/sense/v1/deltas/922f4388-c8f6-4014-b6ce-5482289b0200")
                      .contentType(MediaType.APPLICATION_JSON)
                      .header("If-Modified-Since", "Mon, 29 Jan 2018 11:15:12 GMT")
            )
            .andExpect(status().isOk())
            .andExpect(header().string("Last-Modified", "Mon, 29 Jan 2018 11:15:13 GMT"));
  }

  @Test
  public void testPropagateDelta() throws Exception {
    log.info("Testing SENSE-N-RM POST /deltas/{id} resource");

    // This should fail with CONFLICT because a delta with this identifier already exists.
    DeltaRequest deltaRequest = new DeltaRequest();
    deltaRequest.setId("922f4388-c8f6-4014-b6ce-5482289b0200");
    deltaRequest.setModelId("5027e105-ea2f-4452-b843-7a21f3b65bc8");
    deltaRequest.setAddition("H4sIAFW7CVoAA+1aW2/aMBR+76+IwmNLbmxCZB3q2lV96dZqoGlPrUxigtVge7ZD4N/PCRAuCSlJWLtJzktV8Ln5nO+c7yhcUQbHaK6ROHQ1TbucCEFd04zj2Ig7BmGB6ViWY1pdU55o9TXj7GolwfzxAQm71+uZUshx2vJQmy+wAPM25jvic+4fNmibv77dD7wJnIJ9m9w9JGSZUi41uBE8u4wYdkkwdjEUMWEv678fXMeyP7qCUBKSYOFS3j/TVs9a/VIPN6R4agNPQ2nH7phWxxwBDlsTwK+Rjxj0BCIYhI+EiY2aTN3RLrizEGAqlZzb3V63kv88RsKbuLc/hwPjS/vu4f42VbZUlLkEXolwyvgyQtsx16pbg1Q1wsEgGknrfe1imYcfkJOIeVD+n1TPMDmifXr1FgttCBDkL04PHQrE5Hz/CmbENqZgHqAAUEIN+bEbIvzyGYy4YMATboHA80bgORPwCMbL3Gm2foTruQIYwZDggA9J/aznE9ev4wnEHvFlAgocKVfhmLZlQjGBTDrYSmqmlv0TQ0FKd/bRcNEASTUiCoHM7iAGlBZeqy5YBPUUolUKnTJEGBKLPCb3nw2m8nhLeu8jIxQysdiKLT3Go9H6q4dxeczlHm4pTnyBvyM0AyHEIrNcV3GVvpyvBDfNTMXOlkvvfaKkaS8rLhqxoPC9cCgzFBUY15PbSqt1N9y9zN6EgPPt0muYKQ7ZDHnwfBTnM7V/75Vq6RpgP0a+mAyWFvqnGEJgBpBMYAhvAAXeDkg392itH/3pKeEwyQCoazBgAEchYKWmTmBHzj40jaZvFVa+x2V2rGPGbKFSBpNiesvsFMNYDyIg0yYg9KUjFPq1I4owKhiR+ojyFKibcs76egrPtKnvgudQo17BefdwI1L5Okk+UBFkhnzIVwzyhGRpi+VWblbvz/iLaE6lpUVNxtNNxtrssv5V52tJLVd/d7na7hf11pwlPE625W9jtqZHaw50Op+2eFujflTG/8r2m4ZcUPE/xf8U/1P8T/E/TfE/xf8U/1P8T/E/xf8U//uf+F8TEqEmkppITVjoPzGUyt8nrVek70QcWJKOJ7xoDAWapi+RjhYarvysJJRm6o6RiJaKcZRBZhUxN304RrJhyOJtra72a/ZJmbKjX581axFHS29+TVLDdLaaNPsBiyydPz7gLXj4JAAA");

    mvc.perform(
              post("/api/sense/v1/deltas?encode=true")
                      .contentType(MediaType.APPLICATION_JSON)
                      .accept(MediaType.APPLICATION_JSON)
                      .content(proxy.serialize(deltaRequest))
            )
            .andExpect(status().isConflict());

    // Test an invalid modelId returns NOT_FOUND.
    deltaRequest.setId("922f4388-c8f6-4014-b6ce-5482289b02xx");
    deltaRequest.setModelId("???5027e105-ea2f-4452-b843-7a21f3b65bc8");
    mvc.perform(
              post("/api/sense/v1/deltas?encode=true")
                      .contentType(MediaType.APPLICATION_JSON)
                      .accept(MediaType.APPLICATION_JSON)
                      .content(proxy.serialize(deltaRequest))
            )
            .andExpect(status().isNotFound());

    // Now one that should be successful where we specify the delta Id.
    deltaRequest.setId("8907024b-7898-48fd-829d-4f1a577dbbd1");
    deltaRequest.setModelId("5027e105-ea2f-4452-b843-7a21f3b65bc8");
    mvc.perform(
              post("/api/sense/v1/deltas?encode=true")
                      .contentType(MediaType.APPLICATION_JSON)
                      .accept(MediaType.APPLICATION_JSON)
                      .content(proxy.serialize(deltaRequest))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is("8907024b-7898-48fd-829d-4f1a577dbbd1")))
            .andExpect(jsonPath("$.modelId", is("5027e105-ea2f-4452-b843-7a21f3b65bc8")));

    // Make sure we get a delta id assigned when we leave it empty.
    deltaRequest.setId(null);
    mvc.perform(
              post("/api/sense/v1/deltas?encode=true")
                      .contentType(MediaType.APPLICATION_JSON)
                      .accept(MediaType.APPLICATION_JSON)
                      .content(proxy.serialize(deltaRequest))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.modelId", is("5027e105-ea2f-4452-b843-7a21f3b65bc8")));

    // Now try to commit a delta id that does not exist.

    mvc.perform(
              put("/api/sense/v1/deltas/8907024b-7898-48fd-829d-4f1a577dbbd1/actions/commit")
                      .contentType(MediaType.APPLICATION_JSON)
                      .accept(MediaType.APPLICATION_JSON)
                      .content(proxy.serialize(deltaRequest))
            )
            .andExpect(status().isNoContent());
  }
}

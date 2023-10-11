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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 *
 * @author hacksaw
 */
@Schema(implementation = MeasurementType.class, name = "measurement",
    description =" The type of operational measurement.")
@XmlType(name = "measurement")
@XmlEnum
public enum MeasurementType {
  DELTA_RESERVE(1001, "DELTA_RESERVE"),
  DELTA_COMMIT(1002, "DELTA_COMMIT"),

  MODEL_AUDIT(2001, "MODEL_AUDIT"),

  END(9000, "");

  @Schema(name = "code", description = "An integer value for the name of the SENSE measurement.",
      accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.REQUIRED)
  private final int code;

  @Schema(name = "label", description = "A text string name for the SENSE measurement.",
      accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.REQUIRED)
  private final String label;

  private MeasurementType(int code, String label) {
    this.code = code;
    this.label = label;
  }

  /**
   * @return the code
   */
  @Schema(name = "getCode", description = "Returns an integer value for the name of the SENSE measurement.",
      requiredMode = Schema.RequiredMode.REQUIRED)
  public int getCode() {
    return code;
  }

  /**
   * @return the label
   */
  @Schema(name = "getLabel", description = "Returns an string value for the name of the SENSE measurement.",
      requiredMode = Schema.RequiredMode.REQUIRED)
  public String getLabel() {
    return label;
  }

  @Override
  public String toString() {
    return label;
  }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iotdb.tsfile.write.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.read.common.Path;
import org.apache.iotdb.tsfile.write.record.RowBatch;

/**
 * Schema stores the schema of the measurements and devices that exist in this file. All devices
 * written to the same TsFile shall have the same schema. Schema takes the JSON schema file as a
 * parameter and registers measurements in such JSON. Schema also records all existing device IDs in
 * this file.
 */
public class Schema {

  /**
   * Path (device + measurement) -> measurementSchema By default, use the LinkedHashMap to store the
   * order of insertion
   */
  private Map<Path, MeasurementSchema> measurementSchemaMap;

  /**
   * template name -> (measuremnet -> MeasurementSchema)
   */
  private Map<String, Map<String, MeasurementSchema>> deviceTemplates;

  /**
   * device -> template name
   */
  private Map<String, String> devices;

  /**
   * register a measurement schema map.
   */

  public Schema() {
    this.measurementSchemaMap = new LinkedHashMap<>();
  }

  public Schema(Map<Path, MeasurementSchema> knownSchema) {
    this.measurementSchemaMap = knownSchema;
  }

  /**
   * Create a row batch to write aligned data
   *
   * @param deviceId the name of the device specified to be written in
   */
  public RowBatch createRowBatch(String deviceId) {
    return new RowBatch(deviceId, new ArrayList<>(measurementSchemaMap.values()));
  }

  /**
   * Create a row batch to write aligned data
   *
   * @param deviceId     the name of the device specified to be written in
   * @param maxBatchSize max size of rows in batch
   */
  public RowBatch createRowBatch(String deviceId, int maxBatchSize) {
    return new RowBatch(deviceId, new ArrayList<>(measurementSchemaMap.values()), maxBatchSize);
  }

  public void registerTimeseries(Path path, MeasurementSchema descriptor) {
    this.measurementSchemaMap.put(path, descriptor);
  }

  public void registerDeviceTemplate(String templateName, Map<String, MeasurementSchema> template) {
    if (deviceTemplates == null) {
      deviceTemplates = new HashMap<>();
    }
    this.deviceTemplates.put(templateName, template);
  }

  public void extendTemplate(String templateName, MeasurementSchema descriptor) {
    Map<String, MeasurementSchema> template = this.deviceTemplates
        .getOrDefault(templateName, new HashMap<>());
    template.put(descriptor.getMeasurementId(), descriptor);
    this.deviceTemplates.put(templateName, template);
  }

  public void registerDevice(String deviceId, String templateName) {
    if (!deviceTemplates.containsKey(templateName)) {
      return;
    }
    if (devices == null) {
      devices = new HashMap<>();
    }
    this.devices.put(deviceId, templateName);
    Map<String, MeasurementSchema> template = deviceTemplates.get(templateName);
    for (Map.Entry<String, MeasurementSchema> entry : template.entrySet()) {
      Path path = new Path(deviceId, entry.getKey());
      registerTimeseries(path, entry.getValue());
    }
  }

  public MeasurementSchema getSeriesSchema(Path path) {
    return measurementSchemaMap.get(path);
  }

  public TSDataType getTimeseriesDataType(Path path) {
    if (!measurementSchemaMap.containsKey(path)) {
      return null;
    }
    return measurementSchemaMap.get(path).getType();
  }

  public boolean containsDevice(String device) {
    return devices.containsKey(device);
  }

  public Map<Path, MeasurementSchema> getMeasurementSchemaMap() {
    return measurementSchemaMap;
  }

  /**
   * check if this schema contains a measurement named measurementId.
   */
  public boolean containsTimeseries(Path path) {
    return measurementSchemaMap.containsKey(path);
  }

}

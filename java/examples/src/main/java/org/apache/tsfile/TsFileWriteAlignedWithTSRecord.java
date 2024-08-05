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

package org.apache.tsfile;

import org.apache.tsfile.enums.TSDataType;
import org.apache.tsfile.exception.write.WriteProcessException;
import org.apache.tsfile.file.metadata.enums.TSEncoding;
import org.apache.tsfile.fileSystem.FSFactoryProducer;
import org.apache.tsfile.read.common.Path;
import org.apache.tsfile.write.TsFileWriter;
import org.apache.tsfile.write.record.TSRecord;
import org.apache.tsfile.write.record.datapoint.DataPoint;
import org.apache.tsfile.write.schema.IMeasurementSchema;
import org.apache.tsfile.write.schema.MeasurementSchema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TsFileWriteAlignedWithTSRecord {
  private static final Logger logger =
      LoggerFactory.getLogger(TsFileWriteAlignedWithTSRecord.class);

  public static void main(String[] args) throws IOException {
    File f = FSFactoryProducer.getFSFactory().getFile("alignedRecord.tsfile");
    if (f.exists()) {
      try {
        Files.delete(f.toPath());
      } catch (IOException e) {
        throw new IOException("can not delete " + f.getAbsolutePath());
      }
    }

    try (TsFileWriter tsFileWriter = new TsFileWriter(f)) {
      List<IMeasurementSchema> measurementSchemas = new ArrayList<>();
      measurementSchemas.add(
          new MeasurementSchema(Constant.SENSOR_1, TSDataType.INT64, TSEncoding.RLE));
      measurementSchemas.add(
          new MeasurementSchema(Constant.SENSOR_2, TSDataType.INT64, TSEncoding.RLE));
      measurementSchemas.add(
          new MeasurementSchema(Constant.SENSOR_3, TSDataType.INT64, TSEncoding.RLE));
      measurementSchemas.add(
          new MeasurementSchema(Constant.SENSOR_4, TSDataType.BLOB, TSEncoding.PLAIN));
      measurementSchemas.add(
          new MeasurementSchema(Constant.SENSOR_5, TSDataType.STRING, TSEncoding.PLAIN));
      measurementSchemas.add(
          new MeasurementSchema(Constant.SENSOR_6, TSDataType.DATE, TSEncoding.PLAIN));
      measurementSchemas.add(
          new MeasurementSchema(Constant.SENSOR_7, TSDataType.TIMESTAMP, TSEncoding.PLAIN));

      // register timeseries
      tsFileWriter.registerAlignedTimeseries(new Path(Constant.DEVICE_1), measurementSchemas);

      // example1
      writeAligned(tsFileWriter, Constant.DEVICE_1, measurementSchemas, 10000, 0, 0);
    } catch (WriteProcessException e) {
      logger.error("write TSRecord failed", e);
    }
  }

  private static void writeAligned(
      TsFileWriter tsFileWriter,
      String deviceId,
      List<IMeasurementSchema> schemas,
      long rowSize,
      long startTime,
      long startValue)
      throws IOException, WriteProcessException {
    for (long time = startTime; time < rowSize + startTime; time++) {
      // construct TsRecord
      TSRecord tsRecord = new TSRecord(time, deviceId);
      for (IMeasurementSchema schema : schemas) {
        tsRecord.addTuple(
            DataPoint.getDataPoint(
                schema.getType(),
                schema.getMeasurementId(),
                Objects.requireNonNull(DataGenerator.generate(schema.getType(), (int) startValue))
                    .toString()));
        startValue++;
      }
      // write
      tsFileWriter.writeAligned(tsRecord);
    }
  }
}

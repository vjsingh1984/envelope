/**
 * Licensed to Cloudera, Inc. under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Cloudera, Inc. licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.labs.envelope.plan.time;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import com.cloudera.labs.envelope.load.ProvidesAlias;
import com.cloudera.labs.envelope.spark.RowWithSchema;
import com.cloudera.labs.envelope.utils.RowUtils;
import com.google.common.collect.Lists;
import com.typesafe.config.Config;

public class StringDatetimeTimeModel implements TimeModel, ProvidesAlias {

  public static final String DATETIME_FORMAT_CONFIG = "format";
  
  private StructField field;
  private DateFormat format;
  private Date current;
  private Date farFuture;

  @Override
  public void configure(Config config, List<String> fieldNames) {
    if (config.hasPath(DATETIME_FORMAT_CONFIG)) {
      this.format = new SimpleDateFormat(config.getString(DATETIME_FORMAT_CONFIG));
    }
    else {
      this.format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    }
    
    this.field = DataTypes.createStructField(fieldNames.get(0), DataTypes.StringType, true);
    this.farFuture = new Date(253402214400000L);
  }

  @Override
  public void configureCurrentSystemTime(long currentSystemTimeMillis) {
    this.current = new Date(currentSystemTimeMillis);
  }
  
  @Override
  public int compare(Row first, Row second) {
    Date firstDate;
    Date secondDate;
    try {
      firstDate = format.parse(first.<String>getAs(field.name()));
      secondDate = format.parse(second.<String>getAs(field.name()));
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
    
    return firstDate.compareTo(secondDate);
  }

  @Override
  public StructType getSchema() {
    return DataTypes.createStructType(Lists.newArrayList(field));
  }

  @Override
  public Row setFarFutureTime(Row row) {
    return RowUtils.set(row, field.name(), format.format(farFuture));
  }

  @Override
  public Row setCurrentSystemTime(Row row) {
    return RowUtils.set(row, field.name(), format.format(current));
  }

  @Override
  public Row setPrecedingSystemTime(Row row) {
    return RowUtils.set(row, field.name(), format.format(new Date(current.getTime() - 1)));
  }

  @Override
  public Row appendFields(Row row) {
    return RowUtils.append(row, field.name(), field.dataType(), null);
  }

  @Override
  public Row getTime(Row row) {
    return new RowWithSchema(getSchema(), RowUtils.get(row, field.name()));
  }

  @Override
  public Row getPrecedingTime(Row row) {
    Date date;
    try {
      date = format.parse(row.<String>getAs(field.name()));
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
    String precedingDateString = format.format(new Date(date.getTime() - 1));
    
    return new RowWithSchema(getSchema(), precedingDateString);
  }
  
  @Override
  public String getAlias() {
    return "stringdatetime";
  }

}

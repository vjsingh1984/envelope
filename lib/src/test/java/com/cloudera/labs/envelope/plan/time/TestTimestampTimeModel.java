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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;

import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.junit.Before;
import org.junit.Test;

import com.cloudera.labs.envelope.spark.RowWithSchema;
import com.cloudera.labs.envelope.utils.PlannerUtils;
import com.google.common.collect.Lists;
import com.typesafe.config.ConfigFactory;

public class TestTimestampTimeModel {

  private TimeModel tm;
  private StructField field;
  private StructType schema;
  private Row first, second, third;
  
  @Before
  public void before() {
    field = DataTypes.createStructField("time", DataTypes.TimestampType, true);
    schema = DataTypes.createStructType(Lists.newArrayList(field));
    
    tm = new TimestampTimeModel();
    tm.configure(ConfigFactory.empty(), Lists.newArrayList(field.name()));
    
    Timestamp firstTs = new Timestamp(1000L);
    firstTs.setNanos(1000);
    Timestamp secondTs = new Timestamp(2000L);
    secondTs.setNanos(100);
    Timestamp thirdTs = new Timestamp(2000L);
    thirdTs.setNanos(101);
    
    first = new RowWithSchema(schema, firstTs);
    second = new RowWithSchema(schema, secondTs);
    third = new RowWithSchema(schema, thirdTs);
  }
  
  @Test
  public void testSchema() {
    assertEquals(schema, tm.getSchema());
  }

  @Test
  public void testBefore() {
    assertTrue(PlannerUtils.before(tm, first, second));
    assertTrue(PlannerUtils.before(tm, first, third));
    assertTrue(PlannerUtils.before(tm, second, third));
    assertFalse(PlannerUtils.before(tm, second, first));
    assertFalse(PlannerUtils.before(tm, third, second));
    assertFalse(PlannerUtils.before(tm, third, first));
  }

  @Test
  public void testSimultaneous() {
    assertTrue(PlannerUtils.simultaneous(tm, first, first));
    assertFalse(PlannerUtils.simultaneous(tm, first, second));
    assertFalse(PlannerUtils.simultaneous(tm, first, third));
    assertFalse(PlannerUtils.simultaneous(tm, second, third));
  }

  @Test
  public void testAfter() {
    assertFalse(PlannerUtils.after(tm, first, second));
    assertFalse(PlannerUtils.after(tm, first, third));
    assertFalse(PlannerUtils.after(tm, second, third));
    assertTrue(PlannerUtils.after(tm, second, first));
    assertTrue(PlannerUtils.after(tm, third, second));
    assertTrue(PlannerUtils.after(tm, third, first));
  }

  @Test
  public void testFarFuture() {
    Row ff = tm.setFarFutureTime(first);
    
    // > 2100-01-01
    assertTrue(ff.<Timestamp>getAs(field.name()).after(new Timestamp(4102444800000L)));
  }

  @Test
  public void testCurrentSystemTime() {
    Long currentTimeMillis = System.currentTimeMillis();
    tm.configureCurrentSystemTime(currentTimeMillis);
    Row current = tm.setCurrentSystemTime(first);
    
    assertEquals(current.<Timestamp>getAs(field.name()), new Timestamp(currentTimeMillis));
  }

  @Test
  public void testPrecedingSystemTime() {
    Long currentTimeMillis = System.currentTimeMillis();
    tm.configureCurrentSystemTime(currentTimeMillis);
    Row current = tm.setPrecedingSystemTime(first);
    
    Timestamp expected = new Timestamp(currentTimeMillis - 1L);
    expected.setNanos(expected.getNanos() + 999999);
    
    assertEquals(current.<Timestamp>getAs(field.name()), expected);
  }

  @Test
  public void testAppendFields() {
    StructType withoutSchema = DataTypes.createStructType(
        Lists.newArrayList(
            DataTypes.createStructField("other", DataTypes.StringType, true)));
    
    Row without = new RowWithSchema(withoutSchema, "hello");
    Row with = tm.appendFields(without);
    
    assertEquals(with.schema(), withoutSchema.add(field));
  }
  
}

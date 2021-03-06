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
package com.cloudera.labs.envelope.plan;

import com.cloudera.labs.envelope.load.LoadableFactory;
import com.typesafe.config.Config;

public class PlannerFactory extends LoadableFactory<Planner> {

  public static final String TYPE_CONFIG_NAME = "type";

  public static Planner create(Config plannerConfig) {
    if (!plannerConfig.hasPath(TYPE_CONFIG_NAME)) {
      throw new RuntimeException("Planner type not specified");
    }

    String plannerType = plannerConfig.getString(TYPE_CONFIG_NAME);
    Planner planner = null;
    try {
      planner = loadImplementation(Planner.class, plannerType);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    planner.configure(plannerConfig);

    return planner;
  }

}

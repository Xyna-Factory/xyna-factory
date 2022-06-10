/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */
package com.gip.xyna.xnwh.pools;

import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableReasonDetector;
import com.gip.xyna.utils.db.pool.ConnectionBuildStrategy;
import com.gip.xyna.utils.db.pool.ValidationStrategy;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xmcp.PluginDescription;


public abstract class ConnectionPoolType {
  
  public ConnectionPoolType() { /* for creation via reflection */ }

  public abstract String getName();
  
  public abstract PluginDescription getPluginDescription();
  
  public abstract NoConnectionAvailableReasonDetector getNoConnectionAvailableReasonDetector();
  
  public abstract ConnectionBuildStrategy createConnectionBuildStrategy(TypedConnectionPoolParameter cpp);

  public abstract ValidationStrategy createValidationStrategy(TypedConnectionPoolParameter cpp);
  
  public abstract boolean changeEntailsConnectionRebuild(StringParameter<?> param);
  
}

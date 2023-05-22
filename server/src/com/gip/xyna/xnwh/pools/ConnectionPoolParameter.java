/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableReasonDetector;
import com.gip.xyna.utils.db.pool.ConnectionBuildStrategy;
import com.gip.xyna.utils.db.pool.RetryStrategy;
import com.gip.xyna.utils.db.pool.ValidationStrategy;

public abstract class ConnectionPoolParameter implements com.gip.xyna.utils.db.pool.ConnectionPoolParameter {
  
  protected String name;
  protected int size;
  protected String user;
  protected String password;
  protected String connectString;
  protected Map<String, Object> additionalParams = new HashMap<String, Object>();
  protected boolean canDynamicGrow = false;
  
  private static Logger logger = CentralFactoryLogging.getLogger(ConnectionPoolParameter.class);
  
  protected ConnectionPoolParameter() {}
  
  public abstract NoConnectionAvailableReasonDetector getNoConnectionAvailableReasonDetector();
  public abstract ConnectionBuildStrategy getConnectionBuildStrategy();
  public abstract RetryStrategy getRetryStrategy();
  public abstract ValidationStrategy getValidationStrategy();
  
  
  
  public String getName() {
    return name;
  }

  public ConnectionPoolParameter name(String name) {
    this.name = name;
    return this;
  }
  
  public int getSize() {
    return size;
  }
  
  public ConnectionPoolParameter size(int size) {
    this.size = size;
    if( size == 0 ) {
      canDynamicGrow = true;
    }
    return this;
  }
  
  public String getUser() {
    return user;
  }
  
  public ConnectionPoolParameter user(String user) {
    this.user = user;
    return this;
  }
  
  public String getPassword() {
    return password;
  }
  
  public ConnectionPoolParameter password(String password) {
    this.password = password;
    return this;
  }
  
  public String getConnectString() {
    return connectString;
  }

  public ConnectionPoolParameter connectString(String connectString) {
    this.connectString = connectString;
    return this;
  }
  

  public Map<String, Object> getAdditionalParams() {
    return additionalParams;
  }
  
  public ConnectionPoolParameter additionalParams(Map<String, Object> additionalParams) {
    this.additionalParams = additionalParams;
    return this;
  }

  public int getMaxRetries() {
    return getRetryStrategy().getMaxRetries();
  }

  public long getValidationInterval() {
    return getValidationStrategy().getValidationInterval();
  }
  public abstract ConnectionPoolParameter maxRetries(int maxRetries);

  public abstract ConnectionPoolParameter validationInterval(long validationInterval);
  
  public boolean canDynamicGrow() {
    return canDynamicGrow;
  }
  
  public ConnectionPoolParameter canDynamicGrow(boolean canDynamicGrow) {
    this.canDynamicGrow = canDynamicGrow;
    return this;
  }
  
  public abstract boolean parameterChangeEntailsConnectionRebuild(String parameter);
  
  public abstract ConnectionPoolParameter clone();
}

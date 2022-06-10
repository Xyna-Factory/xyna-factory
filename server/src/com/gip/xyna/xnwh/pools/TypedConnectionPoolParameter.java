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

import java.util.List;

import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableReasonDetector;
import com.gip.xyna.utils.db.pool.ConnectionBuildStrategy;
import com.gip.xyna.utils.db.pool.DefaultRetryStrategy;
import com.gip.xyna.utils.db.pool.RetryStrategy;
import com.gip.xyna.utils.db.pool.ValidationStrategy;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;

public class TypedConnectionPoolParameter extends ConnectionPoolParameter {
  
  private final String type;
  protected ConnectionBuildStrategy connectionBuildStrategy;
  protected ValidationStrategy validationStrategy;
  protected RetryStrategy retryStrategy;
  private long validationInterval = 10000;
  private String uuid;

  
  public TypedConnectionPoolParameter(String type) {
    super();
    this.type = type;
    this.retryStrategy = new DefaultRetryStrategy(5);
  }
  
  @Override
  public NoConnectionAvailableReasonDetector getNoConnectionAvailableReasonDetector() {
    return getPoolType(type).getNoConnectionAvailableReasonDetector();
  }
  
  public String getType() {
    return type;
  }

  public PluginDescription getAdditionalDescription() {
    return getPoolType(type).getPluginDescription();
  }
  
  private static ConnectionPoolType getPoolType(String type) {
    ConnectionPoolType poolType = ConnectionPoolManagement.getInstance().getRegisteredPooltype(type);
    if (poolType == null) {
      throw new RuntimeException("PoolType is not registred!");
    }
    return poolType;
  }

  @Override
  public ConnectionBuildStrategy getConnectionBuildStrategy() {
    if( connectionBuildStrategy == null ) {
      connectionBuildStrategy = getPoolType(type).createConnectionBuildStrategy(this);
    }
    return connectionBuildStrategy;
  }

  @Override
  public RetryStrategy getRetryStrategy() {
    return retryStrategy;
  }

  @Override
  public ValidationStrategy getValidationStrategy() {
    if( validationStrategy == null ) {
      validationStrategy = getPoolType(type).createValidationStrategy(this);
    }
    return validationStrategy;
  }

  @Override
  public ConnectionPoolParameter maxRetries(int maxRetries) {
    retryStrategy.setMaxRetries(maxRetries);
    return this;
  }

  @Override
  public ConnectionPoolParameter validationInterval(long validationInterval) {
    this.validationInterval = validationInterval;
    return this;
  }

  @Override
  public long getValidationInterval() {
    return validationInterval;
  }

  @Override
  public boolean parameterChangeEntailsConnectionRebuild(String parameter) {
    ConnectionPoolType pooltype = getPoolType(type);
    List<StringParameter<?>> params = pooltype.getPluginDescription().getParameters(ParameterUsage.Modify);
    for (StringParameter<?> param : params) {
      if (param.getName().equals(parameter)) {
        if (pooltype.changeEntailsConnectionRebuild(param)) {
          return true;
        }
      }
    }
    return false;
  }

  public TypedConnectionPoolParameter clone() {
    TypedConnectionPoolParameter tcpp = new TypedConnectionPoolParameter(type);
    tcpp.name = name;
    tcpp.user = user;
    tcpp.password = password;
    tcpp.connectString = connectString;
    tcpp.size = size;
    tcpp.canDynamicGrow = canDynamicGrow;
    tcpp.validationInterval = validationInterval;
    tcpp.additionalParams = additionalParams;
    tcpp.getRetryStrategy().setMaxRetries(getRetryStrategy().getMaxRetries());
    return tcpp;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }
}

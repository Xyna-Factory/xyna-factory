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
package com.gip.xyna.utils.db.pool;

import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableReasonDetector;
import com.gip.xyna.utils.db.IConnectionFactory;

public class ConnectionPoolParameterImpl implements ConnectionPoolParameter {
  private String name;
  private int size;
  private NoConnectionAvailableReasonDetector reasonDetector;
  private boolean canDynamicGrow;
  private ValidationStrategy validationStrategy;
  private RetryStrategy retryStrategy;
  private ConnectionBuildStrategy connectionBuildStrategy;
  
  public ConnectionPoolParameterImpl(String name, int size,
      NoConnectionAvailableReasonDetector reasonDetector,
      boolean canDynamicGrow,
      ValidationStrategy validationStrategy,
      RetryStrategy retryStrategy,
      ConnectionBuildStrategy connectionBuildStrategy
      ) {
    this.name = name;
    this.size = size;
    this.reasonDetector = reasonDetector;
    this.canDynamicGrow = canDynamicGrow;
    this.validationStrategy = validationStrategy;
    this.retryStrategy = retryStrategy;
    this.connectionBuildStrategy = connectionBuildStrategy;
  }

  public ConnectionPoolParameterImpl( ConnectionPoolParameterImpl cppi) {
    this.name = cppi.name;
    this.size = cppi.size;
    this.reasonDetector = cppi.reasonDetector;
    this.canDynamicGrow = cppi.canDynamicGrow;
    this.validationStrategy = cppi.validationStrategy;
    this.retryStrategy = cppi.retryStrategy;
    this.connectionBuildStrategy = cppi.connectionBuildStrategy;
  }
  
  public String getName() {
    return name;
  }

  public int getSize() {
    return size;
  }

  public NoConnectionAvailableReasonDetector getNoConnectionAvailableReasonDetector() {
    return reasonDetector;
  }
  
  public boolean canDynamicGrow() {
    return canDynamicGrow;
  }
 
  public ValidationStrategy getValidationStrategy() {
    return validationStrategy;
  }

  public RetryStrategy getRetryStrategy() {
    return retryStrategy;
  }

  public ConnectionBuildStrategy getConnectionBuildStrategy() {
    return connectionBuildStrategy;
  }


  public static class Builder extends AbstractBuilder<ConnectionPoolParameterImpl> {

    @Override
    protected ConnectionPoolParameterImpl buildInternal() {
      return buildConnectionPoolParameterImpl();
    }
    
  }
  
  public abstract static class AbstractBuilder<I> { 
    protected String name;
    protected int size = 0;
    protected NoConnectionAvailableReasonDetector reasonDetector = null;
    protected boolean canDynamicGrow = false;
    protected ValidationStrategy validationStrategy = new DefaultValidationStrategy(10000);
    protected RetryStrategy retryStrategy = new DefaultRetryStrategy(5);
    protected ConnectionBuildStrategy connectionBuildStrategy;
   
    public AbstractBuilder<I> connectionFactory(IConnectionFactory cf) {
      this.connectionBuildStrategy = new DefaultConnectionBuildStrategy(cf);
      return this;
    }
    public AbstractBuilder<I> identifiedBy(String name) {
      this.name = name;
      return this;
    }
    public AbstractBuilder<I> size(int size) {
      this.size = size;
      if (size == 0) {
        canDynamicGrow(true);
      }
      return this;
    }
    public AbstractBuilder<I> noConnectionAvailableReasonDetector(NoConnectionAvailableReasonDetector reasonDetector) {
      this.reasonDetector = reasonDetector;
      return this;
    }
    public AbstractBuilder<I> maxRetries(int maxRetries) {
      retryStrategy.setMaxRetries(maxRetries);
      return this;
    }
    /**
     * @deprecated use validationInterval
     */
    public AbstractBuilder<I> checkInterval(long checkInterval) {
      validationStrategy.setValidationInterval(checkInterval);
      return this;
    }
    public AbstractBuilder<I> validationInterval(long validationInterval) {
      validationStrategy.setValidationInterval(validationInterval);
      return this;
    }
    
    public AbstractBuilder<I> canDynamicGrow( boolean canDynamicGrow ) {
      this.canDynamicGrow = canDynamicGrow;
      return this;
    }
    
    public AbstractBuilder<I> validationStrategy(ValidationStrategy validationStrategy) {
      this.validationStrategy = validationStrategy;
      return this;
    }
    
    public AbstractBuilder<I> retryStrategy(RetryStrategy retryStrategy) {
      this.retryStrategy = retryStrategy;
      return this;
    }
    
    public AbstractBuilder<I> connectionBuildStrategy(ConnectionBuildStrategy connectionBuildStrategy) {
      this.connectionBuildStrategy = connectionBuildStrategy;
      return this;
    }

    public I build() {
      if( connectionBuildStrategy == null ) {
        throw new IllegalArgumentException("ConnectionBuildStrategy must be set");
      }
      if( name == null ) {
        throw new IllegalArgumentException("Identifying name must be set");
      }
      return buildInternal();
      //
    }
    
    protected ConnectionPoolParameterImpl buildConnectionPoolParameterImpl() {
      return new ConnectionPoolParameterImpl(name,size,reasonDetector,
          canDynamicGrow,validationStrategy,retryStrategy,connectionBuildStrategy);
    }
    
    protected abstract I buildInternal();
    
  }

}

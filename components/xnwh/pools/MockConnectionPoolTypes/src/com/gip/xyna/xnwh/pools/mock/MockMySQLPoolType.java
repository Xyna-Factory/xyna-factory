/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package com.gip.xyna.xnwh.pools.mock;


import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException.Reason;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableReasonDetector;
import com.gip.xyna.utils.db.pool.ConnectionBuildStrategy;
import com.gip.xyna.utils.db.pool.ValidationStrategy;
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xmcp.PluginDescription.PluginType;
import com.gip.xyna.xnwh.pools.ConnectionPoolType;
import com.gip.xyna.xnwh.pools.TypedConnectionPoolParameter;


public class MockMySQLPoolType extends ConnectionPoolType {
  
  public final static String POOLTYPE_IDENTIFIER = "Mock_MySQL";
  
  public static final StringParameter<Duration> CONNECT_TIMEOUT = 
      StringParameter.typeDuration("connectTimeout").
      label("Connect Timeout").
      documentation(Documentation.
                    en("timeout until connection must be established").
                    de("Timeout, bis zu dem Verbindung hergestellt sein muss").
                    build()).
      defaultValue(Duration.valueOf("365 d")). //1 jahr. besser als sonderbehandlung f�r 0
      optional().build();

  public static final StringParameter<Duration> SOCKET_TIMEOUT = 
      StringParameter.typeDuration("socketTimeout").
      label("Socket Timeout").
      documentation(Documentation.
                    en("timeout until connection must have answered").
                    de("Timeout, bis zu dem Verbindung antworten muss").
                    build()).
      defaultValue(Duration.valueOf("0 s")).
      build();

  public static final StringParameter<Duration> VALIDATION_TIMEOUT = 
      StringParameter.typeDuration("validationTimeout").
      label("Validation Timeout").
      documentation(Documentation.
                    en("timeout until connection must have answered on validation").
                    de("Timeout, bis zu dem Verbindung bei Validierung antworten muss").
                    build()).
      defaultValue(Duration.valueOf("10 s")).
      build();

  public static final List<StringParameter<?>> additionalParameters = 
      StringParameter.asList( CONNECT_TIMEOUT, SOCKET_TIMEOUT, VALIDATION_TIMEOUT );

  private PluginDescription pluginDescription;

  
  @Override
  public String getName() {
    return POOLTYPE_IDENTIFIER;
  }

  @Override
  public NoConnectionAvailableReasonDetector getNoConnectionAvailableReasonDetector() {
    return new NoConnectionAvailableReasonDetectorImpl();
  }

  public PluginDescription getPluginDescription() {
    if( pluginDescription == null ) {
      pluginDescription = PluginDescription.create(PluginType.connectionPool).
        name(POOLTYPE_IDENTIFIER).
        description("Mock MySQL Pooltype").
        parameters(ParameterUsage.Create, additionalParameters).
        parameters(ParameterUsage.Modify, additionalParameters).
        build();
    }
    return pluginDescription;
  }


  @Override
  public ConnectionBuildStrategy createConnectionBuildStrategy(TypedConnectionPoolParameter cpp) {
    return new MySQLConnectionBuildStrategy();
  }


  @Override
  public ValidationStrategy createValidationStrategy(TypedConnectionPoolParameter cpp) {
    return new ValidationStrategy() {
      
      @Override
      public Exception validate(Connection con) {
        return null;
      }
      
      
      @Override
      public void setValidationInterval(long validationInterval) {
        
      }
      
      
      @Override
      public boolean rebuildConnectionAfterFailedValidation() {
        return false;
      }
      
      
      @Override
      public boolean isValidationNecessary(long currentTime, long lastcheck) {
        return false;
      }
      
      
      @Override
      public long getValidationInterval() {
        return 0;
      }
    };
  }

  public static class MySQLConnectionBuildStrategy implements ConnectionBuildStrategy {

    public MySQLConnectionBuildStrategy() {
    }

    public Connection createNewConnection() {
      return new MockMySQLConnection();
    }
    
    public void setName(String name) {
    }

    public void markConnectionNotInUse(Connection con) {
    }

    public void markConnection(Connection con, String clientInfo) {
    }
    
  }


  public boolean changeEntailsConnectionRebuild(StringParameter<?> param) {
    return false;
  }
  
  private static class NoConnectionAvailableReasonDetectorImpl implements ConnectionPool.NoConnectionAvailableReasonDetector {

    public Reason detect(SQLException sqlException) {
      return Reason.Other;
    }
    
  }
  
  
}

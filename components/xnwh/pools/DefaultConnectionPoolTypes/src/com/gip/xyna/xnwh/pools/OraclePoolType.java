/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException.Reason;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableReasonDetector;
import com.gip.xyna.utils.db.DBConnectionData;
import com.gip.xyna.utils.db.pool.ConnectionBuildStrategy;
import com.gip.xyna.utils.db.pool.FastValidationStrategy;
import com.gip.xyna.utils.db.pool.ValidationStrategy;
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.EnvironmentVariable.StringEnvironmentVariable;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xmcp.PluginDescription.PluginType;
import com.gip.xyna.xnwh.exception.SQLRetryTransactionRuntimeException;


public class OraclePoolType extends ConnectionPoolType {

  private final static Logger logger = CentralFactoryLogging.getLogger(OraclePoolType.class);
  
  public final static String POOLTYPE_IDENTIFIER = "Oracle";
    
  public static final StringParameter<Duration> CONNECT_TIMEOUT = 
      StringParameter.typeDuration("connectTimeout").
      label("Connect Timeout").
      documentation(Documentation.
                    en("timeout until connection must be established").
                    de("Timeout, bis zu dem Verbindung hergestellt sein muss").
                    build()).
      defaultValue(Duration.valueOf("365 d")). //1 jahr. besser als sonderbehandlung für 0
      build();

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
  
  public static final StringParameter<Boolean> DURABLE_STATEMENT_CACHE = 
      StringParameter.typeBoolean("durableStatementCache").
      label("Durable Statement Cache").
      documentation(Documentation.
                    en("enables the implicit prepared statement cache").
                    de("ermöglicht die Verwendung des impliziten PreparedStatement-Caches").
                    build()).
      defaultValue(false).
      build();

  public static final StringParameter<StringEnvironmentVariable> USERNAME_ENV = StringParameter
      .typeEnvironmentVariable(StringEnvironmentVariable.class, "usernameEnv")
      .label("Username environment variable.")
      .documentation(Documentation.en("Name of the environment variable containing the db username.")
          .de("Name der Umgebungsvariable, die den DB Nutzernamen enthält.").build())
      .optional().build();

  public static final StringParameter<StringEnvironmentVariable> PASSWORD_ENV = StringParameter
      .typeEnvironmentVariable(StringEnvironmentVariable.class, "passwordEnv")
      .label("Password environment variable.")
      .documentation(Documentation.en("Name of the environment variable containing the db password.")
          .de("Name der Umgebungsvariable, die das DB Passwort enthält.").build())
      .optional().build();

  public static final StringParameter<StringEnvironmentVariable> CONNECT_ENV = StringParameter
      .typeEnvironmentVariable(StringEnvironmentVariable.class, "connectStringEnv")
      .label("Connectstring environment variable.")
      .documentation(Documentation.en("Name of the environment variable containing the JDBC connect string.")
          .de("Name der Umgebungsvariable mit den JDBC Verbindungsdaten.").build())
      .optional().build();

  public static final List<StringParameter<?>> additionalParameters = StringParameter.asList(CONNECT_TIMEOUT,
      SOCKET_TIMEOUT, VALIDATION_TIMEOUT, DURABLE_STATEMENT_CACHE, USERNAME_ENV, PASSWORD_ENV, CONNECT_ENV);

  private PluginDescription pluginDescription;


  
  
  public String getName() {
    return POOLTYPE_IDENTIFIER;
  }
  
  public NoConnectionAvailableReasonDetector getNoConnectionAvailableReasonDetector() {
    return new NoConnectionAvailableReasonDetectorImpl();
  }
  
  public PluginDescription getPluginDescription() {
    if( pluginDescription == null ) {
      pluginDescription = PluginDescription.create(PluginType.connectionPool).
        name(POOLTYPE_IDENTIFIER).
        description("Default Oracle Pooltype").
        parameters(ParameterUsage.Create, additionalParameters).
        parameters(ParameterUsage.Modify, additionalParameters).
        build();
    }
    return pluginDescription;
  }
  
  @Override
  public ConnectionBuildStrategy createConnectionBuildStrategy(TypedConnectionPoolParameter cpp) {
    Duration connectTimeout = CONNECT_TIMEOUT.getFromMap(cpp.getAdditionalParams());
    Duration socketTimeout = SOCKET_TIMEOUT.getFromMap(cpp.getAdditionalParams());
    Boolean useDurableStatementCache = DURABLE_STATEMENT_CACHE.getFromMap(cpp.getAdditionalParams());

    Optional<StringEnvironmentVariable> userEnv = Optional
        .ofNullable(USERNAME_ENV.getFromMap(cpp.getAdditionalParams()));
    Optional<StringEnvironmentVariable> pwdEnv = Optional
        .ofNullable(PASSWORD_ENV.getFromMap(cpp.getAdditionalParams()));
    Optional<StringEnvironmentVariable> connectStringEnv = Optional
        .ofNullable(CONNECT_ENV.getFromMap(cpp.getAdditionalParams()));

    String user = userEnv.map(u -> u.getValue().orElse(cpp.getUser())).orElse(cpp.getUser());
    String pwd = pwdEnv.map(p -> p.getValue().orElse(cpp.getPassword())).orElse(cpp.getPassword());
    String connString = connectStringEnv.map(c -> c.getValue().orElse(cpp.getConnectString()))
        .orElse(cpp.getConnectString());

    DBConnectionData dbdata =
      DBConnectionData.newDBConnectionData().
          user(user).password(pwd).url(connString)
          .connectTimeoutInSeconds((int)connectTimeout.getDuration(TimeUnit.SECONDS))
          .socketTimeoutInSeconds((int)socketTimeout.getDuration(TimeUnit.SECONDS))
          .classLoaderToLoadDriver(OraclePoolType.class.getClassLoader()) // enforcing the connector jar to be stored in userlib
          .property("rewriteBatchedStatements", "true")
          .build();
    
    return new OracleConnectionBuildStrategy(dbdata, useDurableStatementCache);
  }


  public static class OracleConnectionBuildStrategy implements ConnectionBuildStrategy {

    private DBConnectionData dbdata;
    private boolean useDurableStatementCache;
    private String poolId;

    public OracleConnectionBuildStrategy(DBConnectionData dbdata, boolean useDurableStatementCache) {
      this.dbdata = dbdata;
      this.useDurableStatementCache = useDurableStatementCache;
    }

    public Connection createNewConnection() {
      try {
        Connection con = dbdata.createConnection();
        if (useDurableStatementCache) { 
          activateImplicitCaching(con);
        }
        markConnectionNotInUse(con);
        return con;
      } catch (Exception e) {
        throw new SQLRetryTransactionRuntimeException(e);
      }
    }

    public void setName(String name) {
      this.poolId = "pool: " + name;
    }

    public void markConnectionNotInUse(Connection con) {
      markConnection(con, poolId);
    }

    public void markConnection(Connection con, String clientInfo) {
      DBConnectionData.markConnection(con, clientInfo);
    }
    
    // done via reflection in favor of a setting not working else we'll get classloading errors if ojdbc is not accessible  
    private static final void activateImplicitCaching(Connection con) {
      try {
        Class<?> clazz = Class.forName("oracle.jdbc.OracleConnection");
        if (clazz.isInstance(con)) {
          Method implicitCachingMethod = clazz.getDeclaredMethod("setImplicitCachingEnabled", boolean.class);
          implicitCachingMethod.invoke(con, true);
        }
      } catch (Exception e) {
        logger.warn("Failed to activate implicitCaching.",e);
      } catch (Error e) {
        logger.warn("Failed to activate implicitCaching.",e);
      }
    }

  }
  
  
  private static class NoConnectionAvailableReasonDetectorImpl implements ConnectionPool.NoConnectionAvailableReasonDetector {

    public Reason detect(SQLException sqlException) {
      
      
      int error = sqlException.getErrorCode();
      switch(error) {
      case 1017:
        return Reason.UserOrPasswordInvalid;
      case 28000:
        return Reason.UserOrPasswordInvalid;
      }
      
      String message = sqlException.getMessage();
      if( message == null ) {
        return Reason.Other;
      }
      if( message.contains ("The Network Adapter could not establish the connection")) {
        return Reason.NetworkUnreachable;
      }
      if( message.contains ("Oracle-URL")) {
        return Reason.URLInvalid;
      }
      if( message.contains ("Listener refused the connection")) {
        return Reason.ConnectionRefused;
      }
      
      return Reason.Other;
    }
  }
  
  
  public boolean changeEntailsConnectionRebuild(StringParameter<?> param) {
    if (param == CONNECT_TIMEOUT ||
        param == SOCKET_TIMEOUT ||
        param == DURABLE_STATEMENT_CACHE) {
      return true;
    } else if (param == VALIDATION_TIMEOUT) {
      return false;
    } else {
      return false;
    }
  }

  
  @Override
  public ValidationStrategy createValidationStrategy(TypedConnectionPoolParameter tcpp) {
    return FastValidationStrategy.validateAfterIntervalWithTimeout(
        tcpp.getValidationInterval(), 
        VALIDATION_TIMEOUT.getFromMap(tcpp.getAdditionalParams()).getDurationInMillis() );
  }
  
}

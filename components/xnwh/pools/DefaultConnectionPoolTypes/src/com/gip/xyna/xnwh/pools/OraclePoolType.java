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
import com.gip.xyna.utils.db.DBConnectionData.DBConnectionDataBuilder;
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
      defaultValue(Duration.valueOf("365 d")). //1 jahr. besser als sonderbehandlung f�r 0
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
                    de("erm�glicht die Verwendung des impliziten PreparedStatement-Caches").
                    build()).
      defaultValue(false).
      build();

  public static final StringParameter<StringEnvironmentVariable> USERNAME_ENV = StringParameter
      .typeEnvironmentVariable(StringEnvironmentVariable.class, "usernameEnv")
      .label("Username environment variable.")
      .documentation(Documentation.en("Name of the environment variable containing the db username.")
          .de("Name der Umgebungsvariable, die den DB Nutzernamen enth�lt.").build())
      .optional().build();

  public static final StringParameter<StringEnvironmentVariable> PASSWORD_ENV = StringParameter
      .typeEnvironmentVariable(StringEnvironmentVariable.class, "passwordEnv")
      .label("Password environment variable.")
      .documentation(Documentation.en("Name of the environment variable containing the db password.")
          .de("Name der Umgebungsvariable, die das DB Passwort enth�lt.").build())
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

    return new OracleConnectionBuildStrategy(cpp, useDurableStatementCache,
        (int) connectTimeout.getDuration(TimeUnit.SECONDS), (int) socketTimeout.getDuration(TimeUnit.SECONDS));
  }


  public static class OracleConnectionBuildStrategy implements ConnectionBuildStrategy {

    private TypedConnectionPoolParameter tcpp;
    private DBConnectionData dbdata;
    private int connectTimeout;
    private int socketTimeout;
    private boolean useDurableStatementCache;
    private String poolId;

    private final Optional<StringEnvironmentVariable> userEnv;
    private final Optional<StringEnvironmentVariable> pwdEnv;
    private final Optional<StringEnvironmentVariable> connectStringEnv;

    public OracleConnectionBuildStrategy(TypedConnectionPoolParameter tcpp, boolean useDurableStatementCache, int connectTimeout, int socketTimeout) {
      this.tcpp = tcpp;
      this.socketTimeout = socketTimeout;
      this.connectTimeout = connectTimeout;
      this.dbdata = null;
      this.useDurableStatementCache = useDurableStatementCache;

      userEnv = Optional
          .ofNullable(USERNAME_ENV.getFromMap(tcpp.getAdditionalParams()));
      pwdEnv = Optional
          .ofNullable(PASSWORD_ENV.getFromMap(tcpp.getAdditionalParams()));
      connectStringEnv = Optional
          .ofNullable(CONNECT_ENV.getFromMap(tcpp.getAdditionalParams()));
    }

    private void updateDBConnectData() {
      String user = userEnv.flatMap(u -> u.getValue()).filter(s -> !s.isEmpty()).orElse(tcpp.getUser()).trim();
      String pwd = pwdEnv.flatMap(p -> p.getValue()).filter(s -> !s.isEmpty()).orElse(tcpp.getPassword()).trim();
      String connString = connectStringEnv.flatMap(c -> c.getValue()).filter(s -> !s.isEmpty())
          .orElse(tcpp.getConnectString()).trim();

      if (dbdata == null) {
        dbdata = DBConnectionData.newDBConnectionData().user(user).password(pwd).url(connString)
            .connectTimeoutInSeconds(connectTimeout)
            .socketTimeoutInSeconds(socketTimeout)
            .classLoaderToLoadDriver(OraclePoolType.class.getClassLoader()) // enforcing the connector jar to be stored
                                                                            // in userlib
            .property("rewriteBatchedStatements", "true")
            .build();
      } else {
        if (dbdata.getUrl().equals(connString) && dbdata.getUser().equals(user) && dbdata.getPassword().equals(pwd))
          return;

        dbdata = (new DBConnectionDataBuilder(dbdata)).user(user).password(pwd).url(connString).build();
      }
    }

    public Connection createNewConnection() {
      updateDBConnectData();

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

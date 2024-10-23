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


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableException.Reason;
import com.gip.xyna.utils.db.ConnectionPool.NoConnectionAvailableReasonDetector;
import com.gip.xyna.utils.db.DBConnectionData;
import com.gip.xyna.utils.db.pool.ConnectionBuildStrategy;
import com.gip.xyna.utils.db.pool.DefaultValidationStrategy;
import com.gip.xyna.utils.db.pool.ValidationStrategy;
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xmcp.PluginDescription.PluginType;
import com.gip.xyna.xnwh.exception.SQLRuntimeException;
import com.gip.xyna.xnwh.utils.ReflectionUtils;


public class MySQLPoolType extends ConnectionPoolType {
  
  private final static Logger logger = CentralFactoryLogging.getLogger(MySQLPoolType.class);
  
  public final static String POOLTYPE_IDENTIFIER = "MySQL";
  
  public static final StringParameter<Duration> CONNECT_TIMEOUT = 
      StringParameter.typeDuration("connectTimeout").
      label("Connect Timeout").
      documentation(Documentation.
                    en("timeout until connection must be established").
                    de("Timeout, bis zu dem Verbindung hergestellt sein muss").
                    build()).
      defaultValue(Duration.valueOf("365 d")). //1 jahr. besser als sonderbehandlung für 0
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


 


  private static class ConnectionCreator implements Runnable {

    private volatile Connection con;
    private final CountDownLatch latch;
    private volatile Throwable t;
    private volatile boolean cancelled = false;
    private final DBConnectionData dbdata;


    public ConnectionCreator(CountDownLatch latch, DBConnectionData dbdata) {
      this.latch = latch;
      this.dbdata = dbdata;
    }


    public void cancel() {
      cancelled = true;
    }


    public void run() {
      try {
        if (cancelled) {
          return;
        }

        con = dbdata.createConnection();
        if (cancelled) {
          con.close();
          con = null;
        }
      } catch (Throwable t) {
        if (cancelled) {
          logger.warn("abandoned connection creator thread failed", t);
        }
        this.t = t;
      } finally {
        latch.countDown();
      }
    }

  }


  @Override
  public NoConnectionAvailableReasonDetector getNoConnectionAvailableReasonDetector() {
    return new NoConnectionAvailableReasonDetectorImpl();
  }

  public PluginDescription getPluginDescription() {
    if( pluginDescription == null ) {
      pluginDescription = PluginDescription.create(PluginType.connectionPool).
        name(POOLTYPE_IDENTIFIER).
        description("Default MySQL Pooltype").
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
    DBConnectionData dbdata =
        DBConnectionData.newDBConnectionData().
            user(cpp.getUser()).password(cpp.getPassword()).url(cpp.getConnectString())
            .connectTimeoutInSeconds((int)connectTimeout.getDuration(TimeUnit.SECONDS))
            .socketTimeoutInSeconds((int)socketTimeout.getDuration(TimeUnit.SECONDS))
            .classLoaderToLoadDriver(MySQLPoolType.class.getClassLoader()) // enforcing the connector jar to be stored in userlib
            .property("rewriteBatchedStatements", "true")
            .build();

    return new MySQLConnectionBuildStrategy(dbdata, (int)connectTimeout.getDuration(TimeUnit.SECONDS) );
  }


  @Override
  public ValidationStrategy createValidationStrategy(TypedConnectionPoolParameter cpp) {
    Duration validationTimeout = VALIDATION_TIMEOUT.getFromMap(cpp.getAdditionalParams());
    Duration socketTimeout = SOCKET_TIMEOUT.getFromMap(cpp.getAdditionalParams());
    return new MySQLValidationStrategy(cpp.getValidationInterval(), validationTimeout.getDurationInMillis(), socketTimeout.getDurationInMillis());
  }

  public static class MySQLConnectionBuildStrategy implements ConnectionBuildStrategy {
    
    private static ThreadPoolExecutor threadpool = new ThreadPoolExecutor(1, 1000, 10, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
    private DBConnectionData dbdata;
    private int connectTimeout;
    private String poolId;

    public MySQLConnectionBuildStrategy(DBConnectionData dbdata, int connectTimeout) {
      this.dbdata = dbdata;
      this.connectTimeout = connectTimeout;
    }

    public Connection createNewConnection() {
      Connection con = createNewConnectionInternal();
      if (con == null) {
        throw new RuntimeException("Could not create connection but returned null.");
      }
      markConnectionNotInUse(con);
      return con;
    }
    
    public void setName(String name) {
      this.poolId = "pool: " + name;
    }

    public void markConnectionNotInUse(Connection con) {
      markConnection(con, poolId);
    }

    public void markConnection(Connection con, String clientInfo) {
      DBConnectionData.markConnection(con, clientInfo); //TODO das kann doch nicht klappen
    }
    
    private Connection createNewConnectionInternal() {
      final CountDownLatch latch = new CountDownLatch(1);
      ConnectionCreator cc = new ConnectionCreator(latch, dbdata);

      //in eigenem thread ausführen, damit timeouts ordentlich behandelt werden können. 
      //jdbc hat dafür zwar auch properties, aber wenn diese versagen, bleibt der thread nicht hängen.
      boolean executed = false;
      int cnt = 0;
      while (!executed) {
        try {
          threadpool.execute(cc);
          executed = true;
        } catch (RejectedExecutionException e) {
          if (cnt++ > connectTimeout * 10) { //doppelter connectTimeout
            throw new SQLRuntimeException("No free thread for connection creation available.", e);
          }
          try {
            Thread.sleep(200);
          } catch (InterruptedException e1) {
            logger.warn("Could not create connection.", e1);
            return null;
          }
        }
      }
      try {
        if (latch.await(connectTimeout * 2, TimeUnit.SECONDS)) {
          if (cc.con != null) {
            return cc.con;
          } else if (cc.t != null) {
            throw new SQLRuntimeException("Could not create connection (" + dbdata.getUser() + "@" + dbdata.getUrl() + ")", cc.t);
          } else {
            return null;
          }
        } else {
          cc.cancel();
          Connection con = cc.con;
          if (con != null) {
            return con;
          }
          throw new SQLRuntimeException("Timeout while creating connection. MySQL did not provide a connection after "
              + connectTimeout * 2 + " seconds. connection=" + dbdata.getUser() + " @ " + dbdata.getUrl());
        }
      } catch (InterruptedException e) {
        throw new SQLRuntimeException("Could not create connection.", e);
      }
    }

  }


  public boolean changeEntailsConnectionRebuild(StringParameter<?> param) {
    if (param == CONNECT_TIMEOUT ||
        param == SOCKET_TIMEOUT) {
      return true;
    } else if (param == VALIDATION_TIMEOUT) {
      return false;
    } else {
      return false;
    }
  }
  
  private static AtomicReference<Field> MYSQL_CONNECTION_SESSION_FIELD = new AtomicReference<Field>();
  private static AtomicReference<Field> MYSQL_CONNECTION_IO_FIELD = new AtomicReference<Field>();
  private static AtomicReference<Field> MYSQL_IO_CONNECTION_FIELD = new AtomicReference<Field>();
  private static boolean loggedException = false;
  
  private static boolean tryToAdjustSocketTimeout(Connection con, long socketTimeout) {
    if (loggedException) {
      //es wird nicht angenommen, dass sich der fehler reparieren kann
      return false;
    }
    
    try {
      con.setNetworkTimeout(null, (int) socketTimeout);
      return true;
    } catch (Exception e) {
      //try using reflection
      //may throw java.sql.SQLFeatureNotSupportedException
    }
    
    
    try {
      try { // try using mysql-connector-java 8.0.31
        if (!ReflectionUtils.ensureFieldRecursive(MYSQL_CONNECTION_SESSION_FIELD, con.getClass(), "session", logger)) {
          throw new IllegalAccessException("Field " + MYSQL_CONNECTION_SESSION_FIELD + " not found.");
        }
  
        Object session = ReflectionUtils.get(MYSQL_CONNECTION_SESSION_FIELD, con, logger);
        if (session == null) {
          logger.debug("Failed to adjust socketTimeout for validation: no NativeSession instance in connection");
          return false;
        }

        Method setSocketTimeout = session.getClass().getMethod("setSocketTimeout", int.class);

        setSocketTimeout.invoke(session, (int) socketTimeout);

        return true;
        
      } catch (Throwable e) { // try using mysql-connector-java 5.1.19 instead
        if (!ReflectionUtils.ensureFieldRecursive(MYSQL_CONNECTION_IO_FIELD, con.getClass(), "io", logger)) {
          throw new IllegalAccessException("Field " + MYSQL_CONNECTION_IO_FIELD + " not found.");
        }
        Object mio = ReflectionUtils.get(MYSQL_CONNECTION_IO_FIELD, con, logger);
        if (mio == null) {
          logger.debug("Failed to adjust socketTimeout for validation: no MysqlIO instance in connection");
          return false;
        }
  
        if (!ReflectionUtils.ensureField(MYSQL_IO_CONNECTION_FIELD, mio.getClass(), "mysqlConnection", logger)) {
          throw new IllegalAccessException("Field " + MYSQL_IO_CONNECTION_FIELD + " not found.");
        }

        Socket s = (Socket) ReflectionUtils.get(MYSQL_IO_CONNECTION_FIELD, mio, logger);
        if (s == null) {
          logger.debug("Failed to adjust socketTimeout for validation: no Socket instance in connection");
          return false;
        }
        
        s.setSoTimeout((int)socketTimeout);
        return true;
      }
    } catch (Throwable e) {
      if (!loggedException) {
        logger.warn("Could not adjust socketTimeout for validation. It will not be tried again.", e);
        loggedException = true;
      }
      return false;
    }
  }

  private static class MySQLValidationStrategy extends DefaultValidationStrategy {

    private final long validationTimeout;
    private final long socketTimeeout;
    
    MySQLValidationStrategy(long validationInterval, long validationTimeout, long socketTimeeout) {
      super(validationInterval);
      this.validationTimeout = validationTimeout;
      this.socketTimeeout = socketTimeeout;
    }

    public Exception validate(Connection con) {
      boolean reset = false;
      if (validationTimeout > 0) {
        reset = MySQLPoolType.tryToAdjustSocketTimeout(con, validationTimeout);
      }
      try {
        return super.validate(con);
      } finally {
        if (reset) {
          MySQLPoolType.tryToAdjustSocketTimeout(con, socketTimeeout);
        }
      }
    }
    
  }
  
  
  private static class NoConnectionAvailableReasonDetectorImpl implements ConnectionPool.NoConnectionAvailableReasonDetector {

    public Reason detect(SQLException sqlException) {
      switch (sqlException.getErrorCode()) {
        case 1045 :
          return Reason.UserOrPasswordInvalid;
        default :
          break;
      }
      if (sqlException.getMessage().startsWith("Communications link failure")) {
        return Reason.NetworkUnreachable;
      }
      return Reason.Other;
    }
    
  }
  
  
}

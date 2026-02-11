/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package com.gip.xyna.xnwh.sharedresources.synchronizers.sqlsharedresourcesynchronizer;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.concurrent.AtomicEnum;
import com.gip.xyna.utils.db.DBConnectionData;
import com.gip.xyna.utils.db.DBConnectionData.DBConnectionDataBuilder;
import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.types.BLOB;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xnwh.sharedresources.SharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceInstance;
import com.gip.xyna.xnwh.sharedresources.SharedResourceRequestResult;
import com.gip.xyna.xnwh.sharedresources.SharedResourceSynchronizer;



public class SQLSharedResourceSynchronizer implements SharedResourceSynchronizer {

  private static final Logger logger = CentralFactoryLogging.getLogger(SQLSharedResourceSynchronizer.class);

  private static final Exception NO_CONNECTION_AVAILABLE_EXCEPTION = new RuntimeException("No connection available");
  private static final Exception MISSING_ENTRIES_EXCEPTION = new RuntimeException("Some entries to update are missing");
  private static final Exception UPDATE_INTERRUPTED_EXCEPTION = new RuntimeException("Update method returned null");
  private static final Exception UPDATE_MODIFIED_ID_EXCEPTION = new RuntimeException("Update modified instance id");
  private static final String INSERT_VALUE_PLACEHOLDER = "(?, ?, ?),\n";
  private static final String DESC_FORMAT = "SQLSharedResourceSynchronizer - %s - %s - Connections[idle/configured/missing]: %d/%d/%d";

  static {
    NO_CONNECTION_AVAILABLE_EXCEPTION.setStackTrace(new StackTraceElement[0]);
  }


  private String CREATE_TABLE_STATEMENT;
  private String INSERT_TEMPLATE;
  private String DELETE_TEMPLATE;
  private String SELECT_TEMPLATE;
  private String SELECT_ALL_TEMPLATE;
  private String UPDATE_STATEMENT;

  private String tableName;
  private String url;
  private String username;
  private String password;
  private int numConnections;
  private Duration connectionTimeout;
  private Duration socketTimeout;
  private int queryTimeoutSeconds;

  private DBConnectionData connectionData;
  private Set<Connection> allConnections;
  private final ConcurrentLinkedQueue<Connection> idleConnections;

  /**
   * number of connections that cannot be used due to some exception.
   * If a connection is requested and there are no idle connections
   * available, a missing connection is recreated.
   */
  private final AtomicInteger missingConnections;

  private final AtomicEnum<State> state;

  private final ExecutorService cleanupExecutor;


  private enum State {
    starting, running, stopping, stopped
  }


  public SQLSharedResourceSynchronizer(String tableName, String url, String username, String password, int numConnections,
                                       Duration connectionTimeout, Duration socketTimeout) {
    CREATE_TABLE_STATEMENT = String
        .format("CREATE TABLE IF NOT EXISTS %s (sr_path VARCHAR(128) NOT NULL, sr_id VARCHAR(128) NOT NULL, sr_data MEDIUMBLOB NULL DEFAULT NULL, PRIMARY KEY (sr_path, sr_id) USING BTREE)",
                tableName);
    DELETE_TEMPLATE = String.format("DELETE FROM %s WHERE sr_id IN (", tableName);
    INSERT_TEMPLATE = String.format("INSERT INTO %s (sr_path, sr_id, sr_data) VALUES\n", tableName);
    SELECT_TEMPLATE = String.format("SELECT * FROM %s WHERE sr_path = ? AND sr_id IN (", tableName);
    SELECT_ALL_TEMPLATE = String.format("SELECT * FROM %s", tableName);
    UPDATE_STATEMENT = String.format("UPDATE %s SET sr_data = ? WHERE sr_path = ? AND sr_id = ?", tableName);

    this.tableName = tableName;
    this.url = url;
    this.username = username;
    this.password = password;
    this.numConnections = numConnections;
    this.connectionTimeout = connectionTimeout;
    this.socketTimeout = socketTimeout;
    this.queryTimeoutSeconds = (int) connectionTimeout.getDuration(TimeUnit.SECONDS);

    allConnections = new HashSet<>();
    idleConnections = new ConcurrentLinkedQueue<>();
    missingConnections = new AtomicInteger();
    state = new AtomicEnum<>(State.class, State.stopped);
    cleanupExecutor = Executors.newSingleThreadExecutor();
  }


  @Override
  public void start() {
    if (!state.compareAndSet(State.stopped, State.starting)) {
      if (logger.isWarnEnabled()) {
        logger.warn("rejecting start of " + this + ". Current state is " + state.get());
        return;
      }
    }
    try {
      DBConnectionDataBuilder cdb = DBConnectionData.newDBConnectionData();
      cdb.user(username);
      cdb.password(password);
      cdb.url(url);
      cdb.socketTimeoutInSeconds((int) socketTimeout.getDuration(TimeUnit.SECONDS));
      cdb.connectTimeoutInSeconds((int) connectionTimeout.getDuration(TimeUnit.SECONDS));
      cdb.autoCommit(true);
      connectionData = cdb.build();

      try (Connection c = connectionData.createConnection()) {
        try (PreparedStatement stmt = c.prepareStatement(CREATE_TABLE_STATEMENT)) {
          stmt.execute();
        }
      } catch (Exception e) {
        logger.error("Error during table check", e);
      }

      Set<Connection> createdConnections = new HashSet<>();
      for (int i = 0; i < numConnections; i++) {
        try {
          Connection con = connectionData.createConnection();
          createdConnections.add(con);
          idleConnections.add(con);
        } catch (Exception e) {
          missingConnections.incrementAndGet();
          if (logger.isWarnEnabled()) {
            logger.warn("Could not create connection to " + url, e);
          }
        }
      }

      synchronized (allConnections) {
        allConnections.addAll(createdConnections);
      }
    } finally {
      state.set(State.running);
    }
  }


  @Override
  public void stop() {
    if (!state.compareAndSet(State.running, State.stopping)) {
      if (logger.isWarnEnabled()) {
        logger.warn("rejecting stop of " + this + ". Current state is " + state.get());
      }
      return;
    }
    try {
      List<Connection> capturedConnections = new ArrayList<Connection>();
      Connection idleConnection = null;
      do {
        idleConnection = idleConnections.poll();
        if (idleConnection != null) {
          capturedConnections.add(idleConnection);
        }
      } while (idleConnection != null);

      int missingConnectionCount = missingConnections.addAndGet(-numConnections);
      missingConnectionCount = numConnections + missingConnectionCount;
      if (logger.isDebugEnabled()) {
        String format = "Stopping %d connections. Initially captured %d idle connections. %d connections were missing";
        logger.debug(String.format(format, numConnections, capturedConnections.size(), missingConnectionCount));
      }
      for (Connection con : capturedConnections) {
        try {
          con.close();
        } catch (Exception e) {
          if (logger.isWarnEnabled()) {
            logger.warn("Could not close connection " + con, e);
          }
        }
      }


      synchronized (allConnections) {
        allConnections.removeAll(capturedConnections);
        for (Connection c : allConnections) {
          try {
            c.abort(cleanupExecutor);
          } catch (Exception e) {
            if (logger.isErrorEnabled()) {
              logger.error("Could not abort Connection " + c, e);
            }
          }
        }
        allConnections.clear();
      }

      do {
        idleConnection = idleConnections.poll();
      } while (idleConnection != null);

      missingConnections.set(0);
    } finally {
      state.set(State.stopped);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Stopped " + getInstanceDescription());
    }
  }


  @Override
  public String getInstanceDescription() {
    return String.format(DESC_FORMAT, url, tableName, idleConnections.size(), numConnections, missingConnections.get());
  }


  private Connection getConnection() {
    if (state.get() != State.running) {
      return null;
    }

    Connection result = idleConnections.poll();
    if (result != null) {
      return result; // there was a free connection
    }
    if (missingConnections.get() <= 0) {
      return null; // there was no free connection and there are no missing connections to replace
    }
    int actualMissingConnections = missingConnections.decrementAndGet();
    if (actualMissingConnections < 0) {
      missingConnections.incrementAndGet();
      return null; // we thought there were missing connections, but some other thread was faster
    }
    try {
      synchronized (allConnections) {
        if (state.get() != State.running) {
          return null;
        }
        result = connectionData.createConnection();
        allConnections.add(result);
      }
      if (logger.isInfoEnabled()) {
        logger.info("Recreated missing connection");
      }
    } catch (Exception e) {
      if (logger.isWarnEnabled()) {
        logger.warn("Could not recreate missing connection", e);
      }
      missingConnections.incrementAndGet();
    }
    return result;
  }


  private <T> Parameter createFullParameter(SharedResourceDefinition<T> resource, List<SharedResourceInstance<T>> data, boolean blobFirst)
      throws IOException {
    Parameter result = new Parameter();
    for (int i = 0; i < data.size(); i++) {
      SharedResourceInstance<T> instance = data.get(i);
      byte[] serializedData = resource.serialize(instance.getValue());
      ByteArrayOutputStream stream = new ByteArrayOutputStream(serializedData.length);
      stream.write(serializedData);
      stream.flush();
      if (blobFirst) {
        result.addParameter(new BLOB(stream));
      }
      result.addParameter(resource.getPath());
      result.addParameter(instance.getId());
      if (!blobFirst) {
        result.addParameter(new BLOB(stream));
      }
    }
    return result;
  }


  @Override
  public <T> SharedResourceRequestResult<T> create(SharedResourceDefinition<T> resource, List<SharedResourceInstance<T>> data) {
    if (data == null || data.isEmpty()) {
      return new SharedResourceRequestResult<T>(true, null, null);
    }
    Connection con = getConnection();
    if (con == null) {
      return new SharedResourceRequestResult<T>(false, NO_CONNECTION_AVAILABLE_EXCEPTION, null);
    }
    try {
      String sql = INSERT_TEMPLATE + INSERT_VALUE_PLACEHOLDER.repeat(data.size());
      sql = sql.substring(0, sql.length() - 2); // remove final ,\n
      try (PreparedStatement ps = con.prepareStatement(sql)) {
        Parameter params = createFullParameter(resource, data, false);
        params.addParameterTo(ps);
        ps.setQueryTimeout(queryTimeoutSeconds);
        ps.executeUpdate();
      }
    } catch (Exception e) {
      return new SharedResourceRequestResult<T>(false, e, null);
    } finally {
      idleConnections.add(con);
    }

    return new SharedResourceRequestResult<T>(true, null, null);
  }


  @Override
  public <T> SharedResourceRequestResult<T> delete(SharedResourceDefinition<T> resource, List<String> ids) {
    if (ids == null || ids.size() == 0) {
      if (logger.isDebugEnabled()) {
        logger.debug("No ids passed to delete request. resource: " + resource.getPath());
      }
      return new SharedResourceRequestResult<T>(true, null, null);
    }
    
    Connection con = getConnection();
    if (con == null) {
      return new SharedResourceRequestResult<T>(false, NO_CONNECTION_AVAILABLE_EXCEPTION, null);
    }
    try {
      String parameterString = "?, ".repeat(ids.size());
      parameterString = parameterString.substring(0, parameterString.length() - 2);
      String sql = String.format("%s%s)", DELETE_TEMPLATE, parameterString);
      try (PreparedStatement ps = con.prepareStatement(sql)) {
        Parameter params = new Parameter(ids.toArray(new Object[0]));
        params.addParameterTo(ps);
        ps.setQueryTimeout(queryTimeoutSeconds);
        ps.executeUpdate();
      }
    } catch (Exception e) {
      return new SharedResourceRequestResult<T>(false, e, null);
    } finally {
      idleConnections.add(con);
    }

    return new SharedResourceRequestResult<T>(true, null, null);
  }


  @Override
  public <T> SharedResourceRequestResult<T> read(SharedResourceDefinition<T> resource, List<String> ids) {
    Connection con = getConnection();
    if (con == null) {
      return new SharedResourceRequestResult<T>(false, NO_CONNECTION_AVAILABLE_EXCEPTION, null);
    }
    List<SharedResourceInstance<T>> resources = new ArrayList<SharedResourceInstance<T>>();
    try {
      resources = executeRead(con, resource, ids, false);
    } catch (Exception e) {
      return new SharedResourceRequestResult<T>(false, e, null);
    } finally {
      idleConnections.add(con);
    }
    return new SharedResourceRequestResult<T>(true, null, resources);
  }


  private <T> List<SharedResourceInstance<T>> executeRead(Connection con, SharedResourceDefinition<T> resource, List<String> ids,
                                                          boolean forUpdate)
      throws Exception {
    if (ids == null || ids.size() == 0) {
      return new ArrayList<>();
    }
    List<SharedResourceInstance<T>> resources = new ArrayList<>();
    String parameter = "?, ".repeat(ids.size());
    parameter = parameter.substring(0, parameter.length() - 2); //remove final ", "
    String sql = String.format("%s%s) %s", SELECT_TEMPLATE, parameter, forUpdate ? "FOR UPDATE" : "");
    PreparedStatement ps = con.prepareStatement(sql);
    Parameter params = new Parameter(resource.getPath());
    for (String id : ids) {
      params.addParameter(id);
    }
    params.addParameterTo(ps);
    ps.setQueryTimeout(queryTimeoutSeconds);
    try (ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        resources.add(readFromResultSet(resource, rs));
      }
    }
    return resources;
  }


  @Override
  public <T> SharedResourceRequestResult<T> readAll(SharedResourceDefinition<T> resource) {
    Connection con = getConnection();
    if (con == null) {
      return new SharedResourceRequestResult<T>(false, NO_CONNECTION_AVAILABLE_EXCEPTION, null);
    }
    List<SharedResourceInstance<T>> resources = new ArrayList<>();
    try {
      try (PreparedStatement ps = con.prepareStatement(SELECT_ALL_TEMPLATE)) {
        ps.setString(1, resource.getPath());
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            resources.add(readFromResultSet(resource, rs));
          }
        }
      }
    } catch (Exception e) {
      return new SharedResourceRequestResult<T>(false, e, null);
    } finally {
      idleConnections.add(con);
    }
    return new SharedResourceRequestResult<T>(true, null, resources);
  }


  private <T> SharedResourceInstance<T> readFromResultSet(SharedResourceDefinition<T> resource, ResultSet rs) throws SQLException {
    String id = rs.getString(2);
    byte[] data = rs.getBytes(3);
    SharedResourceInstance<T> instance = resource.deserialize(data, id);
    return instance;
  }


  @Override
  public <T> SharedResourceRequestResult<T> update(SharedResourceDefinition<T> resource, List<String> ids,
                                                   Function<SharedResourceInstance<T>, SharedResourceInstance<T>> update) {
    if (ids == null || ids.isEmpty()) {
      return new SharedResourceRequestResult<T>(true, null, null);
    }
    Connection con = getConnection();
    if (con == null) {
      return new SharedResourceRequestResult<T>(false, NO_CONNECTION_AVAILABLE_EXCEPTION, null);
    }

    List<SharedResourceInstance<T>> resources = new ArrayList<>();
    List<SharedResourceInstance<T>> newInstances = new ArrayList<>();
    try {
      con.setAutoCommit(false);
      resources = executeRead(con, resource, ids, true);
      if (resources.size() != ids.size()) {
        con.setAutoCommit(true);
        return new SharedResourceRequestResult<T>(false, MISSING_ENTRIES_EXCEPTION, null);
      }
      for (SharedResourceInstance<T> oldInstance : resources) {
        SharedResourceInstance<T> newInstance = update.apply(oldInstance);
        if (newInstance == null) {
          con.setAutoCommit(true);
          return new SharedResourceRequestResult<T>(false, UPDATE_INTERRUPTED_EXCEPTION, null);
        }
        if (!Objects.equals(newInstance.getId(), oldInstance.getId())) {
          con.setAutoCommit(true);
          return new SharedResourceRequestResult<T>(false, UPDATE_MODIFIED_ID_EXCEPTION, null);
        }
        newInstances.add(newInstance);
      }

      try (PreparedStatement ps = con.prepareStatement(UPDATE_STATEMENT)) {
        for (SharedResourceInstance<T> newInstance : newInstances) {
          Parameter params = createFullParameter(resource, List.of(newInstance), true);
          params.addParameterTo(ps);
          ps.addBatch();
        }
        ps.setQueryTimeout(queryTimeoutSeconds);
        ps.executeBatch();
        con.commit();
        con.setAutoCommit(true);
      }
    } catch (Exception e) {
      con = rollbackOrCloseConnection(con);
      return new SharedResourceRequestResult<T>(false, e, null);
    } finally {
      if (con != null) {
        idleConnections.add(con);
      }
    }

    return new SharedResourceRequestResult<T>(true, null, null);
  }


  private Connection rollbackOrCloseConnection(Connection con) {
    Connection result = con;
    try {
      con.rollback();
      if (logger.isDebugEnabled()) {
        logger.debug("Successfully rolled back connection " + con);
      }
      return result;
    } catch (Exception e) {
      try {
        con.close();
      } catch (Exception e1) {
        if (state.get() == State.running) {
          int totalMissingConnections = missingConnections.incrementAndGet();
          if (logger.isWarnEnabled()) {
            logger.warn("Could not close connection " + e + " - new total of missing connections: " + totalMissingConnections, e1);
          }
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug("Could not close connection " + con + " but synchronizer is no longer running.", e);
          }
        }
      }
    }

    return result;
  }
}

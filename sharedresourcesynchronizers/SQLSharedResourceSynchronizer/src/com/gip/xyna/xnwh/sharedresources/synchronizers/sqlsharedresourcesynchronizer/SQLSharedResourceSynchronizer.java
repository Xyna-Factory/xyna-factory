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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
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
  private static final String INSERT_VALUE_PLACEHOLDER = "(?, ?, ?),\n";

  private String INSERT_TEMPLATE;
  private String DELETE_TEMPLATE;
  private String SELECT_TEMPLATE;
  private String tableName;
  private String url;
  private String username;
  private String password;
  private int numConnections;
  private Duration connectionTimeout;
  private Duration socketTimeout;
  private int queryTimeoutSeconds;

  private final ConcurrentLinkedQueue<Connection> connections;


  public SQLSharedResourceSynchronizer(String tableName, String url, String username, String password, int numConnections,
                                       Duration connectionTimeout, Duration socketTimeout) {
    INSERT_TEMPLATE = String.format("INSERT INTO \"%s\" (sr_path, sr_id, sr_data) VALUES\n", tableName);
    DELETE_TEMPLATE = String.format("DELETE FROM \"%s\" WHERE sr_id in (", tableName);
    SELECT_TEMPLATE = String.format("SELECT * FROM \"%s\"", tableName);
    this.tableName = tableName;
    this.url = url;
    this.username = username;
    this.password = password;
    this.numConnections = numConnections;
    this.connectionTimeout = connectionTimeout;
    this.socketTimeout = socketTimeout;
    this.queryTimeoutSeconds = (int) connectionTimeout.getDuration(TimeUnit.SECONDS);
    connections = new ConcurrentLinkedQueue<>();

    NO_CONNECTION_AVAILABLE_EXCEPTION.setStackTrace(new StackTraceElement[0]);
  }


  @Override
  public void start() {
    DBConnectionDataBuilder cdb = DBConnectionData.newDBConnectionData();
    cdb.user(username);
    cdb.password(password);
    cdb.url(url);
    cdb.socketTimeoutInSeconds((int) socketTimeout.getDuration(TimeUnit.SECONDS));
    cdb.connectTimeoutInSeconds((int) connectionTimeout.getDuration(TimeUnit.SECONDS));
    cdb.autoCommit(true);
    DBConnectionData cb = cdb.build();

    try {
      for (int i = 0; i < numConnections; i++) {
        connections.add(cb.createConnection());
      }
    } catch (Exception e) {
      logger.warn("Could not create connection to" + url + ".", e);
    }

  }


  @Override
  public void stop() {
    // TODO Auto-generated method stub

  }


  @Override
  public String getInstanceDescription() {
    return String.format("SQLSharedResourceSynchronizer - %s - %s", url, tableName);
  }


  private Connection getConnection() {
    return connections.poll();
  }


  private <T> Parameter createFullParameter(SharedResourceDefinition<T> resource, List<SharedResourceInstance<T>> data) throws IOException {
    Parameter result = new Parameter();
    for (int i = 0; i < data.size(); i++) {
      SharedResourceInstance<T> instance = data.get(i);
      byte[] serializedData = resource.serialize(instance.getValue());
      ByteArrayOutputStream stream = new ByteArrayOutputStream(serializedData.length);
      stream.write(serializedData);
      result.addParameter(resource.getPath());
      result.addParameter(instance.getId());
      result.addParameter(new BLOB(stream));
    }
    return result;
  }


  @Override
  public <T> SharedResourceRequestResult<T> create(SharedResourceDefinition<T> resource, List<SharedResourceInstance<T>> data) {
    Connection con = getConnection();
    if (con == null) {
      return new SharedResourceRequestResult<T>(false, NO_CONNECTION_AVAILABLE_EXCEPTION, null);
    }
    try {
      String sql = INSERT_TEMPLATE + INSERT_VALUE_PLACEHOLDER.repeat(data.size());
      sql = sql.substring(0, sql.length() - 2); // remove final ,\n
      PreparedStatement ps = con.prepareStatement(sql);
      Parameter params = createFullParameter(resource, data);
      params.addParameterTo(ps);
      ps.setQueryTimeout(queryTimeoutSeconds);
      ps.executeUpdate();

    } catch (Exception e) {
      return new SharedResourceRequestResult<T>(false, e, null);
    } finally {
      connections.add(con);
    }

    return new SharedResourceRequestResult<T>(true, null, null);
  }


  @Override
  public <T> SharedResourceRequestResult<T> delete(SharedResourceDefinition<T> resource, List<String> ids) {
    Connection con = getConnection();
    if (con == null) {
      return new SharedResourceRequestResult<T>(false, NO_CONNECTION_AVAILABLE_EXCEPTION, null);
    }
    try {
      String sql = DELETE_TEMPLATE + "? ".repeat(ids.size()) + ")";
      PreparedStatement ps = con.prepareStatement(sql);
      Parameter params = new Parameter(ids);
      params.addParameterTo(ps);
      ps.setQueryTimeout(queryTimeoutSeconds);
      ps.executeUpdate();
    } catch(Exception e) {
      return new SharedResourceRequestResult<T>(false, e, null);
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
      PreparedStatement ps = con.prepareStatement("");
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        String id = rs.getString(1);
        byte[] data = rs.getBytes(2);
        SharedResourceInstance<T> instance = resource.deserialize(data, id);
        resources.add(instance);
      }
    } catch (Exception e) {
      return new SharedResourceRequestResult<T>(false, e, null);
    }
    return new SharedResourceRequestResult<T>(true, null, resources);
  }


  @Override
  public <T> SharedResourceRequestResult<T> readAll(SharedResourceDefinition<T> resource) {
    Connection con = getConnection();
    if (con == null) {
      return new SharedResourceRequestResult<T>(false, NO_CONNECTION_AVAILABLE_EXCEPTION, null);
    }
    List<SharedResourceInstance<T>> resources = new ArrayList<SharedResourceInstance<T>>();
    try {
      PreparedStatement ps = con.prepareStatement(SELECT_TEMPLATE);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        String id = rs.getString(1);
        byte[] data = rs.getBytes(2);
        SharedResourceInstance<T> instance = resource.deserialize(data, id);
        resources.add(instance);
      }
    } catch (Exception e) {
      return new SharedResourceRequestResult<T>(false, e, null);
    }
    return new SharedResourceRequestResult<T>(true, null, resources);
  }


  @Override
  public <T> SharedResourceRequestResult<T> update(SharedResourceDefinition<T> resource, List<String> ids,
                                                   Function<SharedResourceInstance<T>, SharedResourceInstance<T>> update) {
    // TODO Auto-generated method stub
    return null;
  }

}

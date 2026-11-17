/*
 * Copyright 2026 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package com.gip.xyna.xnwh.sharedresources.synchronizers.sqlsharedresourcesynchronizer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.gip.xyna.utils.db.DBConnectionData;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xnwh.sharedresources.SharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceInstance;
import com.gip.xyna.xnwh.sharedresources.SharedResourceRequestResult;

import junit.framework.TestCase;

public class SQLSharedResourceSynchronizerTest extends TestCase {

  private SQLSharedResourceSynchronizer synchronizer;
  private TestConnectionData connectionData;

  @Override
  protected void setUp() throws Exception {
    initialize("0 s");
  }

  private void initialize(String threshold) throws Exception {
    synchronizer = new SQLSharedResourceSynchronizer("test", "jdbc:mysql://localhost/test", "user", "password",
                                                     2, 0, Duration.valueOf("10 s"), Duration.valueOf("10 s"),
                                                     Duration.valueOf(threshold));
    connectionData = new TestConnectionData();
    field("connectionData").set(synchronizer, connectionData);
    Object state = field("state").get(synchronizer);
    Object running = null;
    for (Object value : Class.forName(SQLSharedResourceSynchronizer.class.getName() + "$State").getEnumConstants()) {
      if (value.toString().equals("running")) {
        running = value;
      }
    }
    assertNotNull(running);
    state.getClass().getMethod("set", Enum.class).invoke(state, running);
  }

  @Override
  protected void tearDown() {
    synchronizer.stop();
  }

  public void testValidConnectionIsCheckedOnEachBorrow() throws Exception {
    TestConnection connection = new TestConnection(true, false);
    addIdle(connection.proxy);

    assertSame(connection.proxy, borrow());
    returnConnection(connection.proxy);
    assertSame(connection.proxy, borrow());
    assertEquals(2, connection.validations);
    assertEquals(10, connection.validationTimeout);
    assertEquals(0, connectionData.creations);
  }

  public void testInvalidConnectionIsClosedAndReplaced() throws Exception {
    TestConnection invalid = new TestConnection(false, false);
    TestConnection replacement = new TestConnection(true, false);
    connectionData.next = replacement.proxy;
    addIdle(invalid.proxy);

    assertSame(replacement.proxy, borrow());
    assertTrue(invalid.closed);
    assertFalse(allConnections().contains(invalid.proxy));
    assertTrue(allConnections().contains(replacement.proxy));
    assertEquals(1, connectionData.creations);
  }

  public void testValidationExceptionUsesAnotherIdleConnection() throws Exception {
    TestConnection invalid = new TestConnection(false, true);
    TestConnection valid = new TestConnection(true, false);
    addIdle(invalid.proxy);
    addIdle(valid.proxy);

    assertSame(valid.proxy, borrow());
    assertTrue(invalid.closed);
    assertEquals(0, connectionData.creations);
    assertSame(connectionData.next, borrow());
    assertEquals(1, connectionData.creations);
  }

  public void testFailedReplacementCanBeRetried() throws Exception {
    TestConnection invalid = new TestConnection(false, false);
    TestConnection replacement = new TestConnection(true, false);
    addIdle(invalid.proxy);
    connectionData.fail = true;

    assertNull(borrow());
    assertTrue(invalid.closed);
    connectionData.fail = false;
    connectionData.next = replacement.proxy;
    assertSame(replacement.proxy, borrow());
    assertEquals(2, connectionData.creations);
  }

  public void testConnectionThatCannotBeClosedRemainsTracked() throws Exception {
    TestConnection invalid = new TestConnection(false, false);
    invalid.closeFails = true;
    addIdle(invalid.proxy);

    assertSame(connectionData.next, borrow());
    assertTrue(allConnections().contains(invalid.proxy));
  }

  public void testValidationOnlyAfterIdleThresholdAndResetsOnReturn() throws Exception {
    synchronizer.stop();
    initialize("60 s");
    TestConnection connection = new TestConnection(true, false);
    addIdle(connection.proxy);

    ageIdleConnection(59);
    assertSame(connection.proxy, borrow());
    assertEquals(0, connection.validations);
    returnConnection(connection.proxy);
    ageIdleConnection(61);
    assertSame(connection.proxy, borrow());
    assertEquals(1, connection.validations);
    returnConnection(connection.proxy);
    assertSame(connection.proxy, borrow());
    assertEquals(1, connection.validations);
  }

  public void testRequestUsesValidConnectionAfterInvalidConnections() throws Exception {
    TestConnection invalid = new TestConnection(false, false);
    TestConnection valid = new TestConnection(true, false);
    addIdle(invalid.proxy);
    addIdle(valid.proxy);

    SharedResourceRequestResult<String> result = synchronizer.readAll(resource());
    assertTrue(result.isSuccess());
    assertTrue(invalid.closed);
    assertEquals(1, valid.queries);
    assertEquals(0, connectionData.creations);
  }

  public void testRequestFailsWhenAllConnectionsAndReplacementFail() throws Exception {
    TestConnection first = new TestConnection(false, false);
    TestConnection second = new TestConnection(false, true);
    addIdle(first.proxy);
    addIdle(second.proxy);
    connectionData.fail = true;

    SharedResourceRequestResult<String> result = synchronizer.readAll(resource());
    assertFalse(result.isSuccess());
    assertEquals("No connection available", result.getException().getMessage());
    assertTrue(first.closed);
    assertTrue(second.closed);
    assertEquals(1, connectionData.creations);
  }

  public void testRecentlyReturnedConnectionReportsQueryFailure() throws Exception {
    synchronizer.stop();
    initialize("60 s");
    TestConnection connection = new TestConnection(false, false);
    connection.failQueries = true;
    addIdle(connection.proxy);

    SharedResourceRequestResult<String> result = synchronizer.readAll(resource());
    assertFalse(result.isSuccess());
    assertEquals("Query failed", result.getException().getMessage());
    assertEquals(0, connection.validations);
  }

  public void testFactoryConfiguresIdleThreshold() throws Exception {
    SQLSharedResourceSynchronizerFactory factory = new SQLSharedResourceSynchronizerFactory();
    SQLSharedResourceSynchronizer defaultSynchronizer = (SQLSharedResourceSynchronizer) factory.createSynchronizer(
        Arrays.asList("url=jdbc:mysql://localhost/test", "username=user", "password=password"));
    SQLSharedResourceSynchronizer configuredSynchronizer = (SQLSharedResourceSynchronizer) factory.createSynchronizer(
        Arrays.asList("url=jdbc:mysql://localhost/test", "username=user", "password=password", "validationIdleThreshold=5 min"));

    assertEquals(TimeUnit.SECONDS.toNanos(60), field("validationIdleThresholdNanos").getLong(defaultSynchronizer));
    assertEquals(TimeUnit.MINUTES.toNanos(5), field("validationIdleThresholdNanos").getLong(configuredSynchronizer));
  }

  private SharedResourceDefinition<String> resource() {
    return new SharedResourceDefinition<String>("test") {
      @Override
      public byte[] serialize(String value) {
        return value.getBytes();
      }

      @Override
      public SharedResourceInstance<String> deserialize(byte[] value, String id, Long created) {
        throw new UnsupportedOperationException();
      }
    };
  }

  private Field field(String name) throws Exception {
    Field field = SQLSharedResourceSynchronizer.class.getDeclaredField(name);
    field.setAccessible(true);
    return field;
  }

  @SuppressWarnings("unchecked")
  private Set<Connection> allConnections() throws Exception {
    return (Set<Connection>) field("allConnections").get(synchronizer);
  }

  private void addIdle(Connection connection) throws Exception {
    allConnections().add(connection);
    returnConnection(connection);
  }

  private void ageIdleConnection(long seconds) throws Exception {
    Queue<?> idleConnections = (Queue<?>) field("idleConnections").get(synchronizer);
    Object idleConnection = idleConnections.peek();
    assertNotNull(idleConnection);
    Field idleSince = idleConnection.getClass().getDeclaredField("idleSinceNanos");
    idleSince.setAccessible(true);
    idleSince.setLong(idleConnection, System.nanoTime() - TimeUnit.SECONDS.toNanos(seconds));
  }

  private Connection borrow() throws Exception {
    Method method = SQLSharedResourceSynchronizer.class.getDeclaredMethod("getConnection");
    method.setAccessible(true);
    return (Connection) method.invoke(synchronizer);
  }

  private void returnConnection(Connection connection) throws Exception {
    Method method = SQLSharedResourceSynchronizer.class.getDeclaredMethod("returnConnection", Connection.class);
    method.setAccessible(true);
    method.invoke(synchronizer, connection);
  }

  private static class TestConnectionData extends DBConnectionData {
    private Connection next = new TestConnection(true, false).proxy;
    private boolean fail;
    private int creations;

    @Override
    public Connection createConnection() throws SQLException {
      creations++;
      if (fail) {
        throw new SQLException("Connection creation failed");
      }
      return next;
    }
  }

  private static class TestConnection implements InvocationHandler {
    private final Connection proxy = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                                                                          new Class<?>[] { Connection.class }, this);
    private final boolean valid;
    private final boolean validationFails;
    private boolean closed;
    private boolean closeFails;
    private boolean failQueries;
    private int validations;
    private int validationTimeout;
    private int queries;

    private TestConnection(boolean valid, boolean validationFails) {
      this.valid = valid;
      this.validationFails = validationFails;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws SQLException {
      switch (method.getName()) {
        case "isValid":
          validations++;
          validationTimeout = (Integer) args[0];
          if (validationFails) {
            throw new SQLException("Validation failed");
          }
          return valid;
        case "close":
          if (closeFails) {
            throw new SQLException("Close failed");
          }
          closed = true;
          return null;
        case "abort":
          closed = true;
          return null;
        case "isClosed":
          return closed;
        case "prepareStatement":
          if (failQueries) {
            throw new SQLException("Query failed");
          }
          return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(), new Class<?>[] { PreparedStatement.class },
              (statement, statementMethod, statementArgs) -> {
                switch (statementMethod.getName()) {
                  case "setString":
                  case "close":
                    return null;
                  case "executeQuery":
                    queries++;
                    return Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class<?>[] { ResultSet.class },
                        (resultSet, resultMethod, resultArgs) -> {
                          if (resultMethod.getName().equals("next")) {
                            return false;
                          }
                          if (resultMethod.getName().equals("close")) {
                            return null;
                          }
                          throw new UnsupportedOperationException(resultMethod.getName());
                        });
                  default:
                    throw new UnsupportedOperationException(statementMethod.getName());
                }
              });
        case "hashCode":
          return System.identityHashCode(proxy);
        case "equals":
          return proxy == args[0];
        case "toString":
          return "TestConnection";
        default:
          throw new UnsupportedOperationException(method.getName());
      }
    }
  }
}

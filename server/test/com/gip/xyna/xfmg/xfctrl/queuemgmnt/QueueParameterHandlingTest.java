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

package com.gip.xyna.xfmg.xfctrl.queuemgmnt;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.utils.collections.CSVStringList;
import com.gip.xyna.xnwh.securestorage.SecureStorage;

import junit.framework.TestCase;


public class QueueParameterHandlingTest extends TestCase {

  private static final String SECURE_STORAGE_SEEDFILE_PROPERTY = "xnwh.securestorage.seedfile";
  private static final String SECURE_STORAGE_SEED_KEY = "securestorage.seed";

  private String previousSeedFile;
  private File seedFile;


  protected void setUp() throws Exception {
    super.setUp();

    previousSeedFile = System.getProperty(SECURE_STORAGE_SEEDFILE_PROPERTY);
    seedFile = File.createTempFile("queue-parameter-handling", ".properties");
    try (FileWriter writer = new FileWriter(seedFile)) {
      writer.write(SECURE_STORAGE_SEED_KEY + "=queue-parameter-handling-test-seed\n");
    }

    System.setProperty(SECURE_STORAGE_SEEDFILE_PROPERTY, seedFile.getAbsolutePath());
    resetSecureStorageSeed();
  }


  protected void tearDown() throws Exception {
    if (previousSeedFile == null) {
      System.clearProperty(SECURE_STORAGE_SEEDFILE_PROPERTY);
    } else {
      System.setProperty(SECURE_STORAGE_SEEDFILE_PROPERTY, previousSeedFile);
    }

    resetSecureStorageSeed();

    if (seedFile != null) {
      seedFile.delete();
    }

    super.tearDown();
  }

  public void testInitialVersionNullUsesLegacyBlobParameters() {
    Queue queue = new Queue();
    queue.setVersion(null);

    ActiveMQConnectData legacyConnectData = createActiveMqConnectData("legacy-host", 1111);
    queue.setConnectData(legacyConnectData);
    queue.setQueueType(QueueType.ACTIVE_MQ);

    QueueConnectData resolvedConnectData = queue.getConnectDataForCurrentVersion();
    assertTrue(resolvedConnectData instanceof ActiveMQConnectData);
    ActiveMQConnectData activeMq = (ActiveMQConnectData) resolvedConnectData;
    assertEquals("legacy-host", activeMq.getHostname());
    assertEquals(1111, activeMq.getPort());
    assertEquals(QueueType.ACTIVE_MQ, queue.getQueueTypeForCurrentVersion());
  }


  public void testInitialVersionZeroUsesLegacyBlobParameters() {
    Queue queue = new Queue();
    queue.setVersion(0);

    ActiveMQConnectData legacyConnectData = createActiveMqConnectData("legacy-zero", 2222);
    queue.setConnectData(legacyConnectData);
    queue.setQueueType(QueueType.ACTIVE_MQ);

    QueueConnectData resolvedConnectData = queue.getConnectDataForCurrentVersion();
    assertTrue(resolvedConnectData instanceof ActiveMQConnectData);
    ActiveMQConnectData activeMq = (ActiveMQConnectData) resolvedConnectData;
    assertEquals("legacy-zero", activeMq.getHostname());
    assertEquals(2222, activeMq.getPort());
    assertEquals(QueueType.ACTIVE_MQ, queue.getQueueTypeForCurrentVersion());
  }


  public void testCurrentVersionConvertsConnectDataToStringAndReadsItBack() {
    Queue queue = new Queue();
    queue.setVersion(1);

    ActiveMQConnectData connectData = createActiveMqConnectData("new-host", 61616);
    queue.setConnectDataForCurrentVersion(connectData);
    queue.setQueueTypeForCurrentVersion(QueueType.ACTIVE_MQ);

    assertNull(queue.getConnectData());
    assertNull(queue.getQueueType());
    assertNotNull(queue.getConnectDataStr());
    assertNotNull(queue.getQueueTypeStr());

    QueueConnectData resolvedConnectData = queue.getConnectDataForCurrentVersion();
    assertTrue(resolvedConnectData instanceof ActiveMQConnectData);
    ActiveMQConnectData activeMq = (ActiveMQConnectData) resolvedConnectData;
    assertEquals("new-host", activeMq.getHostname());
    assertEquals(61616, activeMq.getPort());
    assertEquals(QueueType.ACTIVE_MQ, queue.getQueueTypeForCurrentVersion());
  }


  public void testCurrentVersionHonorsNewStringParametersOverLegacyBlobFields() {
    Queue queue = new Queue();
    queue.setVersion(1);

    ActiveMQConnectData legacyConnectData = createActiveMqConnectData("legacy-host", 1000);
    queue.setConnectData(legacyConnectData);
    queue.setQueueType(QueueType.ORACLE_AQ);

    ActiveMQConnectData newConnectData = createActiveMqConnectData("new-host", 2000);
    QueueConnectStringData converter = new QueueConnectStringData();
    queue.setConnectDataStr(converter.fromConnectData(newConnectData));
    queue.setQueueTypeStr(QueueType.ACTIVE_MQ.name());

    QueueConnectData resolvedConnectData = queue.getConnectDataForCurrentVersion();
    assertTrue(resolvedConnectData instanceof ActiveMQConnectData);
    ActiveMQConnectData activeMq = (ActiveMQConnectData) resolvedConnectData;
    assertEquals("new-host", activeMq.getHostname());
    assertEquals(2000, activeMq.getPort());

    assertEquals(QueueType.ACTIVE_MQ, queue.getQueueTypeForCurrentVersion());
  }


  public void testInitialVersionIgnoresStringParametersAndUsesLegacyBlobFields() {
    Queue queue = new Queue();
    queue.setVersion(0);

    ActiveMQConnectData legacyConnectData = createActiveMqConnectData("legacy-host", 3456);
    queue.setConnectData(legacyConnectData);
    queue.setQueueType(QueueType.ACTIVE_MQ);

    ActiveMQConnectData conflictingNewConnectData = createActiveMqConnectData("string-host", 7890);
    QueueConnectStringData converter = new QueueConnectStringData();
    queue.setConnectDataStr(converter.fromConnectData(conflictingNewConnectData));
    queue.setQueueTypeStr(QueueType.ORACLE_AQ.name());

    QueueConnectData resolvedConnectData = queue.getConnectDataForCurrentVersion();
    assertTrue(resolvedConnectData instanceof ActiveMQConnectData);
    ActiveMQConnectData activeMq = (ActiveMQConnectData) resolvedConnectData;
    assertEquals("legacy-host", activeMq.getHostname());
    assertEquals(3456, activeMq.getPort());
    assertEquals(QueueType.ACTIVE_MQ, queue.getQueueTypeForCurrentVersion());
  }


  public void testCurrentVersionConvertsOracleAQConnectDataToStringAndReadsItBack() {
    Queue queue = new Queue();
    queue.setVersion(1);

    OracleAQConnectData connectData = createOracleAqConnectData("jdbc:oracle:thin:@//db.example:1521/XE", "queue_user", "secret");
    queue.setConnectDataForCurrentVersion(connectData);
    queue.setQueueTypeForCurrentVersion(QueueType.ORACLE_AQ);

    QueueConnectData resolvedConnectData = queue.getConnectDataForCurrentVersion();
    assertTrue(resolvedConnectData instanceof OracleAQConnectData);
    OracleAQConnectData oracle = (OracleAQConnectData) resolvedConnectData;
    assertEquals("jdbc:oracle:thin:@//db.example:1521/XE", oracle.getJdbcUrl());
    assertEquals("queue_user", oracle.getUserName());
    assertEquals("secret", oracle.getPassword());
    assertEquals(QueueType.ORACLE_AQ, queue.getQueueTypeForCurrentVersion());
  }


  public void testCurrentVersionEncryptsOracleAQPasswordInCsvString() {
    QueueConnectStringData converter = new QueueConnectStringData();
    String serialized = converter.fromConnectData(createOracleAqConnectData("jdbc:oracle:thin:@//db.example:1521/XE", "queue_user", "super-secret"));

    assertTrue(serialized.contains("password="));
    assertFalse(serialized.contains("super-secret"));

    QueueConnectData resolvedConnectData = converter.fromStringParameters(serialized);
    assertTrue(resolvedConnectData instanceof OracleAQConnectData);
    OracleAQConnectData oracle = (OracleAQConnectData) resolvedConnectData;
    assertEquals("jdbc:oracle:thin:@//db.example:1521/XE", oracle.getJdbcUrl());
    assertEquals("queue_user", oracle.getUserName());
    assertEquals("super-secret", oracle.getPassword());
  }


  public void testCurrentVersionConvertsWebSphereMQConnectDataToStringAndReadsItBack() {
    Queue queue = new Queue();
    queue.setVersion(1);

    WebSphereMQConnectData connectData = createWebSphereMqConnectData("wsmq-host", 1414, "QM1", "DEV.ADMIN.SVRCONN");
    queue.setConnectDataForCurrentVersion(connectData);
    queue.setQueueTypeForCurrentVersion(QueueType.WEBSPHERE_MQ);

    QueueConnectData resolvedConnectData = queue.getConnectDataForCurrentVersion();
    assertTrue(resolvedConnectData instanceof WebSphereMQConnectData);
    WebSphereMQConnectData wsmq = (WebSphereMQConnectData) resolvedConnectData;
    assertEquals("wsmq-host", wsmq.getHostname());
    assertEquals(1414, wsmq.getPort());
    assertEquals("QM1", wsmq.getQueueManager());
    assertEquals("DEV.ADMIN.SVRCONN", wsmq.getChannel());
    assertEquals(QueueType.WEBSPHERE_MQ, queue.getQueueTypeForCurrentVersion());
  }


  public void testQueueConnectStringDataReturnsNullWhenConnectDataTypeIsMissing() {
    QueueConnectStringData converter = new QueueConnectStringData();
    String serialized = converter.fromConnectData(createActiveMqConnectData("host-without-type", 61616));

    List<String> params = new ArrayList<String>(CSVStringList.valueOf(serialized));
    for (int i = 0; i < params.size(); i++) {
      if (params.get(i).contains("connectDataType")) {
        params.remove(i);
        break;
      }
    }

    String withoutType = new CSVStringList(params).serializeToString();
    assertNull(converter.fromStringParameters(withoutType));
  }


  public void testWebSphereParserRejectsMissingMandatoryChannel() {
    WebSphereMQConnectData connectData = createWebSphereMqConnectData("ws-host", 1414, "QM2", "CH1");
    List<String> params = new ArrayList<String>(WebSphereMQConnectStringData.fromConnectData(connectData).toParameters());

    removeNamedParameter(params, "channel");

    try {
      WebSphereMQConnectStringData.fromStringParameters(params);
      fail("Expected IllegalArgumentException for missing channel parameter");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("channel"));
    }
  }


  public void testWebSphereParserRejectsMissingMandatoryPort() {
    WebSphereMQConnectData connectData = createWebSphereMqConnectData("ws-host", 1414, "QM2", "CH1");
    List<String> params = new ArrayList<String>(WebSphereMQConnectStringData.fromConnectData(connectData).toParameters());

    removeNamedParameter(params, "port");

    try {
      WebSphereMQConnectStringData.fromStringParameters(params);
      fail("Expected IllegalArgumentException for missing port parameter");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("port"));
    }
  }


  public void testWebSphereParserRejectsMissingMandatoryHostname() {
    WebSphereMQConnectData connectData = createWebSphereMqConnectData("ws-host", 1414, "QM2", "CH1");
    List<String> params = new ArrayList<String>(WebSphereMQConnectStringData.fromConnectData(connectData).toParameters());

    removeNamedParameter(params, "hostname");

    try {
      WebSphereMQConnectStringData.fromStringParameters(params);
      fail("Expected IllegalArgumentException for missing hostname parameter");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("hostname"));
    }
  }


  public void testWebSphereParserRejectsMissingMandatoryQueueManager() {
    WebSphereMQConnectData connectData = createWebSphereMqConnectData("ws-host", 1414, "QM2", "CH1");
    List<String> params = new ArrayList<String>(WebSphereMQConnectStringData.fromConnectData(connectData).toParameters());

    removeNamedParameter(params, "queueManager");

    try {
      WebSphereMQConnectStringData.fromStringParameters(params);
      fail("Expected IllegalArgumentException for missing queueManager parameter");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("queueManager"));
    }
  }


  public void testActiveMqParserRejectsMissingMandatoryPort() {
    ActiveMQConnectData connectData = createActiveMqConnectData("amq-host", 61616);
    List<String> params = new ArrayList<String>(ActiveMQConnecStringtData.fromConnectData(connectData).toParameters());

    for (int i = 0; i < params.size(); i++) {
      if (params.get(i).startsWith("port=")) {
        params.remove(i);
        break;
      }
    }

    try {
      ActiveMQConnecStringtData.fromStringParameters(params);
      fail("Expected IllegalArgumentException for missing port parameter");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("port"));
    }
  }


  public void testActiveMqParserRejectsMissingMandatoryHostname() {
    ActiveMQConnectData connectData = createActiveMqConnectData("amq-host", 61616);
    List<String> params = new ArrayList<String>(ActiveMQConnecStringtData.fromConnectData(connectData).toParameters());

    for (int i = 0; i < params.size(); i++) {
      if (params.get(i).startsWith("hostname=")) {
        params.remove(i);
        break;
      }
    }

    try {
      ActiveMQConnecStringtData.fromStringParameters(params);
      fail("Expected IllegalArgumentException for missing hostname parameter");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("hostname"));
    }
  }


  public void testCreateQueueConnectDataSupportsLegacyActiveMqParameters() {
    QueueConnectData connectData = QueueManagement.createQueueConnectData(QueueType.ACTIVE_MQ,
                                                                           new String[] { "legacy-amq-host", "61616" });

    assertTrue(connectData instanceof ActiveMQConnectData);
    ActiveMQConnectData activeMq = (ActiveMQConnectData) connectData;
    assertEquals("legacy-amq-host", activeMq.getHostname());
    assertEquals(61616, activeMq.getPort());
  }


  public void testCreateQueueConnectDataSupportsNamedWebsphereParameters() {
    QueueConnectData connectData = QueueManagement.createQueueConnectData(QueueType.WEBSPHERE_MQ,
                                                                           new String[] { "queueManager=QM_TEST",
                                                                               "hostname=mq.example.org", "port=1414",
                                                                               "channel=DEV.ADMIN.SVRCONN" });

    assertTrue(connectData instanceof WebSphereMQConnectData);
    WebSphereMQConnectData websphereMq = (WebSphereMQConnectData) connectData;
    assertEquals("QM_TEST", websphereMq.getQueueManager());
    assertEquals("mq.example.org", websphereMq.getHostname());
    assertEquals(1414, websphereMq.getPort());
    assertEquals("DEV.ADMIN.SVRCONN", websphereMq.getChannel());
  }


  public void testCreateQueueConnectDataSupportsNamedOracleParametersWithoutUuid() {
    QueueConnectData connectData = QueueManagement.createQueueConnectData(QueueType.ORACLE_AQ,
                                                                           new String[] {
                                                                               "user=queue_user",
                                                                               "password=cleartext-secret",
                                                                               "jdbc=jdbc:oracle:thin:@//db.example:1521/XE" });

    assertTrue(connectData instanceof OracleAQConnectData);
    OracleAQConnectData oracle = (OracleAQConnectData) connectData;
    assertEquals("queue_user", oracle.getUserName());
    assertEquals("cleartext-secret", oracle.getPassword());
    assertEquals("jdbc:oracle:thin:@//db.example:1521/XE", oracle.getJdbcUrl());
  }


  public void testCreateQueueConnectDataRejectsMixedNamedAndUnnamedParameters() {
    try {
      QueueManagement.createQueueConnectData(QueueType.ACTIVE_MQ, new String[] { "hostname=amq.example.org", "61616" });
      fail("Expected IllegalArgumentException for mixed named and unnamed parameters");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("Mixed named and unnamed"));
    }
  }


  private static ActiveMQConnectData createActiveMqConnectData(String hostname, int port) {
    ActiveMQConnectData data = new ActiveMQConnectData();
    data.setHostname(hostname);
    data.setPort(port);
    return data;
  }


  private static void removeNamedParameter(List<String> params, String parameterName) {
    for (int i = 0; i < params.size(); i++) {
      if (params.get(i).startsWith(parameterName + "=")) {
        params.remove(i);
        return;
      }
    }
  }


  private static void resetSecureStorageSeed() throws Exception {
    Field seedField = SecureStorage.class.getDeclaredField("seed");
    seedField.setAccessible(true);
    seedField.set(null, null);
  }


  private static OracleAQConnectData createOracleAqConnectData(String jdbcUrl, String userName, String password) {
    OracleAQConnectData data = new OracleAQConnectData();
    data.setJdbcUrl(jdbcUrl);
    data.setUserName(userName);
    data.setPassword(password);
    return data;
  }


  private static WebSphereMQConnectData createWebSphereMqConnectData(String hostname, int port, String queueManager,
      String channel) {
    WebSphereMQConnectData data = new WebSphereMQConnectData();
    data.setHostname(hostname);
    data.setPort(port);
    data.setQueueManager(queueManager);
    data.setChannel(channel);
    return data;
  }
}

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
package com.gip.xyna.utils.jms;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;

import oracle.jms.AQjmsFactory;
import oracle.jms.AQjmsQueueConnectionFactory;
import oracle.jms.AQjmsSession;

/**
 *         �bergeben. Entsprechend gibt es die Methoden close und commit um die ggfs interne Connection zu schliessen.
 * @deprecated use JMSQueueUtils
 */
public class AQConnector implements JMSConnector {

  /**
   * Default timeout for dequeue. Value is ten seconds.
   */
  public static final int DEFAULT_TIMEOUT = 10000;
  private static boolean debugMode = false;

  public static boolean isDebugMode() {
    return debugMode;
  }

  public static void setDebugMode(boolean _debugMode) {
    AQConnector.debugMode = _debugMode;
  }

  private boolean privateDBConnection;

  private QueueSession session = null;
  private QueueConnection connection = null;

  /**
   * Konstruktor f�r direktes AQ-ing speziell fuer RAC geeignet Beispiel: new AQConnector("jdbc:oracle:thin@(DESCRIPTION = (ADDRESS = (PROTOCOL = TCP)(HOST = gipsun162-vip)(PORT = 1521)) (ADDRESS = (PROTOCOL = TCP)(HOST = gipsun163-vip)(PORT = 1521))(LOAD_BALANCE = yes) (CONNECT_DATA = (SERVER = DEDICATED) (SERVICE_NAME = pallasha2) (FAILOVER_MODE = (TYPE = SELECT) (METHOD = BASIC) (RETRIES = 180) (DELAY =5)))"
   * "user", "user");
   * 
   * @param jdbcUrl url
   * @param dbUser benutzer, mit dem man auf queue zugreifen kann
   * @param dbPassword zugeh�riges pw
   * @throws Exception
   */
  public AQConnector(String jdbcUrl, String dbUser, String dbPassword) throws JMSException {
    QueueConnectionFactory qcfact = AQjmsFactory.getQueueConnectionFactory(jdbcUrl,
      new Properties());
    connection = qcfact.createQueueConnection(dbUser, dbPassword);
    session = connection.createQueueSession(true, Session.CLIENT_ACKNOWLEDGE);
    connection.start();
    privateDBConnection = true;
  }

  /**
   * Konstruktor f�r direktes AQ-ing.
   * 
   * @param dbHost ip des datenbank rechners
   * @param dbPort port des datenbank rechners f�r datenbank zugriff
   * @param dbSid SID der datenbank
   * @param dbUser benutzer, mit dem man auf queue zugreifen kann
   * @param dbPassword zugeh�riges pw
   * @throws Exception
   */
  public AQConnector(String dbHost, String dbPort, String dbSid, String dbUser, String dbPassword)
                  throws JMSException {
    QueueConnectionFactory qcfact = AQjmsFactory.getQueueConnectionFactory(dbHost, dbSid, Integer
      .parseInt(dbPort), "thin");
    connection = qcfact.createQueueConnection(dbUser, dbPassword);
    session = connection.createQueueSession(true, Session.CLIENT_ACKNOWLEDGE);
    connection.start();
    privateDBConnection = true;
  }

  /**
   * Erzeugen der QueueConnection und QueueSession auf Basis der uebergebenen Connection
   * 
   * @param con sql connection
   */
  public AQConnector(Connection con) throws JMSException {
    connection = AQjmsQueueConnectionFactory.createQueueConnection(con);
    session = connection.createQueueSession(true, Session.CLIENT_ACKNOWLEDGE);
    connection.start();
    privateDBConnection = false;
  }

  /*
   * (non-Javadoc)
   * @see com.gip.xyna.utils.jms.JMSConnector#close()
   */

  public void close() throws JMSException {
    if (session != null) {
      Connection con = ((AQjmsSession) session).getDBConnection();
      // ueber die externe connection moechte man selbst kontrolle behalten
      if (con != null && privateDBConnection) {
        try {
          con.commit();
          con.close();
        }
        catch (Exception e) {
          throw new JMSException("Database connection couldn't be closed, caused by " + e
            .getMessage());
        }
      }
      session.close();
    }
    if (connection != null) {
      connection.close();
    }
  }

  public void commit() throws Exception {
    if (session != null) {
      Connection con = ((AQjmsSession) session).getDBConnection();
      if (con != null) {
        con.commit();
      }
    }
  }

  public void rollback() throws Exception {
    if (session != null) {
      Connection con = ((AQjmsSession) session).getDBConnection();
      if (con != null) {
        con.rollback();
      }
    }
  }

  /*
   * (non-Javadoc)
   * @see com.gip.xyna.utils.jms.JMSConnector#dequeue(java.lang.String)
   */
  public String dequeue(String queueName) throws JMSException {
    return dequeue(queueName, null, DEFAULT_TIMEOUT);
  }

  /*
   * (non-Javadoc)
   * @see com.gip.xyna.utils.jms.JMSConnector#dequeue(java.lang.String, java.lang.String)
   */
  public String dequeue(String queueName, String correlationID) throws JMSException {
    return dequeue(queueName, correlationID, DEFAULT_TIMEOUT);
  }

  /*
   * (non-Javadoc)
   * @see com.gip.xyna.utils.jms.JMSConnector#dequeue(java.lang.String, java.lang.String, long)
   */
  public String dequeue(String queueName, String correlationID, long timeout) throws JMSException {
    String[] msgCorrId = _dequeue(queueName, correlationID, timeout, null);
    return msgCorrId[0];
  }

  /*
   * (non-Javadoc)
   * @see com.gip.xyna.utils.jms.JMSConnector#dequeue(java.lang.String, java.lang.String, long)
   */
  public String[] _dequeue(String queueName, String correlationID, long timeout)
                  throws JMSException {
    return _dequeue(queueName, correlationID, timeout, null);
  }

  public String[] _dequeue(String queueName, String correlationID, long timeout,
                           String dequeueCondition) throws JMSException {
    Connection con = ((AQjmsSession) session).getDBConnection();

    if (null == con)
      throw new JMSException("PLSQLQueue nicht initialisiert, keine Connection");

    String plsql = "DECLARE\n" + "  text        clob;\n" + "  agent       sys.aq$_agent := sys.aq$_agent(' ', null, 0);\n" + "  message     sys.aq$_jms_text_message;\n" + "  dequeue_options    dbms_aq.dequeue_options_t;\n" + "  message_properties dbms_aq.message_properties_t;\n" + "  msgid               raw(16);\n" + "  correlation   varchar2(128);\n" + "BEGIN\n" + "  dequeue_options.navigation := dbms_aq.first_message;\n";

    // CorrelationId beruecksichtigen?
    if (null != correlationID && correlationID.length() > 0) {
      if (debugMode) {
        System.out.println("PLSQLQueue.readMessage: Setting correlation id: " + correlationID);
      }
      plsql += ("  dequeue_options.correlation := '" + correlationID + "';\n");
    }
    else if (dequeueCondition != null && dequeueCondition.length() > 0) {
      if (debugMode) {
        System.out
          .println("PLSQLQueue.readMessage: Setting dequeue condition: " + dequeueCondition);
      }
      plsql += "   dequeue_options.deq_condition := '" + dequeueCondition.replaceAll("'", "''") + "';\n";
    }

    // Ist ein Timeout angegeben? [Java ms, PL/SQL sec]
    if (timeout >= 0) {
      int tmout = (int) (timeout / 1000);
      if (debugMode) {
        System.out.println("PLSQLQueue.readMessage: Setting timeout: " + timeout);
      }
      plsql += ("  dequeue_options.wait := " + tmout + ";\n");
    }
    plsql += ("  dbms_aq.dequeue(queue_name => '" + queueName + "',\n" + "                  dequeue_options => dequeue_options,\n" + "                  message_properties => message_properties,\n" + "                  payload => message, msgid => msgid);\n" + "-- message.get_text(text);\n" + "IF message.text_vc IS NOT NULL THEN text := message.text_vc; ELSE text := message.text_lob; END IF;\n" + "correlation := message_properties.correlation;\n" + "? := text;\n" + "? := correlation;\n" + "END;");

    if (debugMode)
      System.out.println(plsql);
    CallableStatement stmt = null;
    try {
      stmt = con.prepareCall(plsql);
      stmt.registerOutParameter(1, Types.CLOB);
      stmt.registerOutParameter(2, Types.VARCHAR);
      stmt.execute();
    }
    catch (SQLException sx) {
      int err = sx.getErrorCode();
      if (debugMode)
        System.out.println("SQLException " + err);
      if (timeout > 0 && err == 25228) { // ORA-25228:
        // Zeitueberschreitung
        if (debugMode) {
          System.out.println("Timeout beim Lesen von Queue " + queueName);
        }
        if (stmt != null)
          try {
            stmt.close();
          }
          catch (SQLException e) {
            JMSException exp = new JMSException(e.getSQLState());
            exp.setLinkedException(e);
            throw exp;
          }
        return new String[] {null, null};
      }
      if (debugMode) {
        sx.printStackTrace();
      }
      return new String[] {null, null};
    }
    // System.out.println("Getting correlationId ...");
    // String corrId = stmt.getString(2);
    // if (DEBUG) {
    // System.out.println("PLSQLQueue.readMessage: CorrelationId: " + corrId);
    // }
    String msg = null;
    String corrId = null;
    try {
      msg = stmt.getString(1);
      corrId = stmt.getString(2);
      stmt.close();
    }
    catch (SQLException e) {
      JMSException exp = new JMSException(e.getSQLState());
      exp.setLinkedException(e);
      throw exp;
    }
    return new String[] {msg, corrId};
  }

  /*
   * (non-Javadoc)
   * @see com.gip.xyna.utils.jms.JMSConnector#enqueue(java.lang.String, java.lang.String, java.lang.String)
   */
  public void enqueue(String queueName, String msgText, String correlationID) throws JMSException {
    enqueue(queueName, msgText, correlationID, null);
  }

  /**
   * @param correlationId
   * @param consumer
   * @return
   * @throws Exception
   */
  public Map browseQueue(String queueName, String correlationId, String consumer)
                  throws JMSException {

    Connection con = ((AQjmsSession) session).getDBConnection();

    if (null == con)
      throw new JMSException("Queue nicht initialisiert, keine Connection");
    LinkedHashMap hm = new LinkedHashMap();
    String plsql = "DECLARE\n" + "  text        clob;\n" + "  typ         varchar2(32767);\n" + "  header      sys.aq$_jms_header;\n" + "  agent       sys.aq$_agent := sys.aq$_agent(' ', null, 0);\n" + "  message     sys.aq$_jms_text_message;\n" + "  dequeue_options    dbms_aq.dequeue_options_t;\n" + "  message_properties dbms_aq.message_properties_t;\n" + "  msgid               raw(16);\n" + "  correlation   varchar2(128);\n" + "BEGIN\n" + "  dequeue_options.wait := dbms_aq.no_wait;\n" + "  dequeue_options.navigation := dbms_aq.next_message;\n" + "  dequeue_options.dequeue_mode := dbms_aq.locked;\n";

    if (null != correlationId && correlationId.length() > 0) {
      // FIXME: System.out durch vernuenftiges Logging ersetzen
      if (debugMode)
        System.out.println("PLSQLQueue.readMessage: Setting correlation id: " + correlationId);
      plsql += ("  dequeue_options.correlation := '" + correlationId + "';\n");
    }

    // Recipient (consumer) angegeben?
    if (null != consumer && consumer.length() > 0) {
      plsql += ("  dequeue_options.consumer_name := '" + consumer + "';\n");
    }

    plsql += ("  dbms_aq.dequeue(queue_name => '" + queueName + "',\n" + "                  dequeue_options => dequeue_options,\n" + "                  message_properties => message_properties,\n" + "                  payload => message, msgid => msgid);\n" + "-- message.get_text(text);\n" + "IF message.text_vc IS NOT NULL THEN text := message.text_vc; ELSE text := message.text_lob; END IF;\n" + "correlation := message_properties.correlation;\n" + "header := message.header;\n" + "typ := header.get_type();\n" + "? := text;\n" + "? := correlation;\n" + "? := typ;\n" + "END;");

    if (debugMode) {
      System.out.println("PLSQL:\n" + plsql);
    }
    java.sql.CallableStatement stmtNext;
    java.sql.CallableStatement stmtFirst;
    try {
      stmtNext = con.prepareCall(plsql);
      stmtNext.registerOutParameter(1, java.sql.Types.CLOB);
      stmtNext.registerOutParameter(2, java.sql.Types.VARCHAR);
      stmtNext.registerOutParameter(3, java.sql.Types.VARCHAR);

      // Das erste Mal mit first_message
      String plsqlFirst = plsql.replaceFirst("dbms_aq.next_message", "dbms_aq.first_message");
      stmtFirst = con.prepareCall(plsqlFirst);
      stmtFirst.registerOutParameter(1, java.sql.Types.CLOB);
      stmtFirst.registerOutParameter(2, java.sql.Types.VARCHAR);
      stmtFirst.registerOutParameter(3, java.sql.Types.VARCHAR);
    }
    catch (SQLException e) {
      JMSException exp = new JMSException(e.getSQLState());
      exp.setLinkedException(e);
      throw exp;
    }

    String msg, corrId;
    java.sql.CallableStatement stmt;

    for (int i = 0; true; i++) {

      try {
        // System.out.println("Executing statement ...");
        stmt = (i == 0 ? stmtFirst : stmtNext);
        stmt.execute();
        msg = stmt.getString(1);
        corrId = stmt.getString(2);
      }
      catch (SQLException sx) {
        if (sx.getErrorCode() == 25228) { // ORA-25228:
          // Zeitueberschreitung
          // FIXME: System.out durch vernuenftiges Logging ersetzen
          if (debugMode) {
            System.out.println("Fertig, keine weiteren Messages in Queue " + queueName);
          }
          break;
        }
        JMSException exp = new JMSException(sx.getSQLState());
        exp.setLinkedException(sx);
        throw exp;
      }
      if (null == msg && null == corrId)
        break;
      // System.out.println("Message: " + msg);
      // System.out.println();
      // String jmstype = stmt.getString(3);
      // Es koennten mehrere Messages mit der gleichen correlationId
      // in der Queue sein. Ist das der Fall wird an die folgenden
      // corrId-Key in der Map der Suffix <n>, n beginnend mit 1,
      // angefeugt.
      StringBuffer key = new StringBuffer(corrId);
      String value;
      for (int k = 1; true; k++) {
        value = (String) hm.get(key.toString());
        if (null == value)
          break;
        key.append("<");
        key.append(k);
        key.append(">");
      }
      hm.put(key, msg);
    }
    try {
      stmtFirst.close();
      stmtNext.close();
    }
    catch (SQLException e) {
      JMSException exp = new JMSException(e.getSQLState());
      exp.setLinkedException(e);
      throw exp;
    }

    return hm;
  }

  /*
   * (non-Javadoc)
   * @see com.gip.xyna.utils.jms.JMSConnector#enqueue(java.lang.String, java.lang.String, java.lang.String,
   * java.util.Map)
   */
  public void enqueue(String queueName, String msgText, String correlationID, Map properties)
                  throws JMSException {
    Connection con = ((AQjmsSession) session).getDBConnection();

    if (null == con)
      throw new JMSException("PLSQLQueue nicht initialisiert, keine Connection");

    String plsql = getPLSQLEnqueueString(queueName, correlationID, properties);
    CallableStatement stmt = null;
    try {
      stmt = con.prepareCall(plsql);
      oracle.sql.CLOB clob = oracle.sql.CLOB.createTemporary(con, true,
        oracle.sql.CLOB.DURATION_SESSION);
      clob.putString(1, msgText);
      stmt.setClob(1, clob);
      stmt.execute();
      con.commit();
      clob.freeTemporary();
    }
    catch (SQLException e) {
      JMSException exp = new JMSException(e.getSQLState());
      exp.setLinkedException(e);
      throw exp;
    }
    finally {
      try {
        stmt.close();
      }
      catch (SQLException e) {
        JMSException exp = new JMSException(e.getSQLState());
        exp.setLinkedException(e);
        throw exp;
      }
    }
  }

  private String getPLSQLEnqueueString(String queueName, String correlationID, Map properties) {
    String plsql = "DECLARE\n" + "agent              sys.aq$_agent   := sys.aq$_agent(' ', null, 0);\n" + 
      "message            sys.aq$_jms_text_message;\n" + "enqueue_options    dbms_aq.enqueue_options_t;\n" + "message_properties dbms_aq.message_properties_t;\n" + "msgid               raw(16);\n" + "BEGIN\n" + "message := sys.aq$_jms_text_message.construct;\n" + "message_properties.correlation := '" + correlationID + "';\n";

    // Weitere MessageProperties, falls vorhanden
    if (null != properties && properties.size() > 0) {
      // Hmm, AQ unterstuetzt keine beliebigen Properties
      if (properties.containsKey("delay") && (properties.get("delay") instanceof Integer)) {
        if (debugMode)
          System.out
            .println("PLSQLQueue: Setze Delay fuer Message " + correlationID + ": " + ((Integer) properties
              .get("delay")).intValue());
        plsql += ("message_properties.delay := " + ((Integer) properties.get("delay")).intValue() + ";\n");
      }
      if (properties.containsKey("expiration") && (properties.get("expiration") instanceof Integer)) {
        plsql += ("message_properties.expiration := " + ((Integer) properties.get("expiration"))
          .intValue() + ";\n");
      }
    }

    plsql += ("message.set_text(?);\n" + "dbms_aq.enqueue(queue_name => '" + queueName + "',\n" + "                enqueue_options => enqueue_options,\n" + "                message_properties => message_properties,\n" + "                payload => message,\n" + "                msgid => msgid);\n" + "END;");

    if (debugMode)
      System.out.println(plsql);
    return plsql;
  }

}

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

import java.sql.CallableStatement;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.db.DBConnectionData;
import com.gip.xyna.utils.db.OutputParam;
import com.gip.xyna.utils.db.OutputParamFactory;
import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.SQLUtilsLogger;
import com.gip.xyna.xact.trigger.oracleaq.ResponseMessage;
import com.gip.xyna.xact.trigger.oracleaq.shared.QueueData;


public class TestOracleAQTrigger {

  public static class Constant {
    public static final String JDBC_URL = "";
  }

  private static Logger _logger = Logger.getLogger(TestOracleAQTrigger.class);

  public static class SQLLogger implements SQLUtilsLogger {
    private Logger _logger;
    public SQLLogger(Logger logger) {
      _logger = logger;
    }
    public void logException(Exception e) {
      _logger.error("", e);
    }
    public void logSQL(String sql) {
      _logger.info("SQL= " + sql);
    }
  }

  private static SQLUtils getSQLUtils() throws Exception {
    String user = "";
    String password = "";
    String url = Constant.JDBC_URL;

    DBConnectionData dbd = DBConnectionData.newDBConnectionData().user(user)
        .password(password).url(url).connectTimeoutInSeconds(15).socketTimeoutInSeconds(15).build();
    SQLUtils sqlUtils = dbd.createSQLUtils(new SQLLogger(_logger));
    if (sqlUtils == null) {
      _logger.error("Creating SQLUtils has failed.");
      throw new Exception("Unable to create SQLUtils.");
    }
    return sqlUtils;
  }


  public static void test_enqueue() throws Exception {
    sendToQueue("222", "Hallo Test abcxyz; test message content.");
  }

  public static void test_enqueue2() throws Exception {
   // sendToQueueNoPackage("QWERT", "This is Test 2. zzzzzz; test message content. OKOK");
  }

  public static void test_dequeue() throws Exception {
    dequeue("corrId LIKE '%'");
  }


  public static void test_dequeue2() throws Exception {
    //dequeueWithoutPackage("corrId LIKE '%'");
    dequeueWithoutPackage(null);
  }


  private static void sendToQueue(String corrID, String text) throws Exception {
    SQLUtils utils = null;
    String queuename = "TEST_Q";
    try {
      utils = getSQLUtils();
      Parameter param = new Parameter();
      param.addParameter(queuename);
      param.addParameter(corrID);
      param.addParameter(text);

      utils.executeCall("{call XYNA_ORACLEAQ_TRIGGER.enq(?,?,?)}", param);
      Exception e = utils.getLastException();
      if (e != null) {
        _logger.error("", e);
      }
    }
    finally {
      try {
        utils.closeConnection();
      }
      catch (Exception e) {
        _logger.debug("error closing connection", e);
      }
    }
  }


  protected static void dequeue(String condition) {
    SQLUtils sqlUtils = null;
      try {
        sqlUtils = getSQLUtils();
        OutputParam<String> output = OutputParamFactory.createString();
        OutputParam<String> corrId = OutputParamFactory.createString();

        Parameter param = new Parameter();
        param.addParameter("TEST_Q");
        param.addParameter(5);
        param.addParameter(corrId);
        param.addParameter(output);
        param.addParameter(condition);

        boolean success = false;
        _logger.debug("Starting listening to queue...");
        success = sqlUtils.executeCall("{call XYNA_ORACLEAQ_TRIGGER.dequeue(?,?,?,?,?)}", param);
        Exception e = sqlUtils.getLastException();
        if (e != null) {
          _logger.error("", e);
        }
        else if (success) {
          _logger.info("dequeued message with corrId = " + corrId.get() + ", text = " + output.get());
        }
      }
      catch (Exception e) {
        _logger.error("Exception while dequeueing.", e);
      }
      finally {
        try {
          sqlUtils.closeConnection();
        }
        catch (Exception e) {
          _logger.debug("error closing connection", e);
        }
      }
  }


  private static void dequeueWithoutPackage(String condition) {
    SQLUtils sqlUtils = null;
    try {
      sqlUtils = getSQLUtils();
      /*
      OutputParam<String> output = OutputParamFactory.createString();
      OutputParam<String> corrId = OutputParamFactory.createString();

      Parameter param = new Parameter();
      param.addParameter(5);
      param.addParameter(condition);
      param.addParameter("TEST_Q");
      param.addParameter(corrId);
      param.addParameter(output);
      */
      boolean success = false;
      _logger.debug("Starting listening to queue...");
      String sql = buildProcedure();

      //success = sqlUtils.executeCall("{call " + sql + "}", param);
      //success = sqlUtils.query(sql, param);
      CallableStatement cs = null;
      try {
        cs = sqlUtils.prepareCall(sql);
        //sqlUtils.addParameter(cs, param);
        //sqlUtils.addParameter(cs, param);
        cs.setString(1, "5");
        cs.setString(2, condition);
        cs.setString(3, "RESP_Q");
        cs.registerOutParameter(4, java.sql.Types.VARCHAR);
        cs.registerOutParameter(5, java.sql.Types.INTEGER);
        cs.registerOutParameter(6, java.sql.Types.VARCHAR);
        sqlUtils.excuteUpdate(cs);

        System.err.println( cs.getString(4) +"  "+ cs.getString(5)+"  "+ cs.getString(6) );
      }
      finally {
        sqlUtils.finallyClose(null,cs);
      }
      //success = sqlUtils.executeDDL(sql, param);
      Exception e = sqlUtils.getLastException();
      if (e != null) {
        _logger.error("", e);
      }
      else if (success) {
        //_logger.info("dequeued message with corrId = " + corrId.get() + ", text = " + output.get());
      }
    }
    catch (Exception e) {
      _logger.error("Exception while dequeueing.", e);
    }
    finally {
      try {
        sqlUtils.closeConnection();
      }
      catch (Exception e) {
        _logger.debug("error closing connection", e);
      }
    }

  }


  private static String buildProcedure() {
    StringBuilder builder = new StringBuilder();
    builder.append("DECLARE ").append("\n");
    /*
    builder.append("  qname VARCHAR2;").append("\n");
    builder.append("  wait INTEGER;").append("\n");
    builder.append("  correlationId VARCHAR2;").append("\n");
    builder.append("  msg CLOB;").append("\n");
    builder.append("  deq_condition VARCHAR2; ").append("\n");
    */
    builder.append("  msg CLOB;").append("\n");
    builder.append("  opts DBMS_AQ.DEQUEUE_OPTIONS_T; ").append("\n");
    builder.append("  props DBMS_AQ.MESSAGE_PROPERTIES_T; ").append("\n");
    builder.append("  my_msg SYS.AQ$_JMS_TEXT_MESSAGE; ").append("\n");
    builder.append("  msg_Id RAW(16);").append("\n");
    builder.append("BEGIN ").append("\n");
    builder.append("    opts.wait := ?; ").append("\n");
    builder.append("    opts.navigation:= dbms_aq.first_message; ").append("\n");
    builder.append("    opts.deq_condition := ?; ").append("\n");
    builder.append("    dbms_aq.DEQUEUE (queue_name => ?, ").append("\n");
    builder.append("        dequeue_options => opts,").append("\n");
    builder.append("        message_properties => props, ").append("\n");
    builder.append("        payload => my_msg,").append("\n");
    builder.append("        msgid => msg_id);").append("\n");
    builder.append("    ? := props.correlation; ").append("\n");
    builder.append("    ? := props.priority; ").append("\n");
    builder.append("    IF MY_Msg.text_vc IS NOT NULL THEN ").append("\n");
    builder.append("      msg := MY_Msg.text_vc;").append("\n");
    builder.append("    ELSE").append("\n");
    builder.append("      msg := MY_Msg.text_lob;").append("\n");
    builder.append("    END IF; ").append("\n");
    builder.append("    ? := msg;").append("\n");
    builder.append("END; ").append("\n");

    return builder.toString();
  }


  public static void main(String[] args) {
    try {
      test_enqueue2();
      //test_dequeue2();
    }
    catch (Exception e) {
      _logger.error("", e);
    }
  }

}

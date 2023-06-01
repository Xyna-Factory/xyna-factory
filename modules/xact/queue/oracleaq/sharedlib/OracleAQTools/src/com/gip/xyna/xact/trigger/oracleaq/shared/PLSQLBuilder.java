/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

package com.gip.xyna.xact.trigger.oracleaq.shared;

import java.text.SimpleDateFormat;

import com.gip.xyna.utils.collections.Pair;



/**
 * builds PL/SQL strings containing anonymous blocks, which provide functionality 
 * for dequeueing and enqueueing oracle AQ messages
 */
public class PLSQLBuilder {

  public static final String MSG_ID = "msgId";
  public static final String ENQUEUE_TIME = "enqueueTime";
  public static final SimpleDateFormat ENQUEUE_TIME_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
  public static final String ENQUEUE_TIME_FORMAT_PLSQL = "YYYY.MM.DD HH24:MI:SS";
  
  private static class PLSQLCode {

    StringBuilder sb = new StringBuilder(1000);
    
    public PLSQLCode line(String line) {
      sb.append(line).append("\n");
      return this;
    }
    public PLSQLCode line() {
      sb.append("\n");
      return this;
    }
    public PLSQLCode add(String add) {
      sb.append(add);
      return this;
    }
    
    @Override
    public String toString() {
      return sb.toString();
    }
  }

  
  public static String buildCreateQueue(boolean multiConsumer) {
    boolean both = true;
    PLSQLCode createQueue = new PLSQLCode();
    if( both ) {
      createQueue.line("DECLARE");
      createQueue.line("  queueName VARCHAR2(100) := ?;");
      createQueue.line("  queueTable VARCHAR2(100) := ?;");
    }
    if( multiConsumer ) {
      createQueue.line("  subscriber sys.aq$_agent := sys.aq$_agent(?,NULL,NULL);");
    }
    if( both ) {
      createQueue.line("BEGIN");
      createQueue.line("  dbms_aqadm.create_queue_table(");
      createQueue.line("     queue_table => queueTable,");
    }
    if( multiConsumer ) {
      createQueue.line("     multiple_consumers => true,");
    } else {
      createQueue.line("     multiple_consumers => false,");
    }
    if( both ) {
      createQueue.line("     queue_payload_type => 'SYS.AQ$_JMS_TEXT_MESSAGE'");
      createQueue.line("  );");
      createQueue.line("  dbms_aqadm.create_queue(");
      createQueue.line("     queue_name => queueName,");
      createQueue.line("     queue_table => queueTable");
      createQueue.line("  );");
      createQueue.line("  dbms_aqadm.start_queue(");
      createQueue.line("     queue_name => queueName");
      createQueue.line("  );");
    }
    if( multiConsumer ) {
      createQueue.line("  dbms_aqadm.add_subscriber(");
      createQueue.line("     queue_name => queueName,");
      createQueue.line("     subscriber => subscriber");
      createQueue.line("  );");
    }
    if( both ) {
      createQueue.line("END;");
    }
    return createQueue.toString();
  }
  
  public static String buildDropQueue(boolean multiConsumer) {
    boolean both = true;
    PLSQLCode dropQueue = new PLSQLCode();
    if( both ) {
      dropQueue.line("DECLARE");
      dropQueue.line("  queueName VARCHAR2(100) := ?;");
      dropQueue.line("  queueTable VARCHAR2(100) := ?;");
    }
    if( multiConsumer ) {
      dropQueue.line("  subscriber sys.aq$_agent := sys.aq$_agent(?,NULL,NULL);");
    }
    if( both ) {
      dropQueue.line("BEGIN");
    }
    if( multiConsumer ) {
      dropQueue.line("  dbms_aqadm.remove_subscriber(");
      dropQueue.line("     queue_name => queueName,");
      dropQueue.line("     subscriber => subscriber");
      dropQueue.line("  );");
    }
    if( both ) {
      dropQueue.line("  dbms_aqadm.stop_queue(");
      dropQueue.line("     queue_name => queueName");
      dropQueue.line("  );");
      dropQueue.line("  dbms_aqadm.drop_queue_table(");
      dropQueue.line("     queue_table => queueTable,");
      dropQueue.line("     force => TRUE");
      dropQueue.line("  );");
      dropQueue.line("END;");
    }
    return dropQueue.toString();
  }
    public static String buildDequeueBlock(DequeueOptions dequeueOptions) {
    PLSQLCode dequeue = new PLSQLCode()
    .line("DECLARE")
    .line("  opts DBMS_AQ.DEQUEUE_OPTIONS_T;")
    .line("  props DBMS_AQ.MESSAGE_PROPERTIES_T;")
    .line("  jms_msg SYS.AQ$_JMS_TEXT_MESSAGE;")
    .line("  msg_id RAW(16);")
    .line("BEGIN")
    .line("  opts.wait := ?;")
    .line("  opts.navigation:= dbms_aq.first_message;")
    .line("  opts.deq_condition := ?;")
    .line("  dbms_aq.DEQUEUE (queue_name => ?, ")
    .line("                   dequeue_options => opts,")
    .line("                   message_properties => props,")
    .line("                   payload => jms_msg,")
    .line("                   msgid => msg_id")
    .line("                  );")
    .line("  ? := props.correlation;")
    .line("  ? := props.priority;")
    .line("  jms_msg.get_text( ? );");
    for( String param : dequeueOptions.getAdditional() ) {
      if( MSG_ID.equals(param) ) {
        dequeue.add("    ").add(param).add("_out := rawToHex(msg_id);").line();
      } else if( ENQUEUE_TIME.equals(param) ) {
        dequeue.add("    ").add(param).add("_out := to_date( props.enqueue_time, '").add(ENQUEUE_TIME_FORMAT_PLSQL).line("' );");
      } else {
        dequeue.add("    ").add(param).add("_out := jms_msg.get_string_property('").add(param).add("');").line();
      }
    }
    dequeue.line("END;");
    return dequeue.toString();
  }

  public static String buildEnqueueBlock(EnqueueOptions enqueueOptions) {
    PLSQLCode enqueue = new PLSQLCode()
    .line("DECLARE ")
    .line("  opts DBMS_AQ.ENQUEUE_OPTIONS_T;")
    .line("  props DBMS_AQ.MESSAGE_PROPERTIES_T;")
    .line("  jms_msg SYS.AQ$_JMS_TEXT_MESSAGE;")
    .line("  msg_id RAW(16);")
    .line("  qname VARCHAR2(200);")
    .line("BEGIN")
    .line("  qname := ?;")
    .line("  jms_msg := sys.aq$_jms_text_message.construct;")
    .line("  jms_msg.set_text(?);")
    .line("  props.correlation := ?;")
    .line("  props.priority := ?;")
    .line("  props.delay := ?;")
    .line("  props.expiration := ?;");
    for( String key : enqueueOptions.getAdditional() ) {
      enqueue.add("  jms_msg.set_string_property('").add(key).line("', ?);");
    }
    enqueue.line("  dbms_aq.enqueue (queue_name => qname,")
    .line("                   enqueue_options => opts,")
    .line("                   message_properties => props,")
    .line("                   payload => jms_msg,")
    .line("                   msgid => msg_id);")
    .line("END;");
    return enqueue.toString();
  }
  
  public static Pair<String, String> buildCreatePackage(String packageName,
                                                        DequeueOptions dequeueOptions,
                                                        EnqueueOptions enqueueOptions ) {
    StringBuilder dequeueParam = new StringBuilder();
    dequeueParam.append("qname IN VARCHAR2, wait in INTEGER, correlationId OUT VARCHAR2, ").
      append("msg OUT CLOB, deq_condition IN VARCHAR2, priority OUT INTEGER");
    for( String add : dequeueOptions.getAdditional() ) {
      dequeueParam.append(", ").append(add).append("_out OUT VARCHAR2");
    }
    StringBuilder enqueueParam = new StringBuilder();
    enqueueParam.append("qname IN VARCHAR2, correlationId IN VARCHAR2, ").
      append("msg IN CLOB, priority IN INTEGER");
    for( String add : enqueueOptions.getAdditional() ) {
      enqueueParam.append(", ").append(add).append("_in IN VARCHAR2");
    }
    
    PLSQLCode header = new PLSQLCode()
    .line("CREATE OR REPLACE PACKAGE "+packageName)
    .line("  AS")
    .line("  PROCEDURE dequeue("+dequeueParam.toString()+"); ")
    .line("  PROCEDURE enqueue("+enqueueParam.toString()+"); ")
    .line("END "+packageName +";");
    
    PLSQLCode body = new PLSQLCode()
    .line("CREATE OR REPLACE PACKAGE BODY "+packageName)
    .line("  AS")
    .add("  PROCEDURE dequeue(").add(dequeueParam.toString()).line(")")
    .line("  IS")
    .line("    opts DBMS_AQ.DEQUEUE_OPTIONS_T;")
    .line("    props DBMS_AQ.MESSAGE_PROPERTIES_T;")
    .line("    jms_msg SYS.AQ$_JMS_TEXT_MESSAGE;")
    .line("    msg_id RAW(16);")
    .line("  BEGIN")
    .line("    opts.wait := wait;")
    .line("    opts.navigation:= dbms_aq.first_message;");
    if( dequeueOptions.getConsumerName() != null ) {
      body.add("    opts.consumer_name := '").add(dequeueOptions.getConsumerName()).add("';").line();
    }
    body.line("    opts.deq_condition := deq_condition;")
    .line("    dbms_aq.DEQUEUE (queue_name => qname,")
    .line("        dequeue_options => opts,")
    .line("        message_properties => props,")
    .line("        payload => jms_msg,")
    .line("        msgid => msg_id);")
    .line("    correlationId := props.correlation;")
    .line("    priority := props.priority; ")
    .line("    jms_msg.get_text( msg );");
    for( String param : dequeueOptions.getAdditional() ) {
      if( MSG_ID.equals(param) ) {
        body.add("    ").add(param).add("_out := rawToHex(msg_id);").line();
      } else if( ENQUEUE_TIME.equals(param) ) {
        body.add("    ").add(param).add("_out := to_char( props.enqueue_time, 'YYYY.MM.DD HH24:MI:SS' );").line();
      } else {
        body.add("    ").add(param).add("_out := jms_msg.get_string_property('").add(param).add("');").line();
      }
    }
    body.line("  END dequeue;");
    
    body.line("")
    .add("  PROCEDURE enqueue(").add(enqueueParam.toString()).line(")")
    .line(" IS")
    .line("    opts DBMS_AQ.ENQUEUE_OPTIONS_T;")
    .line("    props DBMS_AQ.MESSAGE_PROPERTIES_T;")
    .line("    jms_msg SYS.AQ$_JMS_TEXT_MESSAGE;")
    .line("    msg_id RAW(16);")
    .line("  BEGIN")
    .line("    jms_msg := sys.aq$_jms_text_message.construct;")
    .line("    jms_msg.set_text(msg);")
    .line("    props.correlation := correlationId;");
    for( String key : enqueueOptions.getAdditional() ) {
      body.add("  jms_msg.set_string_property('").add(key).add("', ").add(key).line("_in);");
    }
    body
    .line("    dbms_aq.enqueue (queue_name => qname,")
    .line("                     enqueue_options => opts,")
    .line("                     message_properties => props,")
    .line("                     payload => jms_msg,")
    .line("                     msgid => msg_id);")
    .line("  END enqueue;")
    .line("")
    .line("END "+packageName+";");
    return Pair.of(header.toString(), body.toString());
  }
  
  public static String buildDropPackage(String packageName) {
    return "DROP PACKAGE "+packageName;
  }
  
  public static String buildDequeueWithPackage(String packageName, DequeueOptions dequeueOptions) {
    StringBuilder sb = new StringBuilder();
    sb.append("{call ").append(packageName).append(".dequeue(");
    appendPlaceHolder(sb, 6 + dequeueOptions.getAdditional().size() );
    sb.append(")}");
    return sb.toString();
  }
  
  public static String buildEnqueueWithPackage(String packageName, EnqueueOptions enqueueOptions) {
    StringBuilder sb = new StringBuilder();
    sb.append("{call ").append(packageName).append(".enqueue(");
    appendPlaceHolder(sb, 4 + enqueueOptions.getAdditional().size() );
    sb.append(")}");
    return sb.toString();
  }
  
  public static String buildDequeueBlock() {
    return buildDequeueBlock( DequeueOptions.newDequeueOptions().build() ); 
  }
  
  public static String buildEnqueueBlock() {
    return buildEnqueueBlock( EnqueueOptions.newEnqueueOptions().build() ); 
  }

  private static void appendPlaceHolder(StringBuilder sb, int number) {
    if( number == 0 ) {
      return; //nichts zu tun
    } else if( number == 1 ) {
      sb.append("?"); 
    } else {
      sb.append("?"); 
      for( int i=1; i<number; ++i ) {
        sb.append(",?");
      }
    }
  }
  
}

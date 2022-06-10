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
package com.gip.xyna.persistence.xsor.helper;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.XynaSQLUtilsLogger;
import com.gip.xyna.xnwh.persistence.XynaSQLUtilsLogger.LoggingMessageGenerator;
import com.gip.xyna.xsor.TransactionContext;
import com.gip.xyna.xsor.indices.search.SearchValue;
import com.gip.xyna.xsor.protocol.XSORPayload;


public class ClusterPLLogger {
  
  private XynaSQLUtilsLogger traceLogger;
  private XynaSQLUtilsLogger debugLogger;
  private XynaSQLUtilsLogger infoLogger;
  
  private static final Logger logger = CentralFactoryLogging.getLogger(ClusterPLLogger.class);
  
  public ClusterPLLogger(boolean useDynamicLogging) {
    traceLogger = new XynaSQLUtilsLogger(useDynamicLogging ? null : logger, ODSImpl.CONNECTIONCLASSNAME, Level.TRACE) {
      public void logException(Exception arg0) {
        throw new RuntimeException();
      }
    };
    debugLogger = new XynaSQLUtilsLogger(useDynamicLogging ? null : logger, ODSImpl.CONNECTIONCLASSNAME, Level.DEBUG) {
      public void logException(Exception arg0) {
        throw new RuntimeException();
      }
    };
    infoLogger = new XynaSQLUtilsLogger(useDynamicLogging ? null : logger, ODSImpl.CONNECTIONCLASSNAME, Level.INFO) {
      public void logException(Exception arg0) {
        throw new RuntimeException();
      }
    };
  }

  
  public void debug(String message, TransactionContext transaction) {
    debugLogger.logSQL(buildSqlMessage(message, transaction));
  }
  
  
  public void debug(String message, Parameter params, TransactionContext transaction) {
    debugLogger.logSQL(buildSqlMessage(message, params, transaction));
  }
   
  public void trace(String message, TransactionContext transaction) {
    traceLogger.logSQL(buildSqlMessage(message, transaction));
  }
    
  public void info(String message, TransactionContext transaction) {
    infoLogger.logSQL(buildSqlMessage(message, transaction));
  }
  
  
  public void buildAndLogDeletion(XSORPayload payload, TransactionContext transaction) {
    StringBuilder messageBuilder = new StringBuilder();                        // TODO name of PK?
    messageBuilder.append("DELETE FROM ").append(payload.getTableName()).append(" WHERE {PrimaryKey}=").append(getPkString(payload));
    debug(messageBuilder.toString(), transaction);
  }
  
  
  private String getPkString(XSORPayload payload) {
    Object pk = payload.getPrimaryKey();
    String pkAsString;
    if (pk.getClass().isArray() && pk.getClass().getComponentType() == byte.class) {
      pkAsString = Arrays.toString((byte[])pk);
      //TODO andere arrays unterstützen
    } else {
      pkAsString = String.valueOf(pk);
    }
    return pkAsString;
  }


  public void buildAndLogPersist(XSORPayload payload, TransactionContext transaction) {
    StringBuilder messageBuilder = new StringBuilder();                         // TODO name of PK?
    messageBuilder.append("UPSERT INTO ").append(payload.getTableName()).append(" WHERE {PrimaryKey}=").append(getPkString(payload));
    debug(messageBuilder.toString(), transaction);
  }
  
  
  public void logPrepreparedParameter(final Map<Integer, SearchValue> values) {
    traceLogger.logSQL(new LoggingMessageGenerator() {
      public String generateLogMessage() {
        StringBuilder messageBuilder = new StringBuilder("Converted fixed parameter [ ");
        Iterator<Entry<Integer, SearchValue>> iter = values.entrySet().iterator();
        while (iter.hasNext()) {
          Entry<Integer, SearchValue> entry = iter.next();
          messageBuilder.append(entry.getKey()).append(": ").append(entry.getValue().getValue().toString());
          if (iter.hasNext()) {
            messageBuilder.append(", ");
          }
        }
        messageBuilder.append("]");
        return messageBuilder.toString();
      }
    }); 
  }
  
  
  public void buildAndLogAllColumns(final Storable storable) {
    traceLogger.logSQL(new LoggingMessageGenerator() {
      public String generateLogMessage() {
        Column[] columns = Storable.getColumns(storable.getClass());
        StringBuilder messageBuilder = new StringBuilder("COLUMNS FROM ").append(storable.getTableName()).append(" (");
        StringBuilder paramBuilder = new StringBuilder(" VALUES(");
        for (int i=0; i < columns.length; i++) {
          messageBuilder.append(columns[i].name());
          paramBuilder.append(String.valueOf(storable.getValueByColName(columns[i])));
          if (i+1 < columns.length) {
            messageBuilder.append(", ");
            paramBuilder.append(", ");
          }
        }
        paramBuilder.append(")");
        messageBuilder.append(")").append(paramBuilder.toString());
        return messageBuilder.toString();
      }
    }); 
  }
  
  
  private String buildSqlMessage(String message, TransactionContext transaction) {
    StringBuilder messageBuilder = new StringBuilder(message);
    if (transaction != null) {
      messageBuilder.append(" {").append(transaction.getTransactionId()).append("}");
    }
    return messageBuilder.toString();
  }
  
  
  private String buildSqlMessage(String message, Parameter params, TransactionContext transaction) {
    StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append(message).append(" ").append(params);
    return buildSqlMessage(messageBuilder.toString(), transaction);
  }


}

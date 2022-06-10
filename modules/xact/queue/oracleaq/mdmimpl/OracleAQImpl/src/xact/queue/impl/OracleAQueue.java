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
package xact.queue.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import xact.queue.CorrelationId;
import xact.queue.DequeueFailedException;
import xact.queue.DequeueOptions;
import xact.queue.EnqueueFailedException;
import xact.queue.EnqueueOptions;
import xact.queue.MessageProperties;
import xact.queue.NoConnectionException;
import xact.queue.NoSuchMessageException;
import xact.queue.Property;
import xact.queue.QueueMessage;
import xact.queue.admin.DBConnectionData;
import xact.queue.admin.OracleAQConfig;
import xact.queue.impl.PoolCreation.ConnectionCreationException;
import xact.queue.impl.QueueInstanceOperationImpl.MessageIdentification;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.xact.trigger.oracleaq.shared.DequeueOptions.DequeueOptionsBuilder;
import com.gip.xyna.xact.trigger.oracleaq.shared.EnqueueOptions.EnqueueOptionsBuilder;
import com.gip.xyna.xact.trigger.oracleaq.shared.OracleAQMessage.Builder;
import com.gip.xyna.xact.trigger.oracleaq.shared.OracleAQMessage;
import com.gip.xyna.xact.trigger.oracleaq.shared.PLSQLExecUtils;
import com.gip.xyna.xprc.xsched.timeconstraint.AbsRelTime;
import com.gip.xyna.xprc.xsched.xynaobjects.Date;


/**
 *
 */
public class OracleAQueue {

  private static Logger logger = CentralFactoryLogging.getLogger(OracleAQueue.class);
  
  private EnqueueOptionsBuilder enqueueOptionsBuilder;
  private DequeueOptionsBuilder dequeueOptionsBuilder;
  private ConnectionPool pool;
  private String connectionTarget;

  
  public OracleAQueue(OracleAQConfig config) {
    enqueueOptionsBuilder = new EnqueueOptionsBuilder().queueName(config.getName_externalQueue());
    dequeueOptionsBuilder = new DequeueOptionsBuilder().queueName(config.getName_externalQueue());
    
    DBConnectionData dcd = config.getDBConnectionData();
    connectionTarget = dcd.getUsername()+"@"+dcd.getJdbc_URL();
    
    Pair<ConnectionPool,String> pair = PoolCreation.getOrCreatePool(config.getName_unique().getName(), config);
    pool = pair.getFirst();
    connectionTarget = pair.getSecond();
  }

  public void close() {
    //derzeit nichts zu tun, da Connections immer sofort zurückgegeben werden
  }

  public QueueMessage dequeue(DequeueOptions dequeueOptions) throws DequeueFailedException, NoSuchMessageException, NoConnectionException {
    dequeueOptionsBuilder.
    consumerName(dequeueOptions.getConsumerName());
    
    MessageIdentification identification = MessageIdentification.valueOf(dequeueOptions.getIdentification());
    switch( identification ) {
      case ByCorrelationId:
        dequeueOptionsBuilder.dequeueCondition_CorrId(identification.getIdentification(dequeueOptions));
        break;
      case ByMessageId:
        throw new UnsupportedOperationException("Message identification ByMessageId not supported yet" );
        //TODO dequeueOptionsBuilder.dequeueCondition_MsgId(identification.getIdentification(dequeueOptions));
        //break;
      case Next:
        throw new UnsupportedOperationException("Message identification Next not supported yet" );
        //TODO dequeueOptionsBuilder.dequeueCondition_Next();
        //break;
      default:
        throw new UnsupportedOperationException("Unknown message identification "+dequeueOptions.getIdentification() );
    }
    
    boolean hasProperties = false;
    List<? extends Property> properties = dequeueOptions.getProperty();
    if( properties != null && properties.size() != 0 ) {
      List<String> adds = new ArrayList<String>();
      for( Property p : properties ) {
        adds.add(p.getKey());
      }
      dequeueOptionsBuilder.additional(adds);
      hasProperties = true;
    }
    
    if( dequeueOptions.getWait() != null ) {
      dequeueOptionsBuilder.timeout( toRelativeSeconds( dequeueOptions.getWait() ) );
    }
    
    OracleAQMessage aqMessage = null;
   
    SQLUtils sqlUtils = getConnection();
    boolean doRollback = true;
    try {
      aqMessage = PLSQLExecUtils.dequeueDirect(sqlUtils, dequeueOptionsBuilder.build(), true );
      
      if( sqlUtils.getLastException() != null ) {
        throw new DequeueFailedException( sqlUtils.getLastException().getMessage(), sqlUtils.getLastException());
      }
      if( aqMessage == null ) {
        throw new NoSuchMessageException(identification.toMessageIdentificationString(dequeueOptions) );
      }
      
      sqlUtils.commit();
      doRollback = false;
    } finally {
      finallyRollbackAndClose(sqlUtils, doRollback);
    }
    
    MessageProperties messageProperties = new MessageProperties();
    //FIXME füllen
    messageProperties.setCorrelationId(new CorrelationId(aqMessage.getCorrelationID()));
    messageProperties.setPriority(aqMessage.getPriority());
    if( hasProperties ) {
      List<Property> props = new ArrayList<Property>();
      if( aqMessage.getProperties() != null ) {
        for( Map.Entry<String,Object> entry : aqMessage.getProperties().entrySet() ) {
          String val = entry.getValue() == null ? null : String.valueOf(entry.getValue());
          props.add( new Property( entry.getKey(), val ) );
        }
      }
      messageProperties.setProperties(props);
    }
     //TODO 
    //aqMessage.getEnqueueTime();
    //aqMessage.getMsgId();
    
    QueueMessage qm = new QueueMessage();
    qm.setMessage(aqMessage.getText());
    qm.setMessageProperties(messageProperties);
    return qm;
  }

  public void enqueue(CorrelationId correlationId, QueueMessage queueMessage, EnqueueOptions enqueueOptions) throws NoConnectionException, EnqueueFailedException {
    
    
    enqueueOptionsBuilder.defaultPriority( getPriority(queueMessage) );
    //enqueueOptionsBuilder.delay(enqueueOptions.getDelay()). //nur für Oracle, nicht bei anderen Queue-Implementierungen. Daher aktuell nicht unterstützt
    
    if( enqueueOptions.getExpiration() != null ) {
      enqueueOptionsBuilder.expiration( toRelativeSeconds( enqueueOptions.getExpiration() ) );
    }
    
    Builder messageBuilder = OracleAQMessage.newOracleAQMessage().
        corrID(correlationId.getCorrelationId()).
        text(queueMessage.getMessage());

    List<? extends Property> properties = null;
    if( queueMessage.getMessageProperties() != null ) {
      properties = queueMessage.getMessageProperties().getProperties();
    }
    if( properties != null && properties.size() != 0 ) {
      List<String> adds = new ArrayList<String>();
      for( Property p : properties ) {
        messageBuilder.addProperty(p.getKey(), p.getValue());
        adds.add(p.getKey());
      }
      enqueueOptionsBuilder.additional(adds);
    }
    
    //TODO Eingaben validieren?
    
    
    SQLUtils sqlUtils = getConnection();
    boolean doRollback = true;
    try {
      PLSQLExecUtils.enqueueDirect(sqlUtils, messageBuilder.build(), enqueueOptionsBuilder.build() );
      if( sqlUtils.getLastException() != null ) {
        throw new EnqueueFailedException( sqlUtils.getLastException().getMessage(), sqlUtils.getLastException());
      }
      sqlUtils.commit();
      doRollback = false;
    } finally {
      finallyRollbackAndClose(sqlUtils, doRollback);
    }
  }


  private Integer getPriority(QueueMessage queueMessage) {
    if( queueMessage.getMessageProperties() != null ) {
      return queueMessage.getMessageProperties().getPriority(); //TODO validieren?
    }
    return null;
  }

  private SQLUtils getConnection() throws NoConnectionException {
    try {
      return new SQLUtils(pool.getConnection(OracleAQServiceOperationImpl.ConnectionPoolTimeout.getMillis(), "Oracle AQ"));
    } catch (ConnectionCreationException e) {
      throw new NoConnectionException(connectionTarget, e);
    } catch (SQLException e) {
      throw new NoConnectionException( connectionTarget, e);
    }
  }

  private static void finallyRollbackAndClose(SQLUtils sqlUtils, boolean doRollback) {
    if( doRollback ) {
      try {
        sqlUtils.rollback();
      } catch (Exception e) {
        logger.warn("Failed to rollback connection", e);
      }
    }
    try {  
      sqlUtils.closeConnection();
    } catch (Exception e) {
      logger.warn("Failed to close connection", e);
    }
  }
  
  private Integer toRelativeSeconds(Date date) {
    AbsRelTime absRelTime = date.toAbsRelTime();
    if( absRelTime.isAbsolute() ) {
      absRelTime = absRelTime.toRelative(System.currentTimeMillis());
    }
    return Integer.valueOf( (int)absRelTime.getTime()/1000 );
  }


}

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

import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.db.SQLUtils;


/**
 * class for message that can be enqueued;
 * adds properties to parent class that are only needed for enqueueing
 * @deprecated
 */
public class EnqueueableOracleAQMessage extends OracleAQMessage {

  private static final long serialVersionUID = 1L;

  private static Logger _logger = CentralFactoryLogging.getLogger(EnqueueableOracleAQMessage.class);

  protected int delaySeconds = 0;
  protected Integer expirationSeconds = null;


  public EnqueueableOracleAQMessage(OracleAQMessage msg) {
    super(msg);
  }


  public EnqueueableOracleAQMessage(String corrID, String text, Integer priority) {
    super(corrID, text, priority);
  }


  public EnqueueableOracleAQMessage(String corrID, String text) {
    super(corrID, text);
  }


  public void sendToQueue(QueueData qdata, SQLUtils sqlUtils) {
    EnqueueOptions enqueueOptions = EnqueueOptions.newEnqueueOptions().
        queueName(qdata.getQueueName()).
        delay(getDelaySeconds()).
        expiration(getExpirationSeconds()).
        defaultPriority(1).
        build();
    boolean committed = false;
    SQLException lastException = null;
    try {
      PLSQLExecUtils.enqueueDirect(sqlUtils, this, enqueueOptions);
      lastException = sqlUtils.getLastException();
      sqlUtils.commit();
      committed = true;
    } finally {
      if( ! committed ) {
        try {
          sqlUtils.rollback();
        } catch( Exception e) {
          _logger.warn("Failed to rollback sqlUtils connection", e);
        }
      }
      sqlUtils.setLastException(lastException); //Aufrufer mag evtl. Exception sehen
    }
  }




  public boolean isExpirationSet() {
    return (expirationSeconds != null);
  }

  public int getDelaySeconds() {
    return delaySeconds;
  }

  public void setDelaySeconds(int delaySeconds) {
    this.delaySeconds = delaySeconds;
  }

  public Integer getExpirationSeconds() {
    return expirationSeconds;
  }

  public void setExpirationSeconds(Integer expirationSeconds) {
    this.expirationSeconds = expirationSeconds;
  }

  public void setPriority(Integer prio) {
    this.priority = prio;
  }

  public void setCorrelationID(String corrID) {
    this.corrID = corrID;
  }
}

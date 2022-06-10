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

package com.gip.xyna.xfmg.xfctrl.queuemgmnt;



import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class QueueManagement extends FunctionGroup {

  public static final String DEFAULT_NAME = "QueueManagement";


  private static final Comparator<Queue> queueComparator = new Comparator<Queue>() {

    public int compare(Queue o1, Queue o2) {
      if (!o1.getQueueType().toString().equals(o2.getQueueType().toString())) {
        return o1.getQueueType().toString().compareTo(o2.getQueueType().toString());
      }
      if (o1.getUniqueName() == null) {
        return -1;
      }
      if (o2.getUniqueName() == null) {
        return 1;
      }
      if (!o1.getUniqueName().toLowerCase().equals(o2.getUniqueName().toLowerCase())) {
        return o1.getUniqueName().toLowerCase().compareTo(o2.getUniqueName().toLowerCase());
      } else {
        return o1.getUniqueName().compareTo(o2.getUniqueName());
      }
    }
  };

  private ODS ods;
  private QueueInstanceBuilders queueInstanceBuilders = new QueueInstanceBuilders();


  public QueueManagement() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {

    logger.trace("Executing queuemanagement.init()");

    ods = ODSImpl.getInstance();
    ods.registerStorable(Queue.class);

    // TODO performance: alle Einträge nach DEFAULT kopieren und bei Lesezugriffen dadurch Caching
    //                   auf Applikations-Ebene erlauben (Konfiguration von DEFAULT auf Memory)

  }


  @Override
  protected void shutdown() throws XynaException {
    if (ods != null) {
      ods.unregisterStorable(Queue.class);
    }
  }


  public void registerQueue(String uniqueName, String externalName, QueueType queueType, QueueConnectData connectData)
      throws PersistenceLayerException {
    ODSConnection conn = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      Queue queue = new Queue();
      queue.setUniqueName(uniqueName);
      queue.setExternalName(externalName);
      queue.setConnectData(connectData);
      queue.setQueueType(queueType);
      try {
        conn.persistObject(queue);
        conn.commit();
      } catch (PersistenceLayerException e) {
        throw e;
      }
    } finally {
      try {
        conn.closeConnection();
      } catch (Exception e) {
        logger.debug("Error closing connection", e);
      }
    }
  }


  public void deregisterQueue(String uniqueName) throws PersistenceLayerException {
    ODSConnection conn = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      Queue queue = new Queue();
      queue.setUniqueName(uniqueName);
      try {
        conn.deleteOneRow(queue);
        conn.commit();
      } catch (PersistenceLayerException e) {
        throw e;
      }
    } finally {
      try {
        conn.closeConnection();
      } catch (Exception e) {
        logger.debug("Error closing connection", e);
      }
    }
  }


  public Queue getQueue(String uniqueName) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    // wird aus dem Service heraus aufgerufen

    if (logger.isTraceEnabled()) {
      logger.trace("Executing queuemanagement.getQueue()");
    }
    ODSConnection conn = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      Queue queue = new Queue();
      queue.setUniqueName(uniqueName);
      try {
        conn.queryOneRow(queue);
        return queue;
      } catch (PersistenceLayerException e) {
        throw e;
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw e;
      }
    } finally {
      try {
        conn.closeConnection();
      } catch (Exception e) {
        logger.debug("Error closing connection", e);
      }
    }
  }


  public Collection<Queue> listQueues() throws PersistenceLayerException {
    ODSConnection conn = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      Collection<Queue> queues = conn.loadCollection(Queue.class);
      List<Queue> queuesSorted = new ArrayList<Queue>(queues);
      Collections.sort(queuesSorted, queueComparator);
      if (logger.isTraceEnabled()) {
        logger.trace("Number of registered queues: " + queues.size());
      }
      return queuesSorted;
    } finally {
      try {
        conn.closeConnection();
      } catch (Exception e) {
        logger.debug("Error closing connection", e);
      }
    }
  }

  /**
   * @param name
   * @return
   * @throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY 
   * @throws PersistenceLayerException 
   */
  public Object buildQueueInstance(long revision, String name) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    Queue queue = getQueue(name);
    QueueInstanceBuilder builder = queueInstanceBuilders.get( queue.getQueueType(), revision );
    if( builder == null ) {
      throw new IllegalArgumentException("No registered QueueImplBuilder for queue "+name+" ("+queue.getQueueType()+") in revision "+revision);
    }
    return builder.build(queue);
  }

  public interface QueueInstanceBuilder {
    
    //TODO wie kann richtiger XynaObject-Typ xact.queue.Queue hier bekannt sein?
    Object build(Queue queue);
    
  }

  public void registerQueueInstanceBuilder(QueueType type, long revision,
                                       QueueInstanceBuilder queueInstanceBuilder) {
    logger.info("##### Registering QueueInstanceBuilder for "+type+" in revision "+revision);
    queueInstanceBuilders.put(type,revision, queueInstanceBuilder );
  }
  
  public void unregisterQueueInstanceBuilder(QueueType type, long revision) {
    logger.info("##### Unregistering QueueInstanceBuilder for "+type+" in revision "+revision);
    queueInstanceBuilders.remove(type,revision);
  }
  
  private static class QueueInstanceBuilders {

    private EnumMap<QueueType, Map<Long,QueueInstanceBuilder>> map =
        new EnumMap<QueueType, Map<Long,QueueInstanceBuilder>>(QueueType.class);
    
    public void put(QueueType type, long revision, QueueInstanceBuilder queueInstanceBuilder) {
      synchronized(this) {
        Map<Long,QueueInstanceBuilder> revMap = map.get(type);
        if( revMap == null ) {
          revMap = new HashMap<Long,QueueInstanceBuilder>();
          map.put(type, revMap);
        }
        revMap.put(revision,queueInstanceBuilder);
      }
    }

    public QueueInstanceBuilder get(QueueType type, long revision) {
      Map<Long,QueueInstanceBuilder> revMap = map.get(type);
      if( revMap != null ) {
        return revMap.get(revision);
      }
      return null;
    }

    public void remove(QueueType type, long revision) {
      synchronized(this) {
        Map<Long,QueueInstanceBuilder> revMap = map.get(type);
        if( revMap != null ) {
          revMap.remove(revision);
        }
      }
    }

  }
  
}

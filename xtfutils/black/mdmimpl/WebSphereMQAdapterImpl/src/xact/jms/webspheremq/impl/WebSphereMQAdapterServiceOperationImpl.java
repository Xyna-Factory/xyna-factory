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
package xact.jms.webspheremq.impl;


import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.gip.queue.utils.CachedQueueConnection;
import com.gip.queue.utils.ConnectionCache;
import com.gip.queue.utils.MsgToSend;
import com.gip.queue.utils.MsgToSend.JmsProperty;
import com.gip.queue.utils.MsgToSend.MsgType;
import com.gip.queue.utils.QueueConnection;
import com.gip.queue.utils.exception.QueueException;
import com.gip.queue.utils.webSphereMQ.WebSphereMQConfig;
import com.gip.queue.utils.webSphereMQ.WebSphereMQConnectionBuilder;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.Queue;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueManagement;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueType;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.WebSphereMQConnectData;

import xact.WebSphereMQ.datatypes.ConnectionTimeout;
import xact.WebSphereMQ.datatypes.CorrelationID;
import xact.WebSphereMQ.datatypes.FlagJMSPropertyExistsInList;
import xact.WebSphereMQ.datatypes.JMSProperty;
import xact.WebSphereMQ.datatypes.JMSPropertyFilter;
import xact.WebSphereMQ.datatypes.JMSPropertyList;
import xact.WebSphereMQ.datatypes.JMSPropertyListDocument;
import xact.WebSphereMQ.datatypes.JMSPropertyName;
import xact.WebSphereMQ.datatypes.JMSReplyTo;
import xact.WebSphereMQ.datatypes.KeepConnectionOpenFlag;
import xact.WebSphereMQ.datatypes.WebSphereMQMessage;
import xact.WebSphereMQ.datatypes.XynaQueueMgmtQueueName;
import xact.WebSphereMQ.datatypes.msgtypes.BytesMessageType;
import xact.WebSphereMQ.datatypes.msgtypes.JMSMessageType;
import xact.jms.webspheremq.WebSphereMQAdapterServiceOperation;


public class WebSphereMQAdapterServiceOperationImpl implements ExtendedDeploymentTask,
                                                             WebSphereMQAdapterServiceOperation {

  public static class Constant {
    public static String JMS_PROP_DOC_SEPARATOR = " _###_ ";
  }

  private static Logger _logger = CentralFactoryLogging.getLogger(WebSphereMQAdapterServiceOperationImpl.class);
  
  /*
   * objekte werden aufgeräumt, wenn sie für länger als timeout nicht benötigt worden sind.
   * ------------------------------------------------------------------------------------------------------------------------------------
   * ------------------------------------------------------------------------------------------------------------------------------------
   * ------------------------------------------------------------------------------------------------------------------------------------
   * ------------------------------------------------------------------------------------------------------------------------------------
   * ----------------- ACHTUNG: Kopie von com.gip.xyna.collections.UnboundedObjectPool. (hier wegen zu alter Xyna Version geadded)
   * ------------------------------------------------------------------------------------------------------------------------------------
   * ------------------------------------------------------------------------------------------------------------------------------------
   * ------------------------------------------------------------------------------------------------------------------------------------
   * ------------------------------------------------------------------------------------------------------------------------------------
   */
  public static class UnboundedObjectPool<T extends Closeable> {

    private static final Logger logger = CentralFactoryLogging.getLogger(UnboundedObjectPool.class);

    private final LinkedHashSet<T> objects = new LinkedHashSet<>();
    /*
     * angenommener usecase: addIfNeeded wird aufgerufen, nachdem ein objekt in benutzung war.
     * 
     * 3 objekte in pool:
     * [0] -> vor 15 min benutzt
     * [1] -> vor 10 min benutzt
     * [2] -> vor 5 min benutzt
     * falls timeout nun 18 min ist, wird in 3 min das älteste objekt aufgeräumt, d.h. danach ist der stand dann:
     * [0] -> vor 13 min genutzt
     * [1] -> vor 8 min genutzt
     * 
     * beim get() wird immer der niedrigste eintrag (höchster index) entfernt
     * beim addIfNeeded() wird ein neuer eintrag angelegt (höchster index) mit (vor 0 min genutzt)
     */
    private final List<Long> lastAccess = new ArrayList<>();
    private final int timeoutClose; //wenn ein objekt solange nicht benutzt wurde, wird es aufgeräumt
    private final int minSize; //wenn weniger objekte im pool sind, wird beim addIfNeeded sicher hinzugefügt.
    //objekte beim addIfNeeded ablehnen, falls es ein objekt gibt, was so lange nicht in verwendung ist. muss <= timeoutClose sein
    //idee: wieso neues objekt hinzufügen, wenn es noch so lange nicht verwendete objekte gibt...
    private final int timeoutDenyNew;
    private static final Timer timer = new Timer("UnboundedObjectPool-" + Constants.defaultUTCSimpleDateFormat().format(new Date()), true);
    private TimerTask timertask;
    private boolean closed = false;


    /**
     * 
     * @param timeoutCloseMs wenn ein Objekt so lange nicht in Verwendung war, wird es geclosed
     * @param timeoutDenyNewMs beim add werden Objekte nicht angenommen, wenn es Objekte gibt, die mindestens so lange nicht in Verwendung waren.
     * @param minSize beim add werden Objekte angenommen, falls nicht mindestens soviele Objekte im Pool sind
     */
    public UnboundedObjectPool(int timeoutCloseMs, int timeoutDenyNewMs, int minSize) {
      if (timeoutCloseMs <= 10) {
        throw new RuntimeException("Timeout too low");
      }
      this.timeoutClose = timeoutCloseMs;
      this.timeoutDenyNew = timeoutDenyNewMs;
      this.minSize = minSize;
    }


    /**
     * returns null, falls kein objekt vorhanden
     */
    public T get() {
      synchronized (objects) {
        checkClosed();
        if (objects.isEmpty()) {
          return null;
        }
        lastAccess.remove(lastAccess.size() - 1);
        return removeLast();
      }
    }


    private T removeLast() {
      Iterator<T> it = objects.iterator();
      T t = it.next();
      it.remove();
      return t;
    }


    private class MyTimerTask extends TimerTask {

      @Override
      public void run() {
        try {
          cleanup(false);
        } finally {
          synchronized (objects) {
            addTimerTask();
          }
        }
      }
    }


    /**
     * Fügt Objekt zu Pool hinzu, wenn minSize nicht erreicht ist, oder wenn das älteste Objekt weniger lange als timeoutDenyNew wartet.
     * @return gibt false zurück, falls Objekt nicht zum Pool hinzugefügt worden ist. In diesem Fall ist der Aufrufer für das Objekt zuständig (close aufrufen)
     */
    public boolean addIfNeeded(T t) {
      long time = System.currentTimeMillis();
      synchronized (objects) {
        checkClosed();
        if (objects.contains(t)) {
          throw new IllegalArgumentException("Object already in Pool.");
        }
        int s = objects.size();
        if (s > 0 && s >= minSize && time - lastAccess.get(0) >= timeoutDenyNew) {
          return false;
        }
        objects.add(t);
        lastAccess.add(time);
        if (timertask == null) {
          addTimerTask();
        }
        return true;
      }
    }


    private void addTimerTask() {
      if (objects.size() > 0) {
        timertask = new MyTimerTask();
        long delay = timeoutClose - (System.currentTimeMillis() - lastAccess.get(0));
        if (delay < 10) {
          delay = 10;
        }
        timer.schedule(timertask, delay);
      } else {
        timertask = null;
      }
    }


    private void checkClosed() {
      if (closed) {
        throw new RuntimeException("Pool is closed");
      }
    }


    private void cleanup(boolean force) {
      List<T> removed = new ArrayList<T>();
      long timeoutTS = System.currentTimeMillis() - timeoutClose;
      synchronized (objects) {
        while (lastAccess.size() > 0 && (force || lastAccess.get(0) <= timeoutTS)) {
          removed.add(removeLast());
          lastAccess.remove(0);
        }
      }
      for (T t : removed) {
        try {
          t.close();
        } catch (Throwable e) {
          logger.warn("Could not close " + t + ".", e);
        }
      }
    }


    public int size() {
      synchronized (objects) {
        checkClosed();
        return objects.size();
      }
    }


    public boolean isClosed() {
      synchronized (objects) {
        return closed;
      }
    }


    /**
     * räumt alle objekte im pool auf
     */
    public void close() {
      synchronized (objects) {
        checkClosed();
        if (timertask != null) {
          timertask.cancel();
        }
        cleanup(true);
        closed = true;
      }
    }

  }
  
  private static class CachedConnection implements Closeable {
    
    public final QueueConnection conn;
    
    public CachedConnection(QueueConnection con) {
      this.conn = con;
    }

    @Override
    public void close() throws IOException {
      try {
        conn.close();
      } catch (QueueException e) {
        throw new IOException(e);
      }
    }
    
  }
  

  private ConnectionCache _connCache = new ConnectionCache();
  private final Map<String, UnboundedObjectPool<CachedConnection>> pools = new HashMap<>();

  public void onDeployment() throws XynaException {
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() throws XynaException {
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
    _connCache.closeAll();
    synchronized (pools) {
      for (UnboundedObjectPool<CachedConnection> pool : pools.values()) {
        pool.close();
      }
    }
    UnboundedObjectPool.timer.cancel();
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.;    
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }


  public Container selectJMSProperty(JMSPropertyName nameIn, JMSPropertyList list) {
    try {
      String name = nameIn.getJMSPropertyName();

      FlagJMSPropertyExistsInList flag = new FlagJMSPropertyExistsInList();
      flag.setJMSPropertyExistsInList(false);
      if ((name != null) && (name.trim().length() > 0)) {
        for (JMSProperty prop : list.getData()) {
          if (name.equals(prop.getJMSPropertyName())) {
            flag.setJMSPropertyExistsInList(true);
            return new Container(flag, prop);
          }
        }
      }
      JMSProperty empty = new JMSProperty();
      return new Container(flag, empty);
    }
    catch (Exception e) {
      throw new RuntimeException("Error in selectJMSProperty", e);
    }
  }


  public void enqueueExtd(XynaQueueMgmtQueueName xynaQueueMgmtQueueName, WebSphereMQMessage webSphereMQMessage,
                              CorrelationID correlationID, JMSMessageType jMSMessageType,
                              KeepConnectionOpenFlag keepConnectionOpenFlag, ConnectionTimeout connectionTimeout,
                              JMSPropertyList jMSPropertyList) {
    try {
      MsgToSend msg = buildMsg(webSphereMQMessage, correlationID, jMSMessageType, jMSPropertyList,
                               null);
      send(xynaQueueMgmtQueueName, msg, keepConnectionOpenFlag, connectionTimeout);
    }
    catch (Exception e) {
      throw new RuntimeException("Error in sendToQueueExtd", e);
    }
  }


  public void enqueueExtdWithJMSReplyTo(XynaQueueMgmtQueueName xynaQueueMgmtQueueName, WebSphereMQMessage webSphereMQMessage,
                              CorrelationID correlationID, JMSMessageType jMSMessageType,
                              KeepConnectionOpenFlag keepConnectionOpenFlag, ConnectionTimeout connectionTimeout,
                              JMSPropertyList jMSPropertyList, JMSReplyTo jmsReplyTo) {
    try {
      MsgToSend msg = buildMsg(webSphereMQMessage, correlationID, jMSMessageType, jMSPropertyList,
                               jmsReplyTo);
      send(xynaQueueMgmtQueueName, msg, keepConnectionOpenFlag, connectionTimeout);
    }
    catch (Exception e) {
      throw new RuntimeException("Error in sendToQueueExtdWithJMSReplyTo", e);
    }
  }


  private void send(XynaQueueMgmtQueueName xynaQueueMgmtQueueName, MsgToSend msg,
                    KeepConnectionOpenFlag keepConnectionOpenFlag,
                    ConnectionTimeout connectionTimeout) throws QueueException {
    WebSphereMQConfig config = getConfig(xynaQueueMgmtQueueName.getXynaQueueMgmtQueueName());
    WebSphereMQConnectionBuilder builder = new WebSphereMQConnectionBuilder(config);
    if (keepConnectionOpenFlag.getKeepConnectionOpen()) {
      sendCached(xynaQueueMgmtQueueName, msg, builder, connectionTimeout);
    }
    else {
      sendUncached(msg, builder, xynaQueueMgmtQueueName.getXynaQueueMgmtQueueName());
    }
  }


  private void sendCached(XynaQueueMgmtQueueName xynaQueueMgmtQueueName, MsgToSend msg,
                          WebSphereMQConnectionBuilder builder,
                          ConnectionTimeout connectionTimeout) throws QueueException {
    CachedQueueConnection conn = _connCache.get(xynaQueueMgmtQueueName.getXynaQueueMgmtQueueName());
    if (conn == null) {
      conn = _connCache.add(xynaQueueMgmtQueueName.getXynaQueueMgmtQueueName(), builder);
    }
    conn.send(msg, connectionTimeout.getConnectionTimeoutSeconds());
  }
  

  private void sendUncached(MsgToSend msg, WebSphereMQConnectionBuilder builder, String queueName) throws QueueException {
    UnboundedObjectPool<CachedConnection> pool;
    synchronized (pools) {
      pool = pools.get(queueName);
      if (pool == null) {
        pool = new UnboundedObjectPool<>(5 * 60 * 1000, 2 * 60 * 1000, 3);
        pools.put(queueName, pool);
      }
    }
    CachedConnection cached = pool.get();
    if (cached == null) {
      QueueConnection con = builder.build();
      con.open();
      cached = new CachedConnection(con);
    }
    boolean success = false;
    try {
      cached.conn.send(msg);
      success = true;
    } finally {
      if (!success || !pool.addIfNeeded(cached)) {
        try {
          cached.close();
        } catch (IOException e) {
          _logger.warn("Could not close Queue Connection", e);
        }
      } //else success && addIfNeeded=true
    }
  }


  private MsgToSend buildMsg(WebSphereMQMessage webSphereMQMessage,
                             CorrelationID correlationID, JMSMessageType jMSMessageType,
                             JMSPropertyList jMSPropertyList, JMSReplyTo jmsReplyTo) {
    MsgToSend ret = new MsgToSend();
    ret.message(webSphereMQMessage.getMessage());
    if (correlationID != null) {
      ret.corrId(correlationID.getValue());
    }
    if (jMSMessageType instanceof BytesMessageType) {
      ret.messageType(MsgType.BYTES_MSG);
    }
    else {
      ret.messageType(MsgType.TEXT_MSG);
    }
    if (jMSPropertyList != null) {
      if (jMSPropertyList.getData() != null) {
        for (JMSProperty prop : jMSPropertyList.getData()) {
          ret.addJmsProperty(new JmsProperty(prop.getJMSPropertyName(), prop.getJMSPropertyValue()));
        }
      }
    }
    if (jmsReplyTo != null) {
      if (jmsReplyTo.getJMSReplyToQueueName() != null) {
        ret.jmsReplyToQueueName(jmsReplyTo.getJMSReplyToQueueName());

      }
    }
    return ret;
  }


  private WebSphereMQConfig getConfig(String uniqueName) {
    Queue q = getStoredQueue(uniqueName);
    return buildConfig(q);
  }


  private static Queue getStoredQueue(String uniqueName) {
    try {
      QueueManagement mgmt = new QueueManagement();
      Queue ret = mgmt.getQueue(uniqueName);

      _logger.debug("Got Stored Queue: " + ret.toString());
      if (ret.getQueueType() != QueueType.WEBSPHERE_MQ) {
        throw new RuntimeException("Error getting registered queue data (name = " + uniqueName + "):" +
                                   " Wrong queue type.");
      }
      return ret;
    }
    catch (Exception e) {
      throw new RuntimeException("Error getting registered queue data (name = " + uniqueName + ")", e);
    }
  }


  private WebSphereMQConfig buildConfig(Queue queue)  {
    WebSphereMQConnectData connData = null;
    try {
      connData = (WebSphereMQConnectData) queue.getConnectData();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    WebSphereMQConfig ret = new WebSphereMQConfig();
    ret.setChannel(connData.getChannel());
    ret.setHostName(connData.getHostname());
    ret.setPort(connData.getPort());
    ret.setQueueManager(connData.getQueueManager());
    ret.setQueueName(queue.getExternalName());
    ret.setTransportType(null);
    return ret;
  }


  public JMSPropertyListDocument adaptJMSPropertyListToString(JMSPropertyList listIn) {
    JMSPropertyListDocument ret = new JMSPropertyListDocument();
    if (listIn.getData() == null) { return ret; }
    try {
      StringBuilder s = new StringBuilder();
      for (JMSProperty prop : listIn.getData()) {
        s.append(prop.getJMSPropertyName()).append(Constant.JMS_PROP_DOC_SEPARATOR).
          append(prop.getJMSPropertyValue()).append(" \n ");
      }
      ret.setListAsString(s.toString());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    return ret;
  }


  public JMSPropertyList adaptStringToJMSPropertyList(JMSPropertyListDocument docIn) {
    JMSPropertyList ret = new JMSPropertyList();
    try {
      String doc = docIn.getListAsString();
      if (doc == null) {
        return ret;
      }
      String[] lines = doc.split("\n");
      for (String line : lines) {
        String[] parts = line.split(Constant.JMS_PROP_DOC_SEPARATOR);
        if (line.trim().length() < 1) { continue; }
        //if (parts.length < 2) { continue; }
        if (parts.length != 2) {
          throw new RuntimeException("Error parsing JMSPropertyListDocument: Incorrect number of line separators ('" +
                          Constant.JMS_PROP_DOC_SEPARATOR + "') in line '" + line + "'");
        }
        JMSProperty prop = new JMSProperty();
        prop.setJMSPropertyName(parts[0].trim());
        prop.setJMSPropertyValue(parts[1].trim());
        ret.addToData(prop);
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    return ret;
  }


  public JMSPropertyList filterJMSPropertyList(JMSPropertyList listIn, JMSPropertyFilter filter) {
    JMSPropertyList ret = new JMSPropertyList();
    if (listIn.getData() == null) { return ret; }
    if (filter.getAllowedJMSPropertyNames() == null) { return ret; }
    try {
      for (JMSProperty prop : listIn.getData()) {
        if (jmsPropertyIsInFilter(prop, filter)) {
          ret.addToData(prop);
        }
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    return ret;
  }


  private boolean jmsPropertyIsInFilter(JMSProperty prop, JMSPropertyFilter filter) {
    String nameToMatch = prop.getJMSPropertyName();
    if (nameToMatch == null) {
      return false;
    }
    for (JMSPropertyName name : filter.getAllowedJMSPropertyNames()) {
      if (nameToMatch.equals(name.getJMSPropertyName())) {
        return true;
      }
    }
    return false;
  }

}

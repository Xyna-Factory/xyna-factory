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
package com.gip.xyna.xact.trigger;



import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.log.Log4jLogFactory;
import org.snmp4j.log.LogFactory;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmTimeTable;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.snmp.agent.RequestHandler;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStartedException;
import com.gip.xyna.xact.trigger.snmp.SNMPTRIGGER_SocketBindException;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;



public class SNMPTrigger extends EventListener<SNMPTriggerConnection, SNMPStartParameter> {

  private static final Logger logger = CentralFactoryLogging.getLogger(SNMPTrigger.class);

  public SNMPTrigger() {
  }


  private ArrayBlockingQueue<CommandResponderEvent> events;
  private int receiverTimeout;
  private volatile Snmp snmp;
  private SNMPStartParameter startParameters;

  private ArrayList<Thread> receiverThreads = new ArrayList<Thread>();
  private ReentrantLock lock = new ReentrantLock();
  private ThreadPool threadPool;
  private TransportMapping transport;
  private volatile boolean initialized = false;

  private byte[] localEngineID;

  static {
    LogFactory.setLogFactory(new Log4jLogFactory());
  }


  private void setTransportMapping(TransportMapping tm) {
    transport = tm;
  }


  public void start(final SNMPStartParameter sp) throws XACT_TriggerCouldNotBeStartedException {

    startParameters = sp;

    final InetAddress address = sp.getIP();
    final int capFullTimeout = sp.getTimeoutCapacityFull();

    if (logger.isInfoEnabled()) {
      logger.info("Creating SNMPTrigger listening on " + address.getHostAddress() + ":" + sp.getPort() + " ...");
    }
    events = new ArrayBlockingQueue<CommandResponderEvent>(sp.getQueueCapacity());
    receiverTimeout = sp.getReceiverTimeout();

    String version = sp.getVersion();

    try {
      retryBindException(new BindTask() {

        public void execute() throws IOException, XynaException {
          setTransportMapping(new DefaultUdpTransportMapping(new UdpAddress(address, sp.getPort())));
        }

      }, 50, 100);
    } catch (IOException e) {
      throw new SNMPTRIGGER_SocketBindException(address.getHostAddress(), sp.getPort(), e);
    } catch (XynaException e) {
      throw new SNMPTRIGGER_SocketBindException(address.getHostAddress(), sp.getPort(), e);
    }

    threadPool = ThreadPool.create("SNMP-" + address.getHostAddress() + ":" + sp.getPort(), sp.getNumListenerThreads());
    MessageDispatcher mtDispatcher = new MultiThreadedMessageDispatcher(threadPool, new MessageDispatcherImpl());

    snmp = new Snmp(mtDispatcher);

    // add message processing models
    if (version.equals(SNMPStartParameter.VERSION_2C)) {
      mtDispatcher.addMessageProcessingModel(new MPv2c());
    } else if (version.equals(SNMPStartParameter.VERSION_3)) {
      SecurityProtocols.getInstance().addDefaultProtocols();
      localEngineID = getLocalEngineId(address, sp.getPort());

      USM usm = new USM(SecurityProtocols.getInstance(), new OctetString(localEngineID), XynaFactory.getInstance().getBootCount());
      SecurityModels.getInstance().addSecurityModel(usm);

      MPv3 mpv3 = new MPv3(usm);
      if (logger.isDebugEnabled()) {      
        logger.debug("enterpriseid=" + MPv3.getEnterpriseID());
      }
      mtDispatcher.addMessageProcessingModel(mpv3);

      int engineTime = (int) ((System.currentTimeMillis() - XynaFactory.STARTTIME) / 1000);
      if (logger.isDebugEnabled()) {
        OctetString os = new OctetString(snmp.getLocalEngineID());
        logger.debug("local EngineId: " + os.toHexString() + " - " + os.toString() + ", engineBoots = "
            + XynaFactory.getInstance().getBootCount() + ", engineTime = " + engineTime);
      }

      snmp.setLocalEngine(localEngineID, XynaFactory.getInstance().getBootCount(),
                          engineTime);
      setEngineTimePerReflection(snmp.getUSM().getTimeTable(), XynaFactory.STARTTIME);
    } else {
      //startparameter klasse findet das heraus
      throw new IllegalStateException("version " + version + " not supported by SNMPTrigger.");
    }
    //    mtDispatcher.addMessageProcessingModel(new MPv1());

    snmp.addTransportMapping(transport);

    snmp.addCommandResponder(new CommandResponder() {

      public void processPdu(CommandResponderEvent e) {
        if (!e.isProcessed()) {
          try {
            // event zu queue hinzufügen. receive methode liest dann aus queue aus...
            if (!events.offer(e, capFullTimeout, TimeUnit.MILLISECONDS)) {
              // überlastet => nichts tun TODO herausfinden, ob man statt dessen in bestimmten fällen einen fehler
              // zurückgeben sollte?
            }
          } catch (InterruptedException e1) {
            // sollte nicht vorkommen. falls doch, auch nicht so schlimm
          }
        }
      }

    });
    List<Runnable> exec = new ArrayList<Runnable>();
    synchronized (executeOnInit) {
      initialized = true;
      exec.addAll(executeOnInit);
    }
    for (Runnable r : exec) {
      try {
        r.run();
      } catch (RuntimeException e) {
        logger.error("Exception executing runnable " + r, e);
      }
    }
   
    try {
      snmp.listen();
    } catch (IOException e1) {
      //falls bereits ein listener registriert ist.
      throw new RuntimeException(e1);
    }
  }
  
  
  private static Field lastLocalTimeChangeField;


  /**
   * böser hack: SNMP4j speichert die enginetime auf eine merkwürdige art und weise in UsmTimeTable.
   * beim verschicken eines requests mit authorativeEngineTime wird diese auf die vergangene zeit seit dem
   * letzten mal, dass jemand "setEngineTime" gesagt hat, gesetzt. anstatt auf die vergangene zeit seit der
   * derzeit gesetzten enginestarttime.
   * 
   * unklar, wieso das so ist.... aber so funktionierts.
   * gleicher hack auch im SNMPService (FIXME duplicate code!!)
   */
  private static void setEngineTimePerReflection(UsmTimeTable usmTimeTable, long factoryStarttime) {
    if (lastLocalTimeChangeField == null) {
      Field f;
      try {
        f = UsmTimeTable.class.getDeclaredField("lastLocalTimeChange");
      } catch (SecurityException e) {
        throw new RuntimeException("SNMP4j could not be configured with enginetime.", e);
      } catch (NoSuchFieldException e) {
        throw new RuntimeException("SNMP4j could not be configured with enginetime.", e);
      }
      f.setAccessible(true);
      lastLocalTimeChangeField = f;
    }
    try {
      lastLocalTimeChangeField.set(usmTimeTable, factoryStarttime);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("SNMP4j could not be configured with enginetime.", e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("SNMP4j could not be configured with enginetime.", e);
    }
  }

  public byte[] getLocalEngineId() {
    return localEngineID;
  }

  //in snmpserviceimpl sehr ähnlich!
  private static byte[] getLocalEngineId(InetAddress ip, int port) {
    //return MPv3.createLocalEngineID();
    byte[] b = ip.getAddress();
    OctetString os = new OctetString(b);
    os.append(":".getBytes());
    os.append(getIntAsByteArray(port));
    os.append("trg".getBytes()); //trigger
    
    long revision = VersionManagement.REVISION_WORKINGSET;
    if (SNMPTrigger.class.getClassLoader() instanceof ClassLoaderBase) {
      revision = ((ClassLoaderBase)SNMPTrigger.class.getClassLoader()).getRevision();
    }
    os.append(getIntAsByteArray((int)revision));
    
    //darf maximal 27 bytes lang sein. 2 für portbytes, 4-8 für address, 4 rest, 2 revision, macht zusammen 12 - 16
    //für zukünftige änderungen auf nummer sicher gehen:
    if (os.length() > 27) {
      b = os.getValue();
      byte[] b2 = new byte[27];
      System.arraycopy(b, 0, b2, 0, 27);
      os = new OctetString(b2);
    }
    return MPv3.createLocalEngineID(os);
    //FIXME xynainstanz id, damit eindeutigkeit auch bei mehreren instanzen gewährleistet werden kann?
  }


  private static byte[] getIntAsByteArray(int i) {
    return new byte[]{(byte) (i / 256 - 128), (byte) (i % 256 - 128)};
  }


  public Snmp getSnmp() {
    return snmp;
  }
  
  
  /**
   * für jede andere per snmpv3 bekannte snmp engine (also eine, mit der man in der vergangenheit mal kommuniziert hat)
   * werden bootcnt+boottime gespeichert und beim zugriff gegen den letzten gespeicherten wert auf plausibilität
   * vergleichen/kontrolliert. sinn: replay-attacken verhindern.
   * wenn ein device seinen bootcnt+time resettet (z.b. firmware-upgrade), muss der eintrag aus dem cache entfernt werden.
   * dazu ist diese methode.
   * @param engineIdAsBytes
   */
  public void resetUsmTimeEntry(byte[] engineIdAsBytes) {
    if (snmp.getUSM() != null) {
      UsmTimeTable timeTable = snmp.getUSM().getTimeTable();
      timeTable.removeEntry(new OctetString(engineIdAsBytes));
    } //bei v2 nichts tun
  }
  
  public SNMPStartParameter getStartParameter() {
    return startParameters;
  }


  public SNMPTriggerConnection receive() {
    while (true) {
      CommandResponderEvent e = events.poll();
      if (e == null) { //queue empty => wartend pollen. diese threads mï¿½ssen beim trigger-stop unterbrochen werden
        lock.lock();
        try {
          receiverThreads.add(Thread.currentThread());
        } finally {
          lock.unlock();
        }
        try {
          e = events.poll(receiverTimeout, TimeUnit.SECONDS);
        } catch (InterruptedException e1) {
          // maybe stopped
          return null;
        } finally {
          lock.lock();
          try {
            receiverThreads.remove(Thread.currentThread());
          } finally {
            lock.unlock();
          }

        }
      }
      if (e != null) {
        return new SNMPTriggerConnection(e, this);
      }
    }
  }


  /**
   * called by Xyna Processing to stop the Trigger. should make sure, that start() may be called again directly
   * afterwards. connection instances returned by the method receive() should not be expected to work after stop() has
   * been called.
   */
  public void stop() {
    synchronized (executeOnInit) {
      initialized = false;
    }
    try {
      snmp.close();
      threadPool.stop();
      //receiverthreads unterbrechen
      lock.lock();
      try {
        for (Thread t : receiverThreads) {
          t.interrupt();
        }
      } finally {
        lock.unlock();
      }
    } catch (IOException e) {
      logger.error("Error while closing SNMPTrigger", e);
    }
    if (logger.isInfoEnabled()) {
      logger.info("stopped SNMPTrigger " + startParameters.getIP().getHostAddress() + ":" + startParameters.getPort());
    }
  }


  public void onNoFilterFound(SNMPTriggerConnection con) {
    if (logger.isInfoEnabled()) {
      logger.info("No filter found for incoming snmp request " + con.getPDU());
    }
    try {
      con.sendError(RequestHandler.NO_SUCH_NAME, new Exception("No filter found for incoming snmp request "
                      + con.getPDU()));
    } catch (XynaException e) {
      logger.debug("could not send error", e);
    }
  }


  @Override
  public String getClassDescription() {
    return "SNMP Trigger. Accepts SNMP requests of all kinds";
  }


  @Override
  public void onProcessingRejected(String s, SNMPTriggerConnection con) {
    if (logger.isInfoEnabled()) {
      logger.info("snmp request rejected " + con.getPDU() + ": " + s);
    }
    try {
      con.sendError(RequestHandler.NO_SUCH_NAME, new Exception("snmp request rejected " + con.getPDU() + ": " + s));
    } catch (XynaException e) {
      logger.debug("could not send error", e);
    }
  }
  
  private List<Runnable> executeOnInit = new ArrayList<Runnable>();
  

  /**
   * führt runnable aus, sofern oder sobald der trigger gestartet wurde
   */
  public void executeWhenTriggerIsInitialized(Runnable r) {
    while (true) {
      if (initialized) {
        r.run();
        return;
      } else {
        synchronized (executeOnInit) {
          if (!initialized) {
            executeOnInit.add(r);
            return;
          }
          //retry
        }
      }
    }
  }


}

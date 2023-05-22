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
package com.gip.xyna.xfmg.xclusteringservices;



import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.concurrent.JoinedExecutor;
import com.gip.xyna.xact.rmi.GenericRMIAdapter;
import com.gip.xyna.xact.rmi.RMIConnectionFailureException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterConnectionException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClusterInitializationException;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidConnectionParametersForClusterProviderException;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStartParametersForClusterProviderException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnableNoException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnableNoResult;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProviderTools.RMIRunnableNoResultNoException;
import com.gip.xyna.xfmg.xclusteringservices.RMIRetryExecutor.RMIConnectionDownException;
import com.gip.xyna.xfmg.xclusteringservices.RMIRetryExecutor.RMIConnectionNotAvailableHandler;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.InitializableRemoteInterface;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.RMIImplFactory;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.RMIImplProxy;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xmcp.exceptions.XMCP_RMI_BINDING_ERROR;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



/**
 * cluster von knoten, die �ber rmi kommunizieren k�nnen.<br>
 * die interfaces, �ber die kommuniziert wird, k�nnen dynamisch erweitert werden. es wird immer der gleiche port
 * benutzt.
 * <p>
 * verwendung: <br>
 * 1. erstelle neuen {@link RMIClusterProvider} an festem port <br>
 * 2. f�ge andere knoten hinzu.<br>
 * 3. registriere rmi-interfaces<br>
 * 4. greife auf registrierte rmi-interfaces zu zus�tzlich monitoring-m�glichkeiten, die jeder clusterprovider bietet
 */
public class RMIClusterProvider implements ClusterProvider {

  //ACHTUNG: die xynaproperties werden auch indirekt vom xsorclusterprovider verwendet, n�mlich �ber die verwendung vom rmiretryexecutor
  public static final XynaPropertyInt RMI_RETRY_ATTEMPTS = new XynaPropertyInt("xyna.xfmg.xcs.rmi_retry_attempts", 1);
  public static final XynaPropertyInt RMI_RETRY_WAIT = new XynaPropertyInt("xyna.xfmg.xcs.rmi_retry_wait", 10); //sekunden
    
  private static final Logger logger = CentralFactoryLogging.getLogger(RMIClusterProvider.class);
  private static final String INTERCONNECT_BINDING = "RMIClusterProviderInterconnect";
  public static final String TYPENAME = RMIClusterProvider.class.getSimpleName();
  private static final int CONNECTGUARD_TIMEOUT = 15;
  private InterconnectChecker interconnectChecker; 

  private ConnectGuard connectGuard;
  //FIXME codeduplication zusammen mit OracleRACCLusterprovider => extraktion von algorithmus+methoden+innere klassen,
  //wo m�glich, damit der code wiederverwendet werden kann!
  private class ConnectGuard implements Runnable {

    private volatile long waitingRefreshed;
    private long timeout;
    private volatile boolean canceled;

    public ConnectGuard(long timeout) {
      this.timeout = timeout;
      waitingRefreshed = System.currentTimeMillis();
      canceled = false;
      connectGuard = this;
    }

    public void cancel() {
      logger.info("ConnectGuard canceled");
      this.canceled = true;
      synchronized (this) {
        this.notify();
      }
    }

    public void run() {
      logger.info("Started ConnectGuard");
      while (!canceled ) {
        long now = System.currentTimeMillis();
        if( waitingRefreshed + timeout < now ) {
          break; //Abbruch wegen �berschrittener Wartezeit
        } else { 
          try { //weiter warten
            synchronized (this) {
              if (!canceled) { //kann in der zwischenzeit gecanceled worden sein
                this.wait(waitingRefreshed + timeout - now);
              }
            }
          } catch (InterruptedException e) {
            //Exception ignorieren: dann halt k�rzer warten
          }
        }
      }
      if( ! canceled ) {
        //Der erwartete CONNECT kam nun nach dem Timeout nicht. 
        //Es kann davon ausgegangen werden, dass der andere Knoten verstorben ist, 
        //daher wird nun ein �bergang nach DISCONNECTED_MASTER versucht.

        if (logger.isInfoEnabled()) {
          logger.info("No connect with other node after " + timeout
              + " ms timeout. Assuming other node is dead and switching to DISCONNECTED_MASTER");
        }
        changeClusterState(ClusterState.DISCONNECTED_MASTER);
      }
      connectGuard = null;
      logger.info("ConnectGuard finished");
    }


    public void keepWaiting() {
      waitingRefreshed = System.currentTimeMillis();
    }

  }  

  private static final Object RMI_EXCEPTION = new Object();

  class Connector implements Runnable {
    private volatile boolean canceled = false;

    public void run() {
      connector = this;
      if( clusterStateChangeHandler != null ) {
        waitForReadyToConnect();
      }
      if( !canceled ) {
        connect();
      }
    }


    private void waitForReadyToConnect() {

      boolean ready = false;
      while (!(ready || canceled)) {
        ready = clusterStateChangeHandler.isReadyForChange(ClusterState.CONNECTED);
        if (!ready) {
          try {
            logger.debug("waiting for isReadyForChange CONNECTED");
            keepWaiting();
            if (!canceled) {
              synchronized (this) {
                this.wait(500);
              }
            }
          } catch (InterruptedException e) {
            //Ignorieren, dann ist Wartezeit halt k�rzer
          }
        }
      }
      logger.debug("isReadyForChange CONNECTED");
    }

    private void keepWaiting() {
      try {
        List<Object> result = 
        RMIClusterProviderTools
        .executeAndCumulateNoException(RMIClusterProvider.this, interconnectId,
                            new RMIRunnableNoException<Object, RMIClusterProviderInterconnectInterface>() {

                              public Object execute(RMIClusterProviderInterconnectInterface clusteredInterface)
                                  throws RemoteException {
                                clusteredInterface.waiting();
                                return null;
                              }

                            }, null, RMI_EXCEPTION);
        if (result.contains(RMI_EXCEPTION)) {
          logger.info("rmi connection seems broken. cancelling connector thread");
          canceled = true;
        }
      } catch (InvalidIDException e) {
        throw new RuntimeException(e);
      }
    }


    private void connect() {
      //urspr�nglich hat der andere knoten angefragt, dass connected werden soll und wir haben ihm immer bescheid
      //gesagt, dass wird versuchen, nach connected zu gehen, und er bitte warten soll.
      //jetzt sind wir endlich soweit, nach connected gehen, vorher fragen wir aber sicherheitshalber nochmal
      //nach, ob er auch noch ready ist, oder ob in der zwischenzeit irgendein timeout passiert ist.
      //falls der andere knoten nicht mehr ready ist, ist der andere knoten dran, den vorgang erneut zu starten.
      try {
        List<Boolean> result = RMIClusterProviderTools
            .executeAndCumulateNoException(RMIClusterProvider.this, interconnectId,
                                new RMIRunnableNoException<Boolean, RMIClusterProviderInterconnectInterface>() {

                                  public Boolean execute(RMIClusterProviderInterconnectInterface clusteredInterface)
                                      throws RemoteException {
                                    return clusteredInterface.readyForStateChange(ClusterState.CONNECTED);
                                  }

                                }, null);
        if (result.get(0) == false) {
          logger.info("Connector ready for state change to CONNECTED, but original node is not! Aborting attempt to change state.");
          return;
        }
      } catch (InvalidIDException e) {
        logger.warn(e);
      }
      
      //Sicherung gegen RaceCondition ClusterState->CONNECTED und startHeartBeat:
      //startHeartBeat darf erst gerufen werden, wenn fremder Knoten CONNECTED ist
      //1) eigener ClusterState -> CONNECTED
      changeClusterStateInternally(ClusterState.CONNECTED, true);
      //2) fremden Knoten per RMI benachrichtigen, dass er CONNECTED ist
      try {
        RMIClusterProviderTools
            .executeNoException(RMIClusterProvider.this, interconnectId,
                                new RMIRunnableNoResultNoException<RMIClusterProviderInterconnectInterface>() {

                                  public void execute(RMIClusterProviderInterconnectInterface clusteredInterface)
                                      throws RemoteException {
                                    clusteredInterface.connect();
                                  }

                                });
      } catch (InvalidIDException e) {
        logger.warn(e);
      }
      //3) Heartbeat starten
      if( getState() == ClusterState.CONNECTED ) {
        startHeartBeat();
      }
    }

    public void readyForStateChange() {
      synchronized (this) {
        this.notify();
      }
    }
    
  }
  
  class RMIInterconnectImpl implements RMIClusterProviderInterconnectInterface {

    public void register(String hostname, int port, boolean restore) throws XFMG_ClusterConnectionException, RemoteException {
      logger.debug("Other nodes sends register-request ("+hostname+","+port+","+restore+")");
      if (restore) {
        NodeConnectionParameters nc = getNodeConnection(hostname, port);
        addRMIAdaptersForNode(nc);
      } else {
        boolean didNotExist = addNodeInternally(hostname, port);
        if (!didNotExist) {
          throw new IllegalArgumentException("Parameters <hostname='" + hostname + "', port='" + port
              + "'> already exist.");
        }
        try {
          saveOtherNode(hostname, port);
        } catch (PersistenceLayerException e) {
          throw new XFMG_ClusterConnectionException(e);
        }
      }
      try {
        connector = new Connector();
        new Thread(connector, "RMIClusterProvider-Connector").start();
      } catch( NoClassDefFoundError e ) {
        logger.error( "It seems that this running xynafactory ist incompatible with its xynafactory.jar. Maybe the xynafactory.jar was modified. ");
        logger.error( "Detected via ", e );
        //Exception ist f�r Remote-Knoten!
        throw new RemoteException("It seems that the remote running xynafactory ist incompatible with its xynafactory.jar");
      }
    }


    public void connect() {
      logger.debug("Other nodes sends connect-request");
      //auf CONNECTED gehen und Heartbeat starten, der andere Knoten ist bereits CONNECTED
      ConnectGuard cg = connectGuard;
      if( cg != null ) {
        cg.cancel();
      } else {
        logger.warn("Unexpected connect: ConnectGuard is null!");
        
      }
      changeClusterStateInternally(ClusterState.CONNECTED, true);
      startHeartBeat();
    }


    //anderer knoten ist nicht mehr vorhanden
    public void disconnect(String hostname, int port) {
      logger.debug("Other nodes sends disconnect-request");
      //zuk�nftig nicht mehr mit rmi-verbindungen zu diesem knoten aufmachen
      NodeConnectionParameters nc = getNodeConnection(hostname, port);
      removeRMIAdaptersForNode(nc);

      //clusterstatus update
      changeClusterStateInternally(ClusterState.DISCONNECTED_MASTER, true);
    }


    public void heartbeatPing() throws ClusterNodeOnlineButNotExpectingHeartbeatPingException {
      if (ClusterState.valueOf(clusterInstanceStorable.clusterstate) != ClusterState.CONNECTED) {
        logger.warn("Other node sent a heartbeat ping but own clusterState is " + clusterInstanceStorable.clusterstate
            + ". Possible cause: The other node has not been responding for too long and the local state has"
            + " already been changed.");
        throw new ClusterNodeOnlineButNotExpectingHeartbeatPingException();
      }
    }
    
    
    public void waiting() {
      logger.debug("Other nodes sends waiting-request");
      ConnectGuard cg = connectGuard;
      if( cg != null ) {
        cg.keepWaiting();
      } else {
        logger.info("Unexpected waiting: ConnectGuard is null!");
      }
    }


    public boolean readyForStateChange(ClusterState newState) throws RemoteException {
      return clusterStateChangeHandler.isReadyForChange(newState);
    }

  }
  
  /**
   * Pr�ft den InterConnect, indem ein Heartbeat-Ping verschickt wird. 
   * Kann von vielen Threads gleichzeitig gerufen werden, nur ein Thread ist aktiv, die anderen warten
   * dann auf das Ergebnis.
   */
  private class InterconnectChecker extends JoinedExecutor<Boolean> {
    private RMIClusterProvider rmiClusterProvider;

    public InterconnectChecker(RMIClusterProvider rmiClusterProvider) {
      this.rmiClusterProvider = rmiClusterProvider;
    }

    @Override
    protected Boolean executeInternal() {
      HeartBeatAlgorithm hba = new HeartBeatAlgorithm(rmiClusterProvider, 1000 );
      hba.checkOnce();
      return Boolean.TRUE;
    }
    
  }
  

  

  public static class ClusterNodeOnlineButNotExpectingHeartbeatPingException extends Exception {
    private static final long serialVersionUID = 1L;
  }


  @Persistable(tableName = RMIClusterRemoteNodeStorable.TABLE_NAME, primaryKey = RMIClusterRemoteNodeStorable.COL_CLUSTERID)
  public static class RMIClusterRemoteNodeStorable extends Storable<RMIClusterRemoteNodeStorable> {

    private static final long serialVersionUID = 1L;
    public static final String TABLE_NAME = "rmiclusternode";
    //FIXME mehr als einen weiteren knoten unterst�tzen indem man den remote knoten noch eine eigene id als PK verpasst.
    public static final String COL_CLUSTERID = "clusterinstanceid";
    public static final String COL_HOSTNAME = "hostname";
    public static final String COL_PORT = "port";
    public static final ResultSetReader<RMIClusterRemoteNodeStorable> reader =
        new ResultSetReader<RMIClusterRemoteNodeStorable>() {

          public RMIClusterRemoteNodeStorable read(ResultSet rs) throws SQLException {
            RMIClusterRemoteNodeStorable rns = new RMIClusterRemoteNodeStorable();
            rns.clusterinstanceid = rs.getLong(COL_CLUSTERID);
            rns.hostname = rs.getString(COL_HOSTNAME);
            rns.port = rs.getInt(COL_PORT);
            return rns;
          }

        };


    @Column(name = COL_CLUSTERID)
    private long clusterinstanceid;

    @Column(name = COL_HOSTNAME)
    private String hostname;

    @Column(name = COL_PORT)
    private int port;


    public RMIClusterRemoteNodeStorable() {
    }


    public RMIClusterRemoteNodeStorable(long clusterinstanceid) {
      this.clusterinstanceid = clusterinstanceid;
    }


    public RMIClusterRemoteNodeStorable(long clusterinstanceid, String hostname, int port) {
      this.clusterinstanceid = clusterinstanceid;
      this.hostname = hostname;
      this.port = port;
    }


    @Override
    public Object getPrimaryKey() {
      return clusterinstanceid;
    }


    public long getClusterinstanceid() {
      return clusterinstanceid;
    }


    public String getHostname() {
      return hostname;
    }


    public int getPort() {
      return port;
    }


    @Override
    public ResultSetReader<? extends RMIClusterRemoteNodeStorable> getReader() {
      return reader;
    }


    @Override
    public <U extends RMIClusterRemoteNodeStorable> void setAllFieldsFromData(U data) {
      RMIClusterRemoteNodeStorable cast = data;
      clusterinstanceid = cast.clusterinstanceid;
      hostname = cast.hostname;
      port = cast.port;
    }

  }

  @Persistable(tableName = RMIClusterInstanceStorable.TABLE_NAME, primaryKey = RMIClusterInstanceStorable.COL_ID)
  public static class RMIClusterInstanceStorable extends Storable<RMIClusterInstanceStorable> {

    private static final long serialVersionUID = 1L;
    public static final String TABLE_NAME = "rmiclusterinstance";
    public static final String COL_ID = "id";
    public static final String COL_HOSTNAME = "hostname";
    public static final String COL_PORT = "port";
    public static final String COL_CLUSTERSTATE = "clusterstate";
    public static final String COL_ONLINE = "online";
    public static final String COL_HEARTBEAT_INTERVAL = "heartbeatinterval";

    private static ResultSetReader<RMIClusterInstanceStorable> reader =
        new ResultSetReader<RMIClusterInstanceStorable>() {

          public RMIClusterInstanceStorable read(ResultSet rs) throws SQLException {
            RMIClusterInstanceStorable rmici = new RMIClusterInstanceStorable();
            rmici.id = rs.getLong(COL_ID);
            rmici.hostname = rs.getString(COL_HOSTNAME);
            rmici.port = rs.getInt(COL_PORT);
            rmici.clusterstate = rs.getString(COL_CLUSTERSTATE);
            rmici.online = rs.getBoolean(COL_ONLINE);
            return rmici;
          }

        };

    @Column(name = COL_ID)
    private long id;

    @Column(name = COL_HOSTNAME)
    private String hostname;

    @Column(name = COL_PORT)
    private int port;

    @Column(name = COL_CLUSTERSTATE)
    private String clusterstate;

    @Column(name = COL_ONLINE)
    private boolean online;

    @Column(name = COL_HEARTBEAT_INTERVAL)
    private int heartBeatInterval;


    public RMIClusterInstanceStorable() {
    }


    public RMIClusterInstanceStorable(long id, String hostname, int port) {
      this.id = id;
      this.hostname = hostname;
      this.port = port;
      online = true;
      clusterstate = ClusterState.NO_CLUSTER.toString();
    }


    protected RMIClusterInstanceStorable(long id) {
      this.id = id;
    }


    @Override
    public Object getPrimaryKey() {
      return id;
    }


    public long getId() {
      return id;
    }


    public String getHostname() {
      return hostname;
    }


    public int getPort() {
      return port;
    }


    public String getClusterstate() {
      return clusterstate;
    }


    public boolean isOnline() {
      return online;
    }


    public int getHeartbeatinterval() {
      return heartBeatInterval;
    }


    @Override
    public ResultSetReader<? extends RMIClusterInstanceStorable> getReader() {
      return reader;
    }


    @Override
    public <U extends RMIClusterInstanceStorable> void setAllFieldsFromData(U data) {
      RMIClusterInstanceStorable cast = data;
      id = cast.id;
      hostname = cast.hostname;
      port = cast.port;
      clusterstate = cast.clusterstate;
      online = cast.online;
    }

  }

  private static class NodeConnectionParameters {

    private final String hostname;
    private final int port;


    public NodeConnectionParameters(String hostname, int port) {
      if (hostname == null) {
        throw new IllegalArgumentException("hostname may not be null");
      }
      this.hostname = hostname;
      this.port = port;
    }


    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof NodeConnectionParameters)) {
        return false;
      }
      if (this == obj) {
        return true;
      }
      NodeConnectionParameters otherParams = (NodeConnectionParameters) obj;
      return hostname.equals(otherParams.hostname) && port == otherParams.port;
    }


    @Override
    public int hashCode() {
      return hostname.hashCode() + 17159 * port;
    }


  }


  private static class RegisteredRMIInterface {

    private final Map<NodeConnectionParameters, GenericRMIAdapter<? extends Remote>> rmiAdaptersForNodes;
    private final RMIImplProxy<?> rmiImplProxy;
    private final String rmiBindingName;


    public RegisteredRMIInterface(String rmiBindingName, RMIImplProxy<?> rmiImplProxy,
                                  Map<NodeConnectionParameters, GenericRMIAdapter<? extends Remote>> map) {
      this.rmiAdaptersForNodes = map;
      this.rmiImplProxy = rmiImplProxy;
      this.rmiBindingName = rmiBindingName;
    }
  }

  private static class HeartBeatAlgorithm
      implements
        Runnable,
        RMIRunnableNoException<Boolean,RMIClusterProviderInterconnectInterface> {

    private RMIClusterProvider clusterProvider;
    private int heartBeatInterval;
    private boolean heartBeatRunning = true;
    private boolean heartBeatFailed = false;
    private CheckInterconnect_RMIConnectionNotAvailableHandler noConHandler = 
        new CheckInterconnect_RMIConnectionNotAvailableHandler();

    public HeartBeatAlgorithm(RMIClusterProvider clusterProvider, int heartBeatInterval) {
      this.clusterProvider = clusterProvider;
      this.heartBeatInterval = heartBeatInterval;
    }


    public Boolean execute(RMIClusterProviderInterconnectInterface clusteredInterface) throws RemoteException {
      try {
        clusteredInterface.heartbeatPing();
        return Boolean.TRUE;
      } catch (ClusterNodeOnlineButNotExpectingHeartbeatPingException e) {
        // anderer knoten erreichbar, aber der erwartet nicht, dass man online ist. passiert z.b. wenn dieser knoten
        // hier l�ngere zeit "gehangen" hat. beim testen z.b. passiert, als man am profilen war und der knoten deshalb
        // nicht erreichbar gewesen ist. vergleiche bugz 12216.
        if (XynaFactory.getInstance().isShuttingDown()) {
          logger.info("Other cluster node is online but remote node does not expect heartbeat. "
              + "Since the local factory is shutting down anyway, no action is required.");
        } else {
          logger.warn("Other cluster node is online but remote node does not expect heartbeat."
              + " Possible cause: Remote detected timeout and is switching to a disconnected state."
              + " Switching to ClusterState DISCONNECTED");
          if(clusterProvider.isConnected()) {
            heartBeatFailed = true;
            clusterProvider.changeClusterState(ClusterState.DISCONNECTED);
          }
        }
        return Boolean.FALSE;
      }
    }


    public void run() {
      long counter = 0;
      logger.info("starting rmi heartbeat");
      try {
        Random r = new Random();
        while (heartBeatRunning) {
          ++counter;
          List<Boolean> result;
          if( logger.isTraceEnabled() ) {
            long start = System.currentTimeMillis();
            result = RMIClusterProviderTools.executeAndCumulateNoException(clusterProvider, clusterProvider.interconnectId, this, null, Boolean.FALSE );
            long end = System.currentTimeMillis();
            logger.trace( "Heartbeat ping "+ counter+" finished in "+(end-start)+" ms");
          } else {
            result = RMIClusterProviderTools.executeAndCumulateNoException(clusterProvider, clusterProvider.interconnectId, this, null, Boolean.FALSE );
          }
          if( ! result.get(0) ) {
            //HeartBeat-Ausf�hrung hat trotz Retries nicht geklappt. Daher muss die Factory auf DISCONNECTED wechseln
            if(clusterProvider.isConnected()) {
              clusterProvider.changeClusterState(ClusterState.DISCONNECTED);
            }
          }          
          
          try {
            //schl�ft zwischen 85 und 115% von heartBeatInterval
            Thread.sleep(Math.round(heartBeatInterval * (1 + (r.nextDouble() - 0.5) * 0.3)));
          } catch (InterruptedException e) {
            //wenn das unterbrochen werden soll, ist hoffentlich auch heartbeatrunning auf false gesetzt.
          }
        }
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.error("heartBeatExecutor had unexpected error", t);
      } finally {
        logger.info("Stopped RMI heartbeat.");
      }
    }


    public void stopHeartBeat() {
      heartBeatRunning = false;
    }

    private class CheckInterconnect_RMIConnectionNotAvailableHandler implements RMIConnectionNotAvailableHandler {
      
      public <I extends Remote> I getRmiImpl(RMIClusterProvider clusterInstance, GenericRMIAdapter<I> rmiAdapter)
          throws RMIConnectionDownException {
        int maxAttempts = RMI_RETRY_ATTEMPTS.get();
        RMIConnectionFailureException lastException = null;
        for( int r=0; r<maxAttempts; ++r ) {
          if( r > 0 ) {
            try {
              Thread.sleep(RMI_RETRY_WAIT.get()*1000L);
            } catch (InterruptedException e) {
              break; //Retries abbrechen
            } 
          }
          try {
            rmiAdapter.reconnect();
            return rmiAdapter.getRmiInterface();
          } catch (RMIConnectionFailureException e) {
            lastException = e;
            logger.info( "reconnect failed "+e.getCode() ); //FIXME RMIConnectionFailureException ist kaputte XynaException
          }
        }
        logger.warn("Can not connect to other node, changing cluster state to "+ ClusterState.DISCONNECTED, lastException);
        heartBeatFailed = true;
        clusterInstance.changeClusterState(ClusterState.DISCONNECTED); //FIXME: f�r mehr als 2 knoten stimmt das so nicht.
        throw new RMIConnectionDownException("Initiated cluster state change to "+ ClusterState.DISCONNECTED, lastException);
      }
    }
    
    
    public void checkOnce() {
      if( logger.isDebugEnabled() ) {
        logger.debug( "checkInterconnect with heartbeat-Ping");
      }
      try {
        List<Boolean> result = RMIClusterProviderTools.executeAndCumulateNoException(clusterProvider, clusterProvider.interconnectId, this, noConHandler, null, Boolean.FALSE );
        //liste kann leer sein, wenn kein anderer knoten mehr bekannt ist
        if (result.size() == 0 || !result.get(0)) {
          //HeartBeat-Ausf�hrung hat trotz Retries nicht geklappt. Daher muss die Factory auf DISCONNECTED wechseln
          if (logger.isInfoEnabled()) {
            logger.info("checkInterconnect returned " + (result.size() == 0 ? "no result." : "false."));
          }
          if (clusterProvider.isConnected()) {
            heartBeatFailed = true;
            clusterProvider.changeClusterState(ClusterState.DISCONNECTED);
          }
        }
      }
      catch (InvalidIDException e) {
        logger.error("heartBeatExecutor had unexpected error", e);
      }
    }
    
  }


  //ConcurrentHashMap, anstatt ConcurrentMap, weil weakly-consistent iterator benutzt wird. das garantiert ConcurrentMap nicht
  private ConcurrentHashMap<Long, RegisteredRMIInterface> rmiAdapters;
  private List<NodeConnectionParameters> nodeConnections; //andere knoten
  private AtomicLong maxId = new AtomicLong(1); //ids f�r die map der rmi-adapter. bei 1 anfangen zu z�hlen, damit zugriffe mit 0 bessere fehlermeldung bekommen
  private volatile boolean initialized = false;
  private long interconnectId; //die id f�r den rmiadapter f�r das interconnect aus der rmiAdapters-Map
  private RMIClusterProviderInterconnectInterface interconnectImpl;
  private ODS ods;
  private ClusterStateChangeHandler clusterStateChangeHandler;
  private RMIClusterInstanceStorable clusterInstanceStorable;
  private HeartBeatAlgorithm heartBeatRunnable;
  private final Object heartBeatLock = new Object();
  private Connector connector;


  public RMIClusterProvider() {
    rmiAdapters = new ConcurrentHashMap<Long, RegisteredRMIInterface>();
    nodeConnections = new ArrayList<NodeConnectionParameters>();
    interconnectImpl = new RMIInterconnectImpl();
    ods = ODSImpl.getInstance();
    try {
      ods.registerStorable(RMIClusterRemoteNodeStorable.class);
      ods.registerStorable(RMIClusterInstanceStorable.class);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Failed to register RMIClusterRemoteNodeStorable",e);
    }
    interconnectChecker = new InterconnectChecker(this);
    RMI_RETRY_ATTEMPTS.registerDependency(RMIClusterProvider.class.getSimpleName());
    RMI_RETRY_WAIT.registerDependency(RMIClusterProvider.class.getSimpleName());
  }


  private static <T extends Remote> GenericRMIAdapter<T> buildRMIAdapter(String hostname, int port,
                                                                         String rmiBindingName) {
    try {
      return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRMIManagement()
          .<T> createRMIAdapter(hostname, port, rmiBindingName);
    } catch (RMIConnectionFailureException e) {
      throw new RuntimeException("connect must happen later", e);
    }
  }


  /**
   * bindet das implobjekt lokal und erstellt rmiadapter f�r das entsprechende remote-objekt an allen registrierten
   * knoten
   */
  public <T extends Remote> long addRMIInterface(String rmiBindingName, T remoteImpl) {
    try {
      return addRMIInterface(rmiBindingName, remoteImpl, false);
    } catch (XMCP_RMI_BINDING_ERROR e) {
      //sollte nie passieren, weil false �bergeben wird
      throw new RuntimeException(e);
    }
  }


  /**
   * entfernt eine RMI-Schnittstelle. Das zugeh�rige RemoteImpl Objekt wird dabei aus der Registry entfernt.
   * Falls clusterprovider bereits disconnected wurde, passiert einfach gar nichts.
   * @param timeoutMillis wie lange soll gewartet werden (millisekunden), bis aktuell laufende methoden-aufrufe abgebrochen
   *          werden
   */
  public void removeRMIInterface(long rmiInterfaceId, long timeoutMillis) {
    if (!initialized) {
      return;
    }
    RegisteredRMIInterface rri;
    synchronized (rmiAdapters) {
      rri = rmiAdapters.remove(rmiInterfaceId);
      if (rri == null) {
        throw new IllegalArgumentException("rmi interface id " + rmiInterfaceId + " unknown");
      }
    }
    if (timeoutMillis == 0) {
      rri.rmiImplProxy.unregister(true);
      return;
    }
    long endTimeForTimeout = System.currentTimeMillis() + timeoutMillis;
    while (!rri.rmiImplProxy.unregister(false)) {
      long remainingTime = Math.min(50, Math.max(System.currentTimeMillis() - endTimeForTimeout, 0));
      if (remainingTime == 0) {
        rri.rmiImplProxy.unregister(true);
        return;
      }
      try {
        Thread.sleep(remainingTime);
      } catch (InterruptedException e) {
        rri.rmiImplProxy.unregister(true);
        return;
      }
    }
  }


  private <T extends Remote> long addRMIInterface(String rmiBindingName, T remoteImpl, boolean firstTryToCreateRegistry)
      throws XMCP_RMI_BINDING_ERROR {
    checkInitialized();
    RMIImplProxy<T> rmiProxy;
    try {
      rmiProxy =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRMIManagement()
              .createRMIImplProxy(remoteImpl, rmiBindingName, clusterInstanceStorable.hostname,
                                  clusterInstanceStorable.port);
    } catch (XMCP_RMI_BINDING_ERROR e) {
      //beim anlegen des clusterproviders wird das gecheckt. wenn es dann funktioniert hat, funktioniert es sp�ter auch, weil gleiche registry!
      if (firstTryToCreateRegistry) {
        throw e;
      } else {
        //sollte nicht passieren, weil registry anlegen vorher funktioniert hatte
        throw new RuntimeException(e);
      }
    }
    return addRMIInterface(rmiBindingName, rmiProxy);
  }


  private <T extends Remote> long addRMIInterface(String rmiBindingName, RMIImplProxy<T> remoteImplProxy) {
    long id = maxId.getAndIncrement();
    Map<NodeConnectionParameters, GenericRMIAdapter<? extends Remote>> map =
        new HashMap<NodeConnectionParameters, GenericRMIAdapter<? extends Remote>>();
    synchronized (nodeConnections) {
      for (NodeConnectionParameters nodeConnection : nodeConnections) {
        map.put(nodeConnection, buildRMIAdapter(nodeConnection.hostname, nodeConnection.port, rmiBindingName));
      }
    }
    RegisteredRMIInterface rri = new RegisteredRMIInterface(rmiBindingName, remoteImplProxy, map);
    rmiAdapters.put(id, rri);
    return id;
  }


  /**
   * unterst�tzt class-reloading in objekten, die �ber rmi versendet werden, indem das eigentliche rmi-impl mit eigenem
   * classloader reloaded wird. d.h. das ver�ffentlichte remote-objekt, wor�ber man �ber rmi ansprechbar ist, ist nicht
   * immer die gleiche objekt-instanz.<br>
   * ansonsten genauso wie {@link #addRMIInterface(String, Remote)}
   */
  public <T extends InitializableRemoteInterface & Remote> long addRMIInterfaceWithClassReloading(
                                                                                                  String rmiBindingName,
                                                                                                  RMIImplFactory<T> factory)
      throws XMCP_RMI_BINDING_ERROR {
    checkInitialized();
    RMIImplProxy<T> rmiProxy =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRMIManagement()
            .registerClassreloadableRMIImplFactory(factory, rmiBindingName, clusterInstanceStorable.hostname,
                                                   clusterInstanceStorable.port);
    return addRMIInterface(rmiBindingName, rmiProxy);
  }
  
  public static class InvalidIDException extends Exception {

    private static final long serialVersionUID = 1L;
    
  }

  public <T extends Remote> Collection<GenericRMIAdapter<T>> getRMIAdapters(long id) throws InvalidIDException {
    checkInitialized();
    RegisteredRMIInterface rrif = rmiAdapters.get(id);
    if (rrif == null) {
      throw new InvalidIDException();
    }
    synchronized (rrif.rmiAdaptersForNodes) {
      Collection<GenericRMIAdapter<? extends Remote>> rmiAdapters = rrif.rmiAdaptersForNodes.values();
      return (Collection) rmiAdapters;
    }
  }


  public <T extends Remote> GenericRMIAdapter<T> getRMIAdapterResponsibleForBinding(long id, int binding)
      throws RMIConnectionFailureException {
    checkInitialized();
    // TODO SPS retrieve from rmiAdapaters the one with LocalBinding == binding
    throw new RuntimeException("currently unsupported");
  }


  public void setClusterStateChangeHandler(ClusterStateChangeHandler cscHandler) {
    checkInitialized();
    clusterStateChangeHandler = cscHandler;
  }


  private void unregisterFromRMIRegistry(Iterator<RegisteredRMIInterface> iter, boolean force) {
    while (iter.hasNext()) {
      RegisteredRMIInterface next = iter.next();
      if (next.rmiImplProxy.unregister(force)) {
        iter.remove();
      }
    }
  }


  public void disconnect() {
    checkInitialized();

    boolean heartBeatFailed = (initialized && heartBeatRunnable == null) ||
        (heartBeatRunnable != null && heartBeatRunnable.heartBeatFailed);
    
    ClusterState newState;
    ClusterState oldState = ClusterState.valueOf(clusterInstanceStorable.clusterstate);
    switch (oldState) {
      case CONNECTED : {
        stopHeartBeat();
        newState = ClusterState.DISCONNECTED_SLAVE;
        logger.info( "Sending disconnect request to other nodes");
        try {
          RMIClusterProviderTools
              .executeNoException(this, interconnectId,
                                  new RMIRunnableNoResultNoException<RMIClusterProviderInterconnectInterface>() {

                                    public void execute(RMIClusterProviderInterconnectInterface clusteredInterface)
                                        throws RemoteException {
                                      clusteredInterface.disconnect(clusterInstanceStorable.hostname,
                                                                    clusterInstanceStorable.port);
                                    }

                                  });
        } catch (InvalidIDException e) {
          throw new RuntimeException(e);
        }
        break;
      }
      default :
        newState = oldState;
    }
    changeClusterStateInternally(newState, false);

    initialized = false;
    if( heartBeatFailed ) {
      //kein unexportRMIObjects, da RMI -Connection nicht mehr funktionieren und der Unexport daher lange auf 
      //Timeouts warten muss 
    } else {
      unexportRMIObjects();
    }
  }


  private void unexportRMIObjects() {
    //unexport objects von rmi
    if (rmiAdapters.size() > 0) {
      Iterator<RegisteredRMIInterface> iter = rmiAdapters.values().iterator();
      unregisterFromRMIRegistry(iter, false);
      if (rmiAdapters.size() > 0) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          logger.warn("Got interrupted unexpectedly", e);
        }
        iter = rmiAdapters.values().iterator();
        unregisterFromRMIRegistry(iter, true);
        if (rmiAdapters.size() > 0) {
          StringBuilder logMsg =
              new StringBuilder().append("There ").append(
                                                          rmiAdapters.size() == 1 ? " is 1 " : " are "
                                                              + rmiAdapters.size() + " ").append("remote object");
          if (rmiAdapters.size() > 1) {
            logMsg.append("s");
          }
          logMsg.append(" that could not be unexported: ");

          iter = rmiAdapters.values().iterator();
          while (iter.hasNext()) {
            RegisteredRMIInterface next = iter.next();
            logMsg.append(next.rmiBindingName);
            if (iter.hasNext()) {
              logMsg.append(", ");
            }
          }
          logger.warn(logMsg);
        }
      }
    }
  }


  public ClusterInformation getInformation() {
    ClusterInformation ci = new ClusterInformation(XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER, TYPENAME);
    
    StringBuilder sb = new StringBuilder();
    sb.append("hostname=").append(clusterInstanceStorable.hostname).append(",\nport=")
        .append(clusterInstanceStorable.port);
    ci.setOwnNodeInformation(sb.toString());
    sb.append("\n");
    sb.append("knows ").append(nodeConnections.size()).append(" other node");
    if (nodeConnections.size() != 1) {
      sb.append("s");
    }
    if (nodeConnections.size() > 0) {
      sb.append(":\n");
      List<String> otherNodes = new ArrayList<String>();
      synchronized (nodeConnections) {
        for (NodeConnectionParameters otherNode : nodeConnections) {
          String other = otherNode.hostname+":"+otherNode.port;
          otherNodes.add(other);
          sb.append("  -").append(other).append("\n");
        }
      }
      ci.setOtherNodeInformation(otherNodes);
    } else {
      sb.append(".");
    }
    ci.setExtendedInformation(sb.toString());
    ci.setClusterState(getState());
    return ci;
  }


  public boolean isConnected() {
    return initialized;
  }


  private boolean addNodeInternally(String remoteHostname, int remotePort) {
    NodeConnectionParameters nc = new NodeConnectionParameters(remoteHostname, remotePort);
    synchronized (nodeConnections) {
      if (nodeConnections.contains(nc)) {
        return false;
      }
      nodeConnections.add(nc);
    }
    addRMIAdaptersForNode(nc);
    return true;
  }


  private void addRMIAdaptersForNode(NodeConnectionParameters nc) {
    for (RegisteredRMIInterface rri : rmiAdapters.values()) {
      synchronized (rri.rmiAdaptersForNodes) {
        rri.rmiAdaptersForNodes.put(nc, buildRMIAdapter(nc.hostname, nc.port, rri.rmiBindingName));
      }
    }
  }


  private void removeRMIAdaptersForNode(NodeConnectionParameters nc) {
    for (RegisteredRMIInterface rrif : rmiAdapters.values()) {
      synchronized (rrif.rmiAdaptersForNodes) {
        Iterator<NodeConnectionParameters> it = rrif.rmiAdaptersForNodes.keySet().iterator();
        while (it.hasNext()) {
          NodeConnectionParameters node = it.next();
          if (node == nc) {
            it.remove();
          }
        }
      }
    }
  }


  private NodeConnectionParameters getNodeConnection(String hostname, int port) {
    synchronized (nodeConnections) {
      for (NodeConnectionParameters nc : nodeConnections) {
        if (nc.hostname.equals(hostname) && nc.port == port) {
          return nc;
        }
      }
    }
    throw new RuntimeException("nodeconnection not found");
  }


  public void changeClusterState(ClusterState newState) {
    checkInitialized();
    if (newState == ClusterState.DISCONNECTED_MASTER || newState == ClusterState.DISCONNECTED_SLAVE) {
      if (logger.isDebugEnabled()) {
        logger.debug("cluster state was set specifically to " + newState + ".");
      }
    } else if (newState == ClusterState.DISCONNECTED) {
      //ok, passiert beim connection-loss
    } else {
      throw new RuntimeException("rmi cluster provider may not change state to " + newState + " manually");
    }
    changeClusterStateInternally(newState, true);
  }


  public String getStartParameterInformation() {
    return "RMI port: port on which the rmi registry accepts requests\n"
        + "own hostname/ip: under which hostname or ip do other cluster nodes reach this node\n"
        + "[optional: heartBeatInterval in milliseconds]";
  }


  public String getNodeConnectionParameterInformation() {
    return "remote hostname: hostname where the existing node can be reached\n"
        + "remote RMI port: port on which the existing node can be reached\n"
        + "own hostname/ip: under which hostname or ip do other cluster nodes reach this node\n"
        + "local RMI port: port where the rmi registry accepts requests"
        + "[optional: heartBeatInterval in milliseconds]";
  }


  public String getTypeName() {
    return TYPENAME;
  }


  private void checkInitialized() {
    if (!initialized) {
      throw new RuntimeException("rmi cluster provider not initialized");
    }
  }


  public int getLocalBinding() {
    throw new RuntimeException("bindings are not suppported by <" + getClass().getSimpleName() + ">");
  }


  private void createInterconnect() throws XMCP_RMI_BINDING_ERROR {
    //interconnect interface aufmachen
    interconnectId = addRMIInterface(INTERCONNECT_BINDING, interconnectImpl, true);
  }


  private void startHeartBeat() {
    synchronized (heartBeatLock) {
      if (heartBeatRunnable == null) {
        int heartBeatInterval = clusterInstanceStorable.getHeartbeatinterval();
        if (heartBeatInterval <= 0) {
          heartBeatInterval = 9120;
        }
        heartBeatRunnable = new HeartBeatAlgorithm(this, heartBeatInterval);
        Thread heartBeatThread = new Thread(heartBeatRunnable, "HeartBeatExecutor");
        heartBeatThread.setDaemon(true);
        heartBeatThread.start();
      }
    }
  }


  private void stopHeartBeat() {
    synchronized (heartBeatLock) {
      if (heartBeatRunnable != null) {
        heartBeatRunnable.stopHeartBeat();
        heartBeatRunnable = null;
      }
    }
  }


  public synchronized long createCluster(String[] startParameters)
      throws XFMG_InvalidStartParametersForClusterProviderException, XFMG_ClusterInitializationException {
    //rmi port
    if (startParameters == null || startParameters.length < 2 || startParameters.length > 3) {
      throw new XFMG_InvalidStartParametersForClusterProviderException(getTypeName());
    }
    int port;
    try {
      port = Integer.valueOf(startParameters[0]);
    } catch (NumberFormatException e) {
      throw new XFMG_InvalidStartParametersForClusterProviderException(getTypeName(), e);
    }
    String ownHostname = startParameters[1];
    try {
      InetAddress.getByName(ownHostname);
    } catch (UnknownHostException e) {
      throw new XFMG_InvalidStartParametersForClusterProviderException(getTypeName(), e);
    }
    int heartBeatInterval = 100;
    if (startParameters.length >= 3) {
      try {
        heartBeatInterval = Integer.valueOf(startParameters[2]);
      } catch (NumberFormatException e) {
        throw new XFMG_InvalidStartParametersForClusterProviderException(getTypeName(), e);
      }
    }

    initialized = true;
    clusterInstanceStorable = new RMIClusterInstanceStorable(getInternalClusterId(), ownHostname, port);
    clusterInstanceStorable.heartBeatInterval = heartBeatInterval;

    try {
      createInterconnect();
    } catch (XMCP_RMI_BINDING_ERROR e) {
      throw new XFMG_ClusterInitializationException(getTypeName(), e);
    }

    //speichern
    changeClusterStateInternally(ClusterState.SINGLE, true);
    return clusterInstanceStorable.id;
  }


  private long getInternalClusterId() {
    //FIXME SPS korrekte internalclusterId zur�ckgeben
    return 1;
  }


  private void changeClusterStateInternally(ClusterState clusterState, boolean online) {
    ClusterState oldState = ClusterState.valueOf(clusterInstanceStorable.clusterstate);
    boolean oldOnline = clusterInstanceStorable.online;
    clusterInstanceStorable.clusterstate = clusterState.toString();
    clusterInstanceStorable.online = online;
    if (oldState == clusterState && oldOnline == online) {
      return;
    }
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      try {
        con.persistObject(clusterInstanceStorable);
        con.commit();
      } finally {
        con.closeConnection();
      }
    } catch (PersistenceLayerException e) {
      logger.warn("could not persist new clusterstate", e);
    }
    if (clusterState != ClusterState.CONNECTED) {
      stopHeartBeat();
    }
    if (clusterStateChangeHandler != null && oldState != clusterState) {
      clusterStateChangeHandler.onChange(clusterState);
    }
  }


  private boolean registerNodeAtRemoteCluster(String remoteHostname, int remotePort, final boolean registerNodeForTheFirstTime)
      throws XFMG_ClusterConnectionException {

    final AtomicBoolean result = new AtomicBoolean(false); //FIXME das funktioniert so nur f�r einen anderen knoten: unklar wie man mit gr��eren clustern umgeht.

    try {
      new Thread(new ConnectGuard(CONNECTGUARD_TIMEOUT * 1000), "RMI-ConnectGuard-"
          + (registerNodeForTheFirstTime ? "join" : "startup")).start();
      //bei den anderen knoten muss man sich nun selbst auch registrieren
      RMIClusterProviderTools
          .execute(this, interconnectId,
        new RMIRunnableNoResult<RMIClusterProviderInterconnectInterface, XFMG_ClusterConnectionException>() {

          public void execute(RMIClusterProviderInterconnectInterface clusteredInterface)
          throws XFMG_ClusterConnectionException, RemoteException {
            //wird nur ausgef�hrt, wenn anderer knoten erreichbar ist
            clusteredInterface.register(clusterInstanceStorable.hostname, clusterInstanceStorable.port, !registerNodeForTheFirstTime);
            result.set(true);
          }

        });
      
    } catch (InvalidIDException e) {
      throw new RuntimeException(e); //kann nicht passieren
    } finally {
      if (!result.get()) {
        ConnectGuard cg = connectGuard;
        if (cg != null) {
          //connectguard canceln, weil anderer knoten nicht da ist oder ein anderer fehler passiert ist
          connectGuard.cancel();
        } else {
          logger.warn("Could not cancel RMI ConnectGuard.");
        }
      }
    }

    return result.get();
  }


  public long joinCluster(String[] connectionParameters)
      throws XFMG_InvalidConnectionParametersForClusterProviderException, XFMG_ClusterConnectionException {
    if (connectionParameters == null || connectionParameters.length != 4) {
      throw new XFMG_InvalidConnectionParametersForClusterProviderException(getTypeName());
    }
    String remoteHostname = connectionParameters[0];
    try {
      InetAddress.getByName(remoteHostname);
    } catch (UnknownHostException e) {
      throw new XFMG_InvalidConnectionParametersForClusterProviderException(getTypeName(), e);
    }
    int remotePort;
    try {
      remotePort = Integer.valueOf(connectionParameters[1]);
    } catch (NumberFormatException e) {
      throw new XFMG_InvalidConnectionParametersForClusterProviderException(getTypeName(), e);
    }

    String ownHostname = connectionParameters[2];
    try {
      InetAddress.getByName(ownHostname);
    } catch (UnknownHostException e) {
      throw new XFMG_InvalidConnectionParametersForClusterProviderException(getTypeName(), e);
    }
    int port;
    try {
      port = Integer.valueOf(connectionParameters[3]);
    } catch (NumberFormatException e) {
      throw new XFMG_InvalidConnectionParametersForClusterProviderException(getTypeName(), e);
    }
    int heartBeatInterval = 0;
    if (connectionParameters.length >= 5) {
      try {
        heartBeatInterval = Integer.valueOf(connectionParameters[4]);
      } catch (NumberFormatException e) {
        throw new XFMG_InvalidConnectionParametersForClusterProviderException(getTypeName(), e);
      }
    }

    initialized = true;
    clusterInstanceStorable = new RMIClusterInstanceStorable(getInternalClusterId(), ownHostname, port);
    clusterInstanceStorable.heartBeatInterval = heartBeatInterval;

    try {
      createInterconnect();
    } catch (XMCP_RMI_BINDING_ERROR e) {
      throw new XFMG_ClusterConnectionException(e);
    }

    addNodeInternally(remoteHostname, remotePort);
    if (!registerNodeAtRemoteCluster(remoteHostname, remotePort, true)) {
      unexportRMIObjects();
      initialized = false;
      clusterInstanceStorable = null;
      throw new XFMG_ClusterConnectionException();
    }

    changeClusterStateInternally(ClusterState.STARTING, true);
    //speichern
    try {
      saveOtherNode(remoteHostname, remotePort);
    } catch (PersistenceLayerException e) {
      throw new XFMG_ClusterConnectionException(e);
    }

    return clusterInstanceStorable.id;
  }


  private void saveOtherNode(String remoteHostname, int remotePort) throws PersistenceLayerException {
    RMIClusterRemoteNodeStorable storable =
        new RMIClusterRemoteNodeStorable(clusterInstanceStorable.id, remoteHostname, remotePort);
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.persistObject(storable);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  public void restoreClusterConnect() {
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        Collection<RMIClusterRemoteNodeStorable> otherNodes = con.loadCollection(RMIClusterRemoteNodeStorable.class);
        if (otherNodes.size() > 0) {
          //FIXME SPS nur die remote nodes mit gleicher internalclusterid verwenden.

          //cluster connect versuchen
          RMIClusterRemoteNodeStorable otherNode = otherNodes.iterator().next();
          try {
            if (registerNodeAtRemoteCluster(otherNode.getHostname(), otherNode.getPort(), false)) {
              //alter state und ob ein crash vorlag ist hier unerheblich.
              //kurz darauf meldet sich hoffentlich der andere Knoten und es gibt einen �bergang nach CONNECTED
            } else {
              changeClusterStateInternally(ClusterState.DISCONNECTED, true);
            }
          } catch (XFMG_ClusterConnectionException e) {
            //FIXME sollte nicht passieren => evtl register methode in zwei methoden aufteilen mit unterschiedlichen exceptions
            throw new RuntimeException("should not happen", e);
          }

          //FIXME mehr als 1 anderer knoten behandeln...
        } else {
          changeClusterStateInternally(ClusterState.SINGLE, true);
        }
      } finally {
        con.closeConnection();
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e); //should not happen
    }
  }


  public void restoreClusterPrepare(long internalClusterId) throws XFMG_ClusterInitializationException {
    initialized = true;

    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      try {
        Collection<RMIClusterRemoteNodeStorable> otherNodes = con.loadCollection(RMIClusterRemoteNodeStorable.class);
        for(RMIClusterRemoteNodeStorable node : otherNodes) {
          addNodeInternally(node.getHostname(), node.getPort());
        }
        
        clusterInstanceStorable = new RMIClusterInstanceStorable(internalClusterId);
        con.queryOneRow(clusterInstanceStorable);

        try {
          createInterconnect();
        } catch (XMCP_RMI_BINDING_ERROR e) {
          throw new XFMG_ClusterInitializationException(getTypeName(), e);
        }
        ClusterState oldState = ClusterState.valueOf(clusterInstanceStorable.clusterstate);
        ClusterState newState = ClusterState.STARTING;
        if( oldState == ClusterState.CONNECTED ) {
          if (logger.isDebugEnabled()) {
            logger.debug("detected crash. new state is " + newState);
          }
        }
        changeClusterStateInternally(newState, true);
      } finally {
        con.closeConnection();
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XFMG_ClusterInitializationException(getTypeName(), e);
    } catch (PersistenceLayerException e) {
      throw new XFMG_ClusterInitializationException(getTypeName(), e);
    }

  }


  public void leaveCluster() {
    // FIXME SPS leaveCluster f�r RMI implementieren
  }


  public ClusterState getState() {
    if (initialized) {
      return ClusterState.valueOf(clusterInstanceStorable.clusterstate);
    } else {
      return ClusterState.NO_CLUSTER;
    }
  }


  public List<Integer> getAllBindingsIncludingLocal() {
    throw new RuntimeException("unsupported");
  }


  /**
   * Pr�ft den InterConnect, indem ein Heartbeat-Ping verschickt wird.
   * Kann von vielen Threads gleichzeitig gerufen werden, nur ein Thread ist aktiv, die anderen warten
   * dann auf das Ergebnis.
   */
  public void checkInterconnect() {
    if( logger.isDebugEnabled() ) {
      logger.debug( "checkInterconnect");
    }
    try {
      interconnectChecker.execute();
    } catch (InterruptedException e) {
      //dann wird check halt abgebrochen
    }
  }

  public void readyForStateChange() {
    Connector c = connector;
    if( c != null ) {
      c.readyForStateChange();
    }
  }

  @Override
  public String toString() {
    return "RMIClusterProvider("
      +getState()+", "
      +nodeConnections.size()+" other nodes"+
      ")";
  }


  public boolean fastCheckIsMediumReachable() {
    throw new RuntimeException("unsupported");
  }

}

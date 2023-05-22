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

import java.net.SocketTimeoutException;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.db.utils.RepeatedExceptionCheck;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.rmi.GenericRMIAdapter;
import com.gip.xyna.xact.rmi.RMIConnectionFailureException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider.InvalidIDException;

/**
 * Sammlung aller ben�tigten Daten <code>(clusterInstance, rmiInterfaceId, runnable)</code> 
 * sowie der Spezialisierungen 
 * <code>(localImpl, connectionFailureValue, noConHandler, addConnectionFailureValue)</code> mit Defaults.
 * Eigentliche Ausf�hrung �ber {@link #executeAndCumulate()} bzw. {@link #executeAndCumulateNoException()}
 *
 * @param <R> Result
 * @param <I> RMI-Interface
 * @param <E> XynaException
 */
public abstract class RMIRetryExecutor<R, I extends Remote, E extends XynaException> {

  private static final Logger logger = CentralFactoryLogging.getLogger(RMIRetryExecutor.class);
  
  private static DefaultRMIConnectionNotAvailableHandler defNoConHandler = new DefaultRMIConnectionNotAvailableHandler();
  
  private RMIClusterProvider clusterInstance;
  private long rmiInterfaceId;
  
  protected String name;
  
  private I localImpl = null;
  private R connectionFailureValue = null;
  private RMIConnectionNotAvailableHandler noConHandler = defNoConHandler;
  private boolean addConnectionFailureValue = false;
  
  public interface RMIConnectionNotAvailableHandler {

    public <I extends Remote> I getRmiImpl(RMIClusterProvider clusterInstance, GenericRMIAdapter<I> rmiAdapter) throws RMIConnectionDownException;
    
  }
  
  protected RMIRetryExecutor() {
  }
  
  /**
   * Ausf�hrung der gew�nschten Methode auf dem �bergebenen rmiInterface
   * @param rmiInterface
   * @return
   * @throws E
   * @throws RemoteException
   */
  public abstract R execute(I rmiInterface) throws E, RemoteException;

  public RMIRetryExecutor<R,I,E> clusterInstance(RMIClusterProvider clusterInstance) {
    this.clusterInstance = clusterInstance;
    return this;
  }
  public RMIRetryExecutor<R,I,E> rmiInterfaceId(long rmiInterfaceId) {
    this.rmiInterfaceId = rmiInterfaceId;
    return this;
  }
  public RMIRetryExecutor<R,I,E> localImpl(I localImpl) {
    this.localImpl = localImpl;
    return this;
  }
  public RMIRetryExecutor<R,I,E> connectionFailureValue(R connectionFailureValue) {
    this.connectionFailureValue = connectionFailureValue;
    this.addConnectionFailureValue = true;
    return this;
  }
  public RMIRetryExecutor<R,I,E> noConHandler(RMIConnectionNotAvailableHandler noConHandler) {
    this.noConHandler = noConHandler;
    return this;
  }

  /**
   * Ausf�hren des Remote-Aufrufs und evtl. des lokalen Aufrufs, Sammeln der Ergebnisse in einer List
   * @return
   * @throws E
   * @throws InvalidIDException
   */
  public List<R> executeAndCumulate() throws E, InvalidIDException {
    List<R> result = new ArrayList<R>();

    // TODO lokalen call und remote calls parallel in mehreren threads ausf�hren
    if (localImpl != null) {
      addLocalValue(result);
    }

    for (GenericRMIAdapter<I> rmiAdapter : clusterInstance.<I> getRMIAdapters(rmiInterfaceId) ) {
      addRemoteValue(result, rmiAdapter);
    }
    return result;
  }
  
  /**
   * Ausf�hren des Remote-Aufrufs und evtl. des lokalen Aufrufs, Sammeln der Ergebnisse in einer List,
   * Unterdr�cken der nicht erwarteten XynaException 
   * @return
   * @throws InvalidIDException
   */
  public List<R> executeAndCumulateNoException() 
      throws InvalidIDException {
    try {
      return executeAndCumulate();
    } catch( XynaException e ) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Hinzuf�gen des lokalen Ergebnisses
   * @param result
   * @throws E
   */
  protected void addLocalValue(List<R> result) throws E {
    try {
      result.add( execute(localImpl));
    } catch (RemoteException e) {
      //sollte keine remote exception werfen, weil hier kein rmi-aufruf passiert.
      throw new RuntimeException(e);
    }
  }

  /**
   * Hinzuf�gen des remote Ergebnisses
   * @param result
   * @param rmiAdapter
   * @throws E
   */
  protected void addRemoteValue(List<R> result, GenericRMIAdapter<I> rmiAdapter) throws E {
    long start = logTimeStamp(0,null);
    try {
      R singleResult = executeWithRetries(rmiAdapter);
      result.add(singleResult);
    } catch (RemoteException e) {
      addFailureValue(result);
    } catch (RMIConnectionDownException e) {
      logger.info("executeWithRetries failed for "+name, e );
      addFailureValue(result);
    } finally {
      logTimeStamp(start, "executeWithRetries");
    }
  }

  /**
   * Hinzuf�gen des Fehler-Ergebnisses
   * @param result
   */
  protected void addFailureValue(List<R> result) {
    if (addConnectionFailureValue ) {
      result.add( connectionFailureValue );
    }
  }
  
  /**
   * Ausf�hren des Remote-Aufrufs, �ber Property konfigurierte Wiederholung im Fehlerfall
   * @param rmiAdapter
   * @return
   * @throws E
   * @throws RMIConnectionDownException
   * @throws RemoteException
   */
  protected R executeWithRetries(GenericRMIAdapter<I> rmiAdapter) throws E, RMIConnectionDownException, RemoteException {

    int maxAttempts = Math.max(0, RMIClusterProvider.RMI_RETRY_ATTEMPTS.get() );
    Exception lastException = null;
    
    for( int r=RETRY_CNT_START; r<maxAttempts; ++r ) { //Start bei -1: erste Ausf�hrung ist kein Retry!
      Pair<R,Exception> result = executeRetry( r, rmiAdapter );
      
      if( result == null ) {
        break;
      }
      if( result.getSecond() == null ) {
        return result.getFirst();
      } else {
        lastException = result.getSecond();
        if( r == RETRY_CNT_START ) {
          logger.info("Encountered problem within RMI interconnect for "+name+": "+ lastException);
        } else {
          logger.info( "Retry "+(r+1)+": Encountered problem within RMI interconnect for "+name+": "+ lastException);
        }
      }
    }

    //retries nicht erfolgreich, entweder konnte remoteInterface nicht erstellt werden
    //oder Retries wegen Exceptions waren nicht erfolgreich
    if( lastException == null ) {
      throw new RMIConnectionDownException();
    } else {
      throw new RMIConnectionDownException(lastException);
    }
  }

  private static final int RETRY_CNT_START = -1; //nicht �ndern, weil oben die logmeldung davon abh�ngt, dass das -1 ist

  //  k�nnte man �ndern:
  private static final int FIRST_RETRY_CNT__TO_SLEEP = 1;
  private static final int FIRST_RETRY_CNT__TO_RECONNECT = 0;
  
  /**
   * Remote-Ausf�hrung: Evtl. warten bei Retries; Holen des RMI-Interfaces; Ausf�hren des RMI-Calls
   * Wenn Fehler auftreten, wird die Exception entweder im Pair zur�ckgegeben, wenn ein Retry versucht werden soll, 
   * ansonsten wird sie geworfen.
   * 
   * @param retry
   * @param rmiAdapter
   * @return Pair&lt;R, Exception> null: Retries abbrechen, ansonsten Ergebnis oder Exceptiuon, die zum Retry f�hrt;
   * @throws RMIConnectionDownException
   * @throws RemoteException
   * @throws E
   */
  private Pair<R, Exception> executeRetry(int retry, GenericRMIAdapter<I> rmiAdapter) throws RMIConnectionDownException, RemoteException, E {
    long start = logTimeStamp(0,null);
    
    //bei Retries warten
    if( retry >= FIRST_RETRY_CNT__TO_SLEEP ) { //erst nach dem ersten Retry warten (FirstExec,Retry,Wait,Retry,Wait,Retry...)
      try {
        Thread.sleep(RMIClusterProvider.RMI_RETRY_WAIT.get()*1000L);
      } catch (InterruptedException e) {
        return null; //Retries abbrechen
      } finally {
        start = logTimeStamp(start, "sleep");
      }
    }
    
    //Holen der Remote-Implementierung des RMI-Interfaces
    I remoteImpl = null;
    try {
      remoteImpl = connectWithReconnectTrys(rmiAdapter, retry >= FIRST_RETRY_CNT__TO_RECONNECT);
    } finally {
      start = logTimeStamp(start, "connectWithReconnectTrys");
    }
    
    //Ausf�hren des RMI-Calls
    Pair<R,Exception> result = null;
    try {
      result = tryExecute(remoteImpl);
    } finally {
      start = logTimeStamp(start, "tryExecute");
    }
    
    return result;
  }


  /**
   * Loggen auf Trace, wie lange ein Remote-Aufruf dauert
   * @param start
   * @param what
   * @return
   */
  private long logTimeStamp(long start, String what) {
    if( logger.isTraceEnabled() ) {
      long now = System.currentTimeMillis();
      if( start != 0L ) { 
        logger.trace(name + ": "+what+" took "+(now-start)+" ms");
      }
      return now;  
    } else {
      return 0;
    }
  }
  
  /**
   * Ausf�hren des eigentlichen Remote-Calls: Exceptions die zum Retry f�hren werden gefangen und im Pair ausgegeben;
   * Fehler, bei denen ein Retry nicht sinnvoll ist, werden weitergeworfen. 
   * @param remoteImpl
   * @return Pair&lt;R,Exception>
   * @throws RemoteException
   * @throws RMIConnectionDownException
   * @throws E
   */
  private Pair<R,Exception> tryExecute(I remoteImpl) throws RemoteException, RMIConnectionDownException, E {
    try {
      R result = execute(remoteImpl);
      return Pair.of(result, null);
    } catch (NoSuchObjectException e) {
      /*
       * fehler passiert, wenn zb auf der gegenseite das exportierte objekt sich ge�ndert hat. regelm�ssig
       * passiert das beim deployment bei classreloadable-rmi-objekten, zb manualinteractionmgmt.
       * siehe auch bugz 12394
       * helfen tut in diesem fall eine neu-erstellung des remote stubs
       * 
       * java.rmi.NoSuchObjectException: no such object in table
       at sun.rmi.transport.StreamRemoteCall.exceptionReceivedFromServer(StreamRemoteCall.java:247)
       at sun.rmi.transport.StreamRemoteCall.executeCall(StreamRemoteCall.java:223)
       at sun.rmi.server.UnicastRef.invoke(UnicastRef.java:126)
       at java.rmi.server.RemoteObjectInvocationHandler.invokeRemoteMethod(RemoteObjectInvocationHandler.java:179)
       at java.rmi.server.RemoteObjectInvocationHandler.invoke(RemoteObjectInvocationHandler.java:132)
       at $Proxy22.processManualInteractionEntry(Unknown Source)
       at com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionManagement$2.execute(ManualInteractionManagement.java:351)
       */
      return Pair.of(null,(Exception)e);
    } catch (ConnectException e) {
      return Pair.of(null,(Exception)e);
    } catch (ConnectIOException e) { //detail : SocketTimeoutException
      return Pair.of(null,(Exception)e);
    } catch (RemoteException e) { //UnmarshalException detail: SocketTimeoutException
      if (e instanceof UnmarshalException && e.getCause() instanceof SocketTimeoutException) {
        return Pair.of(null,(Exception)e);
      } else {
        logger.error("got remote exception of type " + e.getClass().getName(), e);
        throw e;
      }
    } //TODO andere Exceptions brechen die Retries ab. Ist das ok?
  }

  /**
   * Verbindungsaufbau: Holen der Remote-Implementierung des RmiInterface.
   * Bei Fehlern wird ein unverz�glicher Retry probiert, weitere Retries muss 
   * der RMIConnectionNotAvailableHandler versuchen
   * @param rmiAdapter
   * @return
   * @throws RMIConnectionDownException
   */
  private I connectWithReconnectTrys(GenericRMIAdapter<I> rmiAdapter, boolean forceReconnect) throws RMIConnectionDownException {

    if (!forceReconnect) {
      //RmiInterface holen und zur�ckgeben
      try {
        return rmiAdapter.getRmiInterface();
      } catch (RMIConnectionFailureException e) {
        logger.info("connect to " + e.getCode() + " failed"); //FIXME RMIConnectionFailureException ist kaputte XynaException
      }
    }

    //nicht erfolgreich, daher unverz�glicher Reconnect
    try {
      rmiAdapter.reconnect();
      return rmiAdapter.getRmiInterface();
    } catch (RMIConnectionFailureException e) {
      logger.info("immediate reconnect to " + e.getCode() + " failed"); //FIXME RMIConnectionFailureException ist kaputte XynaException
    }

    //nicht erfolgreich, handler muss sich um das weitere Vorgehen k�mmern 
    return noConHandler.getRmiImpl(clusterInstance, rmiAdapter);
  }


  /**
   * Default-Implementierung des RMIConnectionNotAvailableHandler-Interfaces:
   * Es werden �ber Properties konfiguriert Retries ausgef�hrt.
   * Vor jedem Retry wird erst gewartet, dann ein 
   * {@link com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider#checkInterconnect()} ausgef�hrt.
   */
  private static class DefaultRMIConnectionNotAvailableHandler implements RMIConnectionNotAvailableHandler {

    private RepeatedExceptionCheck repeatedExceptionCheck; //TODO static? 
    
    public <I extends Remote> I getRmiImpl(RMIClusterProvider clusterInstance, GenericRMIAdapter<I> rmiAdapter) throws RMIConnectionDownException {
      //verz�gerte Reconnects und Anfragen an ClusterProvider
      
      int retryCounter = 0;
      ClusterState clusterState;
     
      int maxAttempts = RMIClusterProvider.RMI_RETRY_ATTEMPTS.get();
      for( ; retryCounter<maxAttempts; ++retryCounter ) {
        try {
          Thread.sleep(RMIClusterProvider.RMI_RETRY_WAIT.get()*1000L);
        } catch (InterruptedException e) {
          break; //Retries abbrechen
        }
        clusterInstance.checkInterconnect(); //wartet, bis InterConnect funktioniert hat oder clusterState nicht mehr CONNECTED ist
        clusterState = clusterInstance.getState();
        
        if( clusterState == ClusterState.CONNECTED) {
          try {
            rmiAdapter.reconnect();
            return rmiAdapter.getRmiInterface();
          } catch (RMIConnectionFailureException e) {
            if( repeatedExceptionCheck == null ) {
              repeatedExceptionCheck = new RepeatedExceptionCheck(true);
            }
            int rep = repeatedExceptionCheck.checkRepeationCount(e);
            String msg = "reconnect in retry "+(retryCounter+1)+" to "+ e.getCode()+" failed";
            if( rep == 0 ) {
              logger.info( msg , e ); //FIXME RMIConnectionFailureException ist kaputte XynaException
            } else {
              logger.info( msg + " again ("+rep+")");
            }
          }
        } else {
          break;
        }
      }
            
      clusterState = clusterInstance.getState();
      logger.info("connectWithReconnectTrys stopped after "+retryCounter+" retries, clusterState "+clusterState);
      if( clusterState == ClusterState.CONNECTED ) {
        //Retries haben nicht geklappt, dennoch hat Interconnect funktioniert. 
        //Der Cluster kann aber nicht in Ordnung sein, wenn hier trotz Retries keine Verbindung aufgebaut werden kann.
        //Daher Wechsel nach DISCONNECTED.
        clusterInstance.changeClusterState(ClusterState.DISCONNECTED);//wie in HeartBeatExecutor.checkOnce()
        throw new RMIConnectionDownException("maximum retries reached");
      } else {
        throw new RMIConnectionDownException("clusterState "+clusterState);
      }
    }
    
  }

  /**
   * Interne Exception, mit der ein Fehlschlagen des Aufbaus der RMI-Verbindung kommuniziert wird.
   */
  public static class RMIConnectionDownException extends Exception {
    private static final long serialVersionUID = 1L;
    
    public RMIConnectionDownException(String msg) {
      super(msg);
    }
    
    public RMIConnectionDownException(String msg,Throwable t ) {
      super(msg,t);
    }

    public RMIConnectionDownException(Throwable t) {
      super(t);
    }

    public RMIConnectionDownException() {
    }

  }

}

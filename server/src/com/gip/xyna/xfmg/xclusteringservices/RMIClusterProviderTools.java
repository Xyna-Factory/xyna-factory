/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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



import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xclusteringservices.RMIClusterProvider.InvalidIDException;
import com.gip.xyna.xfmg.xclusteringservices.RMIRetryExecutor.RMIConnectionNotAvailableHandler;



/**
 *
 */
public class RMIClusterProviderTools {

  static Logger logger = CentralFactoryLogging.getLogger(RMIClusterProviderTools.class);


  public interface RMIRunnable<O, I extends Remote, E extends XynaException> {

    public O execute(I clusteredInterface) throws E, RemoteException;
  }

  public interface RMIRunnableNoResult<I extends Remote, E extends XynaException> {

    public void execute(I clusteredInterface) throws E, RemoteException;
  }

  public interface RMIRunnableNoResultNoException<I extends Remote> {

    public void execute(I clusteredInterface) throws RemoteException;
  }

  public interface RMIRunnableNoException<O, I extends Remote> {

    public O execute(I clusteredInterface) throws RemoteException;
  }


  
  
  /**
   * führt das übergebene runnable für alle registrierten cluster members und für localImpl aus. falls verbindung nicht
   * hergestellt werden kann, wird changeClusterState(DISCONNECTED) aufgerufen, und der entsprechende knoten wird
   * übersprungen. es wird kein fehler geworfen.
   * @param <R> result type
   * @param <I> remote interface
   * @param <E> exception type
   * @param clusterInstance auf welcher cluster instanz
   * @param rmiInterfaceId welches bei der clusterinstanz registrierte rmi interface
   * @param runnable auszuführendes runnable
   * @param localImpl null falls nicht lokal ausgeführt werden soll
   * @throws E
   * @throws InvalidIDException
   */
  public static <R, I extends Remote, E extends XynaException> List<R> executeAndCumulate(final RMIClusterProvider clusterInstance,
                                                                                          long rmiInterfaceId,
                                                                                          RMIRunnable<R, I, E> runnable,
                                                                                          I localImpl 
      ) throws E, InvalidIDException {
    return rmiRetryExecutorForRunnable(runnable).clusterInstance(clusterInstance).rmiInterfaceId(rmiInterfaceId).
        localImpl(localImpl).
        executeAndCumulate();
  }
  
  /**
   * führt das übergebene runnable für alle registrierten cluster members und für localImpl aus. falls verbindung nicht
   * hergestellt werden kann, wird changeClusterState(DISCONNECTED) aufgerufen, und der entsprechende knoten wird
   * übersprungen. es wird kein fehler geworfen.
   * @param <R> result type
   * @param <I> remote interface
   * @param <E> exception type
   * @param clusterInstance auf welcher cluster instanz
   * @param rmiInterfaceId welches bei der clusterinstanz registrierte rmi interface
   * @param runnable auszuführendes runnable
   * @param localImpl null falls nicht lokal ausgeführt werden soll
   * @param connectionFailureValue wird im Fehlerfall (RemoteException, RMIConnectionDownException) in die ResultListe eingetragen
   * @throws E
   * @throws InvalidIDException
   */
  public static <R, I extends Remote, E extends XynaException> List<R> executeAndCumulate(final RMIClusterProvider clusterInstance,
                                                                                          long rmiInterfaceId,
                                                                                          RMIRunnable<R, I, E> runnable,
                                                                                          I localImpl,
                                                                                          R connectionFailureValue
      ) throws E, InvalidIDException {
    return rmiRetryExecutorForRunnable(runnable).clusterInstance(clusterInstance).rmiInterfaceId(rmiInterfaceId).
        localImpl(localImpl).connectionFailureValue(connectionFailureValue).
        executeAndCumulate();
  }
  
  
  public static <I extends Remote> void executeNoException(RMIClusterProvider clusterInstance,
                                                           long rmiInterfaceId,
                                                           RMIRunnableNoResultNoException<I> runnable)
      throws InvalidIDException {
    rmiRetryExecutorForRunnable(runnable).clusterInstance(clusterInstance).rmiInterfaceId(rmiInterfaceId).
      executeAndCumulateNoException();
  }
    

  public static <R, I extends Remote> List<R> executeAndCumulateNoException(RMIClusterProvider clusterInstance,
                                                                            long rmiInterfaceId,
                                                                            final RMIRunnableNoException<R, I> runnable,
                                                                            I localImpl
      ) throws InvalidIDException {
    return rmiRetryExecutorForRunnable(runnable).clusterInstance(clusterInstance).rmiInterfaceId(rmiInterfaceId).
        localImpl(localImpl).
        executeAndCumulateNoException();
  }


  public static <R, I extends Remote> List<R> executeAndCumulateNoException(RMIClusterProvider clusterInstance,
                                                                            long rmiInterfaceId,
                                                                            final RMIRunnableNoException<R, I> runnable,
                                                                            I localImpl, R connectionFailureValue
      ) throws InvalidIDException {
    return rmiRetryExecutorForRunnable(runnable).clusterInstance(clusterInstance).rmiInterfaceId(rmiInterfaceId).
        localImpl(localImpl).connectionFailureValue(connectionFailureValue).
        executeAndCumulateNoException();
  }

  public static <R, I extends Remote> List<R> executeAndCumulateNoException(RMIClusterProvider clusterInstance,
                                                                            long rmiInterfaceId,
                                                                            final RMIRunnableNoException<R, I> runnable,
                                                                            RMIConnectionNotAvailableHandler noConHandler,
                                                                            I localImpl, 
                                                                            R connectionFailureValue
      ) throws InvalidIDException {
    return rmiRetryExecutorForRunnable(runnable).clusterInstance(clusterInstance).rmiInterfaceId(rmiInterfaceId).
        localImpl(localImpl).connectionFailureValue(connectionFailureValue).noConHandler(noConHandler).
        executeAndCumulateNoException();
  }


  public static <I extends Remote, E extends XynaException> void execute(RMIClusterProvider clusterInstance,
                                                                         long rmiInterfaceId,
                                                                         RMIRunnableNoResult<I, E> runnable)
      throws E, InvalidIDException {
    rmiRetryExecutorForRunnable(runnable).clusterInstance(clusterInstance).rmiInterfaceId(rmiInterfaceId).
      executeAndCumulateNoException();
  }


  public static <I extends Remote, E extends XynaException> RMIRetryExecutor<Void,I,E> rmiRetryExecutorForRunnable(RMIRunnableNoResult<I,E> runnable) {
    return new RMIExecutionDataNoResult<I,E>(runnable);
  }

  public static <R, I extends Remote, E extends XynaException> RMIRetryExecutor<R,I,E> rmiRetryExecutorForRunnable(RMIRunnable<R,I,E> runnable) {
    return new RMIExecutionDataResult<R,I,E>(runnable);
  }

  public static <R, I extends Remote> RMIRetryExecutor<R,I,XynaException> rmiRetryExecutorForRunnable(RMIRunnableNoException<R,I> runnable) {
    return new RMIExecutionDataNoException<R,I>(runnable);
  }

  public static <I extends Remote> RMIRetryExecutor<Void,I,XynaException> rmiRetryExecutorForRunnable(RMIRunnableNoResultNoException<I> runnable) {
    return new RMIExecutionDataNoResultNoException<I>(runnable);
  }

  private static class RMIExecutionDataNoResult<I extends Remote, E extends XynaException> extends RMIRetryExecutor<Void,I,E> {

    private RMIRunnableNoResult<I,E> runnable;

    public RMIExecutionDataNoResult(RMIRunnableNoResult<I,E> runnable) {
      this.runnable = runnable;
      this.name = runnable.getClass().getName();
    }

    @Override
    public Void execute(I clusteredInterface) throws E, RemoteException {
      runnable.execute(clusteredInterface);
      return null;
    }

  }

  private static class RMIExecutionDataResult<R, I extends Remote, E extends XynaException> extends RMIRetryExecutor<R,I,E> {

    private RMIRunnable<R,I,E> runnable;

    public RMIExecutionDataResult(RMIRunnable<R,I,E> runnable) {
      this.runnable = runnable;
      this.name = runnable.getClass().getName();
    }

    @Override
    public R execute(I clusteredInterface) throws E, RemoteException {
      return runnable.execute(clusteredInterface);
    }

  }

  private static class RMIExecutionDataNoException<R, I extends Remote> extends RMIRetryExecutor<R,I,XynaException> {

    private RMIRunnableNoException<R,I> runnable;

    public RMIExecutionDataNoException(RMIRunnableNoException<R,I> runnable) {
      this.runnable = runnable;
      this.name = runnable.getClass().getName();
    }

    @Override
    public R execute(I clusteredInterface) throws RemoteException {
      return runnable.execute(clusteredInterface);
    }

  }

  private static class RMIExecutionDataNoResultNoException<I extends Remote> extends RMIRetryExecutor<Void,I,XynaException> {

    private RMIRunnableNoResultNoException<I> runnable;

    public RMIExecutionDataNoResultNoException(RMIRunnableNoResultNoException<I> runnable) {
      this.runnable = runnable;
      this.name = runnable.getClass().getName();
    }

    @Override
    public Void execute(I clusteredInterface) throws RemoteException {
      runnable.execute(clusteredInterface);
      return null;
    }

  }


  
}

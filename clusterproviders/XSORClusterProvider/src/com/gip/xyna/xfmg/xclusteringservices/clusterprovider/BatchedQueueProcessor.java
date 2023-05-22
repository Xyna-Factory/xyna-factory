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
package com.gip.xyna.xfmg.xclusteringservices.clusterprovider;



import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.xprc.xsched.Algorithm;
import com.gip.xyna.xprc.xsched.LazyAlgorithmExecutor;



/**
 * Batch-weise sequentielle Abarbeitung von Aufgaben in einem eigenen Thread. �ber die {@link ProcessingStrategy} kann
 * gesteuert werden, wann das n�chste Batch verarbeitet wird, und ob es synchron oder asynchron verarbeitet werden
 * soll. 
 * {@link ProcessingAction} definiert die eigentliche Verarbeitung eines Batches.
 * Aufgaben werden �ber {@link #add(Object)} hinzugef�gt.
 *
 * @param <E>
 * @param <F> Welchen Fehler die Verarbeitung werfen kann. Wird nur bei synchroner Verarbeitung geworfen, ansonsten geloggt.
 */
public class BatchedQueueProcessor<E, F extends Exception> {

  private static final Logger logger = CentralFactoryLogging.getLogger(BatchedQueueProcessor.class);
  private final LazyAlgorithmExecutor<ProcessingAlgorithm> lazyExecutor;
  private final List<E> buffer;
  private final ProcessingAction<E, F> action;
  private final ProcessingStrategy strategy;


  public interface ProcessingAction<E, F extends Exception> {

    /**
     * abarbeitung des n�chsten batches
     * @param requests
     * @return exception if any
     */
    public F exec(List<E> requestBatch);
  }

  public enum ProcessingType {
    DO_NOT_PROCESS, DO_PROCESS_SYNC, DO_PROCESS_ASYNC;
  }

  public interface ProcessingStrategy {

    /**
     * entscheidung, ob und wie jetzt abgearbeitet werden soll.<p>
     * es werden alle �nderungen abgearbeitet, die sich bis dahin angesammelt haben.
     */
    public ProcessingType decideProcessing();


    /**
     * @param sizeOfBufferInRunningJob anzahl von requests, die gerade vom laufenden job bearbeitet werden
     */
    public void onProcessingIsStillRunning(int sizeOfBufferInRunningJob);
  }


  public BatchedQueueProcessor(ProcessingAction<E, F> action, ProcessingStrategy strategy) {
    if (action == null) {
      throw new NullPointerException("action must not be null");
    }
    if (strategy == null) {
      throw new NullPointerException("strategy must not be null");
    }
    buffer = new ArrayList<E>();
    this.action = action;
    this.strategy = strategy;
    //alle 2 sek buffer checken, auch wenn keine neuen requests kommen -> konfigurierbar
    lazyExecutor = new LazyAlgorithmExecutor<ProcessingAlgorithm>(BatchedQueueProcessor.class.getSimpleName(), 2000);
    lazyExecutor.startNewThread(new ProcessingAlgorithm());
  }


  public void add(E e) throws F {
    synchronized (buffer) {
      buffer.add(e);
    }
    checkProcessing(true, false);
  }


  private class ProcessingAlgorithm implements Algorithm {

    private List<E> requests = new ArrayList<E>();

    private CountDownLatch latch;

    private boolean hasCopiedData = false;
    private F exception;


    public ProcessingAlgorithm(CountDownLatch latch) {
      this.latch = latch;
    }


    public ProcessingAlgorithm() {
    }


    public void exec() {

      try {
        exception = null;
        synchronized (buffer) {
          requests.addAll(buffer);
          buffer.clear();
          hasCopiedData = true;
        }
        if (requests.size() == 0) {
          return;
        }
        if (logger.isDebugEnabled()) {
          logger.debug("starting job with " + requests.size() + " requests.");
        }
        strategy.onProcessingIsStillRunning(requests.size());
        exception = action.exec(requests);
        strategy.onProcessingIsStillRunning(0);
        logger.debug("finished job");
      } catch (RuntimeException e) {
        logger.warn("could not execute some actions.", e);
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.error("severe error. could not execute some actions.", t);
      } finally {
        if (latch != null) {
          latch.countDown();
        } else {
          //asynchrone ausf�hrung
          if (exception != null) {
            logger.warn("exception occurred during asynchronous batch execution", exception);
          }
        }
        latch = null;
        requests.clear();
      }
    }


    public boolean hasCopiedData() {
      return hasCopiedData;
    }

  }


  private void checkProcessing(boolean checkStrategy, boolean sync) throws F {
    final ProcessingType doProcessing;
    CountDownLatch latch = null;
    synchronized (this) {
      if (checkStrategy) {
        doProcessing = strategy.decideProcessing();
        if (doProcessing == ProcessingType.DO_NOT_PROCESS) {
          return;
        }
      } else {
        doProcessing = sync ? ProcessingType.DO_PROCESS_SYNC : ProcessingType.DO_PROCESS_ASYNC;
      }
      if (doProcessing == ProcessingType.DO_PROCESS_SYNC) {
        synchronized (buffer) {
          if (lazyExecutor.getCurrentAlgorithm().hasCopiedData()
          // the latch can be null before the first execution
              || lazyExecutor.getCurrentAlgorithm().latch == null) {
            latch = new CountDownLatch(1);
            //latch �bergeben, wird beim n�chsten algorithmus-durchlauf dann benutzt.
            lazyExecutor.changeAlgorithm(new ProcessingAlgorithm(latch));
          } else {
            latch = lazyExecutor.getCurrentAlgorithm().latch;
          }
        }
      }
      lazyExecutor.requestExecution();
    }
    //ausserhalb des locks warten
    if (doProcessing == ProcessingType.DO_PROCESS_SYNC) {
      try {
        // latch kann nicht mehr null sein
        latch.await();
        if (lazyExecutor.getCurrentAlgorithm().exception != null) {
          throw lazyExecutor.getCurrentAlgorithm().exception;
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }


  public void persistRemainingDataSynchronously() throws F {
    checkProcessing(false, true);
  }


  public void persistRemainingDataASynchronously() throws F {
    checkProcessing(false, false);
  }


  public void clearRemainingData() {
    buffer.clear();
  }


  public long getCurrentlyWaitingRequests() {
    return buffer.size();
  }

}

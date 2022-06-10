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
package com.gip.xyna.coherence.coherencemachine;



import java.util.concurrent.CountDownLatch;

import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectProtocol;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;
import com.gip.xyna.coherence.utils.threadpool.RunnableWithThreadInformation;



public abstract class SynchronousRequest extends RunnableWithThreadInformation {

  protected final InterconnectProtocol nodeConnection;
  private CountDownLatch latch;
  private Throwable exception;
  private String failedThreadName;

  private boolean failed;
  private boolean objectWasRemoved;
  private boolean wasInterrupted;


  public SynchronousRequest(InterconnectProtocol nodeConnection, CountDownLatch latch) {
    this.nodeConnection = nodeConnection;
    this.latch = latch;
  }


  public abstract void exec() throws ObjectNotInCacheException, InterruptedException;


  public void run() {
    try {
      exec();
    } catch (ObjectNotInCacheException e) {
      objectWasRemoved = true;
    } catch (InterruptedException e) {
      wasInterrupted = true;
    } catch (Throwable t) {
      setFailed();
      exception = t;
      failedThreadName = Thread.currentThread().getName();
    } finally {
      latch.countDown();
    }
  }


  private void setFailed() {
    this.failed = true;
  }


  public boolean isFailed() {
    return this.failed;
  }

  public boolean objectWasRemoved() {
    return objectWasRemoved;
  }


  public Throwable getException() {
    return exception;
  }


  public boolean wasInterrupted() {
    return wasInterrupted;
  }


  public String getFailedThreadName() {
    return failedThreadName;
  }


  public InterconnectProtocol getNodeConnection() {
    return nodeConnection;
  }
}

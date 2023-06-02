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
package com.gip.xyna.utils.shell;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import com.gip.xyna.utils.concurrent.SetableFuture;

/*
 * we could have the result gathering be done from a single thread here that gathers from several processes
 * 
 * Component -> createProcess -> creates a readout runnable that is round-robin reading out the stdIn & err
 * The process will currently die after executing a single command, but wrapped our Process could live on
 * There then seems to be no need for round-robin
 */
public class ShellExecutor {
  
  // all shellExecutors with an unspecified executor share a single working thread
  // just caller-run it and return a setable future? :D
  //private static ExecutorService executor = Executors.newSingleThreadExecutor();
  private final ExecutorService customExecutor;
  
  public ShellExecutor() {
    this(null);
  }
  
  
  public ShellExecutor(ExecutorService customExecutor) {
    this.customExecutor = customExecutor;
  }
  
  
  // currently not needed and would not handle TimeoutExceptions as necessary
  /*public Future<ShellExecutionResponse> submit(ShellCommand command) {
    return submitToWrappedExecutor(command);
  }*/
  
  
  
  public ShellExecutionResponse execute(ShellCommand command) throws InterruptedException, ExecutionException, TimeoutException {
    return getFromFuture(submitToWrappedExecutor(command));
  }
  
  
  // currently not needed and would need a way to return all results mixed with possible exceptions
  /*public ShellExecutionResponse[] execute(ShellCommand... command) throws InterruptedException, ExecutionException, TimeoutException {
    FutureCollection<ShellExecutionResponse> resultCollection = new FutureCollection<ShellExecutionResponse>();
    for (ShellCommand shellCommand : command) {
      resultCollection.add(submitToWrappedExecutor(shellCommand));
    }
    return (ShellExecutionResponse[]) resultCollection.get().toArray();
  }*/
  
  
  private Future<ShellExecutionResponse> submitToWrappedExecutor(ShellCommand command) {
    if (customExecutor == null) {
      SetableFuture<ShellExecutionResponse> setableFuture = new SetableFuture<ShellExecutionResponse>();
      try {
        setableFuture.set(new ShellExecution(command).call());
      } catch (Throwable e) {
        setableFuture.setException(e);
      }
      return setableFuture;
    } else {
      return customExecutor.submit(new ShellExecution(command));
    }
    
  }
  
  
  private ShellExecutionResponse getFromFuture(Future<ShellExecutionResponse> future) throws InterruptedException, ExecutionException, TimeoutException {
    try {
      return future.get();
    } catch (ExecutionException e) {
      if (e.getCause() != null && 
          e.getCause() instanceof TimeoutException) {
        throw (TimeoutException)e.getCause();
      } else {
        throw e;
      }
    }
  }
  
  

}

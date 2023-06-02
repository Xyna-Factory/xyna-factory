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
package com.gip.xyna.utils.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.gip.xyna.Department;


// TODO if all known uses of FutureCollection are known we could refactor that
public class ExceptionGatheringFutureCollection<E> extends FutureCollection<E> {
  

  public synchronized List<E> getWithExceptionGathering() throws GatheredException {
    List<E> results = new ArrayList<E>();
    List<Throwable> exceptions = new ArrayList<Throwable>();
    for (Future<E> future : futures) {
      try {
        results.add(future.get());
      } catch (ExecutionException e) {
        exceptions.add(e.getCause());
      } catch (Throwable t) {
        Department.handleThrowable(t);
        exceptions.add(t);
      }
    }
    if (exceptions.size() > 0) {
      throw new GatheredException(exceptions);
    }
    return results;
  }
  
  
  public static class GatheredException extends Exception {
    
    private static final long serialVersionUID = -74583684171101113L;
    
    private final List<Throwable> causes;
    
    public GatheredException() {
      causes = new ArrayList<Throwable>();
    }
    
    public GatheredException(Throwable t) {
      this();
      causes.add(t);
    }
    
    public GatheredException(List<Throwable> ts) {
      this();
      causes.addAll(ts);
    }
    
    
    public List<Throwable> getCauses() {
      return causes;
    }
    
  }

}

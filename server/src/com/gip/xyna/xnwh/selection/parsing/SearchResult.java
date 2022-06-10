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
package com.gip.xyna.xnwh.selection.parsing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xmcp.ErroneousOrderExecutionResponse.SerializableExceptionInformation;


/**
 *
 */
public class SearchResult<T> implements Serializable {
  
  private static final long serialVersionUID = 1L;
  private int count;
  private List<T> result; //local
  private List<SerializableExceptionInformation> exceptions;
  private Map<String, List<T>> remoteResults;
  
  public SearchResult() {
    
  }

  public SearchResult(List<T> result, int count) {
    this.result = result;
    this.count = count;
  }

  public int getCount() {
    return count;
  }
  
  public void setCount(int count) {
    this.count = count;
  }
  
  public List<T> getResult() {
    return result;
  }
  
  public void setResult(List<T> result) {
    this.result = result;
  }

  public void addException(Throwable t) {
    if (exceptions == null) {
      exceptions = new ArrayList<SerializableExceptionInformation>();
    }
    exceptions.add(new SerializableExceptionInformation(t));
  }

  public void addResult(String factoryNode, List<T> r) {
    if (factoryNode.equals("local")) {
      result = r; 
    } else {
      if (remoteResults == null) {
        remoteResults = new HashMap<String, List<T>>();
      }
      remoteResults.put(factoryNode, r);
    }
  }

  public Map<String, List<T>> getRemoteResults() {
    if (remoteResults == null) {
      return Collections.emptyMap();
    }
    return remoteResults;
  }

  public List<SerializableExceptionInformation> getExceptions() {
    return exceptions;
  }
  
}

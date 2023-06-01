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
package com.gip.xyna.xprc.xbatchmgmt.selectbatch;

import java.io.Serializable;
import java.util.List;

import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInformation;


public class BatchProcessSearchResult implements Serializable {
  
  private static final long serialVersionUID = 1L;
  private int count;
  private List<BatchProcessInformation> result;
  
  public BatchProcessSearchResult(List<BatchProcessInformation> orders, int countAll) {
    this.result = orders;
    this.count = countAll;
  }

  public int getCount() {
    return count;
  }
  
  public void setCount(int count) {
    this.count = count;
  }
  
  public List<BatchProcessInformation> getResult() {
    return result;
  }
  
  public void setResult(List<BatchProcessInformation> result) {
    this.result = result;
  }

}

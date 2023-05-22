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
package com.gip.xyna.xprc.xfqctrl.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskInformation;


public class FrequencyControlledTaskSearchResult implements Serializable {

  private static final long serialVersionUID = -3805824936993314898L;
  private int countAll;
  private List<FrequencyControlledTaskInformation> result;
  
  public FrequencyControlledTaskSearchResult(List<FrequencyControlledTaskInformation> tasks, int countAll) {
    this.result = tasks;
    this.countAll = countAll;
  }


  public FrequencyControlledTaskSearchResult(Collection<FrequencyControlledTaskInformation> tasks, int countAll,
                                             int countReturnedFilterHits) {
    this.result = new ArrayList<FrequencyControlledTaskInformation>(tasks);
    this.countAll = countAll;
  }


  public int getCount() {
    return countAll;
  }


  public void setCount(int count) {
    this.countAll = count;
  }


  public List<FrequencyControlledTaskInformation> getResult() {
    return result;
  }


  public void setResult(List<FrequencyControlledTaskInformation> result) {
    this.result = result;
  }
}

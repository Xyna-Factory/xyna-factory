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
package com.gip.xyna.xprc.xprcods.orderarchive;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class OrderInstanceResult implements Serializable {

  private static final long serialVersionUID = -1348736090586592669L;
  private int count;
  private int countWithoutRelatives; //result.size() minus children&parents (if any)
  private List<OrderInstance> result;
  
  public OrderInstanceResult(List<OrderInstance> orders, int countAll, int countWithoutRelatives) {
    this.result = orders;
    this.count = countAll;
    this.countWithoutRelatives = countWithoutRelatives;
  }

  public OrderInstanceResult(Collection<OrderInstance> orders, int countAll, int countWithoutRelatives) {
    this(new ArrayList<OrderInstance>(orders), countAll, countWithoutRelatives);
  }

  public int getCount() {
    return count;
  }
  
  public void setCount(int count) {
    this.count = count;
  }

  public int getCountWithoutRelatives() {
    return countWithoutRelatives;
  }
  
  public List<OrderInstance> getResult() {
    return result;
  }
  
  public void setResult(List<OrderInstance> result) {
    this.result = result;
  }
  
}

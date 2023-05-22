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
package com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gip.xyna.xfmg.xopctrl.usermanagement.User;


public class UserSearchResult implements Serializable {

  private static final long serialVersionUID = -3805824936993314898L;
  private int count;
  private int countReturnedFilterHits;
  private List<User> result;
  
  public UserSearchResult(List<User> users, int countAll, int countReturnedFilterHits) {
    this.result = users;
    this.count = countAll;
    this.countReturnedFilterHits = countReturnedFilterHits;
  }

  public UserSearchResult(Collection<User> users, int countAll, int countReturnedFilterHits) {
    this.result = new ArrayList<User>(users);
    this.count = countAll;
    this.countReturnedFilterHits = countReturnedFilterHits;
  }

  public int getCount() {
    return count;
  }
  
  public void setCount(int count) {
    this.count = count;
  }

  public int getTruncatedCount() {
    return countReturnedFilterHits;
  }
  
  public void setTruncatedCount(int countReturnedFilterHits) {
    this.countReturnedFilterHits = countReturnedFilterHits;
  }
  
  public List<User> getResult() {
    return result;
  }
  
  public void setResult(List<User> result) {
    this.result = result;
  }
}

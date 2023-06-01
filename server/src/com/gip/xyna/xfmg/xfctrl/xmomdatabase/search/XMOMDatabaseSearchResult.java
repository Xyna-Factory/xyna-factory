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
package com.gip.xyna.xfmg.xfctrl.xmomdatabase.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;


public class XMOMDatabaseSearchResult implements Serializable {

  private static final long serialVersionUID = -3805824936993314898L;
  private int count; //summe über counts
  private Map<XMOMDatabaseType, Integer> counts;
  private List<XMOMDatabaseSearchResultEntry> result;
  
  public XMOMDatabaseSearchResult(List<XMOMDatabaseSearchResultEntry> results, int count, Map<XMOMDatabaseType, Integer> counts) {
    this.result = results;
    this.count = count;
    this.counts = counts;
  }


  @Deprecated
  public XMOMDatabaseSearchResult(Collection<XMOMDatabaseSearchResultEntry> results, int count,
                                             int countReturnedFilterHits) {
    this.result = new ArrayList<XMOMDatabaseSearchResultEntry>(results);
    this.count = count;
    this.counts = Collections.<XMOMDatabaseType, Integer>emptyMap();
  }


  public int getCount() {
    return count;
  }


  public void setCount(int count) {
    this.count = count;
  }

  public Map<XMOMDatabaseType, Integer> getCounts() {
    return counts;
  }

  public List<XMOMDatabaseSearchResultEntry> getResult() {
    return result;
  }


  public void setResult(List<XMOMDatabaseSearchResultEntry> result) {
    this.result = result;
  }


  public static XMOMDatabaseSearchResult empty() {
    return new XMOMDatabaseSearchResult(Collections.<XMOMDatabaseSearchResultEntry>emptyList(), 0, Collections.<XMOMDatabaseType, Integer>emptyMap());
  }
}

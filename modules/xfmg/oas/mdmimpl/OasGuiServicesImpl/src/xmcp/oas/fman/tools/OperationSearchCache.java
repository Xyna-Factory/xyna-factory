/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xmcp.oas.fman.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;


public class OperationSearchCache {

  private Map<RtcData, Map<String, List<XMOMDatabaseSearchResultEntry>>> _map = new HashMap<>();
  private OasGuiTools _tools = new OasGuiTools();
  
  
  public void initForRtcIfEmpty(RtcData rtc) {
    if (_map.containsKey(rtc)) { return; }
    List<XMOMDatabaseSearchResultEntry> query = _tools.getOperationsOfRtc(rtc);
    Map<String, List<XMOMDatabaseSearchResultEntry>> pathmap = new HashMap<>();
    for (XMOMDatabaseSearchResultEntry entry : query) {
      String path = entry.getSimplepath();
      List<XMOMDatabaseSearchResultEntry> oplist = pathmap.get(path);
      if (oplist == null) {
        oplist = new ArrayList<XMOMDatabaseSearchResultEntry>();
        pathmap.put(path, oplist);
      }
      oplist.add(entry);
    }
    _map.put(rtc, pathmap);
  }
  
  
  public List<XMOMDatabaseSearchResultEntry> getForRtcAndPath(RtcData rtc, String path) {
    Map<String, List<XMOMDatabaseSearchResultEntry>> pathmap = _map.get(rtc);
    if (pathmap == null) {
      return new ArrayList<XMOMDatabaseSearchResultEntry>();
    }
    List<XMOMDatabaseSearchResultEntry> oplist = pathmap.get(path);
    if (oplist == null) {
      return new ArrayList<XMOMDatabaseSearchResultEntry>();
    }
    return oplist;
  }
  
}

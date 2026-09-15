/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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


package com.gip.xyna.xprc.xsched.vetos.cache;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;
import com.gip.xyna.xprc.xsched.vetos.cache.AllocationRequest.PendingType;
import com.gip.xyna.xprc.xsched.vetos.cache.AllocationRequest.VetoType;


public class AllocationRequestList {

  
  public static enum ListAllocationMode {
    COMPLETE, ONLY_PENDING, NONE
  }
  
  
  private List<AllocationRequest> _list = new ArrayList<>();

  
  public AllocationRequestList(List<String> exclusiveVetos, List<String> sharedVetos, VetoCacheProcessor processor,
                               OrderInformation orderInformation) {
    if (exclusiveVetos != null) {
      for (String veto : exclusiveVetos) {
        _list.add(new AllocationRequest(veto, VetoType.EXCLUSIVE, orderInformation));
      }
    }
    if (sharedVetos != null) {
      boolean allowShared = processor.allowSharedVetos();
      for (String veto : sharedVetos) {
        _list.add(new AllocationRequest(veto, (allowShared ? VetoType.SHARED : VetoType.EXCLUSIVE), orderInformation));
      }
    }
  }


  public List<AllocationRequest> getList() {
    return _list;
  }
  
  public ListAllocationMode determineListAllocationMode() {
    boolean hasPending = false;
    boolean hasResult = false;
    for (AllocationRequest request : _list) {
      if (request == null) { continue; }
      if (request.getCacheEntry() == null) { continue; }
      if (request.getResult() != null) {
        hasResult = true;
      }
      if (request.getPendingType() == PendingType.PENDING) {
        hasPending = true;
      }
    }
    if (!hasResult) {
      return ListAllocationMode.COMPLETE;
    }
    if (hasPending) {
      return ListAllocationMode.ONLY_PENDING;
    }
    return ListAllocationMode.NONE;
  }
  
}

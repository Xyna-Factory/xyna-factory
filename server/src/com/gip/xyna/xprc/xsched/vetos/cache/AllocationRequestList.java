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

import com.gip.xyna.xprc.xsched.vetos.cache.AllocationRequest.PendingType;
import com.gip.xyna.xprc.xsched.vetos.cache.AllocationRequest.VetoType;


public class AllocationRequestList {

  public static enum AllocationMode {
    COMPLETE, ONLY_PENDING, NONE, NONE_REQUIRES_NOTIFY
  }
  
  private List<AllocationRequest> _list = new ArrayList<>();

  
  public AllocationRequestList(List<String> exclusiveVetos, List<String> sharedVetos) {
    if (exclusiveVetos != null) {
      for (String veto : exclusiveVetos) {
        _list.add(new AllocationRequest(veto, VetoType.EXCLUSIVE));
      }
    }
    if (sharedVetos != null) {
      for (String veto : sharedVetos) {
        _list.add(new AllocationRequest(veto, VetoType.SHARED));
      }
    }
  }


  public List<AllocationRequest> getList() {
    return _list;
  }
  
  public AllocationMode determineAllocationMode() {
    boolean hasPending = false;
    boolean hasResult = false;
    for (AllocationRequest request : _list) {
      if (request == null) { continue; }
      if (request.isRequiresNotifyProcessor()) {
        return AllocationMode.NONE_REQUIRES_NOTIFY;
      }
      if (request.getResult() != null) {
        hasResult = true;
      }
      if (request.getPendingType() != PendingType.NON_PENDING) {
        hasPending = true;
      }
    }
    if (!hasResult) {
      return AllocationMode.COMPLETE;
    }
    if (hasPending) {
      return AllocationMode.ONLY_PENDING;
    }
    return AllocationMode.NONE;
  }
  
}

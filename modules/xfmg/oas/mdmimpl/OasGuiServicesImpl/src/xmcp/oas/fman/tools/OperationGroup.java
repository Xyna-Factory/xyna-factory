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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;

import xmcp.oas.fman.tools.OasApiType.OasApiTypeCategory;


public class OperationGroup {

  private final XmomType xmom;
  private Set<String> operations = new HashSet<>();
  private boolean operationsInitialized = false;
  private final OasApiTypeCategory oasApiTypecategory;
  private Optional<OperationSearchCache> operationSearchCache = Optional.empty();

  
  public OperationGroup(XmomType xmom) {
    this(xmom, OasApiTypeCategory.NONE, null);
  }
  
  
  public OperationGroup(XmomType xmom, OasApiTypeCategory oasApiTypecategory, OasGuiContext context) {
    if (xmom == null) {
      throw new IllegalArgumentException("Xmom type is null.");
    }
    this.xmom = xmom;
    this.oasApiTypecategory = oasApiTypecategory;
    if (context != null) {
      this.operationSearchCache = Optional.ofNullable(context.getOperationSearchCache());
    }
  }
  
  
  public OperationGroup(String fqn, RtcData rtc) {
    this(new XmomType(fqn, rtc));
  }
  
  
  public OperationGroup(OasApiType oat, OasGuiContext context) {
    this(oat.getXmomType(), oat.getCategory(), context);
  }
  
  
  public XmomType getXmomType() {
    return xmom;
  }
  
  
  public OasApiTypeCategory getOasApiTypecategory() {
    return oasApiTypecategory;
  }


  private int getNumOperations() {
    return operations.size();
  }
  
  
  private void initOperations() {
    if (operationSearchCache.isPresent()) {
      initOperationsFromCache(operationSearchCache.get());
    } else {
      operations.addAll(new OasGuiTools().getOperationsOfXmomType(xmom));
    }
    operationsInitialized = true;
  }
  
  
  private void initOperationsFromCache(OperationSearchCache cache) {
    cache.initForRtcIfEmpty(xmom.getRtc());
    List<XMOMDatabaseSearchResultEntry> results = cache.getForRtcAndPath(xmom.getRtc(), xmom.getFqNameInstance().getPath());
    String typename = xmom.getFqNameInstance().getTypename();
    for (XMOMDatabaseSearchResultEntry entry : results) {
      String op = entry.getSimplename();
      if (!op.contains(".")) { continue; }
      String prefix = op.substring(0, op.indexOf("."));
      if (!prefix.equals(typename)) { continue; }
      op = op.substring(op.lastIndexOf(".") + 1, op.length());
      operations.add(op);
    }
  }
  
  
  public boolean operationsMatch(OperationGroup input) {
    if (!input.operationsInitialized) {
      input.initOperations();
    }
    if (!operationsInitialized) {
      initOperations();
    }
    if (input.getNumOperations() != getNumOperations()) { return false; }
    for (String val : operations) {
      if (!input.operations.contains(val)) {
        return false;
      }
    }
    return true;
  }
  
}

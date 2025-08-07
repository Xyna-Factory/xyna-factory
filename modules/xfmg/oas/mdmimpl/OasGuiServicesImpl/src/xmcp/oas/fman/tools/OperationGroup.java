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

import java.util.Set;

import xmcp.oas.fman.tools.OasApiType.OasApiTypeCategory;


public class OperationGroup {

  private final XmomType xmom;
  private final Set<String> operations;
  private final OasApiTypeCategory oasApiTypecategory;

  
  public OperationGroup(XmomType xmom) {
    this(xmom, OasApiTypeCategory.NONE);
  }
  
  
  public OperationGroup(XmomType xmom, OasApiTypeCategory oasApiTypecategory) {
    if (xmom == null) {
      throw new IllegalArgumentException("Xmom type is null.");
    }
    this.xmom = xmom;
    this.operations = new OasGuiTools().getOperationsOfXmomType(xmom);
    this.oasApiTypecategory = oasApiTypecategory;
  }
  
  
  public OperationGroup(String fqn, RtcData rtc) {
    this(new XmomType(fqn, rtc));
  }
  
  
  public OperationGroup(OasApiType oat) {
    this(oat.getXmomType(), oat.getCategory());
  }
  
  
  public XmomType getXmomType() {
    return xmom;
  }
  
  
  public OasApiTypeCategory getOasApiTypecategory() {
    return oasApiTypecategory;
  }


  public int getNumOperations() {
    return operations.size();
  }
  
  
  public boolean operationsMatch(OperationGroup input) {
    if (input.getNumOperations() != getNumOperations()) { return false; }
    for (String val : operations) {
      if (!input.operations.contains(val)) {
        return false;
      }
    }
    return true;
  }
  
}

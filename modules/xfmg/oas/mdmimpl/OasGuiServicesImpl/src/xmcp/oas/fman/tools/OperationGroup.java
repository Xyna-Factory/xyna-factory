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


public class OperationGroup {

  private final XmomType xmom;
  private final Set<String> operations;

  
  public OperationGroup(XmomType xmom, RtcData rtc) {
    if (xmom == null) {
      throw new IllegalArgumentException("Xmom type is null.");
    }
    this.xmom = xmom;
    this.operations = new OasGuiTools().getOperationsOfXmomType(xmom, rtc);
  }
  
  
  public OperationGroup(String fqn, RtcData rtc) {
    this(new XmomType(fqn), rtc);
  }
  
  
  public OperationGroup(OasApiType oat) {
    this(oat.getXmomType(), oat.getRtc());
  }
  
  
  public XmomType getXmomType() {
    return xmom;
  }
  
  
  public String getFqName() {
    return xmom.getFqName();
  }
  
  
  public int getNumOperations() {
    return operations.size();
  }
  
  
  public boolean matches(OperationGroup input) {
    if (input.getNumOperations() != getNumOperations()) { return false; }
    for (String val : operations) {
      if (!input.operations.contains(val)) {
        return false;
      }
    }
    return true;
  }
  
}

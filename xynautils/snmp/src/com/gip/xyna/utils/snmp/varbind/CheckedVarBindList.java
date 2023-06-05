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
package com.gip.xyna.utils.snmp.varbind;

import java.util.HashSet;
import java.util.Set;

/**
 * Variable binding list.
 * 
 *
 */
public class CheckedVarBindList extends VarBindList {

  private final Set<String> usedOIDs = new HashSet<String>();
  
  @Override
  public void add(final VarBind variableBinding) {
    if (variableBinding == null) {
      throw new IllegalArgumentException("Variable binding may not be null.");
    }
    addToUsedOIDs(variableBinding.getObjectIdentifier());
    super.add(variableBinding);
  }

  // Note: this code is here to find programming errors - SNMP standard does not forbid duplicate definitions.
  private void addToUsedOIDs(final String oid) {
    if (usedOIDs.contains(oid)) {
      throw new IllegalArgumentException("Value for given OID <" + oid + "> has already been defined.");
    }
    usedOIDs.add(oid);
  }
 
}

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
package com.gip.xyna.xprc.xpce.transaction.parameter;

import java.util.Set;

public class OperationPrevention {
  
  private final Set<TransactionOperation> operations;
  private final boolean throwOnAccess;
  
  public OperationPrevention(Set<TransactionOperation> operations, boolean throwOnAccess) {
    this.operations = operations;
    this.throwOnAccess = throwOnAccess;
  }
  
  public Set<TransactionOperation> getOperations() {
    return operations;
  }
  
  public boolean doThrowOnAccess() {
    return throwOnAccess;
  }
  
  public boolean doPrevent(TransactionOperation operation) {
    if (operations.contains(operation)) {
      if (throwOnAccess) {
        throw new IllegalAccessError("Access to operation '" + String.valueOf(operation) + "' is not allowed.");
      } else {
        return true;
      }
    } else {
      return false;
    }
  }
  
}
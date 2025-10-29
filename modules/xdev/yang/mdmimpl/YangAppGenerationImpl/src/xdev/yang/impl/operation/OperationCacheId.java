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

package xdev.yang.impl.operation;

import java.util.Objects;

import xmcp.yang.LoadYangAssignmentsData;

public class OperationCacheId implements Comparable<OperationCacheId> {

  private final String fqn;
  private final String workspaceName;
  private final String operation;
  
  public OperationCacheId(String fqn, String workspaceName, String operation) {
    if (fqn == null) { throw new IllegalArgumentException("OperationCacheId: Fqn is empty"); }
    if (workspaceName == null) { throw new IllegalArgumentException("OperationCacheId: WorkspaceName is empty"); }
    if (operation == null) { throw new IllegalArgumentException("OperationCacheId: Operation is empty"); }
    this.fqn = fqn;
    this.workspaceName = workspaceName;
    this.operation = operation;
  }
  
  public static OperationCacheId fromLoadYangAssignmentsData(LoadYangAssignmentsData data) {
    String fqn = data.getFqn();
    String workspaceName = data.getWorkspaceName();
    String operation = data.getOperation();
    if (fqn == null) { throw new IllegalArgumentException("OperationCacheId: Fqn is empty"); }
    if (workspaceName == null) { throw new IllegalArgumentException("OperationCacheId: WorkspaceName is empty"); }
    if (operation == null) { throw new IllegalArgumentException("OperationCacheId: Operation is empty"); }
    return new OperationCacheId(fqn, workspaceName, operation);
  }

  public String getFqn() {
    return fqn;
  }
  
  public String getWorkspaceName() {
    return workspaceName;
  }
  
  public String getOperation() {
    return operation;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) { return false; }
    if (obj == this) { return true; }
    if (!(obj instanceof OperationCacheId)) { return false; }
    OperationCacheId id = (OperationCacheId) obj;
    if (!workspaceName.equals(id.getWorkspaceName())) { return false; }
    if (!fqn.equals(id.getFqn())) { return false; }
    if (!operation.equals(id.getOperation())) { return false; }
    return true;
  }
  
   public int hashCode() {
     return Objects.hash(fqn, workspaceName, operation);
   }

  @Override
  public int compareTo(OperationCacheId id) {
    if (id == null) { return 1; }
    int val = workspaceName.compareTo(id.getWorkspaceName());
    if (val != 0) { return val; }
    val = fqn.compareTo(id.getFqn());
    if (val != 0) { return val; }
    val = operation.compareTo(id.getOperation());
    if (val != 0) { return val; }
    return 0;
  }

}

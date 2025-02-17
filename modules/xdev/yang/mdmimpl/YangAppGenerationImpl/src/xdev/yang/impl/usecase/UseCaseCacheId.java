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

package xdev.yang.impl.usecase;

import java.util.Objects;

import xmcp.yang.LoadYangAssignmentsData;

public class UseCaseCacheId implements Comparable<UseCaseCacheId> {

  private final String fqn;
  private final String workspaceName;
  private final String usecase;
  
  public UseCaseCacheId(LoadYangAssignmentsData data) {
    this.fqn = data.getFqn();
    this.workspaceName = data.getWorkspaceName();
    this.usecase = data.getUsecase();
    if (fqn == null) { throw new IllegalArgumentException("UseCaseCacheId: Fqn is empty"); }
    if (workspaceName == null) { throw new IllegalArgumentException("UseCaseCacheId: WorkspaceName is empty"); }
    if (usecase == null) { throw new IllegalArgumentException("UseCaseCacheId: Usecase is empty"); }
  }
  
  public String getFqn() {
    return fqn;
  }
  
  public String getWorkspaceName() {
    return workspaceName;
  }
  
  public String getUsecase() {
    return usecase;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) { return false; }
    if (obj == this) { return true; }
    if (!(obj instanceof UseCaseCacheId)) { return false; }
    UseCaseCacheId id = (UseCaseCacheId) obj;
    if (!workspaceName.equals(id.getWorkspaceName())) { return false; }
    if (!fqn.equals(id.getFqn())) { return false; }
    if (!usecase.equals(id.getUsecase())) { return false; }
    return true;
  }
  
   public int hashCode() {
     return Objects.hash(fqn, workspaceName, usecase);
   }

  @Override
  public int compareTo(UseCaseCacheId id) {
    if (id == null) { return 1; }
    int val = workspaceName.compareTo(id.getWorkspaceName());
    if (val != 0) { return val; }
    val = fqn.compareTo(id.getFqn());
    if (val != 0) { return val; }
    val = usecase.compareTo(id.getUsecase());
    if (val != 0) { return val; }
    return 0;
  }

}

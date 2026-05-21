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

package xmcp.gitintegration.tools;


public class ChangedStatusInfo implements Comparable<ChangedStatusInfo> {
  
  private final String changedPath;
  private final ChangeType changeType;
  
  public ChangedStatusInfo(String changedPath, ChangeType changeType) {
    if (changeType == null) { throw new IllegalArgumentException("ChangeType is empty."); }
    if (changedPath == null) { throw new IllegalArgumentException("ChangedPath is empty."); }
    this.changedPath = changedPath;
    this.changeType = changeType;
  }
  
  public String getChangedPath() {
    return changedPath;
  }
  
  public ChangeType getChangeType() {
    return changeType;
  }

  @Override
  public int compareTo(ChangedStatusInfo input) {
    if (input == null) { return 1; }
    if (equals(input)) { return 0; }
    int val = getChangedPath().compareTo(input.getChangedPath());
    if (val != 0) { return val; }
    val = getChangeType().getChangeTypeString().compareTo(input.getChangeType().getChangeTypeString());
    return val;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) { return false; }
    if (!(obj instanceof ChangedStatusInfo)) { return false; }
    ChangedStatusInfo input = (ChangedStatusInfo) obj;
    if (input.getChangeType() != getChangeType()) { return false; }
    return input.getChangedPath().equals(getChangedPath());
  }
  
  @Override
  public int hashCode() {
    return 100;
  }
  
}

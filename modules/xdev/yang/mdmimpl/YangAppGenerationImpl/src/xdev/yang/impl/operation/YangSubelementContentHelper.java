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

import org.yangcentral.yangkit.base.YangElement;
import org.yangcentral.yangkit.model.api.stmt.Config;
import org.yangcentral.yangkit.model.api.stmt.Description;
import org.yangcentral.yangkit.model.api.stmt.Status;
import org.yangcentral.yangkit.model.api.stmt.StatusStmt;
import org.yangcentral.yangkit.model.api.stmt.YangStatement;

import xmcp.yang.LoadYangAssignmentsData;


public class YangSubelementContentHelper {

  public boolean getConfigSubelementValueBoolean(YangStatement ys) {
    Config result = getSubelementOrNull(ys, Config.class);
    if (result == null) { return true; }
    return result.isConfig();
  }

  public void copyRelevantSubelementValues(YangStatement ys, LoadYangAssignmentsData data) {
    data.setDescription(getNullOrArgStr(getDescriptionSubelementOrNull(ys)));
    data.setStatus(getStatusValueIfNotCurrentOrNull(ys));
  }

  public Description getDescriptionSubelementOrNull(YangStatement ys) {
    return getSubelementOrNull(ys, Description.class);
  }

  public String getStatusValueIfNotCurrentOrNull(YangStatement ys) {
    StatusStmt elem = getStatusSubelementOrNull(ys);
    if (elem == null) { return null; }
    Status status = elem.getStatus();
    if (status == null) { return null; }
    if (status == Status.CURRENT) { return null; }
    return status.getStatus();
  }

  public StatusStmt getStatusSubelementOrNull(YangStatement ys) {
    return getSubelementOrNull(ys, StatusStmt.class);
  }

  protected String getNullOrArgStr(YangStatement ys) {
    return ys == null ? null : ys.getArgStr();
  }


  private <T extends YangStatement> T getSubelementOrNull(YangStatement ys, Class<T> clazz) {
    if (ys == null) { return null; }
    for (YangElement elem: ys.getSubElements()) {
      if (clazz.isInstance(elem)) {
        return clazz.cast(elem);
      }
    }
    return null;
  }

}

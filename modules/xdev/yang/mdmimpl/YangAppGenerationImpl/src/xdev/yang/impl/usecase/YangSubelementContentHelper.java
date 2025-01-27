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

import org.yangcentral.yangkit.base.YangElement;
import org.yangcentral.yangkit.model.api.stmt.YangStatement;

import xmcp.yang.LoadYangAssignmentsData;


public class YangSubelementContentHelper {
    
  public boolean getConfigSubelementValueBoolean(YangStatement ys) {
    String val = this.getConfigSubelementValueOrNull(ys);
    if (val == null) { return true; }
    if ("false".equals(val.trim())) { return false; }
    return true;
  }

  public String getConfigSubelementValueOrNull(YangStatement ys) {
    return this.getSubelementValueOrNull(ys, org.yangcentral.yangkit.model.api.stmt.Config.class);
  }
  
  public String getDescriptionSubelementValueOrNull(YangStatement ys) {
    return this.getSubelementValueOrNull(ys, org.yangcentral.yangkit.model.api.stmt.Description.class);
  }

  public String getStatusSubelementValueOrNull(YangStatement ys) {
    return this.getSubelementValueOrNull(ys, org.yangcentral.yangkit.model.api.stmt.StatusStmt.class);
  }
  
  public void copyRelevantSubelementValues(YangStatement ys, LoadYangAssignmentsData data) {
    data.setDescription(this.getDescriptionSubelementValueOrNull(ys));
    data.setStatus(this.getStatusSubelementValueOrNull(ys));
  }

  private String getSubelementValueOrNull(YangStatement ys, Class<?> clazz) {
    if (ys == null) { return null; }
    if (clazz == null) { return null; }
    for (YangElement elem: ys.getSubElements()) {
      if (!(elem instanceof YangStatement)) { continue; }
      if (clazz.isAssignableFrom(elem.getClass())) {
        String val = ((YangStatement) elem).getArgStr();
        return val; 
      }
    }
    return null;
  }

}

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
import org.yangcentral.yangkit.model.api.stmt.Config;
import org.yangcentral.yangkit.model.api.stmt.Description;
import org.yangcentral.yangkit.model.api.stmt.StatusStmt;
import org.yangcentral.yangkit.model.api.stmt.YangStatement;
import org.yangcentral.yangkit.model.impl.stmt.ConfigImpl;


public class YangSubelementContentHelper {
    
  public boolean getConfigSubelementValueBoolean(YangStatement ys) {
    YangStatement result = getSubelementOrNull(ys, Config.class);
    if (result == null) { return true; }
    if (result instanceof ConfigImpl) {
      return ((ConfigImpl) result).isConfig();
    }
    String val = result.getArgStr();
    if (val == null) { return true; }
    return Boolean.parseBoolean(val.trim());
  }


  public Description getDescriptionSubelementOrNull(YangStatement ys) {
    return (Description) getSubelementOrNull(ys, Description.class);
  }

  public StatusStmt getStatusSubelementOrNull(YangStatement ys) {
    return (StatusStmt) getSubelementOrNull(ys, StatusStmt.class);
  }

  private String getNullOrArgStr(YangStatement ys) {
    return ys == null ? null : ys.getArgStr();
  }


  private YangStatement getSubelementOrNull(YangStatement ys, Class<?> clazz) {
    if (ys == null) { return null; }
    if (clazz == null) { return null; }
    for (YangElement elem: ys.getSubElements()) {
      if (!(elem instanceof YangStatement)) { continue; }
      if (clazz.isAssignableFrom(elem.getClass())) {
        return (YangStatement) elem;
      }
    }
    return null;
  }

}

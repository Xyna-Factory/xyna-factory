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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.monitoring;



import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_ACCESS_VIOLATION;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.ScopedRight;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SearchResult;



public class LocalMonitoringSecurity {

  private static final LocalMonitoring lm = new LocalMonitoring();


  public SearchResult<?> search(Role role, SearchRequestBean searchRequest) throws XynaException {
    switch (searchRequest.getArchiveIdentifier()) {
      case orderarchive : {
        if (hasRight(resolveFunctionToRight("searchOrderInstances"), role)) {
          break;
        } else {
          throw new XFMG_ACCESS_VIOLATION("searchOrderInstances", role.getName());
        }
      }
      case vetos : {
        String scopedRight = getUserManagement().getScopedRight(ScopedRight.VETO, Action.read);
        checkScopedRights(scopedRight, role);
        break;
      }
      default :
        throw new RuntimeException("unsupported search: " + searchRequest.getArchiveIdentifier());
    }

    return lm.search(searchRequest);
  }


  private void checkScopedRights(String scopedRight, Role role) throws XFMG_ACCESS_VIOLATION, PersistenceLayerException {
    if (!hasRight(scopedRight, role)) {
      throw new XFMG_ACCESS_VIOLATION(scopedRight, role.getName());
    }
  }

  private UserManagement getUserManagement() {
    return XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement();
  }


  private boolean hasRight(String right, Role role) throws PersistenceLayerException {
    return getUserManagement().hasRight(right, role);
  }


  private String resolveFunctionToRight(String methodName) {
    return getUserManagement().resolveFunctionToRight(methodName);
  }

}

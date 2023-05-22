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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;
import java.util.Collection;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Right;
import com.gip.xyna.xfmg.xopctrl.usermanagement.RightScope;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.PredefinedCategories;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listrights;



public class ListrightsImpl extends XynaCommandImplementation<Listrights> {

  public void execute(OutputStream statusOutputStream, Listrights payload) throws XynaException {
    UserManagement um = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement();
    Collection<Right> rights = um.getRights(null);
    Collection<RightScope> scopes = factory.getFactoryManagementPortal().getRightScopes(null);
    writeToCommandLine(statusOutputStream, "Found " + (rights.size() + scopes.size()) + " rights.\n");
    for (Right right : rights) {
      writeToCommandLine(statusOutputStream,
                         right.getName(),
                         (um.isPredefined(PredefinedCategories.RIGHT, right.getName()) ? "*" : ""),
                         ((right.getDescription() == null || right.getDescription().length() == 0) ? "" : " - " + right.getDescription()), "\n");
    }
    for (RightScope rightScope : scopes) {
      writeToCommandLine(statusOutputStream,
                         rightScope.getDefinition(),
                         (um.isPredefined(PredefinedCategories.RIGHTSCOPE, rightScope.getName()) ? "*" : ""),
                         ((rightScope.getDocumentation() == null || rightScope.getDocumentation().length() == 0) ? "" : " - " + rightScope.getDocumentation()), "\n");
    }
    writeToCommandLine(statusOutputStream, "  *: Xyna-Right - only restricted access allowed\n");
  }

}

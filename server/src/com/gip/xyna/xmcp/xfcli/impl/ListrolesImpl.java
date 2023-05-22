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
import java.util.Set;
import java.util.TreeSet;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.PredefinedCategories;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listroles;



public class ListrolesImpl extends XynaCommandImplementation<Listroles> {

  private final String INDENT_FOR_LISTS = "     ";
  
  public void execute(OutputStream statusOutputStream, Listroles payload) throws XynaException {
    UserManagement um = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement();
    Collection<Role> roles = um.getRoles();
    StringBuilder rolesOut = new StringBuilder();
    for (Role role : roles) {
      if (um.isPredefined(PredefinedCategories.ROLE, role.getId())) {
        rolesOut.append(role.getName());
        rolesOut.append("* - ");
      } else {
        rolesOut.append(role.getName());
        rolesOut.append(" - ");
      }

      rolesOut.append(role.getDomain());

      if (role.getAlias() != null && !role.getAlias().equals("")) {
        rolesOut.append(" - alias: ");
        rolesOut.append(role.getAlias());
      }

      if (role.getDescription() != null && !role.getDescription().equals("")) {
        rolesOut.append(" - ");
        rolesOut.append(role.getDescription());
      }

      rolesOut.append("\n");
      Set<String> rights = new TreeSet<String>(role.getRightsAsList());
      for (String right : rights) {
        rolesOut.append(INDENT_FOR_LISTS);
        rolesOut.append(right);
        rolesOut.append("\n");
      }
      if (role.getScopedRights() != null) {
        Set<String> scopedRights = new TreeSet<String>(role.getScopedRights());
        for (String scopedRight : scopedRights) {
          rolesOut.append(INDENT_FOR_LISTS);
          rolesOut.append(scopedRight);
          rolesOut.append("\n");
        }
      }
    }
    rolesOut.append("*: Xyna-Role - only restricted access allowed\n");
    String output = rolesOut.toString();
    if (output != null && output.length() != 0) {
      writeToCommandLine(statusOutputStream, output);
    } else {
      writeToCommandLine(statusOutputStream, "No roles defined on server\n");
    }
  }

}

/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_PredefinedXynaObjectException;
import com.gip.xyna.xfmg.exceptions.XFMG_RoleDoesNotExistException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserDoesNotExistException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Changerole;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class ChangeroleImpl extends XynaCommandImplementation<Changerole> {

  public void execute(OutputStream statusOutputStream, Changerole payload) throws XynaException {
    String userId = payload.getUserName();
    String roleName = payload.getRoleName();

    boolean success;
    try {
      success = factory.getFactoryManagementPortal().changeRole(userId, roleName);
    } catch (PersistenceLayerException e) {
      throw e;
    } catch (XFMG_PredefinedXynaObjectException e) {
      writeLineToCommandLine(statusOutputStream, "The internal user '" + userId + "' may not be changed.");
      return;
    } catch (XFMG_UserDoesNotExistException e) {
      writeLineToCommandLine(statusOutputStream, "The user '" + userId + "' does not exist.");
      return;
    } catch (XFMG_RoleDoesNotExistException e) {
      writeLineToCommandLine(statusOutputStream, "The role '" + roleName + "' does not exist.");
      return;
    }

    if (success) {
      writeLineToCommandLine(statusOutputStream, "Role of User '", userId, "' was succesfully changed to '", roleName,
                             "'\n");
    } else {
      writeLineToCommandLine(statusOutputStream, "Role of User '", userId, "' could not be changed to '", roleName,
                             "'\n");
    }
  }

}

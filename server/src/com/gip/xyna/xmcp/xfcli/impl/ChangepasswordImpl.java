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

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserIsLockedException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Changepassword;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class ChangepasswordImpl extends XynaCommandImplementation<Changepassword> {

  private static final Logger logger = CentralFactoryLogging.getLogger(ChangepasswordImpl.class);


  public void execute(OutputStream statusOutputStream, Changepassword payload) throws XynaException {
    try {
      if (factory.getFactoryManagementPortal().changePassword(payload.getUserName(), payload.getOldPassword(),
                                                              payload.getNewPassword(), payload.getIsNewPasswordHashed())) {
        writeLineToCommandLine(statusOutputStream, "Password for user '" + payload.getUserName()
            + "' was succesfully changed");
      } else {
        writeLineToCommandLine(statusOutputStream, "Password for user '" + payload.getUserName()
            + "' could not be changed");
      }
    } catch (PersistenceLayerException e) {
      logger.debug(null, e);
      writeLineToCommandLine(statusOutputStream, "Password for user '" + payload.getUserName()
          + "' could not be changed: " + e.getMessage() + ". See logfile for further info.");
    } catch (XFMG_UserAuthenticationFailedException e) {
      writeLineToCommandLine(statusOutputStream, "Password for user '" + payload.getUserName()
          + "' could not be changed: " + e.getMessage());
    } catch (XFMG_UserIsLockedException e) {
      writeLineToCommandLine(statusOutputStream, "Password for user '" + payload.getUserName()
          + "' could not be changed: " + e.getMessage());
    }
  }

}

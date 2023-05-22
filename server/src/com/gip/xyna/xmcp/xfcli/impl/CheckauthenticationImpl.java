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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Domain;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserAuthentificationMethod;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Checkauthentication;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class CheckauthenticationImpl extends XynaCommandImplementation<Checkauthentication> {

  private static final Logger logger = CentralFactoryLogging.getLogger(CheckauthenticationImpl.class);


  public void execute(OutputStream statusOutputStream, Checkauthentication payload) throws XynaException {
    Domain domain = null;
    try {
      domain = XynaFactory.getInstance().getFactoryManagement().getDomain(payload.getDomain());
    } catch (PersistenceLayerException e) {
      writeLineToCommandLine(statusOutputStream, new StringBuilder().append("Domain ").append(payload.getDomain())
          .append(" could not be found").toString());
      return;
    }
    List<Domain> domains = new ArrayList<Domain>();
    domains.add(domain);

    List<UserAuthentificationMethod> methods = UserAuthentificationMethod.generateAuthenticationMethods(domains);
    for (UserAuthentificationMethod userAuthentificationMethod : methods) {
      try {
        Role authenticatedRole = userAuthentificationMethod.authenticateUser(payload.getUser(), payload.getPassword());
        if (authenticatedRole == null) {
          writeLineToCommandLine(statusOutputStream, new StringBuilder().append("User ").append(payload.getUser())
              .append(" could not be authenticated").toString());
          continue;
        } else {
          writeLineToCommandLine(statusOutputStream, new StringBuilder().append("User ").append(payload.getUser())
              .append(" successfully authenticated and resolved to role ").append(authenticatedRole.getName())
              .toString());
          continue;
        }
      } catch (Throwable t) {
        logger.warn("Error while trying to authenticate, continuing with next method if available", t);
        writeLineToCommandLine(statusOutputStream,
                               "Error while trying to authenticate, continuing with next method if available ("
                                   + t.getMessage() + "). See log for more information.");
        continue;
      }
    }

  }

}

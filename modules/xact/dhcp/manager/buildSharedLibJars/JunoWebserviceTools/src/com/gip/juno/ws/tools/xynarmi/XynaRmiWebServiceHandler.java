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
package com.gip.juno.ws.tools.xynarmi;

import java.rmi.RemoteException;
import org.apache.log4j.Logger;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionCredentials;
import com.gip.juno.ws.exceptions.DPPWebserviceAuthenticationException;

public class XynaRmiWebServiceHandler {

  private static final String JUNO_XYNA_RIGHT_PREFIX = "juno."; 
    
  public static SessionCredentials createSession(String username, String password) throws RemoteException {
   throw new DPPWebserviceAuthenticationException("Session creation not supported.", "00002");
  }
  
  public static void authenticateAndAuthorize(String webServiceIdentifier, String webServiceOperation, String sessionId, String token, Logger logger) throws RemoteException {
    String xynaRightIdentifier = getXynaRightForWebServiceOperation(webServiceIdentifier, webServiceOperation);

    final com.gip.xyna.xfmg.xopctrl.managedsessions.SessionManagement sMgmt = com.gip.xyna.XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement();
    final com.gip.xyna.xfmg.xopctrl.usermanagement.Role role;
    
    try {
      role = sMgmt.getRole(sessionId);
    } catch (Exception e) {
      throw new DPPWebserviceAuthenticationException(new StringBuilder("Could not retrieve Role for Session ").append(sessionId).toString(), "00002", e);
    }
    try {
      final com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement userMgmt = com.gip.xyna.XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement();
      if (userMgmt.hasRight(xynaRightIdentifier, role)) {
        logger.debug(new StringBuilder("Session ").append(sessionId).append(" did possess the right ").append(xynaRightIdentifier).append('.').toString());
      } else {
          throw new DPPWebserviceAuthenticationException(new StringBuilder("Role ").append(role.getName())
                                                         .append(" of Session ").append(sessionId)
                                                         .append(" has no right for the requested operation (").append(xynaRightIdentifier).append(").").toString(), "00001");
      }
    } catch (Exception e) {
        throw new DPPWebserviceAuthenticationException(new StringBuilder("Session ").append(sessionId)
                                                            .append(" was not known to factory.").toString(),"00002", e);
    }
  }
  
  private static String getXynaRightForWebServiceOperation(String webServiceIdentifier, String webServiceOperation) {
    String cleanWebServiceIdentifier = webServiceIdentifier.replaceAll("\\_", "");
    StringBuilder rightBuilder = new StringBuilder(JUNO_XYNA_RIGHT_PREFIX).append(cleanWebServiceIdentifier);
    if (webServiceOperation != null &&
        webServiceOperation.length() > 0) {
      rightBuilder.append('.').append(webServiceOperation);
    }
    return rightBuilder.toString();
  }

}

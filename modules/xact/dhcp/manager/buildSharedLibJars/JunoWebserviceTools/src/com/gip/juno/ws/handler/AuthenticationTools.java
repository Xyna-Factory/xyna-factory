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

package com.gip.juno.ws.handler;

import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.Authentication;
import com.gip.juno.ws.enums.DBSchema;
import com.gip.juno.ws.exceptions.DPPWebserviceAuthenticationException;
import com.gip.juno.ws.tools.DBCommands;
import com.gip.juno.ws.tools.QueryTools;
import com.gip.juno.ws.tools.SQLCommand;
import com.gip.juno.ws.tools.multiuser.MultiUserTools;
import com.gip.juno.ws.tools.xynarmi.XynaRmiWebServiceHandler;
import com.gip.xyna.xact.rmi.RMIConnectionFailureException;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionCredentials;

/**
 * class that checks username, password and user role by comparing with database tables
 * in schema aaa
 * 
 */
public class AuthenticationTools {
  
  public enum AuthenticationMode {
    LEGACY, SESSION, PRIVILEGED_SESSION
  }
  
  @Deprecated
  public static void authenticate(String username, String password, Logger logger) 
        throws java.rmi.RemoteException  {
     if (QueryTools.authenticate(username, password, logger) != Authentication.accepted) {
       logger.error("Access denied for user " + username);
       throw new DPPWebserviceAuthenticationException("Access denied.");
     }
  }
  
  @Deprecated
  public static void checkPermissionsDBEdit(String username, String schema, Logger logger) throws RemoteException {
    checkPermissions(username, schema.toLowerCase(), "edit", logger);
  }
  
  @Deprecated
  public static void checkPermissionsDBSelect(String username, String schema, Logger logger) throws RemoteException {
    checkPermissions(username, schema.toLowerCase(), "select", logger);
  }
  
  @Deprecated
  public static void checkPermissions(String username, String webservice, String wsMethod, 
        Logger logger) throws RemoteException {
    if (QueryTools.permissionsOK(username, webservice, wsMethod, logger)) {
      return;
    }
    logger.error("User " + username + " has no rights for the requested operation.");
    throw new DPPWebserviceAuthenticationException("User " + username + " has no rights for the requested operation.");
  }
  
  
    
  public static void authenticateAndAuthorize(String username, String password, String webservice, WebServiceInvocationIdentifier wsMethod, TableHandler tableHandler) throws RemoteException {
    if (wsMethod.getAuthenticationMode() != AuthenticationMode.PRIVILEGED_SESSION) {
      wsMethod.setSessionId(username);
    }
    XynaRmiWebServiceHandler.authenticateAndAuthorize(webservice, wsMethod.getAssociatedOperationIdentifier(), username, password, tableHandler.getLogger());
    if (tableHandler.supportsCollisionDetection() &&
        wsMethod.needsToSetRetrievalTimestamp()) {
      SQLCommand command = MultiUserTools.generateTableRetrievalTimestampUpdate(tableHandler.getDBTableInfo(), username);
      DBCommands.executeDML(DBSchema.aaa, command, tableHandler.getLogger());
    }    
  }
  
  
  public static void authenticateAndAuthorize(String username, String password, String webservice, WebServiceInvocationIdentifier wsMethod, Logger logger) throws RemoteException {
    if (wsMethod.getAuthenticationMode() != AuthenticationMode.PRIVILEGED_SESSION) {
      wsMethod.setSessionId(username);
    }
    if (wsMethod.needsToSetRetrievalTimestamp()) {
      throw new NullPointerException("No tableHandler specified for Authentication.");
    } else {
      XynaRmiWebServiceHandler.authenticateAndAuthorize(webservice, wsMethod.getAssociatedOperationIdentifier(), username, password, logger);
    }
  }
  
  
  public static SessionCredentials createSession(String username, String password, Logger logger) throws RemoteException {
    SessionCredentials credentials = XynaRmiWebServiceHandler.createSession(username, password);
    SQLCommand command = MultiUserTools.generateInsertGuiSession(username, credentials.getSessionId());
    /*int modedRows = */DBCommands.executeDML(DBSchema.aaa, command, logger);
    // TODO check modedRows and throw?
    return credentials;
    
  }
  
  
  public static final String SELECT_OPERATION_IDENTIFIER = "select";
  public static final String EDIT_OPERATION_IDENTIFIER = "edit"; 
  
  public static class WebServiceInvocationIdentifier {
    
    //does are no longer valid :-/
    public final static WebServiceInvocationIdentifier SELECTION_WEBSERVICE_IDENTIFIER = new WebServiceInvocationIdentifier(true, false, SELECT_OPERATION_IDENTIFIER);
    public final static WebServiceInvocationIdentifier MODIFICATION_WEBSERVICE_IDENTIFIER = new WebServiceInvocationIdentifier(false, true, EDIT_OPERATION_IDENTIFIER);
    public final static WebServiceInvocationIdentifier DELETION_WEBSERVICE_IDENTIFIER = new WebServiceInvocationIdentifier(false, true, EDIT_OPERATION_IDENTIFIER);
    public final static WebServiceInvocationIdentifier INSERTION_WEBSERVICE_IDENTIFIER = new WebServiceInvocationIdentifier(false, false, EDIT_OPERATION_IDENTIFIER);
    public final static WebServiceInvocationIdentifier DUMMY_WEBSERVICE_IDENTIFIER = new WebServiceInvocationIdentifier(false, false, "");
    
    private final boolean needsToSetRetrievalTimestamp;
    private final boolean needsCheckCollision;
    private final String associatedOperationIdentifier;
    private String sessionId;
    private AuthenticationMode authMode = AuthenticationMode.SESSION;
    
    public WebServiceInvocationIdentifier(String associatedOperationIdentifier) {
      this(associatedOperationIdentifier, AuthenticationMode.SESSION);
    }
    
    public WebServiceInvocationIdentifier(String associatedOperationIdentifier, AuthenticationMode authMode) {
      this(false, false, associatedOperationIdentifier);
      this.authMode = authMode;
    }
    
    private WebServiceInvocationIdentifier(boolean needsToSetRetrievalTimestamp, boolean needsCheckCollision,
                                           String associatedOperationIdentifier) {
      this.needsToSetRetrievalTimestamp = needsToSetRetrievalTimestamp;
      this.needsCheckCollision = needsCheckCollision;
      this.associatedOperationIdentifier = associatedOperationIdentifier;
    }
       
    public boolean needsToSetRetrievalTimestamp() {
      return needsToSetRetrievalTimestamp;
    }
    
    public boolean needsCheckCollision() {
      return needsCheckCollision;
    }
    
    public String getAssociatedOperationIdentifier() {
      return associatedOperationIdentifier;
    }
    
    
    public void setSessionId(String sessionId) {
      this.sessionId = sessionId;
    }
    
    public String getSessionId() {
      return sessionId;
    }
    
    public AuthenticationMode getAuthenticationMode() {
      return authMode;
    }
    
    public void setAuthenticationMode(AuthenticationMode authMode) {
      this.authMode = authMode;
    }
    
    
    public WebServiceInvocationIdentifier clone() {
      WebServiceInvocationIdentifier wsid = new WebServiceInvocationIdentifier(this.needsToSetRetrievalTimestamp, this.needsCheckCollision, this.associatedOperationIdentifier);
      wsid.authMode = this.authMode;
      return wsid;
    }

  }
  
}

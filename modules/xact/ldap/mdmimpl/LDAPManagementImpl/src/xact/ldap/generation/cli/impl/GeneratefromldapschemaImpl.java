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
package xact.ldap.generation.cli.impl;

import base.Credentials;
import base.Host;
import base.Port;

import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.utils.exceptions.XynaException;

import java.io.OutputStream;
import java.io.PrintStream;

import xact.ldap.generation.cli.generated.Generatefromldapschema;
import xact.ldap.generation.LDAPManagementImpl;



public class GeneratefromldapschemaImpl extends XynaCommandImplementation<Generatefromldapschema> {

  public void execute(OutputStream statusOutputStream, Generatefromldapschema payload) throws XynaException {
    String portValue = payload.getPort();
    if (portValue == null || portValue.length() <= 0) {
      portValue = factory.getXynaMultiChannelPortalPortal().getProperty("xact.ldap.defaultPort");
    }
    Port port;
    if (portValue == null || portValue.length() <= 0) {
      port = new Port(389);
    } else {
      port = new Port(Integer.parseInt(portValue));
    }
    Credentials creds = new Credentials(payload.getUsername(), payload.getPassword());
    try {
      LDAPManagementImpl.reloadLDAPSchemaAndRegenerateArtifacts(new Host(payload.getHost()), port, creds);
      writeLineToCommandLine(statusOutputStream, "Generation complete.");
    } catch (RuntimeException e) {
      Throwable toLog = e;
      if (e.getCause() != null) {
        toLog = e.getCause();
      }
      writeLineToCommandLine(statusOutputStream, "Generation failed: ");
      toLog.printStackTrace(new PrintStream(statusOutputStream));
    } 
  }

}

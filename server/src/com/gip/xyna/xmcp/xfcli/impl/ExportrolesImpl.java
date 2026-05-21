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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactoryPortal;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSDomainSpecificData;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSServer;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Domain;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.PredefinedCategories;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.LDAPDomainSpecificData;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.LDAPServer;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.SSLKeyAndTruststoreParameter;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.SSLKeystoreParameter;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Exportroles;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class ExportrolesImpl extends XynaCommandImplementation<Exportroles> {

  private static final Logger logger = CentralFactoryLogging.getLogger(ExportrolesImpl.class);


  public void execute(OutputStream statusOutputStream, Exportroles payload) throws XynaException {

    try {

      File scriptFile = new File(payload.getScriptName());
      if (scriptFile.exists()) {
        writeToCommandLine(statusOutputStream, "The script '" + payload.getScriptName() + "' could not be created\n");
        logger.warn("Could not create import script, file already exists");
        return;
      }

      if (!scriptFile.createNewFile()) {
        writeToCommandLine(statusOutputStream, "The script '" + payload.getScriptName() + "' could not be created\n");
        logger.warn("Could not create import script, insufficient rights?");
        return;
      }

      BufferedOutputStream scriptStream = new BufferedOutputStream(new FileOutputStream(scriptFile));

      if (!ExportrightsImpl.generateRightImports(factory, scriptStream)) {
        writeToCommandLine(statusOutputStream, "The script '" + payload.getScriptName() + "' could not be created\n");
        logger.warn("Could not create import script, error while writing to file");
        scriptStream.close();
        scriptFile.delete();
        return;
      }
      if (!generateDomainImports(factory, scriptStream)) {
        writeToCommandLine(statusOutputStream, "The script '" + payload.getScriptName() + "' could not be created\n");
        logger.warn("Could not create import script, error while writing to file");
        scriptStream.close();
        scriptFile.delete();
        return;
      }
      if (!generateRoleImports(factory, scriptStream, payload.getIncludePredefinedRoles())) {
        writeToCommandLine(statusOutputStream, "The script '" + payload.getScriptName() + "' could not be created\n");
        logger.warn("Could not create import script, error while writing to file");
        scriptStream.close();
        scriptFile.delete();
        return;
      }

      scriptStream.flush();
      scriptStream.close();

      writeToCommandLine(statusOutputStream, "The script '" + payload.getScriptName() + "' was succesfully created\n");

    } catch (IOException e) {
      throw new Ex_FileAccessException(payload.getScriptName(), e);
    }

  }


  static boolean generateRoleImports(XynaFactoryPortal factory, OutputStream scriptStream, boolean withPredefinedRoles)
      throws PersistenceLayerException {

    Collection<Role> roles = factory.getFactoryManagementPortal().getRoles();
    for (Role role : roles) {
      if (!factory.getFactoryManagementPortal().isPredefined(PredefinedCategories.ROLE,
                                                             role.getName() + role.getDomain()) || withPredefinedRoles) {
        try {
          scriptStream.write(("./" + Constants.SERVER_SHELLNAME + " createrole " + role.getName() + " "
              + role.getDomain() + "\n").getBytes(Constants.DEFAULT_ENCODING));

          List<String> rights = role.getRightsAsList();
          if (rights != null && rights.size() > 0) {
            for (String right : rights) {
              scriptStream.write(("./" + Constants.SERVER_SHELLNAME + " grantright " + role.getName() + " "
                              + right + "\n").getBytes(Constants.DEFAULT_ENCODING));
            }
          }
          Set<String> scopedRights = role.getScopedRights();
          if (scopedRights != null && scopedRights.size() > 0) {
            for (String right : scopedRights) {
              scriptStream.write(("./" + Constants.SERVER_SHELLNAME + " grantright " + role.getName() + " "
                              + right + "\n").getBytes(Constants.DEFAULT_ENCODING));
            }
          }
        } catch (IOException e) {
          return false;
        }
      }
    }
    for (Role role : roles) {
      try {
        // this is done afterwards to ensure that the role we want to mapped to is already created
        if (!(role.getAlias() == null || role.getAlias().equals(""))) {
          scriptStream.write(("./" + Constants.SERVER_SHELLNAME + " setalias " + role.getName() + " "
                        + role.getDomain() + " " + role.getAlias() + "\n").getBytes(Constants.DEFAULT_ENCODING));
        }
      } catch (IOException e) {
        return false;
      }
    }
    return true;
  }


  static boolean generateDomainImports(XynaFactoryPortal factory, OutputStream scriptStream)
      throws PersistenceLayerException {
    Collection<Domain> domains = factory.getFactoryManagementPortal().getDomains();
    for (Domain domain : domains) {
      if (!factory.getFactoryManagementPortal().isPredefined(PredefinedCategories.DOMAIN, domain.getName())) {
        try {
          scriptStream.write(("./xynafactory.sh createdomain " + domain.getName() + " " + domain.getDomainType()
              + domain.getMaxRetries() + " " + domain.getConnectionTimeout() + "\n")
              .getBytes(Constants.DEFAULT_ENCODING));
          if (domain.getDomainSpecificData() != null) {
            if (domain.getDomainSpecificData() instanceof RADIUSDomainSpecificData) {
              RADIUSDomainSpecificData data = (RADIUSDomainSpecificData) domain.getDomainSpecificData();
              scriptStream.write(("./xynafactory.sh setradiusspecificdata " + domain.getName() + " " + data.getAssociatedOrdertype()
                  + " ").getBytes(Constants.DEFAULT_ENCODING));
              for (RADIUSServer server : data.getServerList()) {
                scriptStream.write((server.getIp().getValue() + "," + server.getPort().getValue() + ","
                    + server.getPresharedKey().getKey() + " ").getBytes(Constants.DEFAULT_ENCODING));
              }
              scriptStream.write(("\n").getBytes(Constants.DEFAULT_ENCODING));
            } else if (domain.getDomainSpecificData() instanceof LDAPDomainSpecificData) {
              LDAPDomainSpecificData data = (LDAPDomainSpecificData) domain.getDomainSpecificData();
              scriptStream.write(("./xynafactory.sh setdomaintypespecificdata -domainName \"" + domain.getName() + "\" -domainTypeSpecificData ordertype=\"" + data.getAssociatedOrdertype() + "\" ").getBytes(Constants.DEFAULT_ENCODING));
              if (data.getRuntimeContext() instanceof Application) {
                Application app = (Application) data.getRuntimeContext();
                scriptStream.write(("application=\"" + app.getName() + "\" version=\"" + app.getVersionName() + "\"").getBytes(Constants.DEFAULT_ENCODING));
              } else if (data.getRuntimeContext() instanceof Workspace) {
                Workspace ws = (Workspace) data.getRuntimeContext();
                scriptStream.write(("workspace=\"" + ws.getName()+"\"").getBytes(Constants.DEFAULT_ENCODING));
              }
              scriptStream.write((" ").getBytes(Constants.DEFAULT_ENCODING));
              for (LDAPServer server : data.getServerList()) {
                scriptStream.write(("server=" + server.getHost() + "," + server.getPort()).getBytes(Constants.DEFAULT_ENCODING));
                if (server.getSSLParameter() != null) {
                  if (server.getSSLParameter() instanceof SSLKeystoreParameter) {
                    SSLKeystoreParameter sslParam = (SSLKeystoreParameter) server.getSSLParameter();
                    writeSSLParams(scriptStream, sslParam, true);
                  } else if (server.getSSLParameter() instanceof SSLKeyAndTruststoreParameter) {
                    SSLKeyAndTruststoreParameter sslParam = (SSLKeyAndTruststoreParameter) server.getSSLParameter();
                    writeSSLParams(scriptStream, sslParam.getSSLKeystore(), false);
                    scriptStream.write((",").getBytes(Constants.DEFAULT_ENCODING));  
                    writeSSLParams(scriptStream, sslParam.getSSLTruststore(), false);
                  }
                }
                scriptStream.write((" ").getBytes(Constants.DEFAULT_ENCODING));  
              }
              scriptStream.write(("\n").getBytes(Constants.DEFAULT_ENCODING));
            }
          }
        } catch (IOException e) {
          return false;
        }
      }
    }
    return true;
  }
  
  
  private static void writeSSLParams(OutputStream scriptStream, SSLKeystoreParameter sslParam, boolean skipPassphraseIfUndefined) throws UnsupportedEncodingException, IOException {
    scriptStream.write((","+ sslParam.getPath() + "," + sslParam.getType()).getBytes(Constants.DEFAULT_ENCODING));
    if (sslParam.getPassphrase() != null && sslParam.getPassphrase().length() > 0) {
      scriptStream.write((","+ sslParam.getPassphrase()).getBytes(Constants.DEFAULT_ENCODING));  
    } else if (!skipPassphraseIfUndefined) {
      scriptStream.write((",").getBytes(Constants.DEFAULT_ENCODING));
    }
  }

}

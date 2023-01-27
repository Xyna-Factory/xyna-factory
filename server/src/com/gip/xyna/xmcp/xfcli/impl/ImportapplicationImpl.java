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

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ImportApplicationCommandParameter;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.XMOMODSNameImportSetting;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Importapplication;



public class ImportapplicationImpl extends XynaCommandImplementation<Importapplication> {

  private static final Logger logger = CentralFactoryLogging.getLogger(ImportapplicationImpl.class);
      
  public void execute(OutputStream statusOutputStream, Importapplication payload) throws XynaException {
   
    CommandLineWriter clw = CommandLineWriter.createCommandLineWriter(statusOutputStream);
    ApplicationManagementImpl applicationManagement = (ApplicationManagementImpl) XynaFactory.getInstance()
                    .getFactoryManagement().getXynaFactoryControl().getApplicationManagement();

    
    ExcludeIncludeSetttings eis = new ExcludeIncludeSetttings(clw);
    eis.configure(payload);
    eis.writeMessages();
    
    TemporarySessionAuthentication tsa =
                    TemporarySessionAuthentication.tempAuthWithUniqueUser("ImportApplication", TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE);
    tsa.initiate();
    
    boolean upgradeRequirements = !(payload.getFailOnIncompatibleRequirements());
    try {
      ImportApplicationCommandParameter iap = new ImportApplicationCommandParameter();
      iap.fileName(payload.getFilename())
         .abortOnCodegeneration(payload.getNoCodeGeneration())
         .force(payload.getForce())
         .stopIfExistingAndRunning(payload.getStop())
         .xynaPropertiesImportSettings(!eis.isIncludeXynaProperties(), payload.getImportOnlyXynaProperties())
         .capacitiesImportSettings(!eis.isIncludeCapacities(), payload.getImportOnlyCapacities())
         .clusterwide(payload.getGlobal())
         .regenerateCode(payload.getRegenerate())
         .verbose(payload.getVerbose())
         .user(tsa.getUsername())
         .statusOutputStream(clw.getPrintStream())
         .upgradeRequirements(upgradeRequirements)
         .odsNames(XMOMODSNameImportSetting.byName(payload.getStorableNameGeneration()));
      
      ApplicationInformation ai = applicationManagement.importApplication(iap);
      if( ai != null ) {
        clw.writeLineToCommandLine( "Imported application \""+ai.getName()+"\" in version \""+ai.getVersion()+"\".");
      }
    } finally {
      tsa.destroy();
    }

  }

  private static class ExcludeIncludeSetttings {
    boolean includeXynaProperties = false;
    boolean includeCapacities = false;
    
    boolean capacitiesConfigured = false;
    boolean xynapropertiesConfigured = false;
    private CommandLineWriter clw;

    public ExcludeIncludeSetttings(CommandLineWriter clw) {
      this.clw = clw;
    }

    public boolean isIncludeCapacities() {
       return includeCapacities;
    }

    public boolean isIncludeXynaProperties() {
      return includeXynaProperties;
    }

    public void configure(Importapplication payload) {
      //importOnlyX > includeX > (keine angabe | exclude X)
      
      
      if (payload.getIncludeXynaProperties()) {
        includeXynaProperties = true;
        xynapropertiesConfigured = true;
      }
      if (payload.getIncludeCapacities()) {
        includeCapacities = true;
        capacitiesConfigured = true;
      }
      
      if (payload.getExcludeCapacities()) {
        if (includeCapacities) {
          throw new IllegalArgumentException("Capacities can not be both, included and excluded.");
        }
        //ok
        capacitiesConfigured = true;
      }
      if (payload.getExcludeXynaProperties()) {
        if (includeXynaProperties) {
          throw new IllegalArgumentException("XynaProperties can not be both, included and excluded.");
        }
        //ok
        xynapropertiesConfigured = true;
      }
      
      if (payload.getImportOnlyCapacities() || payload.getImportOnlyXynaProperties()) {
        capacitiesConfigured = true;
        xynapropertiesConfigured = true;
      }

    }

    public void writeMessages() {
      if (!capacitiesConfigured) {
        if (!xynapropertiesConfigured) {
          msg("XynaProperties and Capacities will NOT be imported.");
        } else {
          msg("Capacities will NOT be imported.");
        }
      } else if (!xynapropertiesConfigured) {
        msg("XynaProperties will NOT be imported.");
      }
    }
    
    private void msg(String msg) {
      clw.writeLineToCommandLine(msg);
      logger.info(msg);
    }


  }
  
}

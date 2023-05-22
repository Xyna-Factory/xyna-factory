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
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Filter;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotLockOperation;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotStopApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotUnlockOperation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationDefinitionInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Stopapplication;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;



public class StopapplicationImpl extends XynaCommandImplementation<Stopapplication> {

  public void execute(OutputStream statusOutputStream, Stopapplication payload) throws XynaException {
    
    ApplicationManagementImpl applicationManagement = (ApplicationManagementImpl) XynaFactory.getInstance()
                    .getFactoryManagement().getXynaFactoryControl().getApplicationManagement();

    Optional<EnumSet<OrderEntranceType>> orderEntrances = null;
    try {
      orderEntrances = createOptional(payload.getDisableOrderEntrance());
    } catch (IllegalArgumentException e) {
      writeLineToCommandLine(statusOutputStream, e.getMessage() );
      writeEndToCommandLine(statusOutputStream, ReturnCode.XYNA_EXCEPTION);
      return;
    }
    
    Application application = new Application(payload.getApplicationName(), payload.getVersionName());
    if( applicationExists(application) ) {
      //Stoppen der Application
      stopApplication(statusOutputStream, applicationManagement, application, payload.getGlobal(), orderEntrances);
      return;
    }
    
    //Sind ApplicationName und VersionName evtl. Pattern?
    ApplicationFilter filter = new ApplicationFilter(application);
    if( ! filter.hasPattern() ) {
      writeLineToCommandLine(statusOutputStream, "Application \""+application.getName()+"\" in version \""+application.getVersionName()+"\" does not exist." );
      writeEndToCommandLine(statusOutputStream, ReturnCode.XYNA_EXCEPTION);
      return;
    }
    
    //Filtern aller ApplicationInformation nach den Patterns
    List<ApplicationInformation> apps = 
        CollectionUtils.filter(applicationManagement.listApplications(false, false), filter );
    if( apps.isEmpty() ) {
      writeLineToCommandLine(statusOutputStream, "No Application matches name=\""+application.getName()+"\" and version=\""+application.getVersionName()+"\"." );
      writeEndToCommandLine(statusOutputStream, ReturnCode.XYNA_EXCEPTION);
      return;
    }
    
    //Stoppen der Applications
    for( ApplicationInformation ai : apps ) {
      application = new Application(ai.getName(), ai.getVersion());
      stopApplication(statusOutputStream, applicationManagement, application, payload.getGlobal(), orderEntrances);
    }
  }

  private boolean applicationExists(Application application) {
    RevisionManagement rm = XynaFactory.getInstance()
        .getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    try {
      rm.getRevision(application);
      return true;
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return false;
    }
  }

  private Optional<EnumSet<OrderEntranceType>> createOptional(String[] orderEntrance) {
    Optional<EnumSet<OrderEntranceType>> orderEntrances;
    if (orderEntrance != null && orderEntrance.length > 0) {
      EnumSet<OrderEntranceType> orderEntranceTypes = EnumSet.noneOf(OrderEntranceType.class);
      for (String type : orderEntrance) {
        try {
          orderEntranceTypes.add(OrderEntranceType.valueOf(type));
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException("Unknown order entrance: '" + type + "'");
        }
      }
      orderEntrances = new Optional<EnumSet<OrderEntranceType>>(orderEntranceTypes);
    } else {
      orderEntrances = Optional.empty();
    }
    return orderEntrances;
  }

  private void stopApplication(OutputStream statusOutputStream, ApplicationManagementImpl applicationManagement, Application application, boolean global, Optional<EnumSet<OrderEntranceType>> orderEntrances ) throws XFMG_CouldNotLockOperation, XFMG_CouldNotStopApplication, XFMG_CouldNotUnlockOperation {
    CommandControl.tryLock(CommandControl.Operation.APPLICATION_STOP, application);
    try {
      writeLineToCommandLine(statusOutputStream, "Stopping Application name \""+application.getName()+"\" with version \""+application.getVersionName()+"\"" );
      applicationManagement.stopApplication(application.getName(), application.getVersionName(), global, orderEntrances);  
    } finally {
      CommandControl.unlock(CommandControl.Operation.APPLICATION_STOP, application );
    }
  }
  
  private static class ApplicationFilter implements Filter<ApplicationInformation> {

    private Pattern namePattern;
    private Pattern versionPattern;
    private boolean hasPattern = false;

    public ApplicationFilter(Application app) {
      this.namePattern = createPattern(app.getName());
      this.versionPattern = createPattern(app.getVersionName());
    }

    public boolean hasPattern() {
      return hasPattern;
    }

    private Pattern createPattern(String pattern) {
      if( pattern.contains("\\E") ) {
        hasPattern = true;
      }
      return Pattern.compile("\\Q"+pattern+"\\E");
    }

    public boolean accept(ApplicationInformation value) {
      boolean isDefinition = value instanceof ApplicationDefinitionInformation;
      if( isDefinition ) {
        return false; //keine Definition gew�nscht
      }
      if( ! namePattern.matcher( value.getName() ).matches() ) {
        return false; //Name passt nicht
      }
      if( ! versionPattern.matcher( value.getVersion() ).matches() ) {
        return false; //Version passt nicht
      }
      return true;
    }
    
  }

}

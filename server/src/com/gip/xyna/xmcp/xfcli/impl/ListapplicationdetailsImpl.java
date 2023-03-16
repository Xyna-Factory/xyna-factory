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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotImportApplication;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationDefinitionInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationState;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationStorable;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RuntimeContextProblem;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RuntimeContextProblem.UnresolvableRequirement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listapplicationdetails;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class ListapplicationdetailsImpl extends XynaCommandImplementation<Listapplicationdetails> {
  
  public void execute(OutputStream statusOutputStream, Listapplicationdetails payload) throws XynaException {

    CommandLineWriter clw = CommandLineWriter.createCommandLineWriter(statusOutputStream);
    
    ApplicationSelection as = new ApplicationSelection(clw);
    as.setApplication(payload.getApplicationName(), payload.getVersionName() );
    as.setParentWorkspace(payload.getParentWorkspace());
    as.setFileName(payload.getFileName());
    
    
    if( ! as.check() ) {
      return; //Fehlerhafte Angaben, daher fertig 
    }
    
    ApplicationInformation appInfo = as.findApplication();
    if( appInfo == null ) {
      clw.writeLineToCommandLine("No information found for "+as.getIdentificationString()+".");
      return;
    }
    
    
    //Ausgabe
    /////////
    if( payload.getVersionOnly()) {
      clw.writeToCommandLine(appInfo.getVersion());
      clw.writeEndToCommandLine(ReturnCode.SUCCESS);
      return;
    }
    
    if( payload.getOnlyMissingRequirements() ) {
      ReturnCode rc = showOnlyMissingRequirements(clw, appInfo);
      clw.writeEndToCommandLine(rc);
      return;
    }
    showApplicationName(clw, appInfo);
    
    showApplicationInfo(clw, appInfo, as.getFoundApplicationCount());
    
    showApplicationProblems(clw, appInfo);
    
    List<String> excludeEntryTypes = payload.getExcludeEntryTypes() == null ? Collections.<String>emptyList() : Arrays.asList(payload.getExcludeEntryTypes());
    if( excludeEntryTypes.contains("ALL") ) {
      return;
    } else {
      List<ApplicationEntryStorable> entries = as.getApplicationDetails(appInfo, payload.getVerbose(), payload.getExcludeSubtypesOf());
      if (entries == null || entries.isEmpty()) {
        if( appInfo.getState() == ApplicationState.FILE ) {
          clw.writeLineToCommandLine( "No entries read.");
        } else {
          clw.writeLineToCommandLine( "Application is empty");
        }
        return;
      }
      for( ApplicationEntryType type : ApplicationEntryType.values() ) {
        if( ! excludeEntryTypes.contains(type.name()) ) {
          showApplicationEntries(clw, type, entries);
        }
      }
      //Auftragseingangsschnittstellen für RuntimeApplications
      if (!(appInfo instanceof ApplicationDefinitionInformation)) {
        StringBuilder sb = new StringBuilder();
        as.appendOrderEntryInterfaces(sb, appInfo.getName(), appInfo.getVersion());
        clw.writeLineToCommandLine( "ORDER ENTRY INTERFACES:\n", sb);
      }
    }

  }
  





  private void showApplicationName(CommandLineWriter clw, ApplicationInformation appInfo) {
    StringBuilder header = new StringBuilder();
    header.append(appInfo.getName()).append(" ").append(appInfo.getVersion());
    if (appInfo.getComment() != null && appInfo.getComment().length() > 0) {
      header.append(" - ").append(appInfo.getComment());
    }
    clw.writeLineToCommandLine(header);
  }
  
  private void showApplicationInfo(CommandLineWriter clw, ApplicationInformation appInfo, int foundApplicationCount) {
    StringBuilder header = new StringBuilder();
    
    
    header.append("\n state ").append(appInfo.getState());
    if( appInfo.getState() == ApplicationState.FILE ) {
      if( foundApplicationCount > 0 ) {
        header.append("\n application seems to be already installed");
      }
    } else if( appInfo instanceof ApplicationDefinitionInformation ) {
      if( foundApplicationCount > 0 ) {
        header.append("\n ").append(foundApplicationCount).append(" applications created for this name");
      }
    } else {
      if( foundApplicationCount > 1 ) {
        header.append("\n ").append(foundApplicationCount).append(" older applications exists");
      }
    }
    if( appInfo.getBuildDate() != null ) {
      header.append("\n buildDate: ").append(appInfo.getBuildDate());
    }
    if( appInfo.getDescription() != null ) {
      for( DocumentationLanguage lang : LANGUAGE_PREFERENCE.get() ) {
        String desc = appInfo.getDescription().get(lang);
        if( desc != null ) {
          header.append("\n description: ").append(desc);
          break;
        }
      }
    }
    if( appInfo.getRequirements() != null ) {
      switch( appInfo.getRequirements().size() ) {
        case 0: 
          break;
        case 1: header.append("\n requires ").append(appInfo.getRequirements().iterator().next());
          break;
        default:
          header.append("\n requires: ");
          for( RuntimeDependencyContext rc : appInfo.getRequirements() ) {
            header.append("\n  ").append(rc);
          }
      }
    }
    clw.writeLineToCommandLine(header);
  }
  
  private void showApplicationProblems(CommandLineWriter clw, ApplicationInformation appInfo) {
    StringBuilder header = new StringBuilder();
    if( appInfo.getProblems() != null ) {
      switch( appInfo.getProblems().size() ) {
        case 0: 
          break;
        case 1:
          header.append("\n has problem ")
                .append(appInfo.getProblems().get(0).getMessage());
          break;
        default:
          header.append("\n has problems: ");
          for( RuntimeContextProblem p : appInfo.getProblems() ) {
            header.append("\n  ").append(p.getMessage());
          }
      }
      clw.writeLineToCommandLine(header);
    }
  }
  
  private ReturnCode showOnlyMissingRequirements(CommandLineWriter clw, ApplicationInformation appInfo) {
    if( appInfo.getProblems() == null ) {
      return ReturnCode.SUCCESS;
    }
    StringBuilder sb = new StringBuilder();
    String sep = "";
    for( RuntimeContextProblem p : appInfo.getProblems() ) {
      if(p instanceof UnresolvableRequirement) {
        RuntimeDependencyContext rc = ((UnresolvableRequirement)p).getRuntimeContext();
        if( rc instanceof Application ) {
          Application a = (Application)rc;
          sb.append(sep).append(a.getName()).append(" ").append(a.getVersionName());
          sep = "\n";
        } else if( rc instanceof Workspace ) {
          Workspace a = (Workspace)rc;
          sb.append(sep).append(a.getName());
          sep = "\n";
        } else {
          //FIXME was nun?
        }
      }
    }
    clw.writeLineToCommandLine(sb);
    return appInfo.getProblems().isEmpty() ? ReturnCode.SUCCESS : ReturnCode.SUCCESS_WITH_PROBLEM;
  }

  
  private void showApplicationEntries(CommandLineWriter clw, ApplicationEntryType type, List<ApplicationEntryStorable> entries) {
    SortedSet<ApplicationEntryStorable> entriesForType =
        new TreeSet<ApplicationEntryStorable>(ApplicationEntryStorable.COMPARATOR);
    for (ApplicationEntryStorable entry : entries) {
      if (entry.getTypeAsEnum() == type) {
        entriesForType.add(entry);
      }
    }
    if (!entriesForType.isEmpty()) {
      clw.writeLineToCommandLine(type.toString() + ":");
      for (ApplicationEntryStorable entry : entriesForType) {
        clw.writeLineToCommandLine(" " + entry.getName());
      }
    }
  }

  private static class ApplicationSelection {

    private CommandLineWriter clw;
    private ApplicationManagementImpl applicationManagement;
    private RevisionManagement revisionManagement;
    private String fileName;
    private String applicationName;
    private String versionName;
    private String parentWorkspace;
    private ApplicationInformation foundApplication;
    private int foundApplicationCount;
    
    public ApplicationSelection(CommandLineWriter clw) {
      this.clw = clw;
      XynaFactoryControl xfc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl();
      this.applicationManagement = (ApplicationManagementImpl) xfc.getApplicationManagement();
      this.revisionManagement = xfc.getRevisionManagement();
    }


    public int getFoundApplicationCount() {
      return foundApplicationCount;
    }


    public void appendOrderEntryInterfaces(StringBuilder output, String name, String version) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      applicationManagement.appendOrderEntryInterfaces(output, name, version); //FIXME auslagern
    }


    public List<ApplicationEntryStorable> getApplicationDetails(ApplicationInformation appInfo, boolean verbose, String[] excludeSubtypesOf) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      if( fileName == null ) {
        List<String> excludeSubtypesOfList = (excludeSubtypesOf != null) ? Arrays.asList(excludeSubtypesOf) : Collections.<String>emptyList();
        
        Long parentRevision = null;
        if (appInfo instanceof ApplicationDefinitionInformation) {
          Workspace parentWorkspace = ((ApplicationDefinitionInformation) appInfo).getParentWorkspace();
          parentRevision = revisionManagement.getRevision(parentWorkspace);
        }
        
        List<ApplicationEntryStorable> result =
            applicationManagement.listApplicationDetails(appInfo.getName(), appInfo.getVersion(), verbose, 
                                                         excludeSubtypesOfList, parentRevision );
        return result;
      } else {
        return Collections.emptyList(); //FIXME
      }
    }


    public String getIdentificationString() {
     return "application '" + applicationName + "', version '" + versionName + "'"; //FIXME
    }


    public void setApplication(String applicationName, String versionName) {
      this.applicationName = applicationName;
      this.versionName = versionName;
    }
    
    public void setFileName(String fileName) {
      this.fileName = fileName;
    }
    
    public void setParentWorkspace(String parentWorkspace) {
      this.parentWorkspace = parentWorkspace;
    }
    
    public boolean check() {
      boolean success = true;
      if( fileName == null ) {
        //applicationName muss gesetzt sein, versionName und parentWorkspace sind optional
        if( applicationName == null ) {
          clw.writeLineToCommandLine("applicationName or fileName is required");
          success = false;
        } 
      } else {
        //fileName ist gesetzt, daher dürfen applicationName, versionName und parentWorkspace nicht gesetzt sein
        if( applicationName != null ) {
          clw.writeLineToCommandLine("applicationName and fileName must not be set at once");
          success = false;
        }
        if( versionName != null || parentWorkspace != null ) {
          clw.writeLineToCommandLine("versionName or parentWorkspace can not be set if fileName is set");
          success = false;
        }
      }
      if( ! success ) {
        clw.writeEndToCommandLine(ReturnCode.XYNA_EXCEPTION);
      }
      return success;
    }
    
    public ApplicationInformation findApplication() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XFMG_CouldNotImportApplication, PersistenceLayerException {
      foundApplication = null;
      foundApplicationCount = 0;
      
      String name = null; 
      String version = null;
      Long parentRevision = null;
      
      if( fileName == null ) {
        name = applicationName;
        version = versionName;
        parentRevision = getParentRevision();
      } else {
        foundApplication = applicationManagement.getApplicationInformationForApplicationFile(fileName, clw.getPrintStream());
        if( foundApplication == null ) {
          return null;
        }
        name = foundApplication.getName();
        version = foundApplication.getVersion();
      }
      
      Collection<ApplicationStorable> appInfos = applicationManagement.listApplicationStorables();
      Pair<List<ApplicationStorable>, List<ApplicationStorable>> pair = 
          filterApplicationInformations(appInfos, name, version, parentRevision);
      List<ApplicationStorable> defs = pair.getFirst();
      List<ApplicationStorable> apps = pair.getSecond();
      foundApplicationCount = apps.size();
      
      if( foundApplication == null ) {
        ApplicationStorable foundApp = null;
        if( defs.size() != 0 ) {
          foundApp = defs.get(0); //es sollte nur eine geben
        } else {
          //aktuellste App anzeigen
          if( foundApplicationCount == 1 ) {
            foundApp = apps.get(0); //gesuchte Application gefunden
          } else if( foundApplicationCount > 1 ) {
            //Mehr als eine Application gefunden, nun die mit höchster Revision zurückgeben
            long maxRev = -1;
            for( ApplicationStorable ai : apps ) {
              long r = revisionManagement.getRevision(ai.getName(), ai.getVersion(), null );
              if( r > maxRev ) {
                foundApp = ai;
              }
            }
          }
        }
        
        if (foundApp != null) {
          foundApplication = applicationManagement.getApplicationInformation(foundApp, true);
        }
      }
      return foundApplication;
    }
    
    private Pair<List<ApplicationStorable>, List<ApplicationStorable>> filterApplicationInformations(Collection<ApplicationStorable> appInfos, String name, String version, Long parentRevision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      List<ApplicationStorable> adis = new ArrayList<ApplicationStorable>();
      List<ApplicationStorable> ais = new ArrayList<ApplicationStorable>();
      for (ApplicationStorable appInfo : appInfos) {
        if ( ! appInfo.getName().equals(name) ) {
          continue;
        }
        
        if( version != null && ! appInfo.getVersion().equals(version) ) {
          continue;
        }

        if (appInfo.isApplicationDefinition()) {
          if ( appInfo.getParentRevision().equals(parentRevision)) {
            adis.add(appInfo);
          }
        } else {
          ais.add(appInfo);
        }
      }
      return Pair.of(adis,ais);
    }

    private Long getParentRevision() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      Long parentRevision = null;
      if (parentWorkspace != null) {
        parentRevision = revisionManagement.getRevision(null, null, parentWorkspace);
      } else if (versionName == null || 
        revisionManagement.isApplicationDefinition(applicationName,
                                                   versionName,
                                                   RevisionManagement.REVISION_DEFAULT_WORKSPACE)) {
        parentRevision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
      }
      return parentRevision;
    }

  }

  
}

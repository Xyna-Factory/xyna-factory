/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package xfmg.oas.generation.impl;



import org.openapitools.codegen.OpenAPIGenerator;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ApplicationPartImportMode;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ImportApplicationParameter;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.LocalRuntimeContextManagementSecurity;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionManagement;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import org.apache.log4j.Logger;

import base.File;
import xfmg.oas.generation.ApplicationGenerationParameter;
import xfmg.oas.generation.ApplicationGenerationServiceOperation;
import xfmg.oas.generation.cli.generated.OverallInformationProvider;
import xfmg.oas.generation.cli.impl.BuildoasapplicationImpl;
import xfmg.xfctrl.filemgmt.ManagedFileId;
import xmcp.forms.plugin.Plugin;
import xprc.xpce.Application;
import xprc.xpce.Workspace;

import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;


public class ApplicationGenerationServiceOperationImpl implements ExtendedDeploymentTask, ApplicationGenerationServiceOperation {

  private static final LocalRuntimeContextManagementSecurity localLrcms =
      new LocalRuntimeContextManagementSecurity();
  private static final SessionManagement sessionManagement = 
      XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement();
  private static final FileManagement fileManagement = 
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
  private static Logger logger = CentralFactoryLogging.getLogger(ApplicationGenerationServiceOperationImpl.class);
  
  
  public void onDeployment() throws XynaException {
    OverallInformationProvider.onDeployment();
    Plugin plugin = createPlugin();
    if (plugin != null) {
      xmcp.forms.plugin.PluginManagement.registerPlugin(plugin);
    }
  }


  public void onUndeployment() throws XynaException {
    OverallInformationProvider.onUndeployment();
    Plugin plugin = createPlugin();
    if (plugin != null) {
      xmcp.forms.plugin.PluginManagement.unregisterPlugin(plugin);
    }
  }


  public Long getOnUnDeploymentTimeout() {
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    return null;
  }


  private Plugin createPlugin() {
    Plugin.Builder plugin = new Plugin.Builder();
    plugin.navigationEntryLabel("OAS Import");
    plugin.navigationEntryName("OAS Import");
    plugin.definitionWorkflowFQN("xmcp.oas.fman.GetOASImportHistoryDefinition");
    xprc.xpce.RuntimeContext rtc = getOwnRtc();
    if (rtc == null) {
      return null;
    }
    plugin.pluginRTC(rtc);
    return plugin.instance();
  }


  private xprc.xpce.RuntimeContext getOwnRtc() {
    try {
      ClassLoaderBase clb = (ClassLoaderBase) getClass().getClassLoader();
      Long revision = clb.getRevision();
      RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      RuntimeContext rtc = rm.getRuntimeContext(revision);
      if(rtc instanceof com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application) {
        return new Application(rtc.getName(), ((com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application)rtc).getVersionName());
      } else {
        return new Workspace(rtc.getName());
      }
    } catch(Exception e) {
      logger.error("Could not determine RTC.", e);
      return null;
    }
  }

  @Override
  public void generateApplication(XynaOrderServerExtension correlatedXynaOrder, ApplicationGenerationParameter applicationGenerationParameter1, File file4) {
    String swagger = file4.getPath();
    String target = "/tmp/Order_" + correlatedXynaOrder.getId();

    try {
      OpenAPIGenerator.main(new String[] {"validate", "-i", swagger, "--recommend"});
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    BuildoasapplicationImpl oasAppBuilder = new BuildoasapplicationImpl();
    
    String id;
    
    id = oasAppBuilder.createOasApp("xmom-data-model", target + "_datatypes", swagger);
    importApplication(correlatedXynaOrder, id);
    
    if (applicationGenerationParameter1.getGenerateProvider()) {
      id = oasAppBuilder.createOasApp("xmom-server", target + "_provider", swagger);
      importApplication(correlatedXynaOrder, id);
    }
    if (applicationGenerationParameter1.getGenerateClient()) {
      id = oasAppBuilder.createOasApp("xmom-client", target + "_client", swagger);
      importApplication(correlatedXynaOrder, id);
    }
  }
  
  private void importApplication(XynaOrderServerExtension correlatedXynaOrder, String id) {
    try {
      String user = sessionManagement.resolveSessionToUser(correlatedXynaOrder.getSessionId());
      ImportApplicationParameter iap = ImportApplicationParameter.with(ApplicationPartImportMode.EXCLUDE,
                                                                       ApplicationPartImportMode.EXCLUDE,
                                                                       true,
                                                                       true,
                                                                       user);
      localLrcms.importApplication(correlatedXynaOrder.getCreationRole(), iap, id);

    } catch (Exception ex) {
      throw new RuntimeException(ex.getMessage(), ex);
    }
  }
  
  @Override
  public void generateApplicationByManagedFileID(XynaOrderServerExtension correlatedXynaOrder, ApplicationGenerationParameter applicationGenerationParameter2, ManagedFileId managedFileId3) {
    File file = new File.Builder()
        .path(fileManagement.getAbsolutePath(managedFileId3.getId()))
        .instance();
    generateApplication(correlatedXynaOrder, applicationGenerationParameter2, file);
  }
}

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



import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ApplicationPartImportMode;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ImportApplicationParameter;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.LocalRuntimeContextManagementSecurity;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionManagement;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import base.File;
import base.math.IntegerNumber;
import xfmg.oas.generation.ApplicationGenerationParameter;
import xfmg.oas.generation.ApplicationGenerationServiceOperation;
import xfmg.oas.generation.cli.generated.OverallInformationProvider;
import xfmg.oas.generation.cli.impl.BuildoasapplicationImpl;
import xfmg.oas.generation.cli.impl.BuildoasapplicationImpl.ValidationResult;
import xfmg.xfctrl.appmgmt.RuntimeContextService;
import xfmg.xfctrl.filemgmt.ManagedFileId;
import xmcp.forms.plugin.Plugin;
import xprc.xpce.Application;
import xprc.xpce.RuntimeContext;
import xprc.xpce.Workspace;

import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;


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
    try {
      Plugin plugin = createPlugin();
      if (plugin != null) {
        xmcp.forms.plugin.PluginManagement.registerPlugin(plugin);
      }
    } catch (Exception e) {
      logger.error("Could not register oas plugin.", e);
    }
  }


  public void onUndeployment() throws XynaException {
    OverallInformationProvider.onUndeployment();
    try {
      Plugin plugin = createPlugin();
      if (plugin != null) {
        xmcp.forms.plugin.PluginManagement.unregisterPlugin(plugin);
      }
    } catch(Exception e) {
      logger.error("Could not unregister oas plugin.", e);
    }
  }


  public Long getOnUnDeploymentTimeout() {
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    return null;
  }


  private Plugin createPlugin() {
    String entryName = "OAS Import";
    RuntimeContext rtc = getOwnRtc();
    if (rtc == null) {
      return null;
    }
    if (rtc instanceof Application) {
      entryName = entryName + " " + ((Application) rtc).getVersion();
    }
    Plugin.Builder plugin = new Plugin.Builder();
    plugin.navigationEntryLabel(entryName);
    plugin.navigationEntryName(entryName);
    plugin.definitionWorkflowFQN("xmcp.oas.fman.GetOASImportHistoryDefinition");
    plugin.pluginRTC(rtc);
    return plugin.instance();
  }


  private RuntimeContext getOwnRtc() {
    ClassLoaderBase clb = (ClassLoaderBase) getClass().getClassLoader();
    Long revision = clb.getRevision();
    return RuntimeContextService.getRuntimeContextFromRevision(new IntegerNumber(revision));
  }

  @Override
  public void generateApplication(XynaOrderServerExtension correlatedXynaOrder, ApplicationGenerationParameter applicationGenerationParameter1, File file4) {
    
    BuildoasapplicationImpl oasAppBuilder = new BuildoasapplicationImpl();
    
    String specFile = file4.getPath();
    String target = "/tmp/Order_" + correlatedXynaOrder.getId();

    ValidationResult result = oasAppBuilder.validate(specFile);
    StringBuilder errors = new StringBuilder("Validation found errors:");
    if (!result.getErrors().isEmpty()) {
      logger.error("Spec: " + specFile + " contains errors.");
      result.getErrors().forEach(error -> {
        logger.error(error);
        errors.append(" ");
        errors.append(error);
      });
    }
    if (!result.getWarnings().isEmpty()) {
      logger.warn("Spec: " + specFile + " contains warnings.");
      result.getWarnings().forEach(warning -> logger.warn(warning));
    }
    if (!result.getErrors().isEmpty()) {
      throw new RuntimeException(errors.toString());
    }
    
    String id;
    
    id = oasAppBuilder.createOasApp("xmom-data-model", target + "_datatypes", specFile);
    importApplication(correlatedXynaOrder, id);
    
    if (applicationGenerationParameter1.getGenerateProvider()) {
      id = oasAppBuilder.createOasApp("xmom-server", target + "_provider", specFile);
      importApplication(correlatedXynaOrder, id);
    }
    if (applicationGenerationParameter1.getGenerateClient()) {
      id = oasAppBuilder.createOasApp("xmom-client", target + "_client", specFile);
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
    String path = fileManagement.getAbsolutePath(managedFileId3.getId());
    if(fileManagement.getFileInfo(managedFileId3.getId()).getOriginalFilename().endsWith(".zip")) {
      path = BuildoasapplicationImpl.decompressArchive(path);
    }
    File file = new File.Builder()
        .path(path)
        .instance();
    generateApplication(correlatedXynaOrder, applicationGenerationParameter2, file);
  }
}

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



import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import base.File;
import base.math.IntegerNumber;
import xfmg.oas.generation.ApplicationGenerationParameter;
import xfmg.oas.generation.ApplicationGenerationServiceOperation;
import xfmg.oas.generation.cli.generated.OverallInformationProvider;
import xfmg.oas.generation.storage.OasImportHistorySortTool;
import xfmg.oas.generation.storage.OasImportHistoryStorage;
import xfmg.oas.generation.tools.GenerateApplicationTool;
import xfmg.xfctrl.appmgmt.RuntimeContextService;
import xfmg.xfctrl.filemgmt.ManagedFileId;
import xmcp.forms.plugin.Plugin;
import xmcp.oas.fman.storables.OAS_ImportHistory;
import xmcp.tables.datatypes.TableInfo;
import xprc.xpce.Application;
import xprc.xpce.RuntimeContext;


public class ApplicationGenerationServiceOperationImpl implements ExtendedDeploymentTask, ApplicationGenerationServiceOperation {

  // Used in xmcp.oas.datatype.client.ClientProcessingHook and xmcp.oas.datatype.OASBaseApi
  @SuppressWarnings("unused")
  private static final XynaPropertyBoolean defaultValidation = new XynaPropertyBoolean("xmcp.oas.validation.default", true)
      .setDefaultDocumentation(DocumentationLanguage.DE, "Bestimmt das Standardverhalten, ob validiert werden soll. Gilt sowohl für Provider als auch Client, request und response.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Default behavior, if it should be validated. Counts for Provider and Client, Request and Response.");

  public static final XynaPropertyBoolean createListWrappers = new XynaPropertyBoolean("xfmg.oas.create_list_wrappers", false)
      .setDefaultDocumentation(DocumentationLanguage.EN, "Create an XmomObject for Schemas of type array")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Erzeuge Xmom Objekte für Schemas mit Typ array");

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
    createListWrappers.registerDependency(UserType.Service, "OAS_Base");
    OasImportHistoryStorage.init();
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
    createListWrappers.unregister();
    OasImportHistoryStorage.shutdown();
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
    plugin.definitionWorkflowFQN("xmcp.oas.fman.GetOasApiEndpointsDefinition");
    plugin.pluginRTC(rtc);
    plugin.path("manager");
    return plugin.instance();
  }


  private RuntimeContext getOwnRtc() {
    ClassLoaderBase clb = (ClassLoaderBase) getClass().getClassLoader();
    Long revision = clb.getRevision();
    return RuntimeContextService.getRuntimeContextFromRevision(new IntegerNumber(revision));
  }

  @Override
  public void generateApplication(XynaOrderServerExtension correlatedXynaOrder,
                                  ApplicationGenerationParameter applicationGenerationParameter1,
                                  File file4) {
    new GenerateApplicationTool().generateApplication(correlatedXynaOrder, applicationGenerationParameter1, file4, Optional.empty());
  }

  
  @Override
  public void generateApplicationByManagedFileID(XynaOrderServerExtension correlatedXynaOrder,
                                                 ApplicationGenerationParameter applicationGenerationParameter2,
                                                 ManagedFileId managedFileId3, OAS_ImportHistory history) {
    new GenerateApplicationTool().generateApplicationByManagedFileID(correlatedXynaOrder, applicationGenerationParameter2,
                                                                     managedFileId3, history);
  }
  
  
  @Override
  public List<? extends OAS_ImportHistory> queryOasImportHistory(TableInfo info) {
    try {
      List<OAS_ImportHistory> ret = new OasImportHistoryStorage().searchOasImportHistory(info);
      new OasImportHistorySortTool().sort(ret, info);
      return ret;
    } catch (RuntimeException e) {
      logger.error(e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e.getMessage(), e);
    }
  }


  @Override
  public OAS_ImportHistory queryOasImportHistoryDetails(OAS_ImportHistory input) {
    try {
      return new OasImportHistoryStorage().searchOasImportHistoryDetails(input);
    } catch (RuntimeException e) {
      logger.error(e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  
  @Override
  public OAS_ImportHistory storeOasImportHistory(OAS_ImportHistory input) {
    try {
      return new OasImportHistoryStorage().storeOasImportHistory(input);
    } catch (RuntimeException e) {
      logger.error(e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e.getMessage(), e);
    }
  }

}

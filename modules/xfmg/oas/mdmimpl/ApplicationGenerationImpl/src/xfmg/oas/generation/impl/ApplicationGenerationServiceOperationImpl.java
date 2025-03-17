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



import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ApplicationPartImportMode;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ImportApplicationParameter;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.LocalRuntimeContextManagementSecurity;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionManagement;
import com.gip.xyna.xmcp.xfcli.impl.SavexmomobjectImpl;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;

import base.File;
import base.math.IntegerNumber;
import xfmg.oas.generation.ApplicationGenerationParameter;
import xfmg.oas.generation.ApplicationGenerationServiceOperation;
import xfmg.oas.generation.cli.generated.OverallInformationProvider;
import xfmg.oas.generation.cli.impl.BuildoasapplicationImpl;
import xfmg.oas.generation.cli.impl.BuildoasapplicationImpl.OASApplicationData;
import xfmg.oas.generation.cli.impl.BuildoasapplicationImpl.ValidationResult;
import xfmg.xfctrl.appmgmt.RuntimeContextService;
import xfmg.xfctrl.filemgmt.ManagedFileId;
import xmcp.forms.plugin.Plugin;
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
  
  public static final XynaPropertyBoolean legacyFilterNames = new XynaPropertyBoolean("xfmg.oas.legacyFilterNames", true)
      .setDefaultDocumentation(DocumentationLanguage.EN, "Name the Filter always as OASFilter.")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Bennent den Filter immer OASFilter.");

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

    createListWrappers.registerDependency(UserType.Service, "OAS_Base");
    legacyFilterNames.registerDependency(UserType.Service, "OAS_Base");
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
    legacyFilterNames.unregister();
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
    plugin.path("manager");
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

    String workspace = applicationGenerationParameter1.getWorkspaceName();
    createAndImportApplication(correlatedXynaOrder, "xmom-data-model", target + "_datatypes", specFile, workspace);
    if (applicationGenerationParameter1.getGenerateProvider()) {
      createAndImportApplication(correlatedXynaOrder, "xmom-server", target + "_provider", specFile, workspace);
    }
    if (applicationGenerationParameter1.getGenerateClient()) {
      createAndImportApplication(correlatedXynaOrder, "xmom-client", target + "_client", specFile, workspace);
    }
  }

  private void createAndImportApplication(XynaOrderServerExtension correlatedXynaOrder, String generator, String target, String specFile, String workspace) {
    BuildoasapplicationImpl oasAppBuilder = new BuildoasapplicationImpl();
    try(OASApplicationData data = oasAppBuilder.createOasApp(generator, target, specFile)) {
      importApplication(correlatedXynaOrder, data.getId(), workspace);
    } catch (IOException e) {
      if(logger.isWarnEnabled()) {
        logger.warn("Could not clean up temporary files for " + generator, e);
      }
    }
  }

  private void importApplicationAsApplication(XynaOrderServerExtension correlatedXynaOrder, String id) {
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


  private void importApplicationAsWorkspace(XynaOrderServerExtension correlatedXynaOrder, String id, String workspace) {
    Path tmpPath = Path.of("/tmp", id + "workspace_import");
    try {
      Long revision;
      RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      revision = revMgmt.getRevision(null, null, workspace);
      String pathStr = RevisionManagement.getPathForRevision(PathType.ROOT, revision, false);
      Path path = Path.of(pathStr, "XMOM");
      if (!Files.exists(path)) {
        Files.createDirectories(path);
      }

      //copy XMOM folder from application to workspace
      FileUtils.unzip(fileManagement.getAbsolutePath(id), tmpPath.toString(), (f) -> true);
      String appXmomDir = Path.of(tmpPath.toString(), "XMOM").toString();
      if(!Files.exists(Path.of(appXmomDir))) {
        logger.debug("Xmom folder does not exist: " + appXmomDir);
        return;
      }
      FileUtils.copyRecursivelyWithFolderStructure(new java.io.File(appXmomDir), path.toFile());

      //refresh new workspace objects
      SavexmomobjectImpl saveImpl = new SavexmomobjectImpl();
      List<java.io.File> files = Files.find(path, 100, (p, bfa) -> bfa.isRegularFile()).map(x -> x.toFile()).collect(Collectors.toList());
      int xmomPathStartIndex = path.toString().length() + 1;
      Collection<String> allObjectNames = CollectionUtils.transformAndSkipNull(files, new Transformation<java.io.File, String>() {
        public String transform(java.io.File from) {
          String xmlName = from.getPath().substring(xmomPathStartIndex).replaceAll(Constants.FILE_SEPARATOR, ".");
          xmlName = xmlName.substring(0, xmlName.length() - ".xml".length());
          return GenerationBase.isReservedServerObjectByFqOriginalName(xmlName) ? null : xmlName;
        }
      });
      for(String f: allObjectNames) {
        saveImpl.saveXmomObject(workspace, f, false);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
        FileUtils.deleteDirectory(tmpPath.toFile());
    }
  }

  private void importApplication(XynaOrderServerExtension correlatedXynaOrder, String id, String workspace) {
    if(workspace == null || workspace.isBlank()) {
      importApplicationAsApplication(correlatedXynaOrder, id);
    } else {
      importApplicationAsWorkspace(correlatedXynaOrder, id, workspace);
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

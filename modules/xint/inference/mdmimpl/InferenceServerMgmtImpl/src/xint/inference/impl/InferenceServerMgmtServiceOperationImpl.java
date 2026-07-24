/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package xint.inference.impl;



import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;

import java.util.List;

import xint.inference.Download;
import xint.inference.DownloadUrl;
import xint.inference.InferenceManagementActionFailedException;
import xint.inference.InferenceServer;
import xint.inference.InferenceServerConfiguration;
import xint.inference.InferenceServerConfigurationCreationData;
import xint.inference.Model;
import xint.inference.RequestHistoryEntry;
import xint.inference.RequestId;
import xint.inference.impl.services.DeleteInferenceServerConfiguration;
import xint.inference.impl.services.ListInferenceServerConfigurations;
import xint.inference.impl.services.ListModels;
import xint.inference.impl.storage.DeleteFile;
import xint.inference.impl.storage.DownloadManager;
import xint.inference.impl.storage.InferenceServerConfigurationStorage;
import xint.inference.impl.storage.InferenceServerList;
import xint.inference.impl.storage.InferenceServerManagementRequestHistoryStorage;
import xint.inference.impl.supportedservers.InferenceServerManagement;
import xint.inference.impl.supportedservers.InferenceServerManagementRegistry;
import xmcp.tables.datatypes.TableInfo;
import xint.inference.InferenceServerMgmtServiceOperation;



public class InferenceServerMgmtServiceOperationImpl implements ExtendedDeploymentTask, InferenceServerMgmtServiceOperation {

  public static final String ID_GENERATOR_REALM = "inferenceMgmt";


  public static final XynaPropertyString SERVER_PATH =
      new XynaPropertyString("xint.inference.inference_server_basepath", "../ai/inference_servers")
          .setDefaultDocumentation(DocumentationLanguage.EN, "Base path for storing inference servers")
          .setDefaultDocumentation(DocumentationLanguage.DE, "Basis Pfad unter dem Inference Server abgelegt werden");

  public static final XynaPropertyString MODEL_PATH = new XynaPropertyString("xint.inference.model_path", "../ai/models")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Path for storing models")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Pfad unter dem Modelle gespreicht werden");

  private InferenceServerManagementRequestHistoryStorage historyStorage;
  private InferenceServerConfigurationStorage serverConfigStorage;
  private DownloadManager downloadManager;


  public void onDeployment() throws XynaException {
    PluginManagement.registerPlugin(this.getClass());
    InferenceServerManagementRequestHistoryStorage.init();
    InferenceServerConfigurationStorage.init();

    historyStorage = new InferenceServerManagementRequestHistoryStorage();
    serverConfigStorage = new InferenceServerConfigurationStorage();
    downloadManager = new DownloadManager();


    SERVER_PATH.registerDependency(UserType.Service, "InferenceServerMgmt");
    MODEL_PATH.registerDependency(UserType.Service, "InferenceServerMgmt");
  }


  public void onUndeployment() throws XynaException {
    downloadManager.shutdown();
    PluginManagement.unregisterPlugin(this.getClass());

    SERVER_PATH.unregister();
    MODEL_PATH.unregister();

    InferenceServerManagementRequestHistoryStorage.shutdown();
    InferenceServerConfigurationStorage.shutdown();
  }


  public Long getOnUnDeploymentTimeout() {
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    return null;
  }


  public RequestId downloadModel(DownloadUrl downloadUrl) {
    long requestId = XynaFactory.getInstance().getIDGenerator().getUniqueId(ID_GENERATOR_REALM);
    downloadManager.downloadModel(requestId, downloadUrl.getUrl());
    return new RequestId.Builder().id(requestId).instance();
  }


  public List<? extends Download> getRunningRequests() {
    return downloadManager.listDownloads();
  }


  public List<? extends InferenceServerConfiguration> listInferenceServerConfigurations() {
    return new ListInferenceServerConfigurations(serverConfigStorage).listInferenceServerConfigurations();
  }


  public List<? extends InferenceServer> listInferenceServers() {
    return InferenceServerList.getInstance().getServers(true);
  }


  public List<? extends Model> listModels() {
    return new ListModels().listModels();
  }


  @Override
  public void createInferenceServerConfiguration(InferenceServerConfigurationCreationData entry) {
    serverConfigStorage.persistEntry(entry);
  }


  @Override
  public List<? extends RequestHistoryEntry> getHistory(TableInfo tableinfo) {
    return historyStorage.loadAllEntries();
  }


  @Override
  public void deleteInferenceServer(InferenceServer server) throws InferenceManagementActionFailedException {
    long requestId = XynaFactory.getInstance().getIDGenerator().getUniqueId(ID_GENERATOR_REALM);
    new DeleteFile().delete(requestId, server);

  }


  @Override
  public void deleteModel(Model model) throws InferenceManagementActionFailedException {
    long requestId = XynaFactory.getInstance().getIDGenerator().getUniqueId(ID_GENERATOR_REALM);
    new DeleteFile().delete(requestId, model);
  }


  @Override
  public RequestId downloadInferenceServer(DownloadUrl downloadUrl) throws InferenceManagementActionFailedException {
    long requestId = XynaFactory.getInstance().getIDGenerator().getUniqueId(ID_GENERATOR_REALM);
    downloadManager.downloadInferenceServer(requestId, downloadUrl.getUrl());
    return new RequestId.Builder().id(requestId).instance();
  }


  @Override
  public void startInferenceServer(InferenceServerConfiguration serverConfig) throws InferenceManagementActionFailedException {
    long requestId = XynaFactory.getInstance().getIDGenerator().getUniqueId(ID_GENERATOR_REALM);
    InferenceServerManagement mgmt = InferenceServerManagementRegistry.getInstance().getServerMgmt(serverConfig.getServerType());
    if (!mgmt.start(requestId, serverConfig)) {
      throw new InferenceManagementActionFailedException("start inference server", requestId);
    }
  }


  @Override
  public void stopInferenceServer(InferenceServerConfiguration serverConfig) throws InferenceManagementActionFailedException {
    long requestId = XynaFactory.getInstance().getIDGenerator().getUniqueId(ID_GENERATOR_REALM);
    InferenceServerManagement mgmt = InferenceServerManagementRegistry.getInstance().getServerMgmt(serverConfig.getServerType());
    if (!mgmt.stop(requestId, serverConfig)) {
      throw new InferenceManagementActionFailedException("start inference server", requestId);
    }
  }


  @Override
  public void clearHistory() {
    historyStorage.clearHistory();
  }


  @Override
  public void deleteInferenceServerConfiguration(InferenceServerConfiguration entry) throws InferenceManagementActionFailedException {
    long requestId = XynaFactory.getInstance().getIDGenerator().getUniqueId(ID_GENERATOR_REALM);
    new DeleteInferenceServerConfiguration().deleteInferenceServerConfiguration(requestId, entry);
  }


  @Override
  public void abortRunningRequest(Download download) {
    downloadManager.abortDownload(download);
  }


  @Override
  public void pauseRunningRequest(Download download) {
    downloadManager.pauseDownload(download);
  }


  @Override
  public void resumeRunningRequest(Download download) {
    downloadManager.resumeDownload(download);
  }


}

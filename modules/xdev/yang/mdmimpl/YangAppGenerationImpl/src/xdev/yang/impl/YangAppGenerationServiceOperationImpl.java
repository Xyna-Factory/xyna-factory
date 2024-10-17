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
package xdev.yang.impl;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xmcp.xfcli.generated.Importapplication;
import com.gip.xyna.xmcp.xfcli.impl.ImportapplicationImpl;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import base.Text;
import xdev.yang.YangAppGenerationInputParameter;
import xdev.yang.YangAppGenerationServiceOperation;
import xdev.yang.cli.generated.OverallInformationProvider;
import xdev.yang.impl.YangApplicationGeneration.YangApplicationGenerationData;
import xmcp.yang.LoadYangAssignmentsData;
import xmcp.yang.UseCaseAssignementTableData;
import xmcp.yang.UseCaseTableData;
import xprc.xpce.Workspace;


public class YangAppGenerationServiceOperationImpl implements ExtendedDeploymentTask, YangAppGenerationServiceOperation {

  private static final Logger logger = CentralFactoryLogging.getLogger(YangAppGenerationServiceOperationImpl.class);
  
  public void onDeployment() throws XynaException {
    OverallInformationProvider.onDeployment();
    PluginManagement.registerPlugin(this.getClass());
  }


  public void onUndeployment() throws XynaException {
    OverallInformationProvider.onUndeployment();
    PluginManagement.unregisterPlugin(this.getClass());
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }

  public void createYangDeviceApp(YangAppGenerationInputParameter yangAppGenerationInputParameter2) {
    String id = null;
    try (YangApplicationGenerationData appData = YangApplicationGeneration.createDeviceApp(yangAppGenerationInputParameter2)) {
      id = appData.getId();
    } catch (IOException e) {
      if (logger.isWarnEnabled()) {
        logger.warn("Could not clean up temporary files for " + yangAppGenerationInputParameter2.getApplicationName(), e);
      }
    }
    importApplication(id);

  }

  public void importModuleCollectionApplication(YangAppGenerationInputParameter yangAppGenerationInputParameter1) {
    String id = null;
    try (YangApplicationGenerationData appData = YangApplicationGeneration.createModuleCollectionApp(yangAppGenerationInputParameter1)) {
      id = appData.getId();
    } catch (IOException e) {
      if (logger.isWarnEnabled()) {
        logger.warn("Could not clean up temporary files for " + yangAppGenerationInputParameter1.getApplicationName(), e);
      }
    }
    importApplication(id);
  }
  
  private void importApplication(String id) {
    FileManagement fileMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
    ImportapplicationImpl importApp = new ImportapplicationImpl();
    Importapplication importPayload = new Importapplication();
    importPayload.setFilename(fileMgmt.retrieve(id).getOriginalFilename());
    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()){
      importApp.execute(stream, importPayload);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<? extends UseCaseTableData> loadUsecases() {
    return new LoadUsecasesTable().loadUsecases();
  }

  @Override
  public void addUsecase(XynaOrderServerExtension order, Text usecaseGroupFqn, Text usecaseName, Workspace workspace, Text rpc, Text deviceFqn) {
      new AddUsecase().addUsecase(usecaseGroupFqn.getText(), usecaseName.getText(), workspace, order, rpc.getText(), deviceFqn.getText());
    
  }

  @Override
  public List<? extends UseCaseAssignementTableData> loadAssignments(LoadYangAssignmentsData data) {
    DetermineUseCaseAssignments executor = new DetermineUseCaseAssignments();
    return executor.determineUseCaseAssignments(data);
  }
}

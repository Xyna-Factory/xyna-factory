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
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xmcp.xfcli.generated.Importapplication;
import com.gip.xyna.xmcp.xfcli.impl.ImportapplicationImpl;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import xdev.yang.OperationCreationParameter;
import xdev.yang.YangAppGenerationInputParameter;
import xdev.yang.YangAppGenerationServiceOperation;
import xdev.yang.cli.generated.OverallInformationProvider;
import xdev.yang.exceptions.ApplicationImportException;
import xdev.yang.exceptions.OperationCreationException;
import xdev.yang.impl.YangApplicationGeneration.YangApplicationGenerationData;
import xdev.yang.impl.operation.AddOperation;
import xdev.yang.impl.operation.AddVariableToOperationSignature;
import xdev.yang.impl.operation.AsyncDeployment;
import xdev.yang.impl.operation.ConfigureList;
import xdev.yang.impl.operation.DeleteOperationAssignmentAction;
import xdev.yang.impl.operation.DetermineOperationAssignments;
import xdev.yang.impl.operation.LoadOperationSignature;
import xdev.yang.impl.operation.LoadOperationsTable;
import xdev.yang.impl.operation.RemoveVariableFromOperationSignature;
import xdev.yang.impl.operation.SaveOperationAssignmentAction;
import xdev.yang.impl.operation.UpdateVariableInOperationSignature;
import xdev.yang.impl.operation.OperationCache;
import xmcp.yang.LoadYangAssignmentsData;
import xmcp.yang.OperationAssignmentTableData;
import xmcp.yang.OperationTableData;
import xmcp.yang.fman.ListConfiguration;
import xmcp.yang.fman.OperationSignatureEntry;


public class YangAppGenerationServiceOperationImpl implements ExtendedDeploymentTask, YangAppGenerationServiceOperation {

  public void onDeployment() throws XynaException {
    OverallInformationProvider.onDeployment();
    PluginManagement.registerPlugin(this.getClass());
    OperationCache.PROP_OPERATION_CACHE_SIZE.registerDependency(UserType.Service, "YangAppGenerationService");
    AsyncDeployment.PROP_ASYNC_DEPLOY.registerDependency(UserType.Service, "YangAppGenerationService");
  }


  public void onUndeployment() throws XynaException {
    OverallInformationProvider.onUndeployment();
    PluginManagement.unregisterPlugin(this.getClass());
    OperationCache.PROP_OPERATION_CACHE_SIZE.unregister();
    AsyncDeployment.PROP_ASYNC_DEPLOY.unregister();
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


  public void createYangDeviceApp(XynaOrderServerExtension order, YangAppGenerationInputParameter input) throws ApplicationImportException {
    try (YangApplicationGenerationData appData = YangApplicationGeneration.createDeviceApp(input)) {
      String id = appData.getId();
      importApplication(id);
    } catch (Exception e) {
      throw new ApplicationImportException(order.getId(), e);
    }
  }


  public void importModuleCollectionApplication(XynaOrderServerExtension order, YangAppGenerationInputParameter input) throws ApplicationImportException {
    try (YangApplicationGenerationData appData = YangApplicationGeneration.createModuleCollectionApp(input)) {
      String id = appData.getId();
      importApplication(id);
    } catch (Exception e) {
      throw new ApplicationImportException(order.getId(), e);
    }
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
  public List<? extends OperationTableData> loadOperations() {
    return new LoadOperationsTable().loadOperations();
  }

  @Override
  public void addOperation(XynaOrderServerExtension order, OperationCreationParameter parameter) throws OperationCreationException {
    try {
      new AddOperation().addOperation(order, parameter);
    } catch (Exception e) {
      throw new OperationCreationException(order.getId(), e);
    }
  }

  @Override
  public List<? extends OperationAssignmentTableData> loadAssignments(LoadYangAssignmentsData data) {
    DetermineOperationAssignments executor = new DetermineOperationAssignments();
    return executor.determineOperationAssignments(data);
  }


  @Override
  public void saveAssignment(XynaOrderServerExtension order, OperationAssignmentTableData data) {
    SaveOperationAssignmentAction executor = new SaveOperationAssignmentAction();
    executor.saveOperationAssignment(order, data);
  }


  @Override
  public void addVariableToOperationSignature(XynaOrderServerExtension order, OperationTableData operation, OperationSignatureEntry signature) {
    AddVariableToOperationSignature executor = new AddVariableToOperationSignature();
    executor.addVariable(order, operation, signature);
  }


  @Override
  public Container loadOperationSignature(OperationTableData operation) {
    LoadOperationSignature executor = new LoadOperationSignature();
    return executor.loadSignature(operation);
  }


  @Override
  public void removeVariableFromOperationSignature(XynaOrderServerExtension order, OperationTableData operation, OperationSignatureEntry signature) {
    RemoveVariableFromOperationSignature executor = new RemoveVariableFromOperationSignature();
    executor.removeVariable(order, operation, signature);
  }


  @Override
  public void configureList(XynaOrderServerExtension order, LoadYangAssignmentsData data, ListConfiguration config) {
    ConfigureList executor = new ConfigureList();
    executor.configure(order, data, config);
  }


  @Override
  public void deleteAssignment(XynaOrderServerExtension order, OperationAssignmentTableData data) {
    DeleteOperationAssignmentAction executor = new DeleteOperationAssignmentAction();
    executor.deleteOperationAssignment(order, data);

  }
  
  @Override
  public void updateVariableInOperationSignature(XynaOrderServerExtension order, OperationTableData operation, OperationSignatureEntry signature) {
    UpdateVariableInOperationSignature executor = new UpdateVariableInOperationSignature();
    executor.updateVariable(order, operation, signature);
  }

}

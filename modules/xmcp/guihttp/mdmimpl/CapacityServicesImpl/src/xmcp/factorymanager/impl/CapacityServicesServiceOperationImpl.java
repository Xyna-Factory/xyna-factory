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
package xmcp.factorymanager.impl;



import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_CAPACITY_ALREADY_DEFINED;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;

import xmcp.factorymanager.CapacityServicesServiceOperation;
import xmcp.factorymanager.capacities.CapacityName;
import xmcp.factorymanager.capacities.exceptions.CapacityCreateException;
import xmcp.factorymanager.capacities.exceptions.CapacityDeleteException;
import xmcp.factorymanager.capacities.exceptions.CapacityLoadException;
import xmcp.factorymanager.capacities.exceptions.CapacityUpdateException;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;
import xmcp.zeta.TableHelper;



public class CapacityServicesServiceOperationImpl implements ExtendedDeploymentTask, CapacityServicesServiceOperation {

  private static final String LIST_ENTRY_KEY_NAME = "name";
  private static final String LIST_ENTRY_KEY_STATE = "state";
  private static final String LIST_ENTRY_KEY_CARDINALITY = "cardinality";
  private static final String LIST_ENTRY_KEY_INUSE = "inuse";

  private static final XynaMultiChannelPortal multiChannelPortal =
      (XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal();


  public void onDeployment() throws XynaException {
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }


  public void onUndeployment() throws XynaException {
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
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


  @Override
  public void createCapacity(xmcp.factorymanager.capacities.CapacityInformation capacityInformation) throws CapacityCreateException {
    CapacityManagement.State state = null;
    if (capacityInformation.getState() != null) {
      state = CapacityManagement.State.valueOf(capacityInformation.getState());
    } else {
      state = CapacityManagement.State.ACTIVE;
    }
    try {
      boolean success = multiChannelPortal.addCapacity(capacityInformation.getName(), capacityInformation.getCardinality(), state);
      if (!success)
        throw new CapacityCreateException(null);
    } catch (XPRC_CAPACITY_ALREADY_DEFINED | PersistenceLayerException e) {
      throw new CapacityCreateException(e.getMessage(), e);
    }
  }


  @Override
  public void removeCapacity(CapacityName capacityName) throws CapacityDeleteException {
    if (capacityName == null)
      throw new CapacityDeleteException("CapacityName is required");
    CapacityInformation capacityInformation = multiChannelPortal.getCapacityInformation(capacityName.getName());
    if (capacityInformation == null)
      throw new CapacityDeleteException("Capacity not found");
    try {
      boolean result =
          XynaFactory.getInstance().getProcessing().getXynaScheduler().getCapacityManagement().removeCapacity(capacityName.getName());
      if (!result)
        throw new CapacityDeleteException(null);
    } catch (PersistenceLayerException e) {
      throw new CapacityDeleteException(e.getMessage(), e);
    }
  }


  @Override
  public void changeCapacity(xmcp.factorymanager.capacities.CapacityInformation capacityInformation) throws CapacityUpdateException {
    try {
      boolean success = multiChannelPortal.changeCapacityCardinality(capacityInformation.getName(), capacityInformation.getCardinality());
      success &= multiChannelPortal.changeCapacityState(capacityInformation.getName(), State.valueOf(capacityInformation.getState()));
      if (!success)
        throw new CapacityUpdateException(null);
    } catch (PersistenceLayerException | XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState
        | XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain e) {
      throw new CapacityUpdateException(e.getMessage(), e);
    }
  }


  @Override
  public xmcp.factorymanager.capacities.CapacityInformation getDetails(CapacityName capacityName) throws CapacityLoadException {
    if (capacityName == null)
      throw new CapacityLoadException("CapacityName is required");
    CapacityInformation capacityInformation = multiChannelPortal.getCapacityInformation(capacityName.getName());
    if (capacityInformation == null)
      throw new CapacityLoadException("Capacity not found");

    xmcp.factorymanager.capacities.CapacityInformation result = new xmcp.factorymanager.capacities.CapacityInformation();
    result.setName(capacityInformation.getName());
    result.setCardinality(capacityInformation.getCardinality());
    result.setState((capacityInformation.getState() != null) ? capacityInformation.getState().name() : "");
    result.setInuse(capacityInformation.getInuse());

    return result;
  }


  public List<? extends xmcp.factorymanager.capacities.CapacityInformation> getListEntries(TableInfo tableInfo) {
    List<xmcp.factorymanager.capacities.CapacityInformation> result;
    Collection<CapacityInformation> capacityInformations = multiChannelPortal.listCapacityInformation();

    TableHelper<xmcp.factorymanager.capacities.CapacityInformation, TableInfo> tableHelper =
        TableHelper.<xmcp.factorymanager.capacities.CapacityInformation, TableInfo> init(tableInfo)
            .limitConfig(TableInfo::getLimit)
            .sortConfig(ti -> {
              for (TableColumn tc : ti.getColumns()) {
                TableHelper.Sort sort = TableHelper.createSortIfValid(tc.getPath(), tc.getSort());
                if(sort != null)
                  return sort;
              }
              return null;
            })
            .filterConfig(ti -> ti.getColumns().stream()
                .filter(tableColumn -> !tableColumn.getDisableFilter() && tableColumn.getPath() != null && tableColumn.getFilter() != null && tableColumn.getFilter().length() > 0)
                .map(tc -> new TableHelper.Filter(tc.getPath(), tc.getFilter())).collect(Collectors.toList()))
            .addSelectFunction(LIST_ENTRY_KEY_NAME, xmcp.factorymanager.capacities.CapacityInformation::getName)
            .addSelectFunction(LIST_ENTRY_KEY_STATE, xmcp.factorymanager.capacities.CapacityInformation::getState)
            .addSelectFunction(LIST_ENTRY_KEY_CARDINALITY, xmcp.factorymanager.capacities.CapacityInformation::getCardinality)
            .addSelectFunction(LIST_ENTRY_KEY_INUSE, xmcp.factorymanager.capacities.CapacityInformation::getInuse);

    result = capacityInformations.stream().map(capacityInformation -> {
      xmcp.factorymanager.capacities.CapacityInformation entry = new xmcp.factorymanager.capacities.CapacityInformation();
      entry.setCardinality(capacityInformation.getCardinality());
      entry.setName(capacityInformation.getName());
      entry.setState((capacityInformation.getState() != null) ? capacityInformation.getState().name() : "");
      entry.setInuse(capacityInformation.getInuse());
      return entry;
    }).filter(tableHelper.filter()).collect(Collectors.toList());

    tableHelper.sort(result);
    return tableHelper.limit(result);
  }
}

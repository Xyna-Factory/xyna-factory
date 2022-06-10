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
package xmcp.factorymanager.impl;


import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoAllocationDenied;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoDeallocationDenied;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;

import xmcp.factorymanager.AdministrativeVetoServicesServiceOperation;
import xmcp.factorymanager.administrativevetoes.AdministrativeVeto;
import xmcp.factorymanager.administrativevetoes.AdministrativeVetoName;
import xmcp.factorymanager.administrativevetoes.exceptions.ChangeAdministrativeVetoException;
import xmcp.factorymanager.administrativevetoes.exceptions.CreateAdministrativeVetoException;
import xmcp.factorymanager.administrativevetoes.exceptions.DeleteAdministrativeVetoException;
import xmcp.factorymanager.administrativevetoes.exceptions.LoadAdministrativeVetoException;
import xmcp.factorymanager.administrativevetoes.exceptions.LoadAdministrativeVetoesException;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;
import xmcp.zeta.TableHelper;


public class AdministrativeVetoServicesServiceOperationImpl implements ExtendedDeploymentTask, AdministrativeVetoServicesServiceOperation {
  
  private static final String TABLE_KEY_NAME = "name";
  private static final String TABLE_KEY_DOCUMENTATION = "documentation";
  
  private final XynaMultiChannelPortal multiChannelPortal =
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
  public void changeVeto(AdministrativeVeto administrativeVeto) throws ChangeAdministrativeVetoException {
    try {
      multiChannelPortal.setDocumentationOfAdministrativeVeto(administrativeVeto.getName(), administrativeVeto.getDocumentation());
    } catch (PersistenceLayerException | XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new ChangeAdministrativeVetoException(e.getMessage(), e);
    }
  }

  @Override
  public void createVeto(AdministrativeVeto administrativeVeto) throws CreateAdministrativeVetoException {
    try {
      multiChannelPortal.allocateAdministrativeVeto(administrativeVeto.getName(), administrativeVeto.getDocumentation());
    } catch (XPRC_AdministrativeVetoAllocationDenied | PersistenceLayerException e) {
      throw new CreateAdministrativeVetoException(e.getMessage(), e);
    }
  }

  @Override
  public void deleteVeto(AdministrativeVetoName administrativeVetoName) throws DeleteAdministrativeVetoException {
    try {
      multiChannelPortal.freeAdministrativeVeto(administrativeVetoName.getName());
    } catch (XPRC_AdministrativeVetoDeallocationDenied | PersistenceLayerException e) {
      throw new DeleteAdministrativeVetoException(e.getMessage(), e);
    }
  }

  @Override
  public AdministrativeVeto getVetoDetails(AdministrativeVetoName administrativeVetoName) throws LoadAdministrativeVetoException {
    try {
      Collection<VetoInformationStorable> vetoInformations = multiChannelPortal.listVetoInformation();
      for (VetoInformationStorable storable : vetoInformations) {
        if(storable.getVetoName() != null && storable.getVetoName().equals(administrativeVetoName.getName()))
          return new AdministrativeVeto(storable.getVetoName(), storable.getDocumentation());
      }
    } catch (PersistenceLayerException e) {
      throw new LoadAdministrativeVetoException(e.getMessage(), e);
    }
    throw new LoadAdministrativeVetoException("Administrative veto not found");
  }
  
  @Override
  public List<? extends AdministrativeVeto> getListEntries(TableInfo tableInfo) throws LoadAdministrativeVetoesException {
    
    TableHelper<AdministrativeVeto, TableInfo> tableHelper = TableHelper.<AdministrativeVeto, TableInfo>init(tableInfo)
        .limitConfig(TableInfo::getLimit)
        .sortConfig(ti -> {
          for (TableColumn tc : ti.getColumns()) {
            TableHelper.Sort sort = TableHelper.createSortIfValid(tc.getPath(), tc.getSort());
            if(sort != null)
              return sort;
          }
          return null;
        })
        .filterConfig(ti -> 
          ti.getColumns().stream()
          .filter(tableColumn -> 
            !tableColumn.getDisableFilter() && tableColumn.getPath() != null && tableColumn.getFilter() != null && tableColumn.getFilter().length() > 0
          )
          .map(tc -> new TableHelper.Filter(tc.getPath(), tc.getFilter()))
          .collect(Collectors.toList())
        )
        .addSelectFunction(TABLE_KEY_NAME, AdministrativeVeto::getName)
        .addSelectFunction(TABLE_KEY_DOCUMENTATION, AdministrativeVeto::getDocumentation);
    
    try {
      Collection<VetoInformationStorable> vetoInformations = multiChannelPortal.listVetoInformation();
      List<AdministrativeVeto> result = vetoInformations.stream()
          .map(storable -> new AdministrativeVeto(storable.getVetoName(), storable.getDocumentation()))
          .filter(tableHelper.filter())
          .collect(Collectors.toList());
      
      tableHelper.sort(result);
      return tableHelper.limit(result);
    } catch (XynaException e) {
      throw new LoadAdministrativeVetoesException(e.getMessage(), e);
    }
  }  
}

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
package xnwh.persistence.impl;


import java.util.Collections;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xnwh.exceptions.XNWH_XMOMPersistenceMaxLengthValidationException;
import com.gip.xyna.xnwh.exceptions.XNWH_XMOMPersistenceValidationException;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.xmom.IFormula;
import com.gip.xyna.xnwh.persistence.xmom.SortCriterion;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceOperations;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import base.Count;
import xnwh.persistence.Alternative;
import xnwh.persistence.Default;
import xnwh.persistence.DeleteParameter;
import xnwh.persistence.ExtendedParameter;
import xnwh.persistence.FilterCondition;
import xnwh.persistence.History;
import xnwh.persistence.PersistenceServicesServiceOperation;
import xnwh.persistence.QueryParameter;
import xnwh.persistence.SelectionMask;
import xnwh.persistence.Storable;
import xnwh.persistence.StoreParameter;
import xnwh.persistence.UpdateParameter;
import xnwh.persistence.XMOMStorableAccessException;
import xnwh.persistence.XMOMStorableMaxLengthValidationException;
import xnwh.persistence.XMOMStorableValidationException;


public class PersistenceServicesServiceOperationImpl implements ExtendedDeploymentTask, PersistenceServicesServiceOperation {

  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.;
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.;
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.;
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.;
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }

  public void delete(XynaOrderServerExtension correlatedOrder, Storable storable, DeleteParameter deleteParameter) throws XMOMStorableAccessException {
    deleteExtended(correlatedOrder, storable, deleteParameter, null);
  }
  
  public void deleteExtended(XynaOrderServerExtension correlatedOrder, Storable storable, DeleteParameter deleteParameter, ExtendedParameter extendedParameter) throws XMOMStorableAccessException {
    try {
      getOperationsImpl().delete(correlatedOrder, storable, convertDeleteParameter(deleteParameter), convertExtendedParameter(extendedParameter));
    } catch (PersistenceLayerException e) {
      throw new XMOMStorableAccessException(storable.getClass().getName(), e);
    }
  }

  
  public List<? extends Storable> query(XynaOrderServerExtension correlatedOrder, SelectionMask selectionMask, FilterCondition filterCondition, QueryParameter queryParameter) throws XMOMStorableAccessException {
    return queryExtended(correlatedOrder, selectionMask, filterCondition, queryParameter, null);
  }
  
  
  public List<? extends Storable> queryExtended(XynaOrderServerExtension correlatedOrder, SelectionMask selectionMask, FilterCondition filterCondition, QueryParameter queryParameter,
                                                ExtendedParameter extendedParameter) throws XMOMStorableAccessException {
    try {
      return (List<? extends Storable>) getOperationsImpl().query(correlatedOrder,
                                                                  convertSelectionMask(selectionMask),
                                                                  new FilterCond(filterCondition),
                                                                  convertQueryParameter(queryParameter), getRevision(correlatedOrder),
                                                                  convertExtendedParameter(extendedParameter));
    } catch (PersistenceLayerException e) {
      throw new XMOMStorableAccessException(selectionMask.getRootType(), e);
    }
  }
  
  public Count count(XynaOrderServerExtension correlatedOrder, SelectionMask selectionMask, FilterCondition filterCondition, QueryParameter queryParameter,
                                                ExtendedParameter extendedParameter) throws XMOMStorableAccessException {
    try {
      return new Count(getOperationsImpl().count(correlatedOrder,
                                                                  convertSelectionMask(selectionMask),
                                                                  new FilterCond(filterCondition),
                                                                  convertQueryParameter(queryParameter), getRevision(correlatedOrder),
                                                                  convertExtendedParameter(extendedParameter)));
    } catch (PersistenceLayerException e) {
      throw new XMOMStorableAccessException(selectionMask.getRootType(), e);
    }
  }


  public void store(XynaOrderServerExtension correlatedOrder, Storable storable, StoreParameter storeParameter) throws XMOMStorableAccessException {
    storeExtended(correlatedOrder, storable, storeParameter, null);
  }
  
  
  public void storeExtended(XynaOrderServerExtension correlatedOrder, Storable storable, StoreParameter storeParameter, ExtendedParameter extendedParameter)
                  throws XMOMStorableAccessException {
    try {
      getOperationsImpl().store(correlatedOrder, storable, convertStoreParameter(storeParameter), convertExtendedParameter(extendedParameter));
    } catch (XNWH_XMOMPersistenceMaxLengthValidationException e) {
      throw new XMOMStorableMaxLengthValidationException.Builder().subtype(e.getType()).path(e.getField()).restriction(e.getRestriction())
          .xmomStorableName(storable.getClass().getName()).length(e.getLength()).instance();
    } catch (XNWH_XMOMPersistenceValidationException e) {
      throw new XMOMStorableValidationException(storable.getClass().getName(), e.getType(), e.getField(), e.getRestriction());
    } catch (PersistenceLayerException e) {
      throw new XMOMStorableAccessException(storable.getClass().getName(), e);
    }
  }
  
  
  public void update(XynaOrderServerExtension xo, Storable storable, SelectionMask toUpdate, UpdateParameter parameter)
                  throws XMOMStorableAccessException {
    updateExtended(xo, storable, toUpdate, parameter, null);
  }


  public void updateExtended(XynaOrderServerExtension xo, Storable storable, SelectionMask toUpdate,
                             UpdateParameter parameter, ExtendedParameter extendedParameter)
                  throws XMOMStorableAccessException {
    try {
      getOperationsImpl().update(xo, storable, toUpdate.getColumns(), convertUpdateParameter(parameter), convertExtendedParameter(extendedParameter));
    } catch (PersistenceLayerException e) {
      throw new XMOMStorableAccessException(storable.getClass().getName(), e);
    }
  }
  
  
  private static class FilterCond implements IFormula {
    
    private final String filterString;
    
    FilterCond(FilterCondition filter) {
      if (filter == null) {
        filterString = null;
      } else {
        filterString = filter.getFormula();
      }
    }

    public List<Accessor> getValues() {
      return Collections.emptyList();
    }

    public String getFormula() {
      return filterString;
    }
    
  }

  
  private com.gip.xyna.xnwh.persistence.xmom.DeleteParameter convertDeleteParameter(DeleteParameter deleteParameter) {
    return new com.gip.xyna.xnwh.persistence.xmom.DeleteParameter(deleteParameter.getIncludingHistory(),
                                                                  deleteParameter.getReferenceHandling() == null ? (String)null : deleteParameter.getReferenceHandling().getForward(),
                                                                  deleteParameter.getReferenceHandling() == null ? (String)null : deleteParameter.getReferenceHandling().getBackward());
  }
  
  private com.gip.xyna.xnwh.persistence.xmom.StoreParameter convertStoreParameter(StoreParameter storeParameter) {
    return new com.gip.xyna.xnwh.persistence.xmom.StoreParameter(storeParameter.getHistorizePreviousObject(),
                                                                 storeParameter.getKeepMetaFieldData(),
                                                                 storeParameter.getForceRecursiveStore());
  }


  private com.gip.xyna.xnwh.persistence.xmom.QueryParameter convertQueryParameter(QueryParameter queryParameter) {
    return new com.gip.xyna.xnwh.persistence.xmom.QueryParameter(queryParameter.getMaxObjects(),
                                                                 queryParameter.getQueryHistory(),
                                                                 convertSortCriterions(queryParameter
                                                                     .getSortCriterion()));
  }
  
  
  private com.gip.xyna.xnwh.persistence.xmom.UpdateParameter convertUpdateParameter(UpdateParameter updateParameter) {
    return new com.gip.xyna.xnwh.persistence.xmom.UpdateParameter(updateParameter.getHistorizePreviousObject(),
                                                                  updateParameter.getKeepMetaFieldData());
  }


  private SortCriterion[] convertSortCriterions(List<? extends xnwh.persistence.SortCriterion> sortCriterion) {
    if (sortCriterion == null || sortCriterion.size() == 0) {
      return null;
    }
    SortCriterion[] ret = new SortCriterion[sortCriterion.size()];
    for (int i = 0; i<ret.length; i++) {
      ret[i] = convertSortCriterion(sortCriterion.get(i));
    }
    return ret;
  }


  private SortCriterion convertSortCriterion(xnwh.persistence.SortCriterion sortCriterion) {    
    return new SortCriterion(sortCriterion.getCriterion(), sortCriterion.getReverse());
  }

  private com.gip.xyna.xnwh.persistence.xmom.SelectionMask convertSelectionMask(SelectionMask mask) {
    return new com.gip.xyna.xnwh.persistence.xmom.SelectionMask(mask.getRootType(), mask.getColumns());
  }
  
  
  private com.gip.xyna.xnwh.persistence.xmom.ExtendedParameter convertExtendedParameter(ExtendedParameter extendedParameter) {
    ODSConnectionType conType;
    if (extendedParameter == null) {
      conType = ODSConnectionType.DEFAULT;
    } else if (extendedParameter.getConnectionType() instanceof Default) {
      conType = ODSConnectionType.DEFAULT;
    } else if (extendedParameter.getConnectionType() instanceof History) {
      conType = ODSConnectionType.HISTORY;
    } else if (extendedParameter.getConnectionType() instanceof Alternative) {
      conType = ODSConnectionType.ALTERNATIVE;
    } else {
      conType = ODSConnectionType.DEFAULT;
    }
    return new com.gip.xyna.xnwh.persistence.xmom.ExtendedParameter(conType);
  }
  
  private Long getRevision(XynaOrderServerExtension xose) {
    return xose.getRootOrder().getRevision();
  }
  
  
  private XMOMPersistenceOperations getOperationsImpl() {
    return XynaFactory.getInstance().getXynaNetworkWarehouse().getXMOMPersistence().getXMOMPersistenceManagement();
  }

  
}

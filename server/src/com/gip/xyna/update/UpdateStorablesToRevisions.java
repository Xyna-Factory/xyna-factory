/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

package com.gip.xyna.update;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.update.utils.StorableUpdater;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.XMOMVersionStorable;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.xpce.dispatcher.DispatcherDestinationStorable;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringDispatcher.MonitoringDispatcherStorable;
import com.gip.xyna.xprc.xprcods.capacitymapping.CapacityMappingStorable;
import com.gip.xyna.xprc.xprcods.ordercontextconfiguration.OrderContextConfigStorable;


public class UpdateStorablesToRevisions extends UpdateJustVersion{

  public UpdateStorablesToRevisions(Version oldVersion, Version newVersion, boolean mustUpdateGeneratedClasses) {
    super(oldVersion, newVersion, mustUpdateGeneratedClasses);
  }

  @Override
  protected void update() throws XynaException {
    Map<Application, Long> applications = getApplications();
    
    StorableUpdater.update(com.gip.xyna.update.outdatedclasses_5_1_4_3.DispatcherDestinationStorable.class,
                   DispatcherDestinationStorable.class,
                   new TransformDispatcherDestination(applications),
                   ODSConnectionType.HISTORY);

    StorableUpdater.update(com.gip.xyna.update.outdatedclasses_5_1_4_3.CapacityMappingStorable.class,
                   CapacityMappingStorable.class,
                   new TransformCapacityMapping(applications),
                   ODSConnectionType.HISTORY);

    StorableUpdater.update(com.gip.xyna.update.outdatedclasses_5_1_4_3.MonitoringDispatcherStorable.class,
                   MonitoringDispatcherStorable.class,
                   new TransformMonitoringDispatcher(applications),
                   ODSConnectionType.HISTORY);

    StorableUpdater.update(com.gip.xyna.update.outdatedclasses_5_1_4_3.OrderContextConfigStorable.class,
                   OrderContextConfigStorable.class,
                   new TransformOrderContextConfig(applications),
                   ODSConnectionType.DEFAULT);
  }
  
  
  /**
   * Liefert alle vorhanden Applications
   * @return
   * @throws PersistenceLayerException
   */
  private Map<Application, Long> getApplications() throws PersistenceLayerException {
    Map<Application, Long> applications = new HashMap<Application, Long>();
    
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(XMOMVersionStorable.class);
    
    ODSConnection conHis = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      String sql = "select * from " + XMOMVersionStorable.TABLE_NAME + " where " + XMOMVersionStorable.COL_APPLICATION +
                    " IS NOT NULL";
      //binding ist egal, weil im cluster immer sichergestellt wird, dass beide knoten die gleiche revision bekommen
      //ausserdem kann zu diesem zeitpunkt das localbinding noch nicht einfach ermittelt werden, weil clusteringservicesmgmt nicht initialisiert ist.
      
      PreparedQuery<XMOMVersionStorable> query = conHis.prepareQuery(new Query<XMOMVersionStorable>(sql, XMOMVersionStorable.getStaticReader(), XMOMVersionStorable.TABLE_NAME));
      List<? extends XMOMVersionStorable> xmomversions = conHis.query(query, new Parameter(), -1);
      
      applications = new HashMap<Application, Long>();
      for(XMOMVersionStorable xmomversion : xmomversions) {
        if (xmomversion.getApplication() != null) {
          applications.put(new Application(xmomversion.getApplication(), xmomversion.getVersionName()), xmomversion.getRevision());
        }
      }
      
      return applications;
    } finally {
      try {
        conHis.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
    }
  }
  
  
  
  /**
   * Transformation, um in einem Storable applicationName und versionName durch revision zu ersetzen.
   */
  private static abstract class TransformStorable<F extends Storable<F>, T extends Storable<T>> implements Transformation<F, T> {

    protected Map<Application, Long> applications;
    
    public TransformStorable(Map<Application, Long> applications) {
      this.applications = applications;
    }

    public T transform(F from) {
      Application application = getApplication(from);
      Long revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
      if (application != null && application.getName() != null && application.getName().length() > 0) {
        revision = applications.get(application);
      }
      
      if (revision == null) {
        logger.warn("Could not find revision for " + application + ", delete " + from.getTableName() + " for ordertype " + getOrderType(from));
        return null;
      }
      
      return getNewStorable(from, revision);
    }
    
    protected abstract Application getApplication(F from);
    protected abstract T getNewStorable(F from, Long revision);
    protected abstract String getOrderType(F from);
  }
  
  
  
  private static class TransformDispatcherDestination extends TransformStorable<com.gip.xyna.update.outdatedclasses_5_1_4_3.DispatcherDestinationStorable, DispatcherDestinationStorable> {

    public TransformDispatcherDestination(Map<Application, Long> applications) {
      super(applications);
    }

    @Override
    protected Application getApplication(com.gip.xyna.update.outdatedclasses_5_1_4_3.DispatcherDestinationStorable from) {
      if (from.getApplicationname() == null) {
        return null;
      }
      return new Application(from.getApplicationname(), from.getVersionname());
    }

    @Override
    protected DispatcherDestinationStorable getNewStorable(com.gip.xyna.update.outdatedclasses_5_1_4_3.DispatcherDestinationStorable from, Long revision) {
      ExecutionType type = ExecutionType.getByTypeString(from.getDestinationType());
      return new DispatcherDestinationStorable(from.getId(),
                                               from.getDestinationKey(),
                                               from.getDispatcherName(),
                                               type,
                                               from.getDestinationValue(),
                                               revision);
    }

    @Override
    protected String getOrderType(com.gip.xyna.update.outdatedclasses_5_1_4_3.DispatcherDestinationStorable from) {
      return from.getDestinationKey();
    }
  }
  
  

  private static class TransformCapacityMapping extends TransformStorable<com.gip.xyna.update.outdatedclasses_5_1_4_3.CapacityMappingStorable, CapacityMappingStorable> {
    
    public TransformCapacityMapping(Map<Application, Long> applications) {
      super(applications);
    }

    @Override
    protected Application getApplication(com.gip.xyna.update.outdatedclasses_5_1_4_3.CapacityMappingStorable from) {
      if (from.getApplicationname() == null) {
        return null;
      }
      return new Application(from.getApplicationname(), from.getVersionname());
    }

    @Override
    protected CapacityMappingStorable getNewStorable(com.gip.xyna.update.outdatedclasses_5_1_4_3.CapacityMappingStorable from,
                                                     Long revision) {
      return new CapacityMappingStorable(from.getOrderType(),
                                         revision,
                                         from.getRequiredCapacities(),
                                         from.getId());
    }

    @Override
    protected String getOrderType(com.gip.xyna.update.outdatedclasses_5_1_4_3.CapacityMappingStorable from) {
      return from.getOrderType();
    }
  }

  
  private static class TransformMonitoringDispatcher extends TransformStorable<com.gip.xyna.update.outdatedclasses_5_1_4_3.MonitoringDispatcherStorable, MonitoringDispatcherStorable> {
    
    public TransformMonitoringDispatcher(Map<Application, Long> applications) {
      super(applications);
    }

    @Override
    protected Application getApplication(com.gip.xyna.update.outdatedclasses_5_1_4_3.MonitoringDispatcherStorable from) {
      if (from.getApplicationname() == null) {
        return null;
      }
      return new Application(from.getApplicationname(), from.getVersionname());
    }

    @Override
    protected MonitoringDispatcherStorable getNewStorable(com.gip.xyna.update.outdatedclasses_5_1_4_3.MonitoringDispatcherStorable from,
                                                          Long revision) {
      return new MonitoringDispatcherStorable(from.getOrderType(),
                                              from.getCompensate(),
                                              from.getMonitoringlevel(),
                                              from.getId(),
                                              revision);
    }

    @Override
    protected String getOrderType(com.gip.xyna.update.outdatedclasses_5_1_4_3.MonitoringDispatcherStorable from) {
      return from.getOrderType();
    }
  }

  private static class TransformOrderContextConfig extends TransformStorable<com.gip.xyna.update.outdatedclasses_5_1_4_3.OrderContextConfigStorable, OrderContextConfigStorable> {
    
    public TransformOrderContextConfig(Map<Application, Long> applications) {
      super(applications);
    }

    @Override
    protected Application getApplication(com.gip.xyna.update.outdatedclasses_5_1_4_3.OrderContextConfigStorable from) {
      if (from.getApplicationName() == null) {
        return null;
      }
      return new Application(from.getApplicationName(), from.getVersionName());
    }

    @Override
    protected OrderContextConfigStorable getNewStorable(com.gip.xyna.update.outdatedclasses_5_1_4_3.OrderContextConfigStorable from,
                                                        Long revision) {
      return new OrderContextConfigStorable(from.getOrderType(),
                                            revision,
                                            from.getId());
    }

    @Override
    protected String getOrderType(com.gip.xyna.update.outdatedclasses_5_1_4_3.OrderContextConfigStorable from) {
      return from.getOrderType();
    }
  }
}

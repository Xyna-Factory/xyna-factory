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
package com.gip.xyna.xfmg.xods.ordertypemanagement;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport.ValueProcessor;
import com.gip.xyna.utils.collections.ObjectWithRemovalSupport;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagementPortal;
import com.gip.xyna.xfmg.exceptions.XFMG_FailedToAddObjectToApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_FailedToRemoveObjectFromApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidCapacityCardinality;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidCreationOfExistingOrdertype;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidModificationOfUnexistingOrdertype;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidXynaOrderPriority;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter.DestinationValueParameter;
import com.gip.xyna.xfmg.xods.priority.PriorityManagement;
import com.gip.xyna.xfmg.xods.priority.PrioritySetting;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.XynaProcessingBase;
import com.gip.xyna.xprc.XynaProcessingPortal.DispatcherIdentification;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_ExecutionDestinationMissingException;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_MONITORING_TYPE;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xpce.cleanup.CleanupDispatcher;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.dispatcher.FractalWorkflowDestination;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher.DestinationChangedHandler;
import com.gip.xyna.xprc.xpce.execution.ExecutionDispatcher;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringDispatcher;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xpce.planning.PlanningDispatcher;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;
import com.gip.xyna.xprc.xprcods.capacitymapping.CapacityMappingChangeListener;
import com.gip.xyna.xprc.xprcods.capacitymapping.CapacityMappingDatabase;
import com.gip.xyna.xprc.xprcods.capacitymapping.CapacityMappingStorable;



/**
 * OrdertypeManagement is a Facade/Adapter for CapacityMappingDataBase, XynaDispatcher, MonitoringDispatcher and stores Documentations
 *   it allows configuration of those FunctionGroups based on an ordertype
 *   Documentation is stored in a probably unsearchable History as a fast access is not needed for gui-access    
 */
public class OrdertypeManagement extends FunctionGroup implements IPropertyChangeListener {

  static {
    addDependencies(OrdertypeManagement.class,
                    new ArrayList<XynaFactoryPath>(Arrays
                        .asList(new XynaFactoryPath[] {new XynaFactoryPath(XynaProcessing.class,
                                                                           XynaProcessingODS.class)})));
  }

  public static final String DEFAULT_NAME = "Ordertype Management";

  private ODS ods;


  public OrdertypeManagement() throws XynaException {
    super();
  }


  public static final String[] INTERNAL_ORDERTYPES = (String[]) XynaDispatcher.INTERNAL_ORDER_TYPES
      .toArray(new String[XynaDispatcher.INTERNAL_ORDER_TYPES.size()]);

  public static final Set<String> internalOrdertypes = XynaDispatcher.INTERNAL_ORDER_TYPES;


  private volatile boolean hideInternalOrders = true;


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  private static class DestinationChangedHandlerOTUpdate implements DestinationChangedHandler {

    private final String dispatcherName;
    private final XynaDispatcher dispatcher;

    private DestinationChangedHandlerOTUpdate(XynaDispatcher xd) {
      this.dispatcherName = xd.getDefaultName();
      this.dispatcher = xd;
    }


    @Override
    public void set(DestinationKey dk, DestinationValue dv) {
      long rev;
      try {
        rev = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
            .getRevision(dk.getRuntimeContext());
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement()
          .updateInCache(rev, dk.getOrderType(), OrderTypeUpdates.setDestination(dv, dispatcherName, dispatcher.isCustom(dk)));
    }


    @Override
    public void remove(DestinationKey dk) {
      OrdertypeManagement otm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement();
      otm.updateInCache(otm.getRevision(dk.getRuntimeContext()), dk.getOrderType(), OrderTypeUpdates.removeDestination(dispatcherName));
    }

  }
  
  
  @Override
  protected void init() throws XynaException {
    ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    ods.registerStorable(OrdertypeInformation.class);
    if (logger.isDebugEnabled()) {
      logger.debug("INTERNAL_ORDERTYPES: " + INTERNAL_ORDERTYPES.length + " " + Arrays.asList(INTERNAL_ORDERTYPES));
      logger.debug("internalOrdertypes: " + internalOrdertypes);
    }
    initCache();
  }

  private final CapacityMappingChangeListener capacityMappingChangeHandler = new CapacityMappingChangeListener() {

    @Override
    public void capacityMappingChanged(DestinationKey key) {
      //TODO hier k�nnte man auch nur den cache notifizieren, dass capacitymappings sp�ter neu ausgelesen werden m�ssen. das verschiebt die aufw�nde zu dem zeitpunkt, wo es gebraucht wird.
      //allerdings ist das �ndern von capacitymappings meistens kein performancekritischer zeitpunkt...
      List<Capacity> caps =
          XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getCapacityMappingDatabase().getCapacities(key);
      if (caps.isEmpty()) {
        updateInCache(getRevision(key.getRuntimeContext()), key.getOrderType(), OrderTypeUpdates.clearCapacityMappings());
      } else {
        updateInCache(getRevision(key.getRuntimeContext()), key.getOrderType(),
                      OrderTypeUpdates.setCapacityMappings(new HashSet<Capacity>(caps)));
      }
    }
  };


  private void initCache() {
    XynaFactory.getInstance().getFutureExecution()
        .addTask("OrderTypeManagement.registerDestinationChangeHandler", "OrderTypeManagement.registerDestinationChangeHandler")
        .after(XynaDispatcher.class).execAsync(new Runnable() {

          @Override
          public void run() {
            XynaDispatcher[] disps = new XynaDispatcher[] {
                XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher(),
                XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaPlanning().getPlanningDispatcher(),
                XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaCleanup().getCleanupEngineDispatcher()};
            for (XynaDispatcher xd : disps) {
              xd.registerCallbackHandler(new DestinationChangedHandlerOTUpdate(xd));
              for (Entry<DestinationKey, DestinationValue> e : xd.getDestinations().entrySet()) {
                DestinationKey dk = e.getKey();
                updateInCache(getRevision(dk.getRuntimeContext()), dk.getOrderType(),
                              OrderTypeUpdates.setDestination(e.getValue(), xd.getDefaultName(), xd.isCustom(dk)));
              }
            }
          }

        });
    XynaFactory.getInstance().getFutureExecution()
        .addTask("OrderTypeManagement.registerCapacityMappingChangeHandler", "OrderTypeManagement.registerCapacityMappingChangeHandler")
        .after(CapacityMappingDatabase.class).execAsync(new Runnable() {

          @Override
          public void run() {
            CapacityMappingDatabase db = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getCapacityMappingDatabase();
            db.registerCapacityMappingChangedListener(capacityMappingChangeHandler); //wird in weakhashmap gespeichert und muss deshalb referenziert bleiben
            for (CapacityMappingStorable cms : db.getAllCapacityMappings()) {
              updateInCache(cms.getRevision(), cms.getOrderType(),
                            OrderTypeUpdates.setCapacityMappings(new HashSet<Capacity>(cms.getRequiredCapacities())));
            }
          }

        });
    XynaFactory.getInstance()
        .getFutureExecution().addTask("OrderTypeManagement.initCacheEntriesForMonitoringDispatcher",
                                      "OrderTypeManagement.initCacheEntriesForMonitoringDispatcher")
        .after(MonitoringDispatcher.class).execAsync(new Runnable() {

          @Override
          public void run() {
            for (Entry<DestinationKey, Integer> e : XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution()
                .getMonitoringDispatcher().getAllMonitoringLevels().entrySet()) {
              updateInCache(getRevision(e.getKey().getRuntimeContext()), e.getKey().getOrderType(),
                            OrderTypeUpdates.setMonitoringLevel(e.getValue()));
            }
          }

        });
    XynaFactory.getInstance().getFutureExecution()
        .addTask("OrderTypeManagement.initCacheEntriesForPriorityManagement", "OrderTypeManagement.initCacheEntriesForPriorityManagement")
        .after(PriorityManagement.class).execAsync(new Runnable() {

          @Override
          public void run() {
            try {
              Collection<PrioritySetting> priorities =
                  XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getPriorityManagement().listPriorities();
              for (PrioritySetting ps : priorities) {
                updateInCache(ps.getRevision(), ps.getOrderType(), OrderTypeUpdates.setPriority(ps.getPriority()));
              }
            } catch (PersistenceLayerException e) {
              throw new RuntimeException(e);
            }

          }

        });
    XynaFactory.getInstance().getFutureExecution()
        .addTask("OrderTypeManagement.initCacheEntriesForParameterInheritance",
                 "OrderTypeManagement.initCacheEntriesForParameterInheritance")
        .after(ParameterInheritanceManagement.class).execAsync(new Runnable() {

          @Override
          public void run() {
            ParameterInheritanceManagement pim =
                XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getParameterInheritanceManagement();
            Set<DestinationKey> dks = pim.discoverInheritanceRuleOrderTypes();
            for (DestinationKey dk : dks) {
              updateInCache(getRevision(dk.getRuntimeContext()), dk.getOrderType(),
                            OrderTypeUpdates.setParameterInheritanceRules(pim.listInheritanceRules(dk)));
            }
          }

        });
    try {
      for (OrdertypeInformation oi : listOrderTypeInformation()) {
        updateInCache(oi.getRevision(), oi.getOrdertypeName(), OrderTypeUpdates.setDocumentation(oi.getDocumentation()));
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  protected void shutdown() throws XynaException {
    ods.unregisterStorable(OrdertypeInformation.class);
  }

  private static class OrderTypeSubMap extends ObjectWithRemovalSupport {

    private final ConcurrentMap<String, OrdertypeParameter> innermap = new ConcurrentHashMap<>();
    
    @Override
    protected boolean shouldBeDeleted() {
      return innermap.isEmpty();
    }
    
  }
  
  private final ConcurrentMapWithObjectRemovalSupport<Long, OrderTypeSubMap> cache = new ConcurrentMapWithObjectRemovalSupport<Long, OrderTypeSubMap>() {

    private static final long serialVersionUID = 1L;

    @Override
    public OrderTypeSubMap createValue(Long key) {
      return new OrderTypeSubMap();
    }
    
  };
  
  public interface OrderTypeParameterUpdate {
    /**
     * @return removedSomething?
     */
    public boolean update(OrdertypeParameter parameter);
    
  }
  
  public static class OrderTypeUpdates {
    public static OrderTypeParameterUpdate setDocumentation(final String doc) {
      return new OrderTypeParameterUpdate() {

        @Override
        public boolean update(OrdertypeParameter parameter) {
          parameter.setDocumentation(doc);
          return false;
        }
        
      };
    }
    
    public static OrderTypeParameterUpdate setCapacityMappings(final Set<Capacity> requiredCapacities) {
      return new OrderTypeParameterUpdate() {

        @Override
        public boolean update(OrdertypeParameter parameter) {
          parameter.setRequiredCapacities(requiredCapacities);
          return false;
        }
        
      };
    }
    
    public static OrderTypeParameterUpdate clearCapacityMappings() {
      return new OrderTypeParameterUpdate() {

        @Override
        public boolean update(OrdertypeParameter parameter) {
          parameter.setRequiredCapacities(null);
          return true;
        }
        
      };
    }

    public static OrderTypeParameterUpdate removeDestination(final String dispatcherName) {
      return new OrderTypeParameterUpdate() {

        @Override
        public boolean update(OrdertypeParameter parameter) {
          if (dispatcherName.equals(ExecutionDispatcher.DEFAULT_NAME)) {
            parameter.setExecutionDestinationValue(null);
          } else if (dispatcherName.equals(PlanningDispatcher.DEFAULT_NAME)) {
            parameter.setPlanningDestinationValue(null);
          } else if (dispatcherName.equals(CleanupDispatcher.DEFAULT_NAME)) {
            parameter.setCleanupDestinationValue(null);
          } else {
            throw new RuntimeException();
          }
          return true;
        }
        
      };
    }

    public static OrderTypeParameterUpdate setDestination(final DestinationValue destinationValue, final String dispatcherName, final boolean isCustom) {
      return new OrderTypeParameterUpdate() {

        @Override
        public boolean update(OrdertypeParameter parameter) {
          if (dispatcherName.equals(ExecutionDispatcher.DEFAULT_NAME)) {
            if (isCustom) {
              parameter.setCustomExecutionDestinationValue(new DestinationValueParameter(destinationValue));
            } else {
              parameter.setExecutionDestinationValue(new DestinationValueParameter(destinationValue));
            }
          } else if (dispatcherName.equals(PlanningDispatcher.DEFAULT_NAME)) {
            if (isCustom) {
              parameter.setCustomPlanningDestinationValue(new DestinationValueParameter(destinationValue));
            } else {
              parameter.setPlanningDestinationValue(new DestinationValueParameter(destinationValue));
            }
          } else if (dispatcherName.equals(CleanupDispatcher.DEFAULT_NAME)) {
            if (isCustom) {
              parameter.setCustomCleanupDestinationValue(new DestinationValueParameter(destinationValue));
            } else {
              parameter.setCleanupDestinationValue(new DestinationValueParameter(destinationValue));              
            }
          } else {
            throw new RuntimeException();
          }
          return false;
        }
        
      };
    }

    public static OrderTypeParameterUpdate setMonitoringLevel(final Integer monitoringLevel) {
      return new OrderTypeParameterUpdate() {

        @Override
        public boolean update(OrdertypeParameter parameter) {
          parameter.setCustomMonitoringLevel(monitoringLevel);
          return monitoringLevel == null;
        }
        
      };
    }

    public static OrderTypeParameterUpdate setPriority(final Integer priority) {
      return new OrderTypeParameterUpdate() {

        @Override
        public boolean update(OrdertypeParameter parameter) {
          parameter.setCustomPriority(priority);
          return priority == null;
        }
        
      };
    }
    

    public static OrderTypeParameterUpdate setParameterInheritanceRules(final Map<ParameterType, List<InheritanceRule>> parameterInheritanceRules) {
      return new OrderTypeParameterUpdate() {

        @Override
        public boolean update(OrdertypeParameter parameter) {
          if (isEmpty(parameterInheritanceRules)) {
            parameter.setParameterInheritanceRules(null);
            return true;
          } else {
            parameter.setParameterInheritanceRules(parameterInheritanceRules);
            return false;
          }
        }
        
        private boolean isEmpty(Map<ParameterType, List<InheritanceRule>> parameterInheritanceRules) {
          if (parameterInheritanceRules == null) {
            return true;
          }
          if (parameterInheritanceRules.size() == 0) {
            return true;
          }
          for (List<InheritanceRule> value : parameterInheritanceRules.values()) {
            if (value.size() > 0) {
              return false;
            }
          }
          return true;
        }
        
      };
    }
  }
 
  
  private void deleteFromCache(long revision, String ordertype) {
    OrderTypeSubMap subMap = cache.lazyCreateGet(revision);
    try {
      subMap.innermap.remove(ordertype);
    } finally {
      cache.cleanup(revision);
    }
  }
  
  public void updateInCache(long revision, String ordertype, OrderTypeParameterUpdate update) {
    OrderTypeSubMap subMap = cache.lazyCreateGet(revision);
    try {
      OrdertypeParameter para = subMap.innermap.get(ordertype);
      if (para == null) {
        para = new OrdertypeParameter();
        para.setOrdertypeName(ordertype);
        try {
          para.setRuntimeContext(getRuntimeContext(revision));
        } catch (RuntimeException e) {
          //runtimeContext nicht gefunden => ignorieren
          logger.warn("Found invalid ordertype config: ot=" + ordertype + ", revision=" + revision, e);
          return;
        }
        OrdertypeParameter prev = subMap.innermap.putIfAbsent(ordertype, para);
        if (prev != null) {
          para = prev;
        }
      }
      synchronized (para) {
        if (update.update(para) && isEmpty(para)) {
          subMap.innermap.remove(ordertype);
        }
      }
    } finally {
      cache.cleanup(revision);
    }
  }
  

  private boolean isEmpty(OrdertypeParameter para) {
    return para.getExecutionDestinationValue() == null && para.getPlanningDestinationValue() == null
        && para.getCleanupDestinationValue() == null && para.getRequiredCapacities() == null && para.getMonitoringLevel() == null
        && para.getPriority() == null && para.getParameterInheritanceRules() == null && para.getDocumentation() == null;
  }


  private MonitoringDispatcher getMonitoringDispatcher() {
    return XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher();
  }

  private ParameterInheritanceManagement getParameterInheritanceManagement() {
    return XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getParameterInheritanceManagement();
  }


  private XynaProcessingBase getProcessing() {
    return XynaFactory.getInstance().getProcessing();
  }


  private XynaFactoryManagementPortal getFactoryManagement() {
    return XynaFactory.getInstance().getFactoryManagementPortal();
  }


  public void createOrdertype(OrdertypeParameter newOrdertype) throws PersistenceLayerException,
      XFMG_InvalidCreationOfExistingOrdertype, XFMG_FailedToAddObjectToApplication {
    createOrdertype(newOrdertype, true);
  }


  public void createOrUpdateOrdertypes(Collection<OrdertypeParameter> values, boolean updateApplication) throws PersistenceLayerException,
      XFMG_InvalidCapacityCardinality, XPRC_INVALID_MONITORING_TYPE {
    if (values.size() == 0) {
      return;
    }
    OrdertypeParameter firstObject = values.iterator().next();
    Long revision = getRevision(firstObject.getRuntimeContext());

    setOrdertypeInformations(values, revision);

    for (OrdertypeParameter otp : values) {
      setDispatcherConfiguration(otp, revision);
      createCapacityMapping(otp);
      setMonitoringSetting(otp);
      setPrioritySetting(otp, revision);
      setParameterInheritanceRules(otp);
    }
  }


  public void createOrdertype(OrdertypeParameter ordertypeParameter, boolean updateApplication) throws PersistenceLayerException,
      XFMG_InvalidCreationOfExistingOrdertype, XFMG_FailedToAddObjectToApplication {
    long revision = getRevision(ordertypeParameter.getRuntimeContext());
    if (orderTypeExists(ordertypeParameter.getOrdertypeName(), revision)) {
      throw new XFMG_InvalidCreationOfExistingOrdertype(ordertypeParameter.getOrdertypeName());
    }
    try {
      createOrdertypeInternally(ordertypeParameter, revision);
    } catch (XFMG_InvalidCapacityCardinality e) {
      deleteOrdertypeInternally(ordertypeParameter, revision);
      throw new XFMG_InvalidCreationOfExistingOrdertype(ordertypeParameter.getOrdertypeName(), e);
    } catch (XPRC_INVALID_MONITORING_TYPE e) {
      deleteOrdertypeInternally(ordertypeParameter, revision);
      throw new XFMG_InvalidCreationOfExistingOrdertype(ordertypeParameter.getOrdertypeName(), e);
    } catch (PersistenceLayerException e) {
      deleteOrdertypeInternally(ordertypeParameter, revision);
      throw e;
    } catch (RuntimeException re) {
      deleteOrdertypeInternally(ordertypeParameter, revision);
      throw re;
    }
    if (updateApplication) {
      if (ordertypeParameter.getApplicationName() != null) {
        //zur applikation hinzuf�gen
        ApplicationManagementImpl applicationManagement =
            (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                .getApplicationManagement();
        try {
          applicationManagement.addNonModelledObjectToApplication(ordertypeParameter.getOrdertypeName(),
                                                       ordertypeParameter.getApplicationName(),
                                                       ordertypeParameter.getVersionName(),
                                                       ApplicationEntryType.ORDERTYPE, null, false, null);
        } catch (XFMG_FailedToAddObjectToApplication e) {
          deleteOrdertype(ordertypeParameter);
          throw e;
        }
      }
    }
  }


  public void modifyOrdertype(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException,
      XFMG_InvalidModificationOfUnexistingOrdertype, XFMG_InvalidCapacityCardinality {
    Long revision = getRevision(ordertypeParameter.getRuntimeContext());
    if (!orderTypeExists(ordertypeParameter.getOrdertypeName(), revision)) {
      try {
        createOrdertype(ordertypeParameter, true);
        return;
      } catch (XFMG_InvalidCreationOfExistingOrdertype e) {
        //wurde gleichzeitig hinzugef�gt? nochmal modify probieren
        logger.warn("OrderType seems to have been added concurrently. Modifying existing OrderType ...", e);
      } catch (XFMG_FailedToAddObjectToApplication e) {
        throw new RuntimeException("OrderType could not be added to application", e);
      }
    }
    OrdertypeParameter backup = null;
    try {
      backup =
          getOrdertype(ordertypeParameter.getOrdertypeName(), ordertypeParameter.getRuntimeContext());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      ; // it was just contained, but it could have been delete in the meantime, no rollback in those cases
    }
    ordertypeParameter.adjustDestinationValueIsCustomSetting(backup);
    try {
      modifyOrdertypeInternally(ordertypeParameter, revision);
    } catch (XFMG_InvalidCapacityCardinality e) {
      if (backup != null) {
        try {
          modifyOrdertypeInternally(backup, revision);
        } catch (XFMG_InvalidCapacityCardinality e1) {
          throw new RuntimeException("previous value has already been invalid.", e1);
        } catch (XPRC_INVALID_MONITORING_TYPE e1) {
          throw new RuntimeException("previous value has already been invalid.", e1);
        }
      }
      throw e;
    } catch (XPRC_INVALID_MONITORING_TYPE e) {
      if (backup != null) {
        try {
          modifyOrdertypeInternally(backup, revision);
        } catch (XFMG_InvalidCapacityCardinality e1) {
          throw new RuntimeException("previous value has already been invalid.", e1);
        } catch (XPRC_INVALID_MONITORING_TYPE e1) {
          throw new RuntimeException("previous value has already been invalid.", e1);
        }
      }
    } catch (PersistenceLayerException e) {
      if (backup != null) {
        try {
          modifyOrdertypeInternally(backup, revision);
        } catch (XFMG_InvalidCapacityCardinality e1) {
          throw new RuntimeException("previous value has already been invalid.", e1);
        } catch (XPRC_INVALID_MONITORING_TYPE e1) {
          throw new RuntimeException("previous value has already been invalid.", e1);
        }
      }
      throw e;
    } catch (RuntimeException re) {
      if (backup != null) {
        try {
          modifyOrdertypeInternally(backup, revision);
        } catch (XFMG_InvalidCapacityCardinality e1) {
          throw new RuntimeException("previous value has already been invalid.", e1);
        } catch (XPRC_INVALID_MONITORING_TYPE e1) {
          throw new RuntimeException("previous value has already been invalid.", e1);
        }
      }
      throw re;
    }
  }

  /**
   * batch operation
   */
  public void deleteOrdertypes(List<OrdertypeParameter> toDelete) throws PersistenceLayerException {
    if (toDelete.size() == 0) {
      return;
    }
    List<OrdertypeInformation> ordertypeInfos = getOrdertypeInfos(toDelete);
    deleteOrdertypeInformations(ordertypeInfos);
    //TODO batching
    for (OrdertypeParameter otp : toDelete) {
      Long revision = getRevision(otp.getRuntimeContext());
      deleteMonitoringSetting(otp);
      deleteDispatcherConfiguration(otp);
      deleteCapacityMapping(otp);
      deletePrioritySetting(otp, revision);
      deleteParameterInheritanceRules(otp);
      if (otp.getApplicationName() != null) {
        //aus applikation entfernen
        ApplicationManagementImpl applicationManagement =
            (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                .getApplicationManagement();
        try {
          applicationManagement.removeNonModelledObjectFromApplication(otp.getApplicationName(),
                                                            otp.getVersionName(),
                                                            otp.getOrdertypeName(),
                                                            ApplicationEntryType.ORDERTYPE, 
                                                            null,
                                                            false, null);
        } catch (XFMG_FailedToRemoveObjectFromApplication e) {
          logger.warn("could not remove object from application", e);
        }
      }
    }
    for (OrdertypeParameter otp : toDelete) {
      deleteFromCache(getRevision(otp.getRuntimeContext()), otp.getOrdertypeName());
    }
  }


  private List<OrdertypeInformation> getOrdertypeInfos(Collection<OrdertypeParameter> toDelete) {
    List<OrdertypeInformation> ordertypeInfos = new ArrayList<OrdertypeInformation>();
    for (OrdertypeParameter otp : toDelete) {
      Long revision = getRevision(otp.getRuntimeContext());
      ordertypeInfos.add(new OrdertypeInformation(otp, revision));
    }
    return ordertypeInfos;
  }

  private Collection<OrdertypeInformation> listOrderTypeInformation() throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return con.loadCollection(OrdertypeInformation.class);
    } finally {
      con.closeConnection();
    }
  }


  private void deleteOrdertypeInformations(List<OrdertypeInformation> toDelete) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.delete(toDelete);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  public void deleteOrdertype(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException {
    long revision = getRevision(ordertypeParameter.getRuntimeContext());
    try {
      deleteOrdertypeInternally(ordertypeParameter, revision);
    } catch (PersistenceLayerException e) {
      try {
        createOrdertypeInternally(ordertypeParameter, revision);
      } catch (XFMG_InvalidCreationOfExistingOrdertype e1) {
        // then the deletion failed at the first method and there is nothing to rollback
      } catch (XFMG_InvalidCapacityCardinality e1) {
        // nothing to be done
      } catch (XPRC_INVALID_MONITORING_TYPE e1) {
        // nothing to be done
      }
      throw e;
    } catch (RuntimeException re) {
      try {
        createOrdertypeInternally(ordertypeParameter, revision);
      } catch (XFMG_InvalidCreationOfExistingOrdertype e1) {
        ; // then the deletion failed at the first method and there is nothing to rollback
      } catch (XFMG_InvalidCapacityCardinality e) {
        // nothing to be done
      } catch (XPRC_INVALID_MONITORING_TYPE e) {
        // nothing to be done
      }
      throw re;
    }
    if (ordertypeParameter.getApplicationName() != null) {
      //aus applikation entfernen
      ApplicationManagementImpl applicationManagement =
          (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
              .getApplicationManagement();
      try {
        applicationManagement.removeNonModelledObjectFromApplication(ordertypeParameter.getApplicationName(),
                                                          ordertypeParameter.getVersionName(),
                                                          ordertypeParameter.getOrdertypeName(),
                                                          ApplicationEntryType.ORDERTYPE, 
                                                          null,
                                                          false, null);
      } catch (XFMG_FailedToRemoveObjectFromApplication e) {
        logger.warn("could not remove object from application", e);
      }
    }
  }
  
  private boolean orderTypeExists(final String ordertypename, long revision) {
    OrderTypeSubMap orderTypeSubMap = cache.get(revision);
    if (orderTypeSubMap == null) {
      return false;
    }
    OrdertypeParameter otp = orderTypeSubMap.innermap.get(ordertypename);
    return otp != null;
  }

  private OrdertypeParameter getOrdertype(final String ordertypename, long revision) {
    OrdertypeParameter otp = cache.process(revision, new ValueProcessor<OrderTypeSubMap, OrdertypeParameter>() {

      @Override
      public OrdertypeParameter exec(OrderTypeSubMap v) {
        OrdertypeParameter o = v.innermap.get(ordertypename);
        if (o == null) {
          return null;
        }
        synchronized (o) {
          return copyOrderTypeParameter(o);
        }
      }

    });
    return otp;
  }


  public OrdertypeParameter getOrdertype(final String ordertypename, RuntimeContext runtimeContext)
      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    OrdertypeParameter otp = getOrdertype(ordertypename, getRevision(runtimeContext));
    if (otp == null) {
      throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(ordertypename, OrdertypeInformation.TABLE_NAME);
    }
    return otp;
  }


  public List<OrdertypeParameter> listOrdertypes(RuntimeContext runtimeContext) throws PersistenceLayerException {
    if (runtimeContext == null) {
      return listOrdertypesOfAllRevisions();
    } else {
      return listOrdertypes(getRevision(runtimeContext));
    }
  }


  public List<OrdertypeParameter> listOrdertypes(Long revision) throws PersistenceLayerException {
    ConcurrentMap<String, OrdertypeParameter> innermap = cache.lazyCreateGet(revision).innermap;
    List<OrdertypeParameter> result = new ArrayList<>();
    try {
       for (OrdertypeParameter otp : innermap.values()) {
         if (hideInternalOrders) {
           if (internalOrdertypes.contains(otp.getOrdertypeName())) {
             continue;
           }
         }
         synchronized (otp) {
           result.add(copyOrderTypeParameter(otp));
         }
       }
    } finally {
      cache.cleanup(revision);
    }
    return result;
  }

  private OrdertypeParameter copyOrderTypeParameter(OrdertypeParameter otp) {
    OrdertypeParameter copy = new OrdertypeParameter();
    if (otp.isCustomCleanupDestinationValue()) {
      copy.setCustomCleanupDestinationValue(otp.getCleanupDestinationValue());
    } else {
      copy.setCleanupDestinationValue(otp.getCleanupDestinationValue());
    }
    if (otp.isCustomPlanningDestinationValue()) {
      copy.setCustomPlanningDestinationValue(otp.getPlanningDestinationValue());
    } else {
      copy.setPlanningDestinationValue(otp.getPlanningDestinationValue());
    }
    if (otp.isCustomExecutionDestinationValue()) {
      copy.setCustomExecutionDestinationValue(otp.getExecutionDestinationValue());
    } else {
      copy.setExecutionDestinationValue(otp.getExecutionDestinationValue());
    }
    copy.setCustomPriority(otp.getPriority());
    copy.setCustomMonitoringLevel(otp.getMonitoringLevel());
    copy.setOrdertypeName(otp.getOrdertypeName());
    copy.setRequiredCapacities(otp.getRequiredCapacities());
    copy.setRuntimeContext(otp.getRuntimeContext());
    copy.setDocumentation(otp.getDocumentation());
    copy.setParameterInheritanceRules(otp.getParameterInheritanceRules());
    return copy;
  }

  
  public List<OrdertypeParameter> listOrdertypes(SearchOrdertypeParameter sop) throws PersistenceLayerException {
    if (sop.allOrdertypes()) {
      return listOrdertypesOfAllRevisions();
    } else {
      Long revision = getRevision(sop.getRuntimeContext());
      List<OrdertypeParameter> searchResult = listOrdertypes(revision);
      if (sop.includeRequirements()) {
        Set<Long> deps = new HashSet<Long>();
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getDependenciesRecursivly(revision, deps);
        for (Long dep : deps) {
          searchResult.addAll(listOrdertypes(dep));
        }
      }
      return searchResult;
    }
  }


  private List<OrdertypeParameter> listOrdertypesOfAllRevisions() throws PersistenceLayerException {
    List<OrdertypeParameter> allOrdertypes = new ArrayList<OrdertypeParameter>();
    RevisionManagement revMgmt = getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    for (Long revision : revMgmt.getAllRevisions()) {
      allOrdertypes.addAll(listOrdertypes(revision));
    }
    return allOrdertypes;
  }


  private void createOrdertypeInternally(OrdertypeParameter ordertypeParameter, long revision)
      throws PersistenceLayerException, XFMG_InvalidCreationOfExistingOrdertype, XFMG_InvalidCapacityCardinality,
      XPRC_INVALID_MONITORING_TYPE {
    setOrdertypeInformation(ordertypeParameter, revision);
    setDispatcherConfiguration(ordertypeParameter, revision);
    createCapacityMapping(ordertypeParameter);
    setMonitoringSetting(ordertypeParameter);
    setPrioritySetting(ordertypeParameter, revision);
    setParameterInheritanceRules(ordertypeParameter);
  }


  private void modifyOrdertypeInternally(OrdertypeParameter ordertypeParameter, long revision)
      throws PersistenceLayerException, XFMG_InvalidModificationOfUnexistingOrdertype, XFMG_InvalidCapacityCardinality,
      XPRC_INVALID_MONITORING_TYPE {
    setOrdertypeInformation(ordertypeParameter, revision);
    modifyMonitoringSetting(ordertypeParameter);
    setDispatcherConfiguration(ordertypeParameter, revision);
    modifyCapacityMapping(ordertypeParameter);
    modifyPrioritySetting(ordertypeParameter, revision);
    modifyParameterInheritanceRules(ordertypeParameter);
  }


  private void deleteOrdertypeInternally(OrdertypeParameter ordertypeParameter, long revision)
      throws PersistenceLayerException {
    deleteOrdertypeInformation(ordertypeParameter, revision);
    deleteMonitoringSetting(ordertypeParameter);
    deleteDispatcherConfiguration(ordertypeParameter);
    deleteCapacityMapping(ordertypeParameter);
    deletePrioritySetting(ordertypeParameter, revision);
    deleteParameterInheritanceRules(ordertypeParameter);
    deleteFromCache(revision, ordertypeParameter.getOrdertypeName());
  }


  private void setOrdertypeInformations(Collection<OrdertypeParameter> ordertypeParameter, long revision) throws PersistenceLayerException {
    List<OrdertypeInformation> ordertypeInfos = getOrdertypeInfos(ordertypeParameter);

    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.persistCollection(ordertypeInfos);
      con.commit();
    } finally {
      con.closeConnection();
    }
    for (final OrdertypeParameter otp : ordertypeParameter) {
      updateInCache(revision, otp.getOrdertypeName(), OrderTypeUpdates.setDocumentation(otp.getDocumentation()));
    }
  }


  private void setOrdertypeInformation(OrdertypeParameter ordertypeParameter, long revision)
      throws PersistenceLayerException {
    OrdertypeInformation orderInformation = new OrdertypeInformation(ordertypeParameter, revision);
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.persistObject(orderInformation);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  private void setDispatcherConfiguration(OrdertypeParameter ordertypeParameter, long revision)
      throws PersistenceLayerException {
    DestinationKey destinationKey =
        new DestinationKey(ordertypeParameter.getOrdertypeName(), ordertypeParameter.getRuntimeContext());
    Map<DispatcherIdentification, DestinationValue> dispatcherConfigurations =
        new HashMap<DispatcherIdentification, DestinationValue>();
    if (ordertypeParameter.getPlanningDestinationValue() != null && ordertypeParameter.isCustomPlanningDestinationValue()) {
      dispatcherConfigurations
          .put(DispatcherIdentification.Planning,
               convertDestinationValueParameterToDestinationValue(ordertypeParameter, DispatcherIdentification.Planning));
    }
    if (ordertypeParameter.getExecutionDestinationValue() != null && ordertypeParameter.isCustomExecutionDestinationValue()) {
      dispatcherConfigurations
          .put(DispatcherIdentification.Execution,
               convertDestinationValueParameterToDestinationValue(ordertypeParameter, DispatcherIdentification.Execution));
    }
    if (ordertypeParameter.getCleanupDestinationValue() != null && ordertypeParameter.isCustomCleanupDestinationValue()) {
      dispatcherConfigurations
          .put(DispatcherIdentification.Cleanup,
               convertDestinationValueParameterToDestinationValue(ordertypeParameter, DispatcherIdentification.Cleanup));
    }
    for (Entry<DispatcherIdentification, DestinationValue> configurationEntry : dispatcherConfigurations.entrySet()) {
      getProcessing().setDestination(configurationEntry.getKey(), destinationKey, configurationEntry.getValue());
    }
  }


  private void createCapacityMapping(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException,
      XFMG_InvalidCapacityCardinality {
    String ordertype = ordertypeParameter.getOrdertypeName();
    if (ordertypeParameter.getRequiredCapacities() != null) {
      for (Capacity requiredCapacity : ordertypeParameter.getRequiredCapacities()) {
        getProcessing().requireCapacityForOrderType(ordertype, requiredCapacity.getCapName(),
                                                    requiredCapacity.getCardinality(),
                                                    ordertypeParameter.getRuntimeContext());
      }
    }
  }


  private void setMonitoringSetting(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException,
      XPRC_INVALID_MONITORING_TYPE {
    if (ordertypeParameter.getMonitoringLevel() != null) { // parse Monitoringlevel to prevent XPRC_INVALID_MONITORING_TYPE and savly ignore it 
      try {
        getMonitoringDispatcher().setMonitoringLevel(new DestinationKey(ordertypeParameter.getOrdertypeName(),
                                                                        ordertypeParameter.getRuntimeContext()),
                                                     ordertypeParameter.getMonitoringLevel());
      } catch (XPRC_ExecutionDestinationMissingException e) {
        // Take note that MonitoringLevel-Settings have to be made after DispatcherSettings because of dependencies
      }
    }
  }


  private void setPrioritySetting(OrdertypeParameter ordertypeParameter, long revision) throws PersistenceLayerException {
    if (ordertypeParameter.getPriority() != null) {
      int priority = PriorityManagement.restrictPriorityToThreadPriorityBounds(ordertypeParameter.getPriority());
      try {
        getFactoryManagement().setPriority(ordertypeParameter.getOrdertypeName(), priority, revision);
      } catch (XFMG_InvalidXynaOrderPriority e) {
        // this, dear compiler, should not be able to happen as we invoked restrictPriorityToThreadPriorityBounds
        throw new RuntimeException(e);
      }
    }
  }

  private void setParameterInheritanceRules(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException {
    if (ordertypeParameter.getParameterInheritanceRules() != null) {
      for (Entry<ParameterType, List<InheritanceRule>> entry : ordertypeParameter.getParameterInheritanceRules().entrySet()) {
        for (InheritanceRule inheritanceRule : entry.getValue()) {
          try {
            getParameterInheritanceManagement().addInheritanceRule(entry.getKey(),
                                                                   new DestinationKey(ordertypeParameter.getOrdertypeName(),
                                                                                      ordertypeParameter.getRuntimeContext()),
                                                                   inheritanceRule);
          } catch (XFMG_NoSuchRevision e) {
            throw new IllegalArgumentException(ordertypeParameter.getRuntimeContext() + " unknown", e);
          }
        }
      }
    }
  }


  private void modifyCapacityMapping(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException,
      XFMG_InvalidCapacityCardinality {
    String ordertype = ordertypeParameter.getOrdertypeName();
    List<Capacity> assignedCapacities =
        getProcessing()
            .getXynaProcessingODS()
            .getCapacityMappingDatabase()
            .getCapacities(new DestinationKey(ordertype, ordertypeParameter.getRuntimeContext()));
    if (assignedCapacities == null) {
      assignedCapacities = new ArrayList<Capacity>();
    }
    Set<Capacity> requiredCapacities = ordertypeParameter.getRequiredCapacities();

    if (requiredCapacities != null) {
      requirements : for (Capacity requiredCapacity : requiredCapacities) {
        for (Capacity assignedCapacity : assignedCapacities) {
          if (requiredCapacity.getCapName().equals(assignedCapacity.getCapName())) {
            if (requiredCapacity.getCardinality() != assignedCapacity.getCardinality()) {
              getProcessing().requireCapacityForOrderType(ordertype, requiredCapacity.getCapName(),
                                                          requiredCapacity.getCardinality(),
                                                          ordertypeParameter.getRuntimeContext());
            }
            continue requirements;
          }
        }
        getProcessing().requireCapacityForOrderType(ordertype, requiredCapacity.getCapName(),
                                                    requiredCapacity.getCardinality(),
                                                    ordertypeParameter.getRuntimeContext());
      }
    }

    surplus : for (Capacity assignedCapacity : assignedCapacities) {
      for (Capacity requiredCapacity : requiredCapacities) {
        if (requiredCapacity.getCapName().equals(assignedCapacity.getCapName())) {
          continue surplus;
        }
      }
      getProcessing().removeCapacityForOrderType(ordertype, assignedCapacity.getCapName(),
                                                 ordertypeParameter.getRuntimeContext());
    }
  }


  private void modifyMonitoringSetting(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException,
      XPRC_INVALID_MONITORING_TYPE {
    if (ordertypeParameter.getMonitoringLevel() == null) {
      deleteMonitoringSetting(ordertypeParameter);
    } else {
      setMonitoringSetting(ordertypeParameter);
    }
  }


  private void modifyPrioritySetting(OrdertypeParameter ordertypeParameter, long revision) throws PersistenceLayerException {
    if (ordertypeParameter.getPriority() == null) {
      deletePrioritySetting(ordertypeParameter, revision);
    } else {
      setPrioritySetting(ordertypeParameter, revision);
    }
  }

  
  private void modifyParameterInheritanceRules(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException {
    if (ordertypeParameter.getParameterInheritanceRules() == null) {
      return; //Regeln nicht �ndern
    }
    
    //aktuell konfigurierte Regeln ermitteln
    Map<ParameterType, List<InheritanceRule>> existing =
                    getParameterInheritanceManagement().listInheritanceRules(new DestinationKey(ordertypeParameter.getOrdertypeName(), ordertypeParameter.getRuntimeContext()));

    DestinationKey dk = new DestinationKey(ordertypeParameter.getOrdertypeName(), ordertypeParameter.getRuntimeContext());
    
    try {
      for (Entry<ParameterType, List<InheritanceRule>> entry : ordertypeParameter.getParameterInheritanceRules().entrySet()) {
        ParameterType type = entry.getKey();
        List<InheritanceRule> newRules = entry.getValue();
        
        if (newRules == null || newRules.size() == 0) {
          //alle aktuellen Regeln l�schen
          for (InheritanceRule oldRule : existing.get(type)) {
            getParameterInheritanceManagement().removeInheritanceRule(type, dk, oldRule.getChildFilter());
          }
        } else {
          for (InheritanceRule oldRule : existing.get(type)) {
            Iterator<InheritanceRule> it = newRules.iterator();
            boolean found = false;
            while(it.hasNext() && !found) {
              InheritanceRule newRule = it.next();
              if (newRule.getChildFilter().equals(oldRule.getChildFilter())) {
                if (!newRule.equals(oldRule)) {
                  //Regel hat sich ge�ndert
                  getParameterInheritanceManagement().addInheritanceRule(type, dk, newRule);
                }
                it.remove();
                found = true;
              }
            }
            if (!found) {
              //Regel soll gel�scht werden
              getParameterInheritanceManagement().removeInheritanceRule(type, dk, oldRule.getChildFilter());
            }
          }
          
          //Regel ist neu hinzugekommen
          for (InheritanceRule newRule : newRules) {
            getParameterInheritanceManagement().addInheritanceRule(type, dk, newRule);
          }
        }
      }
    } catch (XFMG_NoSuchRevision e) {
      throw new IllegalArgumentException(ordertypeParameter.getRuntimeContext() + " unknown", e);
    }
  }


  private void deleteOrdertypeInformation(OrdertypeParameter ordertypeParameter, long revision)
      throws PersistenceLayerException {
    OrdertypeInformation orderInformation = new OrdertypeInformation(ordertypeParameter, revision);
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.deleteOneRow(orderInformation);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }


  private Long getRevision(RuntimeContext runtimeContext) {
    try {
      return getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
          .getRevision(runtimeContext);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new IllegalArgumentException(runtimeContext + " unknown", e);
    }
  }

  private RuntimeContext getRuntimeContext(Long revision) {
    try {
      return getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                      .getRuntimeContext(revision);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new IllegalArgumentException("Revision " + revision + " not found", e);
    }
  }


  private void deleteDispatcherConfiguration(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException {
    DestinationKey destinationToRemove = new DestinationKey(ordertypeParameter.getOrdertypeName(), ordertypeParameter.getRuntimeContext());
    List<DispatcherIdentification> dispatchers =
        Arrays.asList(DispatcherIdentification.Planning, DispatcherIdentification.Execution,
                      DispatcherIdentification.Cleanup);
    for (DispatcherIdentification dispatcher : dispatchers) {
      getProcessing().removeDestination(dispatcher, destinationToRemove);
    }
  }


  private void deleteCapacityMapping(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException {
    getProcessing()
        .getXynaProcessingODS()
        .getCapacityMappingDatabase()
        .removeAllCapacities(new DestinationKey(ordertypeParameter.getOrdertypeName(), ordertypeParameter.getRuntimeContext()));
  }


  private void deleteMonitoringSetting(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException {
    DestinationKey destinationToRemove =
        new DestinationKey(ordertypeParameter.getOrdertypeName(),
                           ordertypeParameter.getRuntimeContext());
    try {
      getMonitoringDispatcher().removeMonitoringLevel(destinationToRemove);
    } catch (XPRC_DESTINATION_NOT_FOUND e) {
      // great, nothing to do
    }
  }


  private void deletePrioritySetting(OrdertypeParameter ordertypeParameter, long revision) throws PersistenceLayerException {
    getFactoryManagement().getXynaFactoryManagementODS().getPriorityManagement()
        .removePriority(ordertypeParameter.getOrdertypeName(), revision);
  }

  private void deleteParameterInheritanceRules(OrdertypeParameter ordertypeParameter) throws PersistenceLayerException {
    DestinationKey destinationToRemove =
                    new DestinationKey(ordertypeParameter.getOrdertypeName(),
                                       ordertypeParameter.getRuntimeContext());
    try {
      getParameterInheritanceManagement().removeInheritanceRules(destinationToRemove);
    } catch (XFMG_NoSuchRevision e) {
      throw new IllegalArgumentException(ordertypeParameter.getRuntimeContext() + " unknown", e);
    }
  }

  private DestinationValue convertDestinationValueParameterToDestinationValue(OrdertypeParameter ordertypeParameter,
                                                                              DispatcherIdentification dispatcher) {
    // TODO we're ignoring destinationType for now, there are open tasks before we can consider returning a ServiceDestination
    //   (currently ServiceDestination require additional setup that is currently done dynamic once a detached ServiceCall is encountered
    //   this would then needed to be performed here and ServiceDestinations will need to be properly setup on serverStart)
    DestinationValueParameter destinationValue = null;
    switch (dispatcher) {
      case Planning :
        destinationValue = ordertypeParameter.getPlanningDestinationValue();
        break;
      case Execution :
        destinationValue = ordertypeParameter.getExecutionDestinationValue();
        break;
      case Cleanup :
        destinationValue = ordertypeParameter.getCleanupDestinationValue();
        break;
    }
    String fullQualifiedName = null;
    if (destinationValue != null) {
      fullQualifiedName = destinationValue.getFullQualifiedName();
    } else {
      try {
        fullQualifiedName = GenerationBase.transformNameForJava(ordertypeParameter.getOrdertypeName());
      } catch (XPRC_InvalidPackageNameException e) {
        throw new IllegalArgumentException(e);
      }
    }
    return new FractalWorkflowDestination(fullQualifiedName);
  }


  public ArrayList<String> getWatchedProperties() {
    ArrayList<String> props = new ArrayList<String>();
    props.add(XynaProperty.HIDE_INTERNAL_ORDERS);
    return props;
  }


  public void propertyChanged() {
    String value = XynaFactory.getInstance().getFactoryManagement().getProperty(XynaProperty.HIDE_INTERNAL_ORDERS);
    if (value == null || value.equals("")) {
      return;
    } else {
      hideInternalOrders = Boolean.parseBoolean(value);
    }
  }
  
  
  public DestinationKey resolveDestinationKey(String orderType, long orderRevision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    if (XynaProperty.THROW_EXCEPTION_ON_DUPLICATE_DESTINATION_RESOLUTION.get()) {
      return resolveDestinationKey_throwOnDuplicate(orderType, orderRevision);
    } else {
      return resolveDestinationKey_returnFirstMatch(orderType, orderRevision);
    }
  }

   
  private static enum DefinitionSite {
    DISPATCHER, CAPACITY_MAPPING, MONITORING, PRIORITY, PARAMETER_INHERITANCE, DOCUMENTATION;
  }

  private DestinationKey resolveDestinationKey_throwOnDuplicate(String orderType, long orderRevision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Set<Long> deps = new HashSet<Long>();
    deps.add(orderRevision);
    rcdm.getDependenciesRecursivly(orderRevision, deps);
    Long found = null;
    for (Long dep : deps) {
      if (orderTypeExists(orderType, dep)) {
        if (found == null) {
          found = dep;
        } else {
          OrdertypeParameter otp1 = getOrdertype(orderType, dep);
          OrdertypeParameter otp2 = getOrdertype(orderType, found);
          String collisionSites = getDefinitionSites(otp1);
          String definitionSites = getDefinitionSites(otp2);
          try {
            throw new RuntimeException("OrdertypeCollision: '" + orderType + "' is defined in "
                + rm.getRuntimeContext(dep).getGUIRepresentation() + " " + collisionSites + " and in "
                + rm.getRuntimeContext(found).getGUIRepresentation() + " " + definitionSites);
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            //racecondition?
            throw new RuntimeException("OrdertypeCollision: '" +orderType + "' is defined in " + dep + " " + collisionSites
                + " and in " + found + " " + definitionSites);
          }
        }
      }
    }
    if (found == null) {
      throw new RuntimeException("Ordertype " + orderType + " can not be resolved from "
          + rm.getRuntimeContext(orderRevision).getGUIRepresentation() + ".");
    }
    return new DestinationKey(orderType, rm.getRuntimeContext(found));
  }

  
  private String getDefinitionSites(OrdertypeParameter otp) {
    if (otp == null) {
      return null;
    }
    EnumSet<DefinitionSite> set = EnumSet.noneOf(DefinitionSite.class);
    if (otp.getExecutionDestinationValue() != null) {
      set.add(DefinitionSite.DISPATCHER);
    }
    if (otp.getRequiredCapacities() != null && otp.getRequiredCapacities().size() > 0) {
      set.add(DefinitionSite.CAPACITY_MAPPING);
    }
    if (otp.getMonitoringLevel() != null) {
      set.add(DefinitionSite.MONITORING);
    }
    if (otp.getPriority() != null) {
      set.add(DefinitionSite.PRIORITY);
    }
    if (otp.getParameterInheritanceRules() != null) {
      set.add(DefinitionSite.PARAMETER_INHERITANCE);
    }
    if (otp.getDocumentation() != null) {
      set.add(DefinitionSite.DOCUMENTATION);
    }
    return set.toString();
  }


  private DestinationKey resolveDestinationKey_returnFirstMatch(String orderType, long rev) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    RevisionManagement revisionManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    if (!orderTypeExists(orderType, rev)) { //fastpath
      RuntimeContext rtc =  revisionManagement.getRuntimeContext(rev);
      Map<DestinationKey, Integer> resultMap = new HashMap<>();
      resolveDestinationKey_returnDeepestExecution_recursivly(new DestinationKey(orderType, rtc), 1, resultMap);
      Entry<DestinationKey, Integer> deepest = null;
      for (Entry<DestinationKey, Integer> entry : resultMap.entrySet()) {
        if (deepest == null ||                       // first entry
            deepest.getValue() < entry.getValue()) { // new deepest
          deepest = entry;
        }
      }
      if (deepest == null) {
        throw new RuntimeException("Ordertype " + orderType + " can not be resolved from " + rtc.getGUIRepresentation() + ".");
      }
      return deepest.getKey();
    }
    return new DestinationKey(orderType, revisionManagement.getRuntimeContext(rev));
  }


  private void resolveDestinationKey_returnDeepestExecution_recursivly(DestinationKey dk, int currentDepth,
                                                                       Map<DestinationKey, Integer> resultMap)  {
    if (orderTypeExists(dk.getOrderType(), getRevision(dk.getRuntimeContext()))) {
      Integer recordedDepth = resultMap.get(dk);
      if (recordedDepth == null ||        // first entry 
          recordedDepth > currentDepth) { // there is a shorter path in the same hierarchy
        resultMap.put(dk, currentDepth);
      } // else keep the old entry
    }
    
    RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    Collection<RuntimeDependencyContext> dependencies = rcdm.getDependencies(RuntimeContextDependencyManagement.asRuntimeDependencyContext(dk.getRuntimeContext()));
    for (RuntimeDependencyContext dependency : dependencies) {
      resolveDestinationKey_returnDeepestExecution_recursivly(new DestinationKey(dk.getOrderType(), dependency.asCorrespondingRuntimeContext()),
                                                              currentDepth + 1, resultMap);
    }
  }


  private static final int ORDERTYPE_MIN_LENGTH = 30;
  private static final int ORDERTYPE_MAX_LENGTH = 90;
  private static final int APPLICATION_MIN_LENGTH = 20;
  private static final int APPLICATION_MAX_LENGTH = 30;
  private static final int VERSION_MIN_LENGTH = 15;
  private static final int VERSION_MAX_LENGTH = 30;
  private static final int WORKSPACE_MIN_LENGTH = 20;
  private static final int WORKSPACE_MAX_LENGTH = 30;


  // TODO refactor to formatUtils or a cliCommand-Helper class?
  public static OrdertypeFormatLength calculateOrdertypeFormatParameter(Collection<DestinationKey> toFormat) {
    int ordertypeMaxLength = ORDERTYPE_MIN_LENGTH;
    int applicationMaxLength = APPLICATION_MIN_LENGTH;
    int versionMaxLength = VERSION_MIN_LENGTH;
    int workspaceMaxLength = WORKSPACE_MIN_LENGTH;
    for (DestinationKey dk : toFormat) {
      if (dk.getOrderType().length() > ordertypeMaxLength) {
        ordertypeMaxLength = dk.getOrderType().length();
      }
      if (dk.getApplicationName() != null && dk.getApplicationName().length() > applicationMaxLength) {
        applicationMaxLength = dk.getApplicationName().length();
      }
      if (dk.getVersionName() != null && dk.getVersionName().length() > versionMaxLength) {
        versionMaxLength = dk.getVersionName().length();
      }
      if (dk.getWorkspaceName() != null && dk.getWorkspaceName().length() > workspaceMaxLength) {
        workspaceMaxLength = dk.getWorkspaceName().length();
      }
    }
    return new OrdertypeFormatLength(Math.min(ordertypeMaxLength, ORDERTYPE_MAX_LENGTH),
                                     Math.min(applicationMaxLength, APPLICATION_MAX_LENGTH),
                                     Math.min(versionMaxLength, VERSION_MAX_LENGTH),
                                     Math.min(workspaceMaxLength, WORKSPACE_MAX_LENGTH));
  }


  public static class OrdertypeFormatLength {

    public int ordertypeLength;
    public int applicationLength;
    public int versionLength;
    public int workspaceLength;


    OrdertypeFormatLength(int ordertypeLength, int applicationLength, int versionLength, int workspaceLength) {
      this.ordertypeLength = ordertypeLength;
      this.applicationLength = applicationLength;
      this.versionLength = versionLength;
      this.workspaceLength = workspaceLength;
    }
  }



}

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
package com.gip.xyna.xprc.xpce.parameterinheritance;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport.ValueProcessor;
import com.gip.xyna.utils.collections.ObjectWithRemovalSupport;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement.OrderTypeUpdates;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_MONITORING_TYPE;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringDispatcher;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule.Builder;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule.PrecedenceComparator;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRuleCollection;
import com.gip.xyna.xprc.xpce.parameterinheritance.storables.InheritanceRuleStorable;



public class ParameterInheritanceManagement extends FunctionGroup {

  public static final String DEFAULT_NAME = "ParameterInheritanceManagement";
  
  //für OrderTypes konfigurierte InheritanceRules
  private static EnumMap<ParameterType, ParameterInheritanceMap> orderTypeInheritanceRules = new EnumMap<ParameterType, ParameterInheritanceMap>(ParameterType.class);
  
  private ParameterInheritanceStorage storage;
  private PrecedenceComparator precedenceComparator = new PrecedenceComparator();
  
  private static class ParameterInheritanceMap extends ConcurrentMapWithObjectRemovalSupport<DestinationKey, InheritanceRuleCollectionWrapper> {
    
    private static final long serialVersionUID = 1L;
    
    @Override
    public InheritanceRuleCollectionWrapper createValue(DestinationKey key) {
      return new InheritanceRuleCollectionWrapper();
    }
  }
  
  
  private static class InheritanceRuleCollectionWrapper extends ObjectWithRemovalSupport {
    private InheritanceRuleCollection collection = new InheritanceRuleCollection();
    
    public InheritanceRule getPreferredInheritanceRule(String childHierarchy) {
      return collection.getPreferredInheritanceRule(childHierarchy);
    }
    
    public boolean shouldBeDeleted() {
      return collection.isEmpty();
    }
  }


  public static enum ParameterType {
    MonitoringLevel {

      @Override
      public Builder createInheritanceRuleBuilder(String value) {
        try {
          return InheritanceRule.createMonitoringLevelRule(value);
        } catch (XPRC_INVALID_MONITORING_TYPE e) {
          throw new IllegalArgumentException(e);
        }
      }
    },
    SuspensionBackupMode {

      @Override
      public Builder createInheritanceRuleBuilder(String value) {
        return InheritanceRule.createSuspensionBackupRule(com.gip.xyna.xprc.xpce.ordersuspension.SuspensionBackupMode.valueOf(value));
      }
      
    };

    public abstract Builder createInheritanceRuleBuilder(String value);
    
    public static Map<ParameterType, InheritanceRuleCollection> createInheritanceRuleMap() {
      EnumMap<ParameterType, InheritanceRuleCollection> rules = new EnumMap<ParameterType, InheritanceRuleCollection>(ParameterType.class);
      for (ParameterType type : ParameterType.values()) {
        rules.put(type, new InheritanceRuleCollection());
      }
      return rules;
    }
  }

  public static enum DefaultPrecedence {
    XynaOrder(100), //bei der XynaOrder gesetztes MonitoringLevel
    OrderType(0);  //für den OrderType konfiguriertes MonitoringLevel
    
    private int value;

    private DefaultPrecedence(int value) {
      this.value = value;
    }
    
    public int getValue() {
      return value;
    }
    
  }
  
  public ParameterInheritanceManagement() throws XynaException {
    super();
  }

  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    for (ParameterType type : ParameterType.values()) {
      orderTypeInheritanceRules.put(type, new ParameterInheritanceMap());
    }
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(ParameterInheritanceStorage.class,"ParameterInheritanceManagement.initStorage").
      after(PersistenceLayerInstances.class).
      execAsync(new Runnable() { public void run() { initStorage(); }});
    
    fExec.addTask(ParameterInheritanceManagement.class,"ParameterInheritanceManagement.initInheritanceRules").
      after(ParameterInheritanceStorage.class, RevisionManagement.class).
      execAsync(new Runnable() { public void run() { initInheritanceRules(); }});
  }

  private void initStorage() {
    try {
      storage = new ParameterInheritanceStorage();
    } catch( PersistenceLayerException e ) {
      logger.warn("Could not initialize ParameterInheritanceManagement", e);
      throw new RuntimeException(e);
    }
  }

  private void initInheritanceRules() {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    try {
      Collection<InheritanceRuleStorable> storables = storage.getAllInheritanceRules();
      for (InheritanceRuleStorable storable : storables) {
        ParameterType parameterType = storable.getParameterTypeAsEnum();
        InheritanceRule rule = parameterType.createInheritanceRuleBuilder(storable.getValue()).childFilter(storable.getChildFilter()).precedence(storable.getPrecedence()).build();
        try {
          DestinationKey dk = new DestinationKey(storable.getOrderType(), revisionManagement.getRuntimeContext(storable.getRevision()));
          addInheritanceRuleToMap(parameterType, dk, rule);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          logger.warn("Could not initialize parameter inheritance rule of type '" + storable.getParameterType() 
                          + "' for order type '" + storable.getOrderType() + "' and childFilter '" 
                          + storable.getChildFilter() + "'", e);
        }
      }
    } catch (PersistenceLayerException e) {
      logger.warn("Could not initialize ParameterInheritanceManagement", e);
      throw new RuntimeException(e);
    }
  }
  
  
  @Override
  protected void shutdown() throws XynaException {
    
  }
  
  
  public void addInheritanceRule(ParameterType parameterType, DestinationKey destinationKey, final InheritanceRule inheritanceRule) throws PersistenceLayerException, XFMG_NoSuchRevision {
    long revision = getRevision(destinationKey);
    addInheritanceRuleToMap(parameterType, destinationKey, inheritanceRule);
    storage.persistInheritanceRule(parameterType, destinationKey, inheritanceRule);
    
    if (parameterType.equals(ParameterType.MonitoringLevel)
         && (inheritanceRule.getChildFilter() == null || inheritanceRule.getChildFilter().length() == 0)) {
      //falls die Regel für den eigenen Ordertype gilt, muss ein evtl. existierendes
      //statisches Monitoringlevel gelöscht werden
      MonitoringDispatcher monitoringDispatcher = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher();
      if (monitoringDispatcher.getMonitoringLevel(destinationKey) != null) {
        try {
          monitoringDispatcher.removeMonitoringLevel(destinationKey);
        } catch (XPRC_DESTINATION_NOT_FOUND e) {
          // ok, nichts zu tun
        }
      }
    }
    
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement()
        .updateInCache(revision, destinationKey.getOrderType(),
                       OrderTypeUpdates.setParameterInheritanceRules(listInheritanceRules(destinationKey)));
  }


  private long getRevision(DestinationKey destinationKey) {
    try {
      return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(destinationKey.getRuntimeContext());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
  }

  private void addInheritanceRuleToMap(ParameterType parameterType, DestinationKey destinationKey, final InheritanceRule inheritanceRule) {
    ParameterInheritanceMap map = orderTypeInheritanceRules.get(parameterType);
    map.process(destinationKey, new ValueProcessor<InheritanceRuleCollectionWrapper, InheritanceRule>() {
      
      public InheritanceRule exec(InheritanceRuleCollectionWrapper v) {
        return v.collection.add(inheritanceRule);
      }
    });
  }
  
  public InheritanceRule removeInheritanceRule(ParameterType parameterType, DestinationKey destinationKey, final String childFilter) throws PersistenceLayerException, XFMG_NoSuchRevision {
    long revision = getRevision(destinationKey);
    ParameterInheritanceMap map = orderTypeInheritanceRules.get(parameterType);
    InheritanceRule removed = map.process(destinationKey, new ValueProcessor<InheritanceRuleCollectionWrapper, InheritanceRule>() {

      public InheritanceRule exec(InheritanceRuleCollectionWrapper v) {
        if (childFilter == null) {
          return v.collection.remove("");
        }
        return v.collection.remove(childFilter);
      }
    });
    
    storage.deleteInheritanceRule(destinationKey, parameterType, childFilter);
    
    
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement()
        .updateInCache(revision, destinationKey.getOrderType(),
                       OrderTypeUpdates.setParameterInheritanceRules(listInheritanceRules(destinationKey)));
    return removed;
  }

  public void removeInheritanceRules(DestinationKey destinationKey) throws PersistenceLayerException, XFMG_NoSuchRevision {
    long revision = getRevision(destinationKey);
    for (ParameterInheritanceMap value : orderTypeInheritanceRules.values()) {
      value.process(destinationKey, new ValueProcessor<InheritanceRuleCollectionWrapper, Boolean>() {
        
        public Boolean exec(InheritanceRuleCollectionWrapper v) {
          v.collection.clear();
          return true;
        }
      });
    }
    
    storage.deleteInheritanceRulesForOrderType(destinationKey);
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement()
    .updateInCache(revision, destinationKey.getOrderType(),
                   OrderTypeUpdates.setParameterInheritanceRules(listInheritanceRules(destinationKey)));
  }

  public Map<ParameterType, List<InheritanceRule>> listInheritanceRules(DestinationKey destinationKey) {
    Map<ParameterType, List<InheritanceRule>> ret = new EnumMap<ParameterType, List<InheritanceRule>>(ParameterType.class);
    for (ParameterType type : orderTypeInheritanceRules.keySet()) {
      ret.put(type, listInheritanceRules(type, destinationKey));
    }
    
    return ret;
  }

  public List<InheritanceRule> listInheritanceRules(ParameterType parameterType, DestinationKey destinationKey) {
    ParameterInheritanceMap map = orderTypeInheritanceRules.get(parameterType);
    return map.process(destinationKey, new ValueProcessor<InheritanceRuleCollectionWrapper, List<InheritanceRule>>() {
      
      public List<InheritanceRule> exec(InheritanceRuleCollectionWrapper v) {
        return v.collection.getInheritanceRulesOrderedByChildFilter();
      }
    });
  }
  
  /**
   * Liefert alle OrderTypes für die eine oder mehrere Vererbungsregeln definiert sind.
   * @return
   */
  public Set<DestinationKey> discoverInheritanceRuleOrderTypes() {
    Set<DestinationKey> orderTypes = new HashSet<DestinationKey>();
    for (ParameterInheritanceMap map : orderTypeInheritanceRules.values()) {
      orderTypes.addAll(map.keySet());
    }
    
    return orderTypes;
  }
  
  /**
   * Ermittelt die Regel die für die XynaOrder für das MonitoringLevel verwendet werden soll,
   * d.h. die Regel mit der höchsten Precedence die für die XynaOrder gilt.
   * @param xo
   * @return
   */
  public InheritanceRule getPreferredMonitoringLevelRule(XynaOrderServerExtension xo) {
    InheritanceRule preferredRule = null;
    
    //in XynaOrder gesetztes MonitoringLevel auch als InheritanceRule betrachten
    if (xo.getMonitoringCode() != null) {
      InheritanceRule monitoringLevelRule = ParameterType.MonitoringLevel.createInheritanceRuleBuilder(xo.getMonitoringCode().toString()).precedence(DefaultPrecedence.XynaOrder.getValue()).build();
      preferredRule = monitoringLevelRule;
    }
    
    //für OrderType konfiguriertes MonitoringLevel auch als InheritanceRule betrachten
    Integer code = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher().getMonitoringLevel(xo.getDestinationKey());
    if (code != null) {
      InheritanceRule  orderTypeMonitoringLevelRule = ParameterType.MonitoringLevel.createInheritanceRuleBuilder(code.toString()).precedence(DefaultPrecedence.OrderType.getValue()).build();
      preferredRule = compareRules(preferredRule, orderTypeMonitoringLevelRule);
    }
    
    //die Regel mit der höchsten Precedence im eigenen Auftrag und seinen Parents ermitteln
    return getPreferredRuleRecursively(ParameterType.MonitoringLevel, preferredRule, xo, "");
  }
  
  
  public InheritanceRule getPreferredSuspensionBackupRule(ParameterType parameterType, XynaOrderServerExtension xo) {
    InheritanceRule global = 
      ParameterType.SuspensionBackupMode.createInheritanceRuleBuilder(
                                         com.gip.xyna.xprc.xpce.ordersuspension.SuspensionBackupMode.DEFAULT_ORDERBACKUP_MODE.get().name()).precedence(0).build();
    return getPreferredRuleRecursively(parameterType, global, xo, "");
  }
  
  
  private InheritanceRule getPreferredRuleRecursively(ParameterType parameterType, InheritanceRule preferredRule, XynaOrderServerExtension xo, String childHierarchy) {
    DestinationKey dk = xo.getDestinationKey();
    
    //in der XynaOrder definierte Regeln
    InheritanceRuleCollection xynaOrderRules = xo.getParameterInheritanceRules(parameterType);
    InheritanceRule xynaOrderRule = xynaOrderRules.getPreferredInheritanceRule(childHierarchy);
    preferredRule = compareRules(preferredRule, xynaOrderRule);
    
    //beim OrderType konfigurierte Regeln
    InheritanceRuleCollectionWrapper orderTypeRules = orderTypeInheritanceRules.get(parameterType).get(dk);
    if (orderTypeRules != null) {
      InheritanceRule orderTypeRule = orderTypeRules.getPreferredInheritanceRule(childHierarchy);
      preferredRule = compareRules(preferredRule, orderTypeRule);
    }
    
    if (xo.hasParentOrder()) {
      //den eigenen Auftrag als Parent zur ChildHierarchy hinzufügen
      childHierarchy = prependOrderTypeToHierarchy(dk.getOrderType(), childHierarchy);
      //beim Parent nach weiteren passenden Regeln suchen
      preferredRule = getPreferredRuleRecursively(parameterType, preferredRule, xo.getParentOrder(), childHierarchy);
    }
    
    return preferredRule;
  }
  
  /**
   * Vergleicht zwei Vererbungsregeln und liefert die mit der höheren Precedence zurück.
   * Bei Gleichheit wird newRule bevorzugt.
   * @param currentRule
   * @param newRule
   * @return
   */
  private InheritanceRule compareRules(InheritanceRule currentRule, InheritanceRule newRule) {
    int comp = precedenceComparator.compare(currentRule, newRule);
    
    if (comp <= 0) {
      //neue Regel hat gleiche oder höhere Precedence
      return newRule;
    } else {
      //alte Regel behalten, da neue Regel keine höhere Precedence hat
      return currentRule;
    }
  }
  
  private final static Pattern backslashPattern = Pattern.compile("[\\\\]");
  private final static Pattern colonPattern = Pattern.compile(":");
  private final static Pattern starPattern = Pattern.compile("[*]");
  
  /**
   * Fügt den OrderType an den Anfang der Hierarchie an.
   * @param orderType
   * @param hierarchy
   * @return
   */
  public static String prependOrderTypeToHierarchy(String orderType, String hierarchy) {
    orderType = backslashPattern.matcher(orderType).replaceAll("\\\\");
    orderType = colonPattern.matcher(orderType).replaceAll("\\\\:");
    String escapedOrderType = starPattern.matcher(orderType).replaceAll("\\\\*");
    if (hierarchy.isEmpty()) {
      return escapedOrderType;
    } else {
      return escapedOrderType + ":" + hierarchy;
    }
  }
  
}

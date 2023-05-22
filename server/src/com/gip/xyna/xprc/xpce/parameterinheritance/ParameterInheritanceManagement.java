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
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBase;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement.OrderTypeUpdates;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_MONITORING_TYPE;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringCodes;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringDispatcher;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspensionBackupMode;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule.Builder;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule.PrecedenceComparator;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRuleCollection;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.XynaPropertyInheritanceRule;
import com.gip.xyna.xprc.xpce.parameterinheritance.storables.InheritanceRuleStorable;



public class ParameterInheritanceManagement extends FunctionGroup {

  public static final String DEFAULT_NAME = "ParameterInheritanceManagement";
  
  //f�r OrderTypes konfigurierte InheritanceRules
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
      
    },
    BackupWhenRemoteCall {

      @Override
      public Builder createInheritanceRuleBuilder(String value) {
        return InheritanceRule.createBackupWhenRemoteCallRule(com.gip.xyna.xprc.xpce.ordersuspension.SuspensionBackupMode.valueOf(value));
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
    OrderType(0);  //f�r den OrderType konfiguriertes MonitoringLevel
    
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

  private static class XynaPropertyMLWrapper extends ObjectWithRemovalSupport {
    
    private static class InheritanceRuleAndDestinationKey {
      
      private final InheritanceRule rule;
      
      private final DestinationKey dk;
      
      public InheritanceRuleAndDestinationKey(InheritanceRule rule, DestinationKey dk) {
        super();
        this.rule = rule;
        this.dk = dk;
      }
      
      @Override
      public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dk == null) ? 0 : dk.hashCode());
        result = prime * result + ((rule == null) ? 0 : rule.hashCode());
        return result;
      }
      
      @Override
      public boolean equals(Object obj) {
        if (this == obj)
          return true;
        if (obj == null)
          return false;
        if (getClass() != obj.getClass())
          return false;
        InheritanceRuleAndDestinationKey other = (InheritanceRuleAndDestinationKey) obj;
        if (dk == null) {
          if (other.dk != null)
            return false;
        } else if (!dk.equals(other.dk))
          return false;
        if (rule == null) {
          if (other.rule != null)
            return false;
        } else if (!rule.equals(other.rule))
          return false;
        return true;
      }
      
    }
    
    private final XynaPropertyML property;
    private final Set<InheritanceRuleAndDestinationKey> rules = new HashSet<>();
    
    XynaPropertyMLWrapper(String key) {
      this.property = new XynaPropertyML(key);
      this.property.setDefaultDocumentation(DocumentationLanguage.EN, "Used by inheritance rules for monitoring level configuration of some ordertypes.");
    }
    
    @Override
    protected void onDeletion() {
      property.unregister();
      super.onDeletion();
    }

    @Override
    protected synchronized boolean shouldBeDeleted() {
      return rules.isEmpty();
    }

    public synchronized void setUsedBy(DestinationKey dk, InheritanceRule inheritanceRule) {
      rules.add(new InheritanceRuleAndDestinationKey(inheritanceRule, dk));
    }

    public synchronized void notUsedAnyMoreBy(DestinationKey dk, InheritanceRule removed) {
      rules.remove(new InheritanceRuleAndDestinationKey(removed, dk));
    }
  }
  
  public static class XynaPropertyML extends XynaPropertyBase<Integer,XynaPropertyInt> {

    public XynaPropertyML(String name) {
      super(name, null, "MonitoringLevel");
    }

    protected Integer fromString(String string) throws Exception {
      return validateXynaPropertyValueForMonitoringLevel(getPropertyName(), string);
    }

  }
  
  private static Integer validateXynaPropertyValueForMonitoringLevel(String propertyName, String string) {
    if (string == null) {
      return null;
    }
    Integer val;
    try {
      val = Integer.valueOf(string);
    } catch (NumberFormatException e) {
      val = -10;
    }
    if (!MonitoringCodes.isValid(val)) {
      throw new RuntimeException("The value of the XynaProperty " + propertyName + " is <" + string + ">, which is not a valid value for a monitoring level configuration.");
    }
    return val;
  }

  
  public void addInheritanceRule(ParameterType parameterType, DestinationKey destinationKey, final InheritanceRule inheritanceRule) throws PersistenceLayerException, XFMG_NoSuchRevision {
    long revision = getRevision(destinationKey);
    if (parameterType == ParameterType.MonitoringLevel && inheritanceRule instanceof XynaPropertyInheritanceRule) {
      //validation: only when manually adding new rule. not when loading at server init
      XynaPropertyInheritanceRule xpir = (XynaPropertyInheritanceRule) inheritanceRule;
      validateXynaPropertyValueForMonitoringLevel(xpir.getPropertyName(), xpir.getValueAsString());
      //deregister previously registered property if overwritten
      InheritanceRule previous = orderTypeInheritanceRules.get(parameterType).process(destinationKey, w -> {
        return w.collection.getRuleByChildFilter(xpir.getChildFilter());
      });
      if (previous != null && previous instanceof XynaPropertyInheritanceRule) {
        XynaPropertyInheritanceRule previousXpir = (XynaPropertyInheritanceRule) previous;
        propertiesUsedByMonitoringLevelRules.process(previousXpir.getPropertyName(), v -> {
          v.notUsedAnyMoreBy(destinationKey, previousXpir);
          return null;
        });
      }
    }
    addInheritanceRuleToMap(parameterType, destinationKey, inheritanceRule);
    storage.persistInheritanceRule(parameterType, destinationKey, inheritanceRule);
    
    if (parameterType.equals(ParameterType.MonitoringLevel)
         && (inheritanceRule.getChildFilter() == null || inheritanceRule.getChildFilter().length() == 0)) {
      //falls die Regel f�r den eigenen Ordertype gilt, muss ein evtl. existierendes
      //statisches Monitoringlevel gel�scht werden
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
  
  /*
   * when properties are used to define the monitoring level used in inheritance rules, the properties should have a validation activated for value changes.
   * this map enables this behavior: when the XynaPropertyML is created, it is registered together with it validation. when the last rule that uses that
   * property is removed the property and its validation will be unregistered.
   */
  private ConcurrentMapWithObjectRemovalSupport<String, XynaPropertyMLWrapper> propertiesUsedByMonitoringLevelRules = new ConcurrentMapWithObjectRemovalSupport<String, XynaPropertyMLWrapper>() {

    private static final long serialVersionUID = 1L;

    @Override
    public XynaPropertyMLWrapper createValue(String key) {
      return new XynaPropertyMLWrapper(key);
    }
    
  };

  private void addInheritanceRuleToMap(ParameterType parameterType, DestinationKey destinationKey, final InheritanceRule inheritanceRule) {
    if (inheritanceRule instanceof XynaPropertyInheritanceRule) {
      XynaPropertyInheritanceRule xpir = (XynaPropertyInheritanceRule) inheritanceRule;
      propertiesUsedByMonitoringLevelRules.process(xpir.getPropertyName(), v -> {
        v.setUsedBy(destinationKey, inheritanceRule);
        return null;
      });
    }
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
    if (removed instanceof XynaPropertyInheritanceRule) {
      XynaPropertyInheritanceRule xpir = (XynaPropertyInheritanceRule) removed;
      propertiesUsedByMonitoringLevelRules.process(xpir.getPropertyName(), v -> {
        v.notUsedAnyMoreBy(destinationKey, removed);
        return null;
      });
    }
    
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
          for (InheritanceRule r : v.collection.getAllInheritanceRules()) {
            if (r instanceof XynaPropertyInheritanceRule) {
              XynaPropertyInheritanceRule xpir = (XynaPropertyInheritanceRule) r;
              propertiesUsedByMonitoringLevelRules.process(xpir.getPropertyName(), new ValueProcessor<XynaPropertyMLWrapper, Void>() {

                @Override
                public Void exec(XynaPropertyMLWrapper v) {
                  v.notUsedAnyMoreBy(destinationKey, r);
                  return null;
                }
                
              });
            }
          }
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
   * Liefert alle OrderTypes f�r die eine oder mehrere Vererbungsregeln definiert sind.
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
   * Ermittelt die Regel die f�r die XynaOrder f�r das MonitoringLevel verwendet werden soll,
   * d.h. die Regel mit der h�chsten Precedence die f�r die XynaOrder gilt.
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
    
    //f�r OrderType konfiguriertes MonitoringLevel auch als InheritanceRule betrachten
    Integer code = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher().getMonitoringLevel(xo.getDestinationKey());
    if (code != null) {
      InheritanceRule  orderTypeMonitoringLevelRule = ParameterType.MonitoringLevel.createInheritanceRuleBuilder(code.toString()).precedence(DefaultPrecedence.OrderType.getValue()).build();
      preferredRule = compareRules(preferredRule, orderTypeMonitoringLevelRule);
    }
    
    //die Regel mit der h�chsten Precedence im eigenen Auftrag und seinen Parents ermitteln
    return getPreferredRuleRecursively(ParameterType.MonitoringLevel, preferredRule, xo, "");
  }
  
  
  public InheritanceRule getPreferredSuspensionBackupRule(XynaOrderServerExtension xo) {
    InheritanceRule global = 
      ParameterType.SuspensionBackupMode.createInheritanceRuleBuilder(
                                         com.gip.xyna.xprc.xpce.ordersuspension.SuspensionBackupMode.DEFAULT_ORDERBACKUP_MODE.get().name()).precedence(0).build();
    return getPreferredRuleRecursively(ParameterType.SuspensionBackupMode, global, xo, "");
  }
  

  public final static XynaPropertyBuilds<SuspensionBackupMode> BACKUP_WHEN_REMOTECALL_DEFAULT =
      new XynaPropertyBuilds<SuspensionBackupMode>("xfmg.xfctrl.nodemgmt.remotecall.defaultbackupmode",
                                                   new XynaPropertyUtils.XynaPropertyBuilds.Builder<SuspensionBackupMode>() {

                                                     public SuspensionBackupMode fromString(String string)
                                                         throws com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds.Builder.ParsingException {
                                                       return SuspensionBackupMode.valueOf(string);
                                                     }


                                                     public String toString(SuspensionBackupMode value) {
                                                       return value.toString();
                                                     }

                                                   }, SuspensionBackupMode.BACKUP)
                                                       .setDefaultDocumentation(DocumentationLanguage.EN,
                                                                                "Possible values are \"BACKUP\" and \"NO_BACKUP\".");


  public InheritanceRule getPreferredBackupWhenRemoteCallRule(XynaOrderServerExtension xo) {
    String defaultVal = BACKUP_WHEN_REMOTECALL_DEFAULT.get().name();
    InheritanceRule global = ParameterType.BackupWhenRemoteCall.createInheritanceRuleBuilder(defaultVal).precedence(0).build();
    return getPreferredRuleRecursively(ParameterType.BackupWhenRemoteCall, global, xo, "");
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
      //den eigenen Auftrag als Parent zur ChildHierarchy hinzuf�gen
      childHierarchy = prependOrderTypeToHierarchy(dk.getOrderType(), childHierarchy);
      //beim Parent nach weiteren passenden Regeln suchen
      preferredRule = getPreferredRuleRecursively(parameterType, preferredRule, xo.getParentOrder(), childHierarchy);
    }
    
    return preferredRule;
  }
  
  /**
   * Vergleicht zwei Vererbungsregeln und liefert die mit der h�heren Precedence zur�ck.
   * Bei Gleichheit wird newRule bevorzugt.
   * @param currentRule
   * @param newRule
   * @return
   */
  private InheritanceRule compareRules(InheritanceRule currentRule, InheritanceRule newRule) {
    int comp = precedenceComparator.compare(currentRule, newRule);
    
    if (comp <= 0) {
      //neue Regel hat gleiche oder h�here Precedence
      return newRule;
    } else {
      //alte Regel behalten, da neue Regel keine h�here Precedence hat
      return currentRule;
    }
  }
  
  private final static Pattern backslashPattern = Pattern.compile("[\\\\]");
  private final static Pattern colonPattern = Pattern.compile(":");
  private final static Pattern starPattern = Pattern.compile("[*]");
  
  /**
   * F�gt den OrderType an den Anfang der Hierarchie an.
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

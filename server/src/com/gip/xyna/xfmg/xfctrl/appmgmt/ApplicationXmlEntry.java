/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xfmg.xfctrl.appmgmt;



import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.update.Updater;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_DETECTION_PROBLEM;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;
import com.gip.xyna.xprc.xsched.CapacityManagement;


public class ApplicationXmlEntry {

  private static final Logger logger = CentralFactoryLogging.getLogger(ApplicationXmlEntry.class);
  private static final String CURRENT_VERSION_IDENTIFIER = "CURRENT_VERSION";

  protected ApplicationInfoEntry applicationInfo;
  protected List<CapacityXmlEntry> capacities;
  protected List<CapacityRequirementXmlEntry> capacityRequirements;
  protected List<FilterInstanceXmlEntry> filterInstances;
  protected List<FilterXmlEntry> filters;
  protected List<OrdertypeXmlEntry> ordertypes;
  protected List<SharedLibXmlEntry> sharedLibs;
  protected List<TriggerXmlEntry> triggers;
  protected List<TriggerInstanceXmlEntry> triggerInstances;
  protected List<XMOMXmlEntry> xmomEntries;
  protected List<XynaPropertyXmlEntry> xynaProperties;
  protected List<PriorityXmlEntry> priorities;
  protected List<MonitoringLevelXmlEntry> monitoringLevels;
  protected List<XMOMStorableXmlEntry> xmomStorables;
  protected List<OrderInputSourceXmlEntry> orderInputSources;
  protected List<InheritanceRuleXmlEntry> parameterInheritanceRules;
  protected List<String> ordertypesWithOrderContext;
  protected String applicationName;
  protected String versionName;
  protected String xmlVersion;
  protected String comment;
  protected String factoryVersion;

  
  public ApplicationXmlEntry(String applicationName, String versionName, String comment) {
    this();
    this.applicationName = applicationName;
    this.versionName = versionName;
    this.comment = comment;
  }

  public ApplicationXmlEntry() {
    applicationInfo = new ApplicationInfoEntry();
    capacities = new ArrayList<CapacityXmlEntry>();
    capacityRequirements = new ArrayList<CapacityRequirementXmlEntry>();
    filterInstances = new ArrayList<FilterInstanceXmlEntry>();
    ordertypes = new ArrayList<OrdertypeXmlEntry>();
    sharedLibs = new ArrayList<SharedLibXmlEntry>();
    triggers = new ArrayList<TriggerXmlEntry>();
    triggerInstances = new ArrayList<TriggerInstanceXmlEntry>();
    xmomEntries = new ArrayList<XMOMXmlEntry>();
    xynaProperties = new ArrayList<XynaPropertyXmlEntry>();
    filters = new ArrayList<FilterXmlEntry>();
    monitoringLevels = new ArrayList<MonitoringLevelXmlEntry>();
    xmomStorables = new ArrayList<XMOMStorableXmlEntry>();
    priorities = new ArrayList<PriorityXmlEntry>();
    orderInputSources = new ArrayList<OrderInputSourceXmlEntry>();
    parameterInheritanceRules = new ArrayList<InheritanceRuleXmlEntry>();
  }


  private <E> boolean listsEqual(List<E> first, List<E> second, String type) {
    if (first == null) {
      return second == null;
    }
    if (second == null) {
      return false;
    }
    Set<E> firstSet = new HashSet<E>(first);
    Set<E> secondSet = new HashSet<E>(second);
    
    boolean equals = firstSet.equals(secondSet);
    if (logger.isDebugEnabled()) {
      if (!equals) {
        logger.debug(type + " changed: \n new: " + first + "\n old: " + second);
      }
    }
    return equals;
  }
  
  public boolean entriesEqual(ApplicationXmlEntry other) {
    if (other == null) {
      return false;
    }
    if (!listsEqual(applicationInfo.getRuntimeContextRequirements(), other.applicationInfo.getRuntimeContextRequirements(), "runtimeContextDependencies")) {
      return false;
    } else if (!listsEqual(capacities, other.capacities, "capacities")) {
      return false;
    } else if (!listsEqual(capacityRequirements, other.capacityRequirements, "capacityRequirements")) {
      return false;
    } else if (!listsEqual(filterInstances, other.filterInstances, "filterInstances")) {
      return false;
    } else if (!listsEqual(filters, other.filters, "filters")) {
      return false;
    } else if (!listsEqual(monitoringLevels, other.monitoringLevels, "monitoringLevels")) {
      return false;
    } else if (!listsEqual(ordertypes, other.ordertypes, "ordertypes")) {
      return false;
    } else if (!listsEqual(ordertypesWithOrderContext, other.ordertypesWithOrderContext, "ordertypesWithOrderContext")) {
      return false;
    } else if (!listsEqual(priorities, other.priorities, "priorities")) {
      return false;
    } else if (!listsEqual(sharedLibs, other.sharedLibs, "sharedLibs")) {
      return false;
    } else if (!listsEqual(triggerInstances, other.triggerInstances, "triggerInstances")) {
      return false;
    } else if (!listsEqual(triggers, other.triggers, "triggers")) {
      return false;
    } else if (!listsEqual(xmomEntries, other.xmomEntries, "xmomEntries")) {
      return false;
    } else if (!listsEqual(xmomStorables, other.xmomStorables, "xmomStorables")) {
      return false;
    } else if (!listsEqual(xynaProperties, other.xynaProperties, "xynaProperties")) {
      return false;
    } else if (!listsEqual(orderInputSources, other.orderInputSources, "orderInputSources")) {
      return false;
    } else if (!listsEqual(parameterInheritanceRules, other.parameterInheritanceRules, "parameterInheritanceRules")) {
      return false;
    }
    
    return true;
  }

  public List<FilterXmlEntry> getFilters() {
    return filters;
  }
  
  
  public List<CapacityXmlEntry> getCapacities() {
    return capacities;
  }


  public List<CapacityRequirementXmlEntry> getCapacityRequirements() {
    return capacityRequirements;
  }


  public List<FilterInstanceXmlEntry> getFilterInstances() {
    return filterInstances;
  }


  public List<OrdertypeXmlEntry> getOrdertypes() {
    return ordertypes;
  }


  public List<SharedLibXmlEntry> getSharedLibs() {
    return sharedLibs;
  }


  public List<TriggerXmlEntry> getTriggers() {
    return triggers;
  }


  public List<TriggerInstanceXmlEntry> getTriggerInstances() {
    return triggerInstances;
  }


  public List<XMOMXmlEntry> getXmomEntries() {
    return xmomEntries;
  }


  public List<XynaPropertyXmlEntry> getXynaProperties() {
    return xynaProperties;
  }


  public List<OrderInputSourceXmlEntry> getOrderInputSources() {
    return orderInputSources;
  }

  
  public List<InheritanceRuleXmlEntry> getParameterInheritanceRules() {
    return parameterInheritanceRules;
  }
  
  
  public List<RuntimeContextRequirementXmlEntry> getRuntimeContextRequirements() {
    return applicationInfo.getRuntimeContextRequirements();
  }
  
  public List<XMOMStorableXmlEntry> getXmomStorableEntries() {
    return xmomStorables;
  }
  
  public String getApplicationName() {
    return applicationName;
  }


  public String getVersionName() {
    return versionName;
  }


  public String getXmlVersion() {
    return xmlVersion;
  }

  public String getComment() {
    return comment;
  }

  public String getFactoryVersion() {
    return factoryVersion;
  }
  
  
  public ApplicationInfoEntry getApplicationInfo() {
    return applicationInfo;
  }
  
  public static void main(String[] args) {
    
  }
  
  /**
   * Setzt die factoryVersion auf die aktuelle Version
   * @throws XPRC_VERSION_DETECTION_PROBLEM
   * @throws PersistenceLayerException
   */
  public void setFactoryVersion() throws XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException {
    factoryVersion = Updater.getInstance().getFactoryVersion().getString();
  }
  
  public void setFactoryVersion(String value) throws XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException {
    if (CURRENT_VERSION_IDENTIFIER.equals(value)) {
      setFactoryVersion();
    } else {
      this.factoryVersion = value;
    }
  }
  
  private static final XynaPropertyString applicationXMLHeaderComment = new XynaPropertyString("xfmg.xfctrl.appmgmt.applicationxml.headercomment", "\n * - - - - - - - - - - - - - - - - - - - - - - - - - -\n" + 
      " * Copyright 2023 GIP SmartMercial GmbH, Germany\n" + 
      " *\n" +
      " * Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
      " * you may not use this file except in compliance with the License.\n" +
      " * You may obtain a copy of the License at\n" +
      " *\n" +
      " * http://www.apache.org/licenses/LICENSE-2.0\n" +
      " *\n" +
      " * distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
      " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
      " * See the License for the specific language governing permissions and\n" +
      " * limitations under the License.\n" +
      " * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n" +
      " ").setDefaultDocumentation(DocumentationLanguage.EN, "This is put into application.xmls as header comment.");
  
  
  public Document buildXmlDocument() throws ParserConfigurationException {
      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document doc = builder.newDocument();
      Comment headerComment = doc.createComment(applicationXMLHeaderComment.get().replace("_FACTORY_VERSION_", factoryVersion));
      doc.appendChild(headerComment);
      Element rootElement = doc.createElement(ApplicationXmlHandler.TAG_APPLICATION);
      doc.appendChild(rootElement);
      rootElement.setAttribute(ApplicationXmlHandler.ATTRIBUTE_APPLICATIONNAME, applicationName);
      rootElement.setAttribute(ApplicationXmlHandler.ATTRIBUTE_VERISONNAME, versionName);
      rootElement.setAttribute(ApplicationXmlHandler.ATTRIBUTE_XMLVERSION, ApplicationXmlHandler.XMLVERSION);
      if (comment != null) {
        rootElement.setAttribute(ApplicationXmlHandler.ATTRIBUTE_COMMENT, comment);
      }
      if (factoryVersion != null) {
        rootElement.setAttribute(ApplicationXmlHandler.ATTRIBUTE_FACTORYVERSION, factoryVersion);
      }
      
      Element appInfo = buildApplicationInfoElement(doc);
      if (appInfo != null) {
        rootElement.appendChild(appInfo);
      }
      
      if(!capacities.isEmpty()) {
        Collections.sort(capacities);
        //rootElement.appendChild( buildCapacitiesElement(doc) );
        Element listElement = doc.createElement(ApplicationXmlHandler.TAG_CAPACITIES);
        rootElement.appendChild(listElement);
        for(CapacityXmlEntry entry : capacities) {
          Element entryElement = doc.createElement(ApplicationXmlHandler.TAG_CAPACITY);
          entryElement.setAttribute(ApplicationXmlHandler.ATTRIBUTE_IMPLICIT_DEPENDENCY, String.valueOf(entry.isImplicitDependency()));
          Element element = doc.createElement(ApplicationXmlHandler.TAG_NAME);
          element.appendChild(doc.createTextNode(entry.getName()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_CARDINALITY);
          element.appendChild(doc.createTextNode(Integer.toString(entry.getCardinality())));
          entryElement.appendChild(element);
          if (entry.getState() != null) {
            element = doc.createElement(ApplicationXmlHandler.TAG_CAPACITY_STATE);
            element.appendChild(doc.createTextNode(String.valueOf(entry.getState())));
            entryElement.appendChild(element);
          }
          listElement.appendChild(entryElement);
        }
      }
      
      if(!capacityRequirements.isEmpty()) {
        Collections.sort(capacityRequirements);
        Element listElement = doc.createElement(ApplicationXmlHandler.TAG_CAPACITYREQUIREMENTS);
        rootElement.appendChild(listElement);
        for(CapacityRequirementXmlEntry entry : capacityRequirements) {
          Element entryElement = doc.createElement(ApplicationXmlHandler.TAG_CAPACITYREQUIREMENT);
          Element element = doc.createElement(ApplicationXmlHandler.TAG_ORDERTYPENAME);
          element.appendChild(doc.createTextNode(entry.getOrdertype()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_CAPACITYNAME);
          element.appendChild(doc.createTextNode(entry.getCapacityName()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_CARDINALITY);
          element.appendChild(doc.createTextNode(Integer.toString(entry.getCardinality())));
          entryElement.appendChild(element);
          listElement.appendChild(entryElement);
        }
      }
      
      if(!filters.isEmpty()) {
        Collections.sort(filters);
        Element listElement  = doc.createElement(ApplicationXmlHandler.TAG_FILTERS);
        rootElement.appendChild(listElement);
        for(FilterXmlEntry entry : filters) {
          Element entryElement = doc.createElement(ApplicationXmlHandler.TAG_FILTER);
          entryElement.setAttribute(ApplicationXmlHandler.ATTRIBUTE_IMPLICIT_DEPENDENCY, String.valueOf(entry.isImplicitDependency()));
          Element element = doc.createElement(ApplicationXmlHandler.TAG_NAME);
          element.appendChild(doc.createTextNode(entry.getName()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_JARFILES);
          element.appendChild(doc.createTextNode(entry.getJarFiles()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_FQFILTERCLASSNAME);
          element.appendChild(doc.createTextNode(entry.getFqFilterClassname()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_TRIGGERNAME);
          element.appendChild(doc.createTextNode(entry.getTriggerName()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_SHAREDLIBS);
          element.appendChild(doc.createTextNode(entry.getSharedLibs()));
          entryElement.appendChild(element);
          listElement.appendChild(entryElement);
        }
      }
      
      if(!filterInstances.isEmpty()) {
        Collections.sort(filterInstances);
        Element listElement  = doc.createElement(ApplicationXmlHandler.TAG_FILTERINSTANCES);
        rootElement.appendChild(listElement);
        for(FilterInstanceXmlEntry entry : filterInstances) {
          Element entryElement = doc.createElement(ApplicationXmlHandler.TAG_FILTERINSTANCE);
          entryElement.setAttribute(ApplicationXmlHandler.ATTRIBUTE_IMPLICIT_DEPENDENCY, String.valueOf(entry.isImplicitDependency()));
          Element element = doc.createElement(ApplicationXmlHandler.TAG_NAME);
          element.appendChild(doc.createTextNode(entry.getName()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_FILTERNAME);
          element.appendChild(doc.createTextNode(entry.getFilterName()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_TRIGGERINSTANCENAME);
          element.appendChild(doc.createTextNode(entry.getTriggerInstanceName()));
          entryElement.appendChild(element);
          if (entry.getDescription() != null) {
            element = doc.createElement(ApplicationXmlHandler.TAG_DESCRIPTION);
            element.appendChild(doc.createTextNode(entry.getDescription()));
            entryElement.appendChild(element);
          }
          if( entry.getConfigurationParameter() != null ) {
            for( String cp : entry.getConfigurationParameter() ) {
              element = doc.createElement(ApplicationXmlHandler.TAG_FILTER_CONFIG);
              element.appendChild(doc.createTextNode(cp));
              entryElement.appendChild(element);
            }
          }
          listElement.appendChild(entryElement);
        }
      }
      
      if(!ordertypes.isEmpty()) {
        Collections.sort(ordertypes);
        Element listElement  = doc.createElement(ApplicationXmlHandler.TAG_ORDERTYPES);
        rootElement.appendChild(listElement);
        for(OrdertypeXmlEntry entry : ordertypes) {
          Element entryElement = doc.createElement(ApplicationXmlHandler.TAG_ORDERTYPE);
          entryElement.setAttribute(ApplicationXmlHandler.ATTRIBUTE_IMPLICIT_DEPENDENCY, String.valueOf(entry.isImplicitDependency()));
          Element element;
          if (entry.getPlanning() != null) {
            element = doc.createElement(ApplicationXmlHandler.TAG_PLANNING);
            element.appendChild(doc.createTextNode(entry.getPlanning()));
            entryElement.appendChild(element);
          }
          if (entry.getExecution() != null) {
            element = doc.createElement(ApplicationXmlHandler.TAG_EXECUTION);
            element.appendChild(doc.createTextNode(entry.getExecution()));
            entryElement.appendChild(element);
          }
          if (entry.getCleanup() != null) {
            element = doc.createElement(ApplicationXmlHandler.TAG_CLEANUP);
            element.appendChild(doc.createTextNode(entry.getCleanup()));
            entryElement.appendChild(element);
          }
          element = doc.createElement(ApplicationXmlHandler.TAG_DESTINATIONKEY);
          element.appendChild(doc.createTextNode(entry.getDestinationKey()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_ORDERCONTEXTMAPPING);
          element.appendChild(doc.createTextNode(Boolean.toString(entry.hasOrdercontextMapping())));
          entryElement.appendChild(element);
          listElement.appendChild(entryElement);
        }
      }
      
      if(!sharedLibs.isEmpty()) {
        Collections.sort(sharedLibs);
        Element listElement  = doc.createElement(ApplicationXmlHandler.TAG_SHAREDLIBS);
        rootElement.appendChild(listElement);
        for(SharedLibXmlEntry entry : sharedLibs) {
          Element entryElement = doc.createElement(ApplicationXmlHandler.TAG_SHAREDLIB);
          entryElement.setAttribute(ApplicationXmlHandler.ATTRIBUTE_IMPLICIT_DEPENDENCY, String.valueOf(entry.isImplicitDependency()));
          Element element = doc.createElement(ApplicationXmlHandler.TAG_SHAREDLIB);
          element.appendChild(doc.createTextNode(entry.getSharedLibName()));
          entryElement.appendChild(element);
          listElement.appendChild(entryElement);
        }
      }
      
      if(!triggers.isEmpty()) {
        Collections.sort(triggers);
        Element listElement  = doc.createElement(ApplicationXmlHandler.TAG_TRIGGERS);
        rootElement.appendChild(listElement);
        for(TriggerXmlEntry entry : triggers) {
          Element entryElement = doc.createElement(ApplicationXmlHandler.TAG_TRIGGER);
          entryElement.setAttribute(ApplicationXmlHandler.ATTRIBUTE_IMPLICIT_DEPENDENCY, String.valueOf(entry.isImplicitDependency()));
          Element element = doc.createElement(ApplicationXmlHandler.TAG_NAME);
          element.appendChild(doc.createTextNode(entry.getName()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_JARFILES);
          element.appendChild(doc.createTextNode(entry.getJarFiles()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_FQTRIGGERCLASSNAME);
          element.appendChild(doc.createTextNode(entry.getFqTriggerClassname()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_SHAREDLIBS);
          element.appendChild(doc.createTextNode(entry.getSharedLibs()));
          entryElement.appendChild(element);
          listElement.appendChild(entryElement);
        }
      }
      
      if(!triggerInstances.isEmpty()) {
        Collections.sort(triggerInstances);
        Element listElement  = doc.createElement(ApplicationXmlHandler.TAG_TRIGGERINSTANCES);
        rootElement.appendChild(listElement);
        for(TriggerInstanceXmlEntry entry : triggerInstances) {
          Element entryElement = doc.createElement(ApplicationXmlHandler.TAG_TRIGGERINSTANCE);
          entryElement.setAttribute(ApplicationXmlHandler.ATTRIBUTE_IMPLICIT_DEPENDENCY, String.valueOf(entry.isImplicitDependency()));
          Element element = doc.createElement(ApplicationXmlHandler.TAG_NAME);
          element.appendChild(doc.createTextNode(entry.getName()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_TRIGGERNAME);
          element.appendChild(doc.createTextNode(entry.getTriggerName()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_STARTPARAMETER);
          element.appendChild(doc.createTextNode(entry.getStartParameter()));
          entryElement.appendChild(element);
          if (entry.getMaxEvents() != null && entry.getRejectRequestsAfterMaxReceives() != null) {
            element = doc.createElement(ApplicationXmlHandler.TAG_TRIGGER_MAXRECEIVES);
            element.appendChild(doc.createTextNode(String.valueOf(entry.getMaxEvents())));
            entryElement.appendChild(element);
            element = doc.createElement(ApplicationXmlHandler.TAG_TRIGGER_REJECTAFTERMAXRECEIVES);
            element.appendChild(doc.createTextNode(String.valueOf(entry.getRejectRequestsAfterMaxReceives())));
            entryElement.appendChild(element);
          }
          listElement.appendChild(entryElement);
        }
      }
      
      if(!xmomEntries.isEmpty()) {
        Collections.sort(xmomEntries);
        Element listElement  = doc.createElement(ApplicationXmlHandler.TAG_XMOMENTRIES);
        rootElement.appendChild(listElement);
        for(XMOMXmlEntry entry : xmomEntries) {
          Element entryElement = doc.createElement(ApplicationXmlHandler.TAG_XMOMENTRY);
          entryElement.setAttribute(ApplicationXmlHandler.ATTRIBUTE_IMPLICIT_DEPENDENCY, String.valueOf(entry.isImplicitDependency()));
          Element element = doc.createElement(ApplicationXmlHandler.TAG_FQNAME);
          element.appendChild(doc.createTextNode(entry.getFqName()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_TYPE);
          element.appendChild(doc.createTextNode(entry.getType()));
          entryElement.appendChild(element);
          listElement.appendChild(entryElement);
        }
      }
      
      if(!xynaProperties.isEmpty()) {
        Collections.sort(xynaProperties);
        Element listElement  = doc.createElement(ApplicationXmlHandler.TAG_XYNAPROPERTIES);
        rootElement.appendChild(listElement);
        for(XynaPropertyXmlEntry entry : xynaProperties) {
          Element entryElement = doc.createElement(ApplicationXmlHandler.TAG_XYNAPROPERTY);
          entryElement.setAttribute(ApplicationXmlHandler.ATTRIBUTE_IMPLICIT_DEPENDENCY, String.valueOf(entry.isImplicitDependency()));
          Element element = doc.createElement(ApplicationXmlHandler.TAG_NAME);
          element.appendChild(doc.createTextNode(entry.getName()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_VALUE);
          element.appendChild(doc.createTextNode(entry.getValue()));
          entryElement.appendChild(element);
          listElement.appendChild(entryElement);
        }
      }
      
      if(!monitoringLevels.isEmpty()) {
        Collections.sort(monitoringLevels);
        Element listElement  = doc.createElement(ApplicationXmlHandler.TAG_MONITORINGLEVELS);
        rootElement.appendChild(listElement);
        for(MonitoringLevelXmlEntry entry : monitoringLevels) {
          Element entryElement = doc.createElement(ApplicationXmlHandler.TAG_MONITORINGLEVEL);
          Element element = doc.createElement(ApplicationXmlHandler.TAG_ORDERTYPENAME);
          element.appendChild(doc.createTextNode(entry.getOrdertype()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_VALUE);
          element.appendChild(doc.createTextNode(entry.getMonitoringLevel().toString()));
          entryElement.appendChild(element);
          listElement.appendChild(entryElement);
        }
      }
      
      if(!priorities.isEmpty()) {
        Collections.sort(priorities);
        Element listElement  = doc.createElement(ApplicationXmlHandler.TAG_PRIORITIES);
        rootElement.appendChild(listElement);
        for(PriorityXmlEntry entry : priorities) {
          Element entryElement = doc.createElement(ApplicationXmlHandler.TAG_PRIORITY);
          Element element = doc.createElement(ApplicationXmlHandler.TAG_ORDERTYPENAME);
          element.appendChild(doc.createTextNode(entry.getOrdertype()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_VALUE);
          element.appendChild(doc.createTextNode(entry.getPriority().toString()));
          entryElement.appendChild(element);
          listElement.appendChild(entryElement);
        }
      }
      
      if (!xmomStorables.isEmpty()) {
        Collections.sort(xmomStorables);
        Element listElement  = doc.createElement(ApplicationXmlHandler.TAG_XMOMSTORABLES);
        rootElement.appendChild(listElement);
        for(XMOMStorableXmlEntry entry : xmomStorables) {
          Element entryElement = doc.createElement(ApplicationXmlHandler.TAG_XMOMSTORABLE);
          Element element = doc.createElement(ApplicationXmlHandler.TAG_XMOMSTORABLE_XMLNAME);
          element.appendChild(doc.createTextNode(entry.getXmlName()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_XMOMSTORABLE_PATH);
          element.appendChild(doc.createTextNode(entry.getPath()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_XMOMSTORABLE_ODSNAME);
          element.appendChild(doc.createTextNode(entry.getOdsName()));
          entryElement.appendChild(element);
          element = doc.createElement(ApplicationXmlHandler.TAG_XMOMSTORABLE_FQPATH);
          element.appendChild(doc.createTextNode(entry.getFqPath()));
          entryElement.appendChild(element);
          if (entry.getColName() != null) {
            element = doc.createElement(ApplicationXmlHandler.TAG_XMOMSTORABLE_COLUMNNAME);
            element.appendChild(doc.createTextNode(entry.getColName()));
          }
          entryElement.appendChild(element);

          listElement.appendChild(entryElement);
        }
      }
      
      if (!orderInputSources.isEmpty()) {
        Collections.sort(orderInputSources);
        Element listElement  = doc.createElement(ApplicationXmlHandler.TAG_ORDERINPUTSOURCES);
        rootElement.appendChild(listElement);
        for(OrderInputSourceXmlEntry entry : orderInputSources) {
          Element entryElement = doc.createElement(ApplicationXmlHandler.TAG_ORDERINPUTSOURCE);
          entryElement.setAttribute(ApplicationXmlHandler.ATTRIBUTE_IMPLICIT_DEPENDENCY, String.valueOf(entry.isImplicitDependency()));
          Element element = doc.createElement(ApplicationXmlHandler.TAG_NAME);
          element.appendChild(doc.createTextNode(entry.getName()));
          entryElement.appendChild(element);
          
          element = doc.createElement(ApplicationXmlHandler.TAG_TYPE);
          element.appendChild(doc.createTextNode(entry.getType()));
          entryElement.appendChild(element);
          
          element = doc.createElement(ApplicationXmlHandler.TAG_ORDERTYPE);
          element.appendChild(doc.createTextNode(entry.getOrderType()));
          entryElement.appendChild(element);
          
          if (entry.getParameter() != null) {
            for (Entry<String, String> param : entry.getParameter().entrySet()) {
              element = doc.createElement(ApplicationXmlHandler.TAG_ORDERINPUTSOURCE_PARAMETER);
              Element key = doc.createElement(ApplicationXmlHandler.TAG_KEY);
              key.appendChild(doc.createTextNode(param.getKey()));
              element.appendChild(key);
              Element value = doc.createElement(ApplicationXmlHandler.TAG_VALUE);
              value.appendChild(doc.createTextNode(param.getValue()));
              element.appendChild(value);
              entryElement.appendChild(element);
            }
          }
          
          if (entry.getDocumentation() != null) {
            element = doc.createElement(ApplicationXmlHandler.TAG_DOCUMENTATION);
            element.appendChild(doc.createTextNode(entry.getDocumentation()));
            entryElement.appendChild(element);
          }
          
          listElement.appendChild(entryElement);
        }
      }

      if (!parameterInheritanceRules.isEmpty()) {
        Collections.sort(parameterInheritanceRules);
        Element listElement  = doc.createElement(ApplicationXmlHandler.TAG_INHERITANCERULES);
        rootElement.appendChild(listElement);
        for(InheritanceRuleXmlEntry entry : parameterInheritanceRules) {
          Element entryElement = doc.createElement(ApplicationXmlHandler.TAG_INHERITANCERULE);
          
          Element element = doc.createElement(ApplicationXmlHandler.TAG_PARAMETERTYPE);
          element.appendChild(doc.createTextNode(entry.getParameterType().toString()));
          entryElement.appendChild(element);
          
          element = doc.createElement(ApplicationXmlHandler.TAG_ORDERTYPENAME);
          element.appendChild(doc.createTextNode(entry.getOrderType()));
          entryElement.appendChild(element);
          
          element = doc.createElement(ApplicationXmlHandler.TAG_CHILDFILTER);
          element.appendChild(doc.createTextNode(entry.getChildFilter()));
          entryElement.appendChild(element);

          element = doc.createElement(ApplicationXmlHandler.TAG_VALUE);
          element.appendChild(doc.createTextNode(entry.getValue()));
          entryElement.appendChild(element);

          element = doc.createElement(ApplicationXmlHandler.TAG_PRECEDENCE);
          element.appendChild(doc.createTextNode(String.valueOf(entry.getPrecedence())));
          entryElement.appendChild(element);
          
          listElement.appendChild(entryElement);
        }
      }

      return doc;
  }
  
  private Element buildApplicationInfoElement(Document doc) {
    Element aiElement = doc.createElement(ApplicationXmlHandler.TAG_APPLICATION_INFO);
    boolean empty = true;
    if (applicationInfo.getDescription() != null && !applicationInfo.getDescription().isEmpty()) {
      empty = false;
      for( DocumentationLanguage lang : DocumentationLanguage.values() ) {
        String desc = applicationInfo.getDescription().get(lang);
        if( desc != null ) {
          Element element = doc.createElement(ApplicationXmlHandler.TAG_AI_DESCRIPTION);
          element.appendChild(doc.createTextNode(desc));
          element.setAttribute(ApplicationXmlHandler.ATTRIBUTE_AI_D_LANG, lang.name());
          aiElement.appendChild(element);
        }
      }
    }
    if (applicationInfo.getBuildDate() != null) {
      empty = false;
      Element element = doc.createElement(ApplicationXmlHandler.TAG_AI_BUILDDATE);
      element.appendChild(doc.createTextNode(applicationInfo.getBuildDate()));
      aiElement.appendChild(element);
    }
    if (applicationInfo.isRemoteStub()) {
      empty = false;
      Element element = doc.createElement(ApplicationXmlHandler.TAG_AI_ISREMOTESTUB);
      element.appendChild(doc.createTextNode("true"));
      aiElement.appendChild(element);
    }
    
    if (!applicationInfo.getRuntimeContextRequirements().isEmpty()) {
      empty = false;
      Collections.sort(applicationInfo.getRuntimeContextRequirements());
      Element listElement  = doc.createElement(ApplicationXmlHandler.TAG_RUNTIMECONTEXT_REQUIREMENTS);
      aiElement.appendChild(listElement);
      for(RuntimeContextRequirementXmlEntry entry : applicationInfo.getRuntimeContextRequirements() ) {
        Element entryElement = doc.createElement(ApplicationXmlHandler.TAG_RUNTIMECONTEXT_REQUIREMENT);
        
        if (entry.getApplication() != null) {
          Element element = doc.createElement(ApplicationXmlHandler.TAG_APPLICATIONNAME);
          element.appendChild(doc.createTextNode(entry.getApplication()));
          entryElement.appendChild(element);
        }
        
        if (entry.getVersion() != null) {
          Element element = doc.createElement(ApplicationXmlHandler.TAG_VERSIONNAME);
          element.appendChild(doc.createTextNode(entry.getVersion()));
          entryElement.appendChild(element);
        }
        
        if (entry.getWorkspace() != null) {
          Element element = doc.createElement(ApplicationXmlHandler.TAG_WORKSPACENAME);
          element.appendChild(doc.createTextNode(entry.getWorkspace()));
          entryElement.appendChild(element);
        }
        
        listElement.appendChild(entryElement);
      }
    }
    if (empty) {
      return null;
    }
    return aiElement;
  }

  protected static class ApplicationInfoEntry {
    private EnumMap<DocumentationLanguage,String> description = new EnumMap<DocumentationLanguage,String>(DocumentationLanguage.class);
    private String buildDate;
    private List<RuntimeContextRequirementXmlEntry> runtimeContextRequirements = new ArrayList<ApplicationXmlEntry.RuntimeContextRequirementXmlEntry>();
    private boolean isRemoteStub;
    
    public void setBuildDate(String buildDate) {
      this.buildDate = buildDate;
    }
    
    public void setIsRemoteStub(boolean isRemoteStub) {
      this.isRemoteStub = isRemoteStub;
    }
   
    public void setDescription(DocumentationLanguage lang, String description) {
      this.description.put(lang,description);
    }
    
    public Map<DocumentationLanguage, String> getDescription() {
      return description;
    }
    
    public String getBuildDate() {
      return buildDate;
    }
    
    public boolean isRemoteStub() {
      return isRemoteStub;
    }

    public void setRuntimeContextRequirements(List<RuntimeContextRequirementXmlEntry> runtimeContextRequirements) {
      this.runtimeContextRequirements = runtimeContextRequirements;
    }

    //explizit keine kopie rausgeben
    public List<RuntimeContextRequirementXmlEntry> getRuntimeContextRequirements() {
      return runtimeContextRequirements;
    }
  }
  
  protected static abstract class XmlEntry implements Comparable<XmlEntry> {
    
    private final boolean isImplicitDependency;
    
    private XmlEntry(boolean isImplicitDependency) {
      this.isImplicitDependency = isImplicitDependency;
    }
    
    public boolean isImplicitDependency() {
      return isImplicitDependency;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (isImplicitDependency ? 1231 : 1237);
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
      XmlEntry other = (XmlEntry) obj;
      if (isImplicitDependency != other.isImplicitDependency)
        return false;
      return true;
    }
    
    
  }
  
  protected static class PriorityXmlEntry implements Comparable<PriorityXmlEntry> {
    
    protected String ordertype;
    protected Integer priority;
    
    
    protected PriorityXmlEntry(String ordertype, Integer priority) {
      this.ordertype = ordertype;
      this.priority = priority;
    }
    
    protected PriorityXmlEntry() {
    }

    public String getOrdertype() {
      return ordertype;
    }

    
    public Integer getPriority() {
      return priority;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((ordertype == null) ? 0 : ordertype.hashCode());
      result = prime * result + ((priority == null) ? 0 : priority.hashCode());
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
      PriorityXmlEntry other = (PriorityXmlEntry) obj;
      if (ordertype == null) {
        if (other.ordertype != null)
          return false;
      }
      else if (!ordertype.equals(other.ordertype))
        return false;
      if (priority == null) {
        if (other.priority != null)
          return false;
      }
      else if (!priority.equals(other.priority))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "[ordertype=" + ordertype + ", priority=" + priority + "]";
    }

    public int compareTo(PriorityXmlEntry o) {
      return ordertype.compareTo(o.ordertype);
    }
  }
  
  protected static class MonitoringLevelXmlEntry implements Comparable<MonitoringLevelXmlEntry> {
    
    protected String ordertype;
    protected Integer monitoringLevel;
   
    protected MonitoringLevelXmlEntry(String ordertype, Integer monitoringLevel) {
      this.ordertype = ordertype;
      this.monitoringLevel = monitoringLevel;
    }
    
    public MonitoringLevelXmlEntry() {
    }

    public String getOrdertype() {
      return ordertype;
    }

    
    public Integer getMonitoringLevel() {
      return monitoringLevel;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((monitoringLevel == null) ? 0 : monitoringLevel.hashCode());
      result = prime * result + ((ordertype == null) ? 0 : ordertype.hashCode());
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
      MonitoringLevelXmlEntry other = (MonitoringLevelXmlEntry) obj;
      if (monitoringLevel == null) {
        if (other.monitoringLevel != null)
          return false;
      }
      else if (!monitoringLevel.equals(other.monitoringLevel))
        return false;
      if (ordertype == null) {
        if (other.ordertype != null)
          return false;
      }
      else if (!ordertype.equals(other.ordertype))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "[ordertype=" + ordertype + ", monitoringLevel=" + monitoringLevel + "]";
    }

    public int compareTo(MonitoringLevelXmlEntry o) {
      return ordertype.compareTo(o.ordertype);
    }
  }
  
  protected static class FilterXmlEntry extends XmlEntry {

    protected String name;
    protected String jarFiles;
    protected String fqFilterClassname;
    protected String triggerName;
    protected String sharedLibs;

    public FilterXmlEntry(boolean isImplicitDependency) {
      super(isImplicitDependency);
    }
    
    protected FilterXmlEntry(boolean isImplicitDependency, String name, String jarFiles, String fqFilterClassname, String triggerName,
                             String sharedLibs) {
      super(isImplicitDependency);
      this.name = name;
      this.jarFiles = jarFiles;
      this.fqFilterClassname = fqFilterClassname;
      this.triggerName = triggerName;
      this.sharedLibs = sharedLibs;
    }

    public String getName() {
      return name;
    }


    public String getJarFiles() {
      return jarFiles;
    }


    public String getFqFilterClassname() {
      return fqFilterClassname;
    }


    public String getTriggerName() {
      return triggerName;
    }


    public String getSharedLibs() {
      return sharedLibs;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((fqFilterClassname == null) ? 0 : fqFilterClassname.hashCode());
      result = prime * result + ((jarFiles == null) ? 0 : jarFiles.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((sharedLibs == null) ? 0 : sharedLibs.hashCode());
      result = prime * result + ((triggerName == null) ? 0 : triggerName.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (getClass() != obj.getClass())
        return false;
      FilterXmlEntry other = (FilterXmlEntry) obj;
      if (fqFilterClassname == null) {
        if (other.fqFilterClassname != null)
          return false;
      }
      else if (!fqFilterClassname.equals(other.fqFilterClassname))
        return false;
      if (jarFiles == null) {
        if (other.jarFiles != null)
          return false;
      }
      else if (!jarFiles.equals(other.jarFiles))
        return false;
      if (name == null) {
        if (other.name != null)
          return false;
      }
      else if (!name.equals(other.name))
        return false;
      if (sharedLibs == null) {
        if (other.sharedLibs != null)
          return false;
      }
      else if (!sharedLibs.equals(other.sharedLibs))
        return false;
      if (triggerName == null) {
        if (other.triggerName != null)
          return false;
      }
      else if (!triggerName.equals(other.triggerName))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "[name=" + name + ", jarFiles=" + jarFiles + ", fqFilterClassname=" + fqFilterClassname + ", triggerName=" + triggerName + ", sharedLibs=" + sharedLibs + "]";
    }

    public int compareTo(XmlEntry o) {
      FilterXmlEntry f = (FilterXmlEntry) o;
      return getName().compareTo(f.getName());
    }
  }

  protected static class XynaPropertyXmlEntry extends XmlEntry  {

    protected String name;
    protected String value;
    
    public XynaPropertyXmlEntry(boolean isImplicitDependency) {
      super(isImplicitDependency);
    }
    
    protected XynaPropertyXmlEntry(boolean isImplicitDependency, String name, String value) {
      super(isImplicitDependency);
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return name;
    }


    public String getValue() {
      return value;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((value == null) ? 0 : value.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (getClass() != obj.getClass())
        return false;
      XynaPropertyXmlEntry other = (XynaPropertyXmlEntry) obj;
      if (name == null) {
        if (other.name != null)
          return false;
      }
      else if (!name.equals(other.name))
        return false;
      if (value == null) {
        if (other.value != null)
          return false;
      }
      else if (!value.equals(other.value))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "[name=" + name + ", value=" + value + "]";
    }

    public int compareTo(XmlEntry o) {
      XynaPropertyXmlEntry x = (XynaPropertyXmlEntry)o;
      return name.compareTo(x.name);
    }
  }

  public static class XMOMXmlEntry extends XmlEntry {

    protected String fqName;
    protected String type;

    public XMOMXmlEntry(boolean isImplicitDependency) {
      super(isImplicitDependency);
    }
    
    public XMOMXmlEntry(boolean isImplicitDependency, String fqName, String type) {
      super(isImplicitDependency);
      this.fqName = fqName;
      this.type = type;
    }
    
    public String getFqName() {
      return fqName;
    }


    public String getType() {
      return type;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((fqName == null) ? 0 : fqName.hashCode());
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (getClass() != obj.getClass())
        return false;
      XMOMXmlEntry other = (XMOMXmlEntry) obj;
      if (fqName == null) {
        if (other.fqName != null)
          return false;
      }
      else if (!fqName.equals(other.fqName))
        return false;
      if (type == null) {
        if (other.type != null)
          return false;
      }
      else if (!type.equals(other.type))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "[fqName=" + fqName + ", type=" + type + "]";
    }

    public int compareTo(XmlEntry o) {
      XMOMXmlEntry x = (XMOMXmlEntry) o;
      return fqName.compareTo(x.fqName);
    }
  }

  protected static class TriggerInstanceXmlEntry extends XmlEntry {

    protected String name;
    protected String triggerName;
    protected String startParameter;
    protected Long maxEvents;
    protected Boolean rejectRequestsAfterMaxReceives;
    
    public TriggerInstanceXmlEntry(boolean isImplicitDependency) {
      super(isImplicitDependency);
    }
    
    protected TriggerInstanceXmlEntry(boolean isImplicitDependency, String name, String triggerName, String startParameter) {
      super(isImplicitDependency);
      this.name = name;
      this.triggerName = triggerName;
      this.startParameter = startParameter;
    }
    
    protected TriggerInstanceXmlEntry(boolean isImplicitDependency, String name, String triggerName, String startParameter, Long maxEvents, Boolean rejectRequestsAfterMaxReceives) {
      this(isImplicitDependency, name, triggerName, startParameter);
      this.maxEvents = maxEvents;
      this.rejectRequestsAfterMaxReceives = rejectRequestsAfterMaxReceives;
    }
    
    public String getName() {
      return name;
    }


    public String getTriggerName() {
      return triggerName;
    }


    public String getStartParameter() {
      return startParameter;
    }
    
    public Long getMaxEvents() {
      return maxEvents;
    }
    
    public Boolean getRejectRequestsAfterMaxReceives() {
      return rejectRequestsAfterMaxReceives;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((maxEvents == null) ? 0 : maxEvents.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((rejectRequestsAfterMaxReceives == null) ? 0 : rejectRequestsAfterMaxReceives
                      .hashCode());
      result = prime * result + ((startParameter == null) ? 0 : startParameter.hashCode());
      result = prime * result + ((triggerName == null) ? 0 : triggerName.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (getClass() != obj.getClass())
        return false;
      TriggerInstanceXmlEntry other = (TriggerInstanceXmlEntry) obj;
      if (maxEvents == null) {
        if (other.maxEvents != null)
          return false;
      }
      else if (!maxEvents.equals(other.maxEvents))
        return false;
      if (name == null) {
        if (other.name != null)
          return false;
      }
      else if (!name.equals(other.name))
        return false;
      if (rejectRequestsAfterMaxReceives == null) {
        if (other.rejectRequestsAfterMaxReceives != null)
          return false;
      }
      else if (!rejectRequestsAfterMaxReceives.equals(other.rejectRequestsAfterMaxReceives))
        return false;
      if (startParameter == null) {
        if (other.startParameter != null)
          return false;
      }
      else if (!startParameter.equals(other.startParameter))
        return false;
      if (triggerName == null) {
        if (other.triggerName != null)
          return false;
      }
      else if (!triggerName.equals(other.triggerName))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "[name=" + name + ", triggerName=" + triggerName + ", startParameter=" + startParameter + ", maxEvents=" + maxEvents + ", rejectRequestsAfterMaxReceives=" + rejectRequestsAfterMaxReceives + "]";
    }

    public int compareTo(XmlEntry o) {
      TriggerInstanceXmlEntry t = (TriggerInstanceXmlEntry) o;
      return name.compareTo(t.name);
    }
  }

  protected static class TriggerXmlEntry extends XmlEntry {

    protected String name;
    protected String jarFiles;
    protected String fqTriggerClassname;
    protected String sharedLibs;
    
    public TriggerXmlEntry(boolean isImplicitDependency) {
      super(isImplicitDependency);
    }
    
    protected TriggerXmlEntry(boolean isImplicitDependency, String name, String jarFiles, String fqTriggerClassname, String sharedLibs) {
      super(isImplicitDependency);
      this.name = name;
      this.jarFiles = jarFiles;
      this.fqTriggerClassname = fqTriggerClassname;
      this.sharedLibs = sharedLibs;        
    }
    
    public String getName() {
      return name;
    }


    public String getJarFiles() {
      return jarFiles;
    }


    public String getFqTriggerClassname() {
      return fqTriggerClassname;
    }


    public String getSharedLibs() {
      return sharedLibs;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((fqTriggerClassname == null) ? 0 : fqTriggerClassname.hashCode());
      result = prime * result + ((jarFiles == null) ? 0 : jarFiles.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((sharedLibs == null) ? 0 : sharedLibs.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (getClass() != obj.getClass())
        return false;
      TriggerXmlEntry other = (TriggerXmlEntry) obj;
      if (fqTriggerClassname == null) {
        if (other.fqTriggerClassname != null)
          return false;
      }
      else if (!fqTriggerClassname.equals(other.fqTriggerClassname))
        return false;
      if (jarFiles == null) {
        if (other.jarFiles != null)
          return false;
      }
      else if (!jarFiles.equals(other.jarFiles))
        return false;
      if (name == null) {
        if (other.name != null)
          return false;
      }
      else if (!name.equals(other.name))
        return false;
      if (sharedLibs == null) {
        if (other.sharedLibs != null)
          return false;
      }
      else if (!sharedLibs.equals(other.sharedLibs))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "[name=" + name + ", jarFiles=" + jarFiles + ", fqTriggerClassname=" + fqTriggerClassname + ", sharedLibs=" + sharedLibs + "]";
    }

    public int compareTo(XmlEntry o) {
      TriggerXmlEntry t = (TriggerXmlEntry) o;
      return name.compareTo(t.name);
    }
  }
  
  protected static class SharedLibXmlEntry extends XmlEntry {
    
    protected String sharedLibName;
    
    public SharedLibXmlEntry(boolean isImplicitDependency) {
      super(isImplicitDependency);
    }

    public SharedLibXmlEntry(boolean isImplicitDependency, String sharedLibName) {
      super(isImplicitDependency);
      this.sharedLibName = sharedLibName;
    }
    
    public String getSharedLibName() {
      return sharedLibName;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((sharedLibName == null) ? 0 : sharedLibName.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (getClass() != obj.getClass())
        return false;
      SharedLibXmlEntry other = (SharedLibXmlEntry) obj;
      if (sharedLibName == null) {
        if (other.sharedLibName != null)
          return false;
      }
      else if (!sharedLibName.equals(other.sharedLibName))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "[sharedLibName=" + sharedLibName + "]";
    }

    public int compareTo(XmlEntry o) {
      SharedLibXmlEntry s = (SharedLibXmlEntry) o;
      return sharedLibName.compareTo(s.sharedLibName);
    }
  }

  protected static class OrdertypeXmlEntry extends XmlEntry {

    protected String planning;
    protected String execution;
    protected String cleanup;
    protected String destinationKey;
    protected boolean ordercontextMapping; 
    
    
    public OrdertypeXmlEntry(boolean isImplicitDependency) {
      super(isImplicitDependency);
    }
    
    public OrdertypeXmlEntry(boolean isImplicitDependency, String planning, String execution, String cleanup, String destinationKey, boolean ordercontextMapping) {
      super(isImplicitDependency);
      this.planning = planning;
      this.execution = execution;
      this.cleanup = cleanup;
      this.destinationKey = destinationKey;
      this.ordercontextMapping = ordercontextMapping;
    }
    
    
    public String getPlanning() {
      return planning;
    }


    public String getExecution() {
      return execution;
    }


    public String getCleanup() {
      return cleanup;
    }


    public String getDestinationKey() {
      return destinationKey;
    }
    
    public boolean hasOrdercontextMapping() {
      return ordercontextMapping;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((cleanup == null) ? 0 : cleanup.hashCode());
      result = prime * result + ((destinationKey == null) ? 0 : destinationKey.hashCode());
      result = prime * result + ((execution == null) ? 0 : execution.hashCode());
      result = prime * result + (ordercontextMapping ? 1231 : 1237);
      result = prime * result + ((planning == null) ? 0 : planning.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (getClass() != obj.getClass())
        return false;
      OrdertypeXmlEntry other = (OrdertypeXmlEntry) obj;
      if (cleanup == null) {
        if (other.cleanup != null)
          return false;
      }
      else if (!cleanup.equals(other.cleanup))
        return false;
      if (destinationKey == null) {
        if (other.destinationKey != null)
          return false;
      }
      else if (!destinationKey.equals(other.destinationKey))
        return false;
      if (execution == null) {
        if (other.execution != null)
          return false;
      }
      else if (!execution.equals(other.execution))
        return false;
      if (ordercontextMapping != other.ordercontextMapping)
        return false;
      if (planning == null) {
        if (other.planning != null)
          return false;
      }
      else if (!planning.equals(other.planning))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "[planning=" + planning + ", execution=" + execution + ", cleanup=" + cleanup + ", destinationKey=" + destinationKey + ", ordercontextMapping=" + ordercontextMapping + "]";
    }

    public int compareTo(XmlEntry o) {
      OrdertypeXmlEntry t = (OrdertypeXmlEntry) o;
      return destinationKey.compareTo(t.destinationKey);
    }
  }

  protected static class FilterInstanceXmlEntry extends XmlEntry {

    protected String name;
    protected String filterName;
    protected String triggerInstanceName;
    protected List<String> configurationParameter;
    protected String description;
    
    public FilterInstanceXmlEntry(boolean isImplicitDependency) {
      super(isImplicitDependency);
    }
    
    protected FilterInstanceXmlEntry(boolean isImplicitDependency, String name, 
        String filterName, String triggerInstanceName, 
        List<String> configurationParameter, String description) {
      super(isImplicitDependency);
      this.name = name;
      this.filterName = filterName;
      this.triggerInstanceName = triggerInstanceName;
      this.configurationParameter = configurationParameter;
      this.description = description;
    }

    public String getName() {
      return name;
    }


    public String getFilterName() {
      return filterName;
    }


    public String getTriggerInstanceName() {
      return triggerInstanceName;
    }


    public List<String> getConfigurationParameter() {
      return configurationParameter;
    }

    public String getDescription() {
      return description;
    }
    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((filterName == null) ? 0 : filterName.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((triggerInstanceName == null) ? 0 : triggerInstanceName.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (getClass() != obj.getClass())
        return false;
      FilterInstanceXmlEntry other = (FilterInstanceXmlEntry) obj;
      if (filterName == null) {
        if (other.filterName != null)
          return false;
      }
      else if (!filterName.equals(other.filterName))
        return false;
      if (name == null) {
        if (other.name != null)
          return false;
      }
      else if (!name.equals(other.name))
        return false;
      if (triggerInstanceName == null) {
        if (other.triggerInstanceName != null)
          return false;
      }
      else if (!triggerInstanceName.equals(other.triggerInstanceName))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "[name=" + name + ", filterName=" + filterName + ", triggerInstanceName=" + triggerInstanceName + "]";
    }

    public int compareTo(XmlEntry o) {
      FilterInstanceXmlEntry f = (FilterInstanceXmlEntry) o;
      return name.compareTo(f.name);
    }

    public void addConfigurationParameter(String value) {
      if( configurationParameter == null ) {
        configurationParameter = new ArrayList<>();
      }
      configurationParameter.add(value);
    }
  }

  protected static class CapacityRequirementXmlEntry implements Comparable<CapacityRequirementXmlEntry> {

    protected String ordertype;
    protected String capacityName;
    protected int cardinality;

    protected CapacityRequirementXmlEntry() {
    }

    protected CapacityRequirementXmlEntry(String ordertype, String capacityName, int cardinality) {
      this.capacityName = capacityName;
      this.ordertype = ordertype;
      this.cardinality = cardinality;
    }

    public int getCardinality() {
      return cardinality;
    }


    public String getOrdertype() {
      return ordertype;
    }


    public String getCapacityName() {
      return capacityName;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((capacityName == null) ? 0 : capacityName.hashCode());
      result = prime * result + cardinality;
      result = prime * result + ((ordertype == null) ? 0 : ordertype.hashCode());
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
      CapacityRequirementXmlEntry other = (CapacityRequirementXmlEntry) obj;
      if (capacityName == null) {
        if (other.capacityName != null)
          return false;
      }
      else if (!capacityName.equals(other.capacityName))
        return false;
      if (cardinality != other.cardinality)
        return false;
      if (ordertype == null) {
        if (other.ordertype != null)
          return false;
      }
      else if (!ordertype.equals(other.ordertype))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "[ordertype=" + ordertype + ", capacityName=" + capacityName + ", cardinality=" + cardinality + "]";
    }

    public int compareTo(CapacityRequirementXmlEntry o) {
      int ret = ordertype.compareTo(o.ordertype);
      if (ret == 0) {
        return capacityName.compareTo(o.capacityName);
      }
      return ret;
    }
  }

  protected static class CapacityXmlEntry extends XmlEntry {

    protected String name;
    protected int cardinality;
    protected CapacityManagement.State state;

    public CapacityXmlEntry(boolean isImplicitDependency) {
      super(isImplicitDependency);
    }
    
    protected CapacityXmlEntry(boolean isImplicitDependency, String name, int cardinality, CapacityManagement.State state) {
      super(isImplicitDependency);
      this.name = name;
      this.cardinality = cardinality;
      this.state = state;
    }

    public String getName() {
      return name;
    }


    public int getCardinality() {
      return cardinality;
    }

    public CapacityManagement.State getState() {
      return state;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + cardinality;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((state == null) ? 0 : state.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (!super.equals(obj))
        return false;
      if (getClass() != obj.getClass())
        return false;
      CapacityXmlEntry other = (CapacityXmlEntry) obj;
      if (cardinality != other.cardinality)
        return false;
      if (name == null) {
        if (other.name != null)
          return false;
      }
      else if (!name.equals(other.name))
        return false;
      if (state != other.state)
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "[name=" + name + ", cardinality=" + cardinality + ", state=" + state + "]";
    }

    public int compareTo(XmlEntry o) {
      CapacityXmlEntry c = (CapacityXmlEntry) o;
      return name.compareTo(c.name);
    }
  }

  
  public List<PriorityXmlEntry> getPriorities() {
    return priorities;
  }

  
  public List<MonitoringLevelXmlEntry> getMonitoringLevels() {
    return monitoringLevels;
  }
  
  protected static class XMOMStorableXmlEntry implements Comparable<XMOMStorableXmlEntry> {

    protected String xmlName;
    protected String path;
    protected String fqPath;
    protected String odsName;
    protected String colName;
    
    protected XMOMStorableXmlEntry() {
    }

    private XMOMStorableXmlEntry(String xmlName, String path, String odsName) {
      this.xmlName = xmlName;
      this.path = path;
      this.odsName = odsName;
    }
    
    protected XMOMStorableXmlEntry(String xmlName, String path, String odsName, String fqPath, String colName) {
      this(xmlName, path, odsName);
      this.fqPath = fqPath;
      this.colName = colName;
    }

    public String getXmlName() {
      return xmlName;
    }


    public String getPath() {
      return path;
    }
    
    public String getOdsName() {
      return odsName;
    }
    
    public String getFqPath() {
      return fqPath;
    }
    
    public String getColName() {
      return colName;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Objects.hashCode(odsName);
      result = prime * result + Objects.hashCode(path);
      result = prime * result + Objects.hashCode(xmlName);
      result = prime * result + Objects.hashCode(fqPath);
      result = prime * result + Objects.hashCode(colName);
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
      XMOMStorableXmlEntry other = (XMOMStorableXmlEntry) obj;
      if (!Objects.equals(odsName, other.odsName)) {
        return false;
      }
      if (!Objects.equals(path, other.path)) {
        return false;
      }
      if (!Objects.equals(xmlName, other.xmlName)) {
        return false;
      }
      if (!Objects.equals(fqPath, other.fqPath)) {
        return false;
      }
      if (!Objects.equals(colName, other.odsName)) {
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return "[xmlName=" + xmlName + ", path=" + path + ", fqPath=" + fqPath + ",  tableName=" + odsName + ", columnName=" + colName + "]";
    }

    public int compareTo(XMOMStorableXmlEntry o) {
      int ret = xmlName.compareTo(o.xmlName);
      if (ret == 0) {
        return fqPath.compareTo(o.fqPath);
      }
      return ret;
    }
  }

  
  protected static class OrderInputSourceXmlEntry extends XmlEntry{
    protected String name;
    protected String type;
    protected String orderType;
    protected Map<String,String> parameter;
    protected String documentation;
    
    public OrderInputSourceXmlEntry(boolean isImplicitDependency) {
      super(isImplicitDependency);
      parameter = new HashMap<String, String>();
    }

    protected OrderInputSourceXmlEntry(boolean isImplicitDependency, String name,
                                       String type, String orderType,
                                       Map<String, String> parameter, String documentation) {
      this(isImplicitDependency);
      this.name = name;
      this.type = type;
      this.orderType = orderType;
      this.parameter = parameter;
      this.documentation = documentation;
    }


    public String getName() {
      return name;
    }

    public String getType() {
      return type;
    }

    public String getOrderType() {
      return orderType;
    }

    public Map<String, String> getParameter() {
      return parameter;
    }

    public String getDocumentation() {
      return documentation;
    }


    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((documentation == null) ? 0 : documentation.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((orderType == null) ? 0 : orderType.hashCode());
      result = prime * result + ((parameter == null) ? 0 : parameter.hashCode());
      result = prime * result + ((type == null) ? 0 : type.hashCode());
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
      OrderInputSourceXmlEntry other = (OrderInputSourceXmlEntry) obj;
      if (documentation == null) {
        if (other.documentation != null)
          return false;
      }
      else if (!documentation.equals(other.documentation))
        return false;
      if (name == null) {
        if (other.name != null)
          return false;
      }
      else if (!name.equals(other.name))
        return false;
      if (orderType == null) {
        if (other.orderType != null)
          return false;
      }
      else if (!orderType.equals(other.orderType))
        return false;
      if (parameter == null) {
        if (other.parameter != null)
          return false;
      }
      else if (!parameter.equals(other.parameter))
        return false;
      if (type == null) {
        if (other.type != null)
          return false;
      }
      else if (!type.equals(other.type))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "[name=" + name + ", type=" + type + ", orderType=" + orderType + ", parameter=" + parameter + ", documentation=" + documentation + "]";
    }

    public int compareTo(XmlEntry o) {
      OrderInputSourceXmlEntry i = (OrderInputSourceXmlEntry) o;
      return name.compareTo(i.name);
    }
  }

  
  protected static class InheritanceRuleXmlEntry implements Comparable<InheritanceRuleXmlEntry> {
    
    protected String orderType;
    protected ParameterType parameterType;
    protected String childFilter;
    protected String value;
    protected Integer precedence;
   
    protected InheritanceRuleXmlEntry(String orderType, ParameterType parameterType, String childFilter,
                                      String value, Integer precedence) {
      this.orderType = orderType;
      this.parameterType = parameterType;
      this.childFilter = childFilter;
      this.value = value;
      this.precedence = precedence;
    }

    public InheritanceRuleXmlEntry() {
    }

    public String getOrderType() {
      return orderType;
    }

    
    public ParameterType getParameterType() {
      return parameterType;
    }

    
    public String getChildFilter() {
      return childFilter;
    }

    
    public String getValue() {
      return value;
    }

    
    public Integer getPrecedence() {
      return precedence;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((childFilter == null) ? 0 : childFilter.hashCode());
      result = prime * result + ((orderType == null) ? 0 : orderType.hashCode());
      result = prime * result + ((parameterType == null) ? 0 : parameterType.hashCode());
      result = prime * result + ((precedence == null) ? 0 : precedence.hashCode());
      result = prime * result + ((value == null) ? 0 : value.hashCode());
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
      InheritanceRuleXmlEntry other = (InheritanceRuleXmlEntry) obj;
      if (childFilter == null) {
        if (other.childFilter != null)
          return false;
      }
      else if (!childFilter.equals(other.childFilter))
        return false;
      if (orderType == null) {
        if (other.orderType != null)
          return false;
      }
      else if (!orderType.equals(other.orderType))
        return false;
      if (parameterType != other.parameterType)
        return false;
      if (precedence == null) {
        if (other.precedence != null)
          return false;
      }
      else if (!precedence.equals(other.precedence))
        return false;
      if (value == null) {
        if (other.value != null)
          return false;
      }
      else if (!value.equals(other.value))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "[paramterType=" + parameterType + ", orderType=" + orderType + ", childFilter=" + childFilter 
                      + ", value=" + value + ", precedence=" + precedence + "]";
    }

    public int compareTo(InheritanceRuleXmlEntry o) {
      int ret = orderType.compareTo(o.orderType);
      if (ret == 0) {
        if (childFilter == null) {
          if (o.childFilter == null) {
            return 0;
          }
          return -1;
        }
        if (o.childFilter == null) {
          return 1;
        }
        return childFilter.compareTo(o.childFilter);
      }
      return ret;
    }
  }

  public static class RuntimeContextRequirementXmlEntry implements Comparable<RuntimeContextRequirementXmlEntry> {
    
    protected String application;
    protected String version;
    protected String workspace;
    
    public RuntimeContextRequirementXmlEntry(String application, String version,
                                             String workspace) {
      this.application = application;
      this.version = version;
      this.workspace = workspace;
    }

    public RuntimeContextRequirementXmlEntry(RuntimeContext runtimeContext) {
      if (runtimeContext instanceof Application) {
        this.application = runtimeContext.getName();
        this.version = ((Application) runtimeContext).getVersionName();
      }
      if (runtimeContext instanceof Workspace) {
        this.workspace = runtimeContext.getName();
      }
    }
    
    public RuntimeContextRequirementXmlEntry() {
    }
    
    public String getApplication() {
      return application;
    }

    
    public String getVersion() {
      return version;
    }

    
    public String getWorkspace() {
      return workspace;
    }

    public RuntimeDependencyContext getRuntimeContext() {
      return RuntimeContextDependencyManagement.getRuntimeDependencyContext(application, version, workspace);
    }
    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((application == null) ? 0 : application.hashCode());
      result = prime * result + ((version == null) ? 0 : version.hashCode());
      result = prime * result + ((workspace == null) ? 0 : workspace.hashCode());
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
      RuntimeContextRequirementXmlEntry other = (RuntimeContextRequirementXmlEntry) obj;
      if (application == null) {
        if (other.application != null)
          return false;
      }
      else if (!application.equals(other.application))
        return false;
      if (version == null) {
        if (other.version != null)
          return false;
      }
      else if (!version.equals(other.version))
        return false;
      if (workspace == null) {
        if (other.workspace != null)
          return false;
      }
      else if (!workspace.equals(other.workspace))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "[application=" + application + ", version=" + version + ", workspace=" + workspace + "]";
    }

    public int compareTo(RuntimeContextRequirementXmlEntry o) {      
      return toString().compareTo(o.toString());
    }

  }


  public static void diff(ApplicationXmlEntry e1, ApplicationXmlEntry e2, CommandLineWriter clw) {
    clw.writeLineToCommandLine("<<< " + e1.getApplicationName() + ", " + e1.getVersionName());
    clw.writeLineToCommandLine(">>> " + e2.getApplicationName() + ", " + e2.getVersionName());
    diff("Ordertypes", e1.getOrdertypes(), e2.getOrdertypes(), clw);
    diff("Capacities", e1.getCapacities(), e2.getCapacities(), clw);
    diff("CapacityRequirements", e1.getCapacityRequirements(), e2.getCapacityRequirements(), clw);
    diff("MonitoringLevel", e1.getMonitoringLevels(), e2.getMonitoringLevels(), clw);
    diff("ParameterInheritanceRules", e1.getParameterInheritanceRules(), e2.getParameterInheritanceRules(), clw);
    diff("Priorities", e1.getPriorities(), e2.getPriorities(), clw);
    diff("SharedLibs", e1.getSharedLibs(), e2.getSharedLibs(), clw);
    diff("RuntimeContextRequirements", e1.getRuntimeContextRequirements(), e2.getRuntimeContextRequirements(), clw);
    diff("XmomEntries", e1.getXmomEntries(), e2.getXmomEntries(), clw);
    diff("XmomStorableEntries", e1.getXmomStorableEntries(), e2.getXmomStorableEntries(), clw);

    diff("Triggers", e1.getTriggers(), e2.getTriggers(), clw);
    diff("Triggerinstances", e1.getTriggerInstances(), e2.getTriggerInstances(), clw);
    diff("Filters", e1.getFilters(), e2.getFilters(), clw);
    diff("Filterinstances", e1.getFilterInstances(), e2.getFilterInstances(), clw);
    diff("XynaProperties", e1.getXynaProperties(), e2.getXynaProperties(), clw);
  }


  private static <A extends Comparable<? super A>> void diff(String name, List<A> l1, List<A> l2, CommandLineWriter clw) {
    clw.writeLineToCommandLine("   ######## " + name + " #########");
    outer : for (Comparable<? super A> x1 : l1) {
      if (l2.remove(x1)) {
        continue;
      }
      for (Comparable<? super A> x2 : l2) {
        if (x1.compareTo((A) x2) == 0) {
          //gleicher identifer!
          clw.writeLineToCommandLine("<<< " + x1.toString());
          clw.writeLineToCommandLine(">>> " + x2.toString());
          clw.writeLineToCommandLine("----");
          l2.remove(x2);
          continue outer;
        }
      }
      clw.writeLineToCommandLine("<<< " + x1.toString());
      clw.writeLineToCommandLine("----");
    }
    for (Comparable<? super A> x2 : l2) {
      clw.writeLineToCommandLine(">>> " + x2.toString());
      clw.writeLineToCommandLine("----");
    }
  }
  
  public void minify() {
    xmomEntries.removeIf(x -> x.isImplicitDependency());
    ordertypes.removeIf(x -> x.isImplicitDependency());
  }

  
}

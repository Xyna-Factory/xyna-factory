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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.ApplicationInfoEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.CapacityRequirementXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.CapacityXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.FilterInstanceXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.FilterXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.InheritanceRuleXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.MonitoringLevelXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.OrderInputSourceXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.OrdertypeXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.PriorityXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.RuntimeContextRequirementXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.SharedLibXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.TriggerInstanceXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.TriggerXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.XMOMStorableXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.XMOMXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.XynaPropertyXmlEntry;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_DETECTION_PROBLEM;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;


/**
 *
 */
public class ApplicationXmlHandler extends DefaultHandler {

  private ApplicationXmlEntry applicationXmlEntry;
  private Stack<Action<?>> actionStack;
  private Locator locator;
  private ValueAction valueAction;
  
  @Override
  public void setDocumentLocator(Locator locator) {
    this.locator = locator;
  }

  @Override 
  public void startDocument() { 
    actionStack = new Stack<Action<?>>();
    actionStack.push( new RootAction(this) );
    valueAction = new ValueAction();
  }
  
  @Override 
  public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException { 
    Action<?> current = actionStack.peek();
    //logger.debug( actionStack.size() + " " + current.getClass().getSimpleName() + " " + qName);
    
    //ist Child "qName" aktuell erlaubt?
    current.checkTag(locator, qName);
    
    //neue Action für den Child erzeugen
    Action<?> child = current.childElement(qName); 
    if( child == null ) {
      child = valueAction;
    }
    
    //Child initialisieren
    child.start(atts);
    actionStack.push( child );
  }
  
  @Override 
  public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
    //Child ist beendet, daher von Stack nehmenm
    Action<?> child = actionStack.pop();
    Action<?> current = actionStack.peek();
    
    //Spezialfall String-Daten auslesen
    String childValue = null;
    if( child instanceof ValueAction ) {
      childValue = ((ValueAction)child).getValue();
    }
    
    //in aktueller Action Ergebnisse des Child bekanntmachen
    current.endElement(qName, childValue, child);
  }
  
  @Override
  public void characters(char[] buf, int offset, int len) throws SAXException {
    Action<?> action = actionStack.peek();
    if( action instanceof ValueAction ) {
      ((ValueAction)action).append(buf, offset, len);
    }
  }
  
  public ApplicationXmlEntry getApplicationXmlEntry() {
    return applicationXmlEntry;
  }


  /**
   * 
   * Action: Behandlung von startElement(..) und endElement(..)
   *
   */
  private static class Action<T> {
    protected String ownTag;
    protected List<String> expectedTags;
    private Locator locator;
    
    protected Action(String ownTag, List<String> expectedTags) {
      this.ownTag = ownTag;
      this.expectedTags = expectedTags;
    }
    
    /**
     * Prüfung, ob der childTag erwartet wird
     * @param locator
     * @param childTag
     * @throws SAXException 
     */
    public void checkTag(Locator locator, String childTag) throws SAXException {
      this.locator = locator;
      if( expectedTags.contains(childTag) ) {
        //ok
      } else {
        error(ownTag+"."+childTag +" is unexpected!");
      }
    }
   
    /**
     * Gibt die entsprechende Action für den childTag zurück
     * @param childTag
     * @return
     * @throws SAXException
     */
    public Action<?> childElement(String childTag) throws SAXException {
      //leere Basis-Implementierung
      return null;
    }
    /**
     * Initialisierung mit den Attributen des StartTags
     * @param atts
     */
    public void start(Attributes atts) throws SAXException {
      //leere Basis-Implementierung
    }
    /**
     * End-Tag des Childs: Übernahme der Daten in eigene Speicherung
     * @param childTag
     * @param value
     * @param started
     * @throws SAXException
     */
    public void endElement(String childTag, String value, Action<?> started) throws SAXException {
      //leere Basis-Implementierung
    }
    
    
    /**
     * Rückgabe des gebauten Objects
     * @return
     */
    public T getValue() {
      return null;
    }
    
    protected void error(String message) throws SAXException {
      throw new SAXException(createSaxMessage(message));
    }
    
    private String createSaxMessage(String message) { 
      return   "Line: " + locator.getLineNumber() + ", Column: " 
             + locator.getColumnNumber() + ", Error: " + message; 
    }

  }
  
  private static class RootAction extends Action<Void> {

    private ApplicationXmlHandler applicationXmlHandler;

    protected RootAction(ApplicationXmlHandler applicationXmlHandler) {
      super("", Arrays.asList(TAG_APPLICATION));
      this.applicationXmlHandler = applicationXmlHandler; 
    }
    
    @Override
    public ApplicationAction childElement(String childTag) throws SAXException {
      return new ApplicationAction();
    }
    
    @Override
    public void endElement(String childName, String value, Action<?> started) throws SAXException {
      applicationXmlHandler.applicationXmlEntry = (ApplicationXmlEntry)started.getValue();
    }
    
  }

  private static class ValueAction extends Action<String> {
    private StringBuilder sb;
    private Attributes attributes;
    
    protected ValueAction() {
      super("",null);
      sb = new StringBuilder();
    }
    
    @Override
    public void start(Attributes atts) {
      this.attributes = atts;
      sb.setLength(0);
    }

    @Override
    public String getValue() {
      return sb.toString();
    }

    public void append(char[] buf, int offset, int len) {
      sb.append(buf, offset, len);
    }

    public String getAttribute(String name) {
      return attributes.getValue(name);
    }

  }

  private static class ListAction<T> extends Action<List<T>> {
    private List<T> list;
    private ListEntryAction<T> childAction;
    
    protected ListAction(String ownTag, String childTag, ListEntryAction<T> childAction) {
      super(ownTag, Arrays.asList(childTag) );
      this.childAction = childAction;
    }

    public void setList(List<T> list) {
      this.list = list;
    }
    
    @Override
    public ListEntryAction<T> childElement(String childTag) throws SAXException {
      return childAction;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void endElement(String childTag, String value, Action<?> started) {
      list.add( (T) started.getValue() );
    }

    @Override
    public List<T> getValue() {
      return list;
    }
  }
  
  private abstract static class ListEntryAction<T> extends Action<T> {

    protected T entry;
    
    protected ListEntryAction(String ownTag, List<String> childTags) {
      super(ownTag, childTags);
    }
    
    public void start(Attributes atts) {
      entry = createEntry(atts);
    }
    
    protected abstract T createEntry(Attributes atts);

    public abstract void endElement(String childTag, String value, Action<?> started) throws SAXException;

    @Override
    public T getValue() {
      return entry;
    }
    
  }
  
  private static class ApplicationAction extends Action<ApplicationXmlEntry> {

    protected ApplicationXmlEntry applicationXmlEntry;
    
    protected ApplicationAction() {
      super(TAG_APPLICATION, Arrays.asList(TAG_APPLICATION_INFO, 
                                           TAG_CAPACITIES,
                                           TAG_CAPACITYREQUIREMENTS,
                                           TAG_FILTERS,
                                           TAG_FILTERINSTANCES,
                                           TAG_ORDERTYPES,
                                           TAG_SHAREDLIBS,
                                           TAG_TRIGGERS,
                                           TAG_TRIGGERINSTANCES,
                                           TAG_XMOMENTRIES,
                                           TAG_XYNAPROPERTIES,
                                           TAG_MONITORINGLEVELS,
                                           TAG_PRIORITIES,
                                           TAG_XMOMSTORABLES,
                                           TAG_ORDERINPUTSOURCES,
                                           TAG_INHERITANCERULES ) );
    }
    
    @Override
    public void start(Attributes atts) throws SAXException {
      applicationXmlEntry = new ApplicationXmlEntry();
      applicationXmlEntry.applicationName = atts.getValue(ATTRIBUTE_APPLICATIONNAME);
      applicationXmlEntry.versionName = atts.getValue(ATTRIBUTE_VERISONNAME);
      applicationXmlEntry.xmlVersion = atts.getValue(ATTRIBUTE_XMLVERSION);
      if (!supportedVersion(applicationXmlEntry.xmlVersion)) {
        error("Unsupported version of application xml: " + applicationXmlEntry.xmlVersion);
      }
      applicationXmlEntry.comment = atts.getValue(ATTRIBUTE_COMMENT);
      
      if (XynaFactory.isFactoryServer()) {
        try {
          applicationXmlEntry.setFactoryVersion(atts.getValue(ATTRIBUTE_FACTORYVERSION));
        } catch (XPRC_VERSION_DETECTION_PROBLEM e) {
          throw new SAXException(e);
        } catch (PersistenceLayerException e) {
          throw new SAXException(e);
        }
      }
    }

    private boolean supportedVersion(String version) {
      if (version.equals(XMLVERSION)) {
        return true;
      }
      if (version.equals("1.0")) {
        return true;
      }
      return false;
    }

    @Override
    public Action<?> childElement(String childTag) throws SAXException {
      if( TAG_APPLICATION_INFO.equals(childTag)) {
        return new ApplicationInfoAction();
      } else if( TAG_CAPACITIES.equals(childTag)) {
        return createListAction(TAG_CAPACITIES, TAG_CAPACITY, applicationXmlEntry.capacities, new CapacityAction() );
      } else if( TAG_CAPACITYREQUIREMENTS.equals(childTag)) {
        return createListAction(TAG_CAPACITYREQUIREMENTS, TAG_CAPACITYREQUIREMENT, applicationXmlEntry.capacityRequirements, new CapacityRequirementAction() );
      } else if( TAG_FILTERS.equals(childTag)) {
        return createListAction(TAG_FILTERS, TAG_FILTER, applicationXmlEntry.filters, new FilterAction() );
      } else if( TAG_FILTERINSTANCES.equals(childTag)) {
        return createListAction(TAG_FILTERINSTANCES, TAG_FILTERINSTANCE, applicationXmlEntry.filterInstances, new FilterInstanceAction() );
      } else if( TAG_ORDERTYPES.equals(childTag)) {
        return createListAction(TAG_ORDERTYPES, TAG_ORDERTYPE, applicationXmlEntry.ordertypes, new OrderTypeAction() );
      } else if( TAG_SHAREDLIBS.equals(childTag)) {
        return createListAction(TAG_SHAREDLIBS, TAG_SHAREDLIB, applicationXmlEntry.sharedLibs, new SharedLibAction() );
      } else if( TAG_TRIGGERS.equals(childTag)) {
        return createListAction(TAG_TRIGGERS, TAG_TRIGGER, applicationXmlEntry.triggers, new TriggerAction() );
      } else if( TAG_TRIGGERINSTANCES.equals(childTag)) {
        return createListAction(TAG_TRIGGERINSTANCES, TAG_TRIGGERINSTANCE, applicationXmlEntry.triggerInstances, new TriggerInstanceAction() );
      } else if( TAG_XMOMENTRIES.equals(childTag)) {
        return createListAction(TAG_XMOMENTRIES, TAG_XMOMENTRY, applicationXmlEntry.xmomEntries, new XMOMAction() );
      } else if( TAG_XYNAPROPERTIES.equals(childTag)) {
        return createListAction(TAG_XYNAPROPERTIES, TAG_XYNAPROPERTY, applicationXmlEntry.xynaProperties, new XynaPropertyAction() );
      } else if( TAG_MONITORINGLEVELS.equals(childTag)) {
        return createListAction(TAG_MONITORINGLEVELS, TAG_MONITORINGLEVEL, applicationXmlEntry.monitoringLevels, new MonitoringLevelAction() );
      } else if( TAG_PRIORITIES.equals(childTag)) {
        return createListAction(TAG_PRIORITIES, TAG_PRIORITY, applicationXmlEntry.priorities, new PriorityAction() );
      } else if( TAG_XMOMSTORABLES.equals(childTag)) {
        return createListAction(TAG_XMOMSTORABLES, TAG_XMOMSTORABLE, applicationXmlEntry.xmomStorables, new XMOMStorableAction() );
      } else if( TAG_ORDERINPUTSOURCES.equals(childTag)) {
        return createListAction(TAG_ORDERINPUTSOURCES, TAG_ORDERINPUTSOURCE, applicationXmlEntry.orderInputSources, new OrderInputSourceAction() );
      } else if( TAG_INHERITANCERULES.equals(childTag)) {
        return createListAction(TAG_INHERITANCERULES, TAG_INHERITANCERULE, applicationXmlEntry.parameterInheritanceRules, new InheritanceRuleAction() );
      }
      return null;
    }
    
    private <T> ListAction<T> createListAction(String ownTag, String childTag,
                                               List<T> list, ListEntryAction<T> listEntryAction) {
      ListAction<T> listAction = new ListAction<T>(ownTag, childTag, listEntryAction );
      listAction.setList(list);
      return listAction;
    }

    @Override
    public void endElement(String childTag, String value, Action<?> started) {
      if( TAG_APPLICATION_INFO.equals(childTag) ) {
        applicationXmlEntry.applicationInfo = (ApplicationInfoEntry)started.getValue();
      } else {
        //nichts mehr zu tun
      }
    }
    
    @Override
    public ApplicationXmlEntry getValue() {
      return applicationXmlEntry;
    }
    
  }

  
  private static class ApplicationInfoAction extends Action<ApplicationInfoEntry> {

    protected ApplicationInfoEntry applicationInfoEntry;
    
    protected ApplicationInfoAction() {
      super(TAG_APPLICATION_INFO, Arrays.asList(TAG_AI_DESCRIPTION, TAG_AI_BUILDDATE, TAG_AI_ISREMOTESTUB, TAG_RUNTIMECONTEXT_REQUIREMENTS ) );
      this.applicationInfoEntry = new ApplicationInfoEntry();
    }

    @Override
    public Action<?> childElement(String childTag) throws SAXException {
      if( TAG_RUNTIMECONTEXT_REQUIREMENTS.equals(childTag) ) {
        applicationInfoEntry.setRuntimeContextRequirements(new ArrayList<RuntimeContextRequirementXmlEntry>() );
        ListAction<RuntimeContextRequirementXmlEntry> action = 
          new ListAction<RuntimeContextRequirementXmlEntry>(TAG_RUNTIMECONTEXT_REQUIREMENTS, TAG_RUNTIMECONTEXT_REQUIREMENT, new RuntimeContextRequirementAction() );
        action.setList(applicationInfoEntry.getRuntimeContextRequirements());
        return action;
      }
      return null;
    }
    
    @Override
    public void endElement(String childTag, String value, Action<?> started) {
      if(TAG_AI_DESCRIPTION.equals(childTag) ) {
        ValueAction va = (ValueAction)started;
        DocumentationLanguage lang = DocumentationLanguage.valueOf( va.getAttribute(ATTRIBUTE_AI_D_LANG) );
        
        applicationInfoEntry.setDescription(lang, value);
      } else if(TAG_AI_BUILDDATE.equals(childTag)) {
        applicationInfoEntry.setBuildDate(value);
      } else if( TAG_RUNTIMECONTEXT_REQUIREMENTS.equals(childTag) ) {
        //ListAction hat Daten bereits übertragen
      } else if (TAG_AI_ISREMOTESTUB.equals(childTag)) {
        applicationInfoEntry.setIsRemoteStub(Boolean.valueOf(value));
      }
    }

    @Override
    public ApplicationInfoEntry getValue() {
      return applicationInfoEntry;
    }
  }
  


  
  private static class OrderTypeAction extends ListEntryAction<OrdertypeXmlEntry> {
    
    protected OrderTypeAction() {
      super(TAG_ORDERTYPE, Arrays.asList(TAG_PLANNING, TAG_EXECUTION, TAG_CLEANUP, TAG_DESTINATIONKEY, TAG_ORDERCONTEXTMAPPING) );
    }
    
    @Override
    protected OrdertypeXmlEntry createEntry(Attributes atts) {
      return new OrdertypeXmlEntry(Boolean.valueOf(atts.getValue(ATTRIBUTE_IMPLICIT_DEPENDENCY)));
    }
    
    @Override
    public void endElement(String childTag, String value, Action<?> started) throws SAXException {
      if(TAG_PLANNING.equals(childTag)) {
        entry.planning = value;
      } else if(TAG_EXECUTION.equals(childTag)) {
        entry.execution = value;
      } else if(TAG_CLEANUP.equals(childTag)) {
        entry.cleanup = value;
      } else if(TAG_DESTINATIONKEY.equals(childTag)) {
        entry.destinationKey = value;
      } else if(TAG_ORDERCONTEXTMAPPING.equals(childTag)) {
        entry.ordercontextMapping = Boolean.parseBoolean(value);
      } else {
        error("Unexpected tag <" + childTag + ">");
      }
    }
    
  }
  
  private static class XMOMAction extends ListEntryAction<XMOMXmlEntry> {
    
    protected XMOMAction() {
      super(TAG_XMOMENTRY, Arrays.asList(TAG_FQNAME, TAG_TYPE) );
    }
    
    @Override
    protected XMOMXmlEntry createEntry(Attributes atts) {
      return new XMOMXmlEntry(Boolean.valueOf(atts.getValue(ATTRIBUTE_IMPLICIT_DEPENDENCY)));
    }
    
    @Override
    public void endElement(String childTag, String value, Action<?> started) throws SAXException {
      if(TAG_FQNAME.equals(childTag)) {
        entry.fqName = value;
      } else if(TAG_TYPE.equals(childTag)) {
        entry.type = value;
      } else {
        error("Unexpected tag <" + childTag + ">");
      }
    }
  }
 
  private static class RuntimeContextRequirementAction extends ListEntryAction<RuntimeContextRequirementXmlEntry> {
    
    protected RuntimeContextRequirementAction() {
      super(TAG_RUNTIMECONTEXT_REQUIREMENT, Arrays.asList(TAG_APPLICATIONNAME, TAG_VERSIONNAME, TAG_WORKSPACENAME) );
    }
    
    @Override
    protected RuntimeContextRequirementXmlEntry createEntry(Attributes atts) {
      return new RuntimeContextRequirementXmlEntry();
    }
    
    @Override
    public void endElement(String childTag, String value, Action<?> started) throws SAXException {
      if(TAG_APPLICATIONNAME.equals(childTag)) {
        entry.application = value;
      } else if(TAG_VERSIONNAME.equals(childTag)) {
        entry.version = value;
      } else if(TAG_WORKSPACENAME.equals(childTag)) {
        entry.workspace = value;
      } else {
        error("Unexpected tag <" + childTag + ">");
      }
    }
  }
 
  private static class MonitoringLevelAction extends ListEntryAction<MonitoringLevelXmlEntry> {
    
    protected MonitoringLevelAction() {
      super(TAG_MONITORINGLEVEL, Arrays.asList(TAG_ORDERTYPENAME, TAG_VALUE) );
    }
    
    @Override
    protected MonitoringLevelXmlEntry createEntry(Attributes atts) {
      return new MonitoringLevelXmlEntry();
    }
    
    @Override
    public void endElement(String childTag, String value, Action<?> started) throws SAXException {
      if(TAG_ORDERTYPENAME.equals(childTag)) {
        entry.ordertype = value;
      } else if(TAG_VALUE.equals(childTag)) {
        try {
          entry.monitoringLevel = Integer.parseInt(value);
        } catch(NumberFormatException e) {
          error("A number is excpected.");
        }
      } else {
        error("Unexpected tag <" + childTag + ">");
      }
    }
  }
 
  private static class PriorityAction extends ListEntryAction<PriorityXmlEntry> {
    
    protected PriorityAction() {
      super(TAG_PRIORITY, Arrays.asList(TAG_ORDERTYPENAME, TAG_VALUE) );
    }
    
    @Override
    protected PriorityXmlEntry createEntry(Attributes atts) {
      return new PriorityXmlEntry();
    }
    
    @Override
    public void endElement(String childTag, String value, Action<?> started) throws SAXException {
      if(TAG_ORDERTYPENAME.equals(childTag)) {
        entry.ordertype = value;
      } else if(TAG_VALUE.equals(childTag)) {
        try {
          entry.priority = Integer.parseInt(value);
        } catch(NumberFormatException e) {
          error("A number is excpected.");
        }
      } else {
        error("Unexpected tag <" + childTag + ">");
      }
    }
  }
 
  private static class SharedLibAction extends ListEntryAction<SharedLibXmlEntry> {
    
    protected SharedLibAction() {
      super(TAG_SHAREDLIB, Arrays.asList(TAG_SHAREDLIB) );
    }
    
    @Override
    protected SharedLibXmlEntry createEntry(Attributes atts) {
      return new SharedLibXmlEntry(Boolean.valueOf(atts.getValue(ATTRIBUTE_IMPLICIT_DEPENDENCY)));
    }
    
    @Override
    public void endElement(String childTag, String value, Action<?> started) throws SAXException {
      if(TAG_SHAREDLIB.equals(childTag)) {
        entry.sharedLibName = value;
      } else {
        error("Unexpected tag <" + childTag + ">");
      }
    }
  }

  private static class XynaPropertyAction extends ListEntryAction<XynaPropertyXmlEntry> {
    
    protected XynaPropertyAction() {
      super(TAG_XYNAPROPERTY, Arrays.asList(TAG_NAME, TAG_VALUE) );
    }
    
    @Override
    protected XynaPropertyXmlEntry createEntry(Attributes atts) {
      return new XynaPropertyXmlEntry(Boolean.valueOf(atts.getValue(ATTRIBUTE_IMPLICIT_DEPENDENCY)));
    }
    
    @Override
    public void endElement(String childTag, String value, Action<?> started) throws SAXException {
      if(TAG_NAME.equals(childTag)) {
        entry.name = value;
      } else if(TAG_VALUE.equals(childTag)) {
        entry.value = value;
      } else {
        error("Unexpected tag <" + childTag + ">");
      }
    }
  }

  private static class TriggerInstanceAction extends ListEntryAction<TriggerInstanceXmlEntry> {
    
    protected TriggerInstanceAction() {
      super(TAG_TRIGGERINSTANCE, Arrays.asList(TAG_NAME, TAG_TRIGGERNAME, TAG_STARTPARAMETER, TAG_TRIGGER_MAXRECEIVES, TAG_TRIGGER_REJECTAFTERMAXRECEIVES) );
    }
    
    @Override
    protected TriggerInstanceXmlEntry createEntry(Attributes atts) {
      return new TriggerInstanceXmlEntry(Boolean.valueOf(atts.getValue(ATTRIBUTE_IMPLICIT_DEPENDENCY)));
    }
    
    @Override
    public void endElement(String childTag, String value, Action<?> started) throws SAXException {
      if(TAG_NAME.equals(childTag)) {
        entry.name = value;
      } else if(TAG_TRIGGERNAME.equals(childTag)) {
        entry.triggerName = value;
      } else if(TAG_STARTPARAMETER.equals(childTag)) {
        entry.startParameter = value;
      } else if(TAG_TRIGGER_MAXRECEIVES.equals(childTag)) {
        entry.maxEvents = Long.parseLong(value);
      } else if(TAG_TRIGGER_REJECTAFTERMAXRECEIVES.equals(childTag)) {
        entry.rejectRequestsAfterMaxReceives = Boolean.parseBoolean(value);
      } else {
        error("Unexpected tag <" + childTag + ">");
      }
    }
  }

  private static class TriggerAction extends ListEntryAction<TriggerXmlEntry> {
    
    protected TriggerAction() {
      super(TAG_TRIGGER, Arrays.asList(TAG_NAME, TAG_JARFILES, TAG_FQTRIGGERCLASSNAME, TAG_SHAREDLIBS) );
    }
    
    @Override
    protected TriggerXmlEntry createEntry(Attributes atts) {
      return new TriggerXmlEntry(Boolean.valueOf(atts.getValue(ATTRIBUTE_IMPLICIT_DEPENDENCY)));
    }
    
    @Override
    public void endElement(String childTag, String value, Action<?> started) throws SAXException {
      if(TAG_NAME.equals(childTag)) {
        entry.name = value;
      } else if(TAG_JARFILES.equals(childTag)) {
        entry.jarFiles = value;
      } else if(TAG_FQTRIGGERCLASSNAME.equals(childTag)) {
        entry.fqTriggerClassname = value;
      } else if(TAG_SHAREDLIBS.equals(childTag)) {
        entry.sharedLibs = value;
      } else {
        error("Unexpected tag <" + childTag + ">");
      }
    }
  }

  
  private static class FilterInstanceAction extends ListEntryAction<FilterInstanceXmlEntry> {
    
    protected FilterInstanceAction() {
      super(TAG_FILTERINSTANCE, Arrays.asList(TAG_NAME, TAG_FILTERNAME, TAG_TRIGGERINSTANCENAME, TAG_FILTER_CONFIG, TAG_DESCRIPTION) );
    }
    
    @Override
    protected FilterInstanceXmlEntry createEntry(Attributes atts) {
      return new FilterInstanceXmlEntry(Boolean.valueOf(atts.getValue(ATTRIBUTE_IMPLICIT_DEPENDENCY)));
    }
    
    @Override
    public void endElement(String childTag, String value, Action<?> started) throws SAXException {
      if(TAG_NAME.equals(childTag)) {
        entry.name = value;
      } else if(TAG_FILTERNAME.equals(childTag)) {
        entry.filterName = value;
      } else if(TAG_TRIGGERINSTANCENAME.equals(childTag)) {
        entry.triggerInstanceName = value;
      } else if(TAG_FILTER_CONFIG.equals(childTag)) {
        entry.addConfigurationParameter(value);
      } else if (TAG_DESCRIPTION.equals(childTag)) {
        entry.description = value;
      } else {
        error("Unexpected tag <" + childTag + ">");
      }
    }
  }

  private static class FilterAction extends ListEntryAction<FilterXmlEntry> {
    
    protected FilterAction() {
      super(TAG_FILTER, Arrays.asList(TAG_NAME, TAG_JARFILES, TAG_FQFILTERCLASSNAME, TAG_TRIGGERNAME, TAG_SHAREDLIBS) );
    }
    
    @Override
    protected FilterXmlEntry createEntry(Attributes atts) {
      return new FilterXmlEntry(Boolean.valueOf(atts.getValue(ATTRIBUTE_IMPLICIT_DEPENDENCY)));
    }
    
    @Override
    public void endElement(String childTag, String value, Action<?> started) throws SAXException {
      if(TAG_NAME.equals( childTag)) {
        entry.name = value;
      } else if(TAG_JARFILES.equals( childTag)) {
        entry.jarFiles = value;
      } else if(TAG_FQFILTERCLASSNAME.equals( childTag)) {
        entry.fqFilterClassname = value;
      } else if(TAG_TRIGGERNAME.equals( childTag)) {
        entry.triggerName = value;
      } else if(TAG_SHAREDLIBS.equals( childTag)) {
        entry.sharedLibs = value;
      } else {
        error("Unexpected tag <" +  childTag + ">");
      }
    }
  }
   
  private static class CapacityRequirementAction extends ListEntryAction<CapacityRequirementXmlEntry> {
    
    protected CapacityRequirementAction() {
      super(TAG_CAPACITYREQUIREMENT, Arrays.asList(TAG_ORDERTYPENAME, TAG_CAPACITYNAME, TAG_CARDINALITY) );
    }
    
    @Override
    protected CapacityRequirementXmlEntry createEntry(Attributes atts) {
      return new CapacityRequirementXmlEntry();
    }
    
    @Override
    public void endElement(String childTag, String value, Action<?> started) throws SAXException {
      if(TAG_ORDERTYPENAME.equals(childTag)) {
        entry.ordertype = value;
      } else if(TAG_CAPACITYNAME.equals(childTag)) {
        entry.capacityName = value;
      } else if(TAG_CARDINALITY.equals(childTag)) {
        try {
          entry.cardinality = Integer.parseInt(value);
        } catch(NumberFormatException e) {
          error("A number is excpected.");
        }
      } else {
        error("Unexpected tag <" + childTag + ">");
      }
    }
  }
   
  private static class CapacityAction extends ListEntryAction<CapacityXmlEntry> {
    
    protected CapacityAction() {
      super(TAG_CAPACITY, Arrays.asList(TAG_NAME, TAG_CARDINALITY, TAG_CAPACITY_STATE) );
    }
    
    @Override
    protected CapacityXmlEntry createEntry(Attributes atts) {
      return new CapacityXmlEntry(Boolean.valueOf(atts.getValue(ATTRIBUTE_IMPLICIT_DEPENDENCY)));
    }
    
    @Override
    public void endElement(String childTag, String value, Action<?> started) throws SAXException {
      if(TAG_NAME.equals(childTag)) {
        entry.name = value;
      } else if(TAG_CARDINALITY.equals(childTag)) {
        try {
          entry.cardinality = Integer.parseInt(value);
        } catch(NumberFormatException e) {
          error("A number is excpected.");
        }
      } else if(TAG_CAPACITY_STATE.equals(childTag)) {
        entry.state = State.valueOf(value);
      } else {
        error("Unexpected tag <" + childTag + ">.");
      }
    }
  }
   
  private static class XMOMStorableAction extends ListEntryAction<XMOMStorableXmlEntry> {
    
    protected XMOMStorableAction() {
      super(TAG_XMOMSTORABLE, Arrays.asList(TAG_XMOMSTORABLE_XMLNAME, TAG_XMOMSTORABLE_PATH, TAG_XMOMSTORABLE_ODSNAME, TAG_XMOMSTORABLE_FQPATH, TAG_XMOMSTORABLE_COLUMNNAME) );
    }
    
    @Override
    protected XMOMStorableXmlEntry createEntry(Attributes atts) {
      return new XMOMStorableXmlEntry();
    }
    
    @Override
    public void endElement(String childTag, String value, Action<?> started) throws SAXException {
      if (TAG_XMOMSTORABLE_XMLNAME.equals(childTag)) {
        entry.xmlName = value;
      } else if (TAG_XMOMSTORABLE_PATH.equals(childTag)) {
        entry.path = value;
      } else if (TAG_XMOMSTORABLE_ODSNAME.equals(childTag)) {
        entry.odsName = value;
      } else if (TAG_XMOMSTORABLE_FQPATH.equals(childTag)) {
        entry.fqPath = value;
      } else if (TAG_XMOMSTORABLE_COLUMNNAME.equals(childTag)) {
        entry.colName = value;
      } else {
        error("Unexpected tag <" + childTag + ">.");
      }
    }
  }
   
  private static class OrderInputSourceAction extends ListEntryAction<OrderInputSourceXmlEntry> {
    
    OrderInputSourceParameterAction oispa;
    
    protected OrderInputSourceAction() {
      super(TAG_ORDERINPUTSOURCE, Arrays.asList(TAG_NAME, TAG_TYPE, TAG_ORDERTYPE, TAG_ORDERINPUTSOURCE_PARAMETER, TAG_DOCUMENTATION) );
    }
    
    @Override
    protected OrderInputSourceXmlEntry createEntry(Attributes atts) {
      return new OrderInputSourceXmlEntry(Boolean.valueOf(atts.getValue(ATTRIBUTE_IMPLICIT_DEPENDENCY)));
    }
    
    @Override
    public Action<?> childElement(String childTag) throws SAXException {
      if( TAG_ORDERINPUTSOURCE_PARAMETER.equals(childTag ) ) {
        if( oispa == null ) {
          oispa = new OrderInputSourceParameterAction();
        }
        return oispa;
      }
      return null;
    }
    
    @Override
    public void endElement(String childTag, String value, Action<?> started) throws SAXException {
      if (TAG_NAME.equals(childTag)) {
        entry.name = value;
      } else if (TAG_TYPE.equals(childTag)) {
        entry.type = value;
      } else if (TAG_ORDERTYPE.equals(childTag)) {
        entry.orderType = value;
      } else if (TAG_DOCUMENTATION.equals(childTag)) {
        entry.documentation = value;
      } else if (TAG_ORDERINPUTSOURCE_PARAMETER.equals(childTag)) {
        Pair<String,String> kv = ((OrderInputSourceParameterAction)started).getValue();
        entry.parameter.put(kv.getFirst(), kv.getSecond());
      } else {
        error("Unexpected tag <" + childTag + ">.");
      }
    }
  }
 
  private static class OrderInputSourceParameterAction extends Action<Pair<String,String>> {
    String key;
    String value;
    
    protected OrderInputSourceParameterAction() {
      super(TAG_ORDERINPUTSOURCE_PARAMETER, Arrays.asList(TAG_KEY, TAG_VALUE) );
    }
    
    @Override
    public void start(Attributes atts) {
      this.key = null;
      this.value = null;
    }
    
    @Override
    public void endElement(String childTag, String value, Action<?> started) throws SAXException {
      if (TAG_KEY.equals(childTag)) {
        this.key = value;
      } else if (TAG_VALUE.equals(childTag)) {
        this.value = value;
      } else {
        error("Unexpected tag <" + childTag + ">.");
      }
    }
    
    @Override
    public Pair<String, String> getValue() {
      return Pair.of(key,value);
    }
  }
  
  private static class InheritanceRuleAction extends ListEntryAction<InheritanceRuleXmlEntry> {
    
    protected InheritanceRuleAction() {
      super(TAG_INHERITANCERULE, Arrays.asList(TAG_ORDERTYPENAME, TAG_PARAMETERTYPE, TAG_CHILDFILTER, TAG_VALUE, TAG_PRECEDENCE) );
    }
    
    @Override
    protected InheritanceRuleXmlEntry createEntry(Attributes atts) {
      return new InheritanceRuleXmlEntry();
    }
    
    @Override
    public void endElement(String childTag, String value, Action<?> started) throws SAXException {
      if(TAG_ORDERTYPENAME.equals(childTag)) {
        entry.orderType = value;
      } else if(TAG_PARAMETERTYPE.equals(childTag)) {
        try {
          entry.parameterType = ParameterType.valueOf(value);
        } catch(IllegalArgumentException e) {
          error("Unexpected value <" + value + "> for "+TAG_PARAMETERTYPE);
        }
      } else if(TAG_CHILDFILTER.equals(childTag)) {
        entry.childFilter = value;
      } else if(TAG_VALUE.equals(childTag)) {
        entry.value = value;
      } else if(TAG_PRECEDENCE.equals(childTag)) {
        try {
          entry.precedence = Integer.parseInt(value);
        } catch(NumberFormatException e) {
          error("A number is expected for "+TAG_PRECEDENCE);
        }
      } else {
        error("Unexpected tag <" + childTag + ">");
      }
    }
  }


  public static ApplicationXmlEntry parseApplicationXml(String path) {
    ApplicationXmlHandler handler = new ApplicationXmlHandler();
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser saxParser = factory.newSAXParser();
      saxParser.parse(new ByteArrayInputStream(FileUtils.readFileAsString(new File(path)).getBytes(Constants.DEFAULT_ENCODING)), handler);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return handler.getApplicationXmlEntry();
  }



  protected final static String TAG_APPLICATION = "Application";
  protected final static String TAG_APPLICATION_INFO = "ApplicationInfo";
  protected final static String TAG_AI_DESCRIPTION = "Description";
  protected final static String TAG_AI_BUILDDATE = "BuildDate";
  protected final static String TAG_AI_ISREMOTESTUB ="IsRemoteStub";
  
  protected final static String TAG_CAPACITIES = "Capacities";
  protected final static String TAG_CAPACITY = "Capacity";
  protected final static String TAG_NAME = "Name";
  protected final static String TAG_CARDINALITY = "Cardinality";
  protected final static String TAG_CAPACITY_STATE = "State";
  protected final static String TAG_CAPACITYREQUIREMENTS = "CapacityRequirements";
  protected final static String TAG_CAPACITYREQUIREMENT = "CapacityRequirement";
  protected final static String TAG_ORDERTYPE = "Ordertype";
  protected final static String TAG_ORDERTYPENAME = "OrdertypeName";
  protected final static String TAG_CAPACITYNAME = "CapacityName";
  protected final static String TAG_FILTERS = "Filters";
  protected final static String TAG_FILTER = "Filter";
  protected final static String TAG_JARFILES = "JarFiles";
  protected final static String TAG_FQFILTERCLASSNAME = "FqFilterClassname";
  protected final static String TAG_TRIGGERNAME = "TriggerName";
  protected final static String TAG_SHAREDLIBS = "SharedLibs";
  protected final static String TAG_FILTERINSTANCES = "FilterInstances";
  protected final static String TAG_FILTERINSTANCE = "FilterInstance";
  protected final static String TAG_FILTERNAME = "FilterName";
  protected final static String TAG_FILTER_CONFIG = "FilterConfigurationParameter";
  protected final static String TAG_DESCRIPTION = "Description";
  protected final static String TAG_TRIGGERINSTANCENAME = "TriggerInstanceName";
  protected final static String TAG_ORDERTYPES = "Ordertypes";
  protected final static String TAG_PLANNING = "Planning";
  protected final static String TAG_EXECUTION = "Execution";
  protected final static String TAG_CLEANUP = "Cleanup";
  protected final static String TAG_DESTINATIONKEY = "DestinationKey";
  protected final static String TAG_SHAREDLIB = "SharedLib";
  protected final static String TAG_TRIGGERS = "Triggers";
  protected final static String TAG_TRIGGER = "Trigger";
  protected final static String TAG_FQTRIGGERCLASSNAME = "FqTriggerClassname";
  protected final static String TAG_TRIGGERINSTANCES = "TriggerInstances";
  protected final static String TAG_TRIGGERINSTANCE = "TriggerInstance";
  protected final static String TAG_STARTPARAMETER = "StartParameter";
  protected final static String TAG_TRIGGER_MAXRECEIVES = "MaxReceives";
  protected final static String TAG_TRIGGER_REJECTAFTERMAXRECEIVES = "RejectAfterMaxReceives";
  protected final static String TAG_XMOMENTRIES = "XMOMEntries";
  protected final static String TAG_XMOMENTRY = "XMOMEntry";
  protected final static String TAG_FQNAME = "FqName";
  protected final static String TAG_TYPE = "Type";
  protected final static String TAG_XYNAPROPERTIES = "XynaProperties";
  protected final static String TAG_XYNAPROPERTY = "XynaProperty";
  protected final static String TAG_VALUE = "Value";
  protected final static String TAG_MONITORINGLEVELS = "MonitoringLevels";
  protected final static String TAG_MONITORINGLEVEL = "MonitoringLevel";
  protected final static String TAG_PRIORITIES = "Priorities";
  protected final static String TAG_PRIORITY = "Priority";
  protected final static String TAG_ORDERCONTEXTMAPPING = "OrderContextMapping";
  protected final static String TAG_XMOMSTORABLES = "XmomStorables";
  protected final static String TAG_XMOMSTORABLE = "XmomStorable";
  protected final static String TAG_XMOMSTORABLE_XMLNAME = "XmlName";
  protected final static String TAG_XMOMSTORABLE_PATH = "Path";
  protected final static String TAG_XMOMSTORABLE_ODSNAME = "OdsName";
  protected final static String TAG_XMOMSTORABLE_FQPATH = "FqPath";
  protected final static String TAG_XMOMSTORABLE_COLUMNNAME = "ColumnName";
  protected final static String TAG_ORDERINPUTSOURCES = "OrderInputSources";
  protected final static String TAG_ORDERINPUTSOURCE = "OrderInputSource";
  protected final static String TAG_ORDERINPUTSOURCE_PARAMETER = "Parameter";
  protected final static String TAG_KEY = "Key";
  protected final static String TAG_DOCUMENTATION = "Documentation";
  protected final static String TAG_INHERITANCERULES = "InheritanceRules";
  protected final static String TAG_INHERITANCERULE = "InheritanceRule";
  protected final static String TAG_PARAMETERTYPE = "ParameterType";
  protected final static String TAG_CHILDFILTER = "ChildFilter";
  protected final static String TAG_PRECEDENCE = "Precedence";
  protected final static String TAG_RUNTIMECONTEXT_REQUIREMENTS = "RuntimeContextRequirements";
  protected final static String TAG_RUNTIMECONTEXT_REQUIREMENT = "RuntimeContextRequirement";
  protected final static String TAG_APPLICATIONNAME = "ApplicationName";
  protected final static String TAG_VERSIONNAME = "VersionName";
  protected final static String TAG_WORKSPACENAME = "WorkspaceName";
  
  protected final static String ATTRIBUTE_APPLICATIONNAME = "applicationName";
  protected final static String ATTRIBUTE_VERISONNAME = "versionName";
  protected final static String ATTRIBUTE_XMLVERSION = "xmlVersion";
  protected final static String ATTRIBUTE_COMMENT = "comment";
  protected final static String ATTRIBUTE_FACTORYVERSION = "factoryVersion";
  protected static final String ATTRIBUTE_IMPLICIT_DEPENDENCY = "implicitDependency";
  
  protected static final String ATTRIBUTE_AI_D_LANG = "Lang";
  
  protected final static String XMLVERSION = "1.1"; //bei änderungen beachte abwärtskompatibilität: man soll auch alte applications importieren können (s.o.)
  
  
 
  

}

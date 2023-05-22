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

package com.gip.xyna.xact.trigger;



import java.io.File;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xact.exceptions.XACT_IncompatibleTriggerImplException;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;
import com.gip.xyna.xact.exceptions.XACT_LibOfTriggerImplNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_TriggerImplClassNotFoundException;
import com.gip.xyna.xact.trigger.TriggerInformation.TriggerInstanceInformation;
import com.gip.xyna.xact.trigger.XynaActivationTrigger.XmlElements;
import com.gip.xyna.xdev.xfractmod.xmdm.EnhancedStartParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListenerInstance;
import com.gip.xyna.xdev.xfractmod.xmdm.StartParameter;
import com.gip.xyna.xfmg.exceptions.XFMG_ClassLoaderRedeploymentException;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.xfctrl.classloading.TriggerClassLoader;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.AdditionalDependencyContainer;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;



public class Trigger implements Serializable {

  private static final Logger logger = CentralFactoryLogging.getLogger(Trigger.class);

  private static final long serialVersionUID = 1L;

  private final File[] jarFiles;
  private final String fqTriggerClassName;
  private final String triggerName;
  private final String[] sharedLibs;
  private final Long revision;

  public Trigger(String name, Long revision, File[] jarFiles, String fqTriggerClassName, String[] sharedLibs) {

    this.jarFiles = jarFiles;
    this.fqTriggerClassName = fqTriggerClassName;
    this.triggerName = name;
    this.sharedLibs = sharedLibs;
    this.revision = revision;
  }

  public Trigger(TriggerStorable triggerStorable) {
    this.jarFiles = triggerStorable.getJarFilesAsArray();

    this.fqTriggerClassName = triggerStorable.getFqTriggerClassName();
    this.triggerName = triggerStorable.getTriggerName();
    
    String[] sharedLibs = new String[0];
    if (triggerStorable.getSharedLibs() != null && triggerStorable.getSharedLibs().contains(":")) {
      sharedLibs = triggerStorable.getSharedLibs().split(":");
    }
    this.sharedLibs = sharedLibs;
    
    this.revision = triggerStorable.getRevision();
  }


  private transient volatile Class<EventListener<?, ?>> cachedClass;
  private transient volatile Class<StartParameter> cachedStartParameterClass;



  public void removeClassLoader() {
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                    .removeTriggerClassLoader(fqTriggerClassName, revision);
  }
  
  public void clearClassCache() {
    cachedClass = null;
    cachedStartParameterClass = null;
  }


  public void resetClassLoader() throws XACT_IncompatibleTriggerImplException, XACT_TriggerImplClassNotFoundException,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException {
    clearClassCache();
    try {
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                      .reloadTrigger(fqTriggerClassName, sharedLibs, revision);
      TriggerClassLoader tcl = getClassLoader();
      //Beim Anlegen eines noch nicht vorhandenen Triggers und beim Factory-Neustart
      //wird reload_dependencies nicht aufgerufen (weil kein alter TriggerClassLoder vorhanden ist),
      //daher hier manuell auf hasBeenDeployed setzen.
      //Ansonsten wird bei einem anschlie�enden reloadSharedLib reload_undeploy nicht
      //aufgerufen und es kommt zu einer ClassCastException, weil der classCache nicht
      //geleert wird.
      tcl.setHasBeenDeployed();
    } catch (XACT_LibOfTriggerImplNotFoundException e) {
      throw new RuntimeException(e);
    } catch (XFMG_ClassLoaderRedeploymentException e) {
      throw new RuntimeException(e);
    }
  }


  private TriggerClassLoader getClassLoader() throws XFMG_SHARED_LIB_NOT_FOUND,
                  XACT_LibOfTriggerImplNotFoundException {
    TriggerClassLoader tcl = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                    .getClassLoaderDispatcher()
                    .getTriggerClassLoaderLazyCreate(fqTriggerClassName, sharedLibs, revision);
    for (File f : jarFiles) {
      try {
        tcl.addJarFile(f.getPath());
      } catch (Ex_FileAccessException e) {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                        .removeTriggerClassLoader(fqTriggerClassName, revision);
        throw new XACT_LibOfTriggerImplNotFoundException(f.getAbsolutePath(), triggerName);
      }
    }
    return tcl;
  }


  @SuppressWarnings("unchecked")
  public synchronized Class<EventListener<?, ?>> getEventListenerClass()
                  throws XACT_TriggerImplClassNotFoundException, XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException {
    if (cachedClass == null) {
      Class<EventListener<?, ?>> c;
      try {
        TriggerClassLoader tcl = getClassLoader();
        c = (Class<EventListener<?, ?>>) tcl.loadClass(fqTriggerClassName);
      } catch (ClassNotFoundException e) {
        StringBuilder sb = new StringBuilder();
        for (File f : jarFiles) {
          sb.append(f.getPath() + ", ");
        }
        throw new XACT_TriggerImplClassNotFoundException(fqTriggerClassName, e);
      }
      cachedClass = c;
    }
    return cachedClass;
  }


  protected EventListener<?, ?> instantiateTrigger(String instanceName, long revisionOfTriggerInstance) throws XACT_IncompatibleTriggerImplException,
                  XACT_TriggerImplClassNotFoundException, XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException {
    Class<EventListener<?, ?>> c = getEventListenerClass();
    EventListener<?, ?> el = null;
    try {
      TriggerInstanceIdentification triggerInstanceId = new TriggerInstanceIdentification(triggerName, revisionOfTriggerInstance, instanceName);
        
      el = c.getConstructor().newInstance();
      el.init( triggerInstanceId );
      
      /*
      TODO konstruktor aufrufen
      Constructor<EventListener<?, ?>> constructor = c.getConstructor(TriggerIdentification.class);
      constructor.setAccessible(true);
      el = constructor.newInstance(triggerId);
      */
    } catch (IllegalAccessException e) {
      throw new XACT_IncompatibleTriggerImplException(fqTriggerClassName, e);
    } catch (InstantiationException e) {
      throw new XACT_IncompatibleTriggerImplException(fqTriggerClassName, e);
    } catch (NoClassDefFoundError e) {
      throw new XACT_LibOfTriggerImplNotFoundException(e.getMessage(), fqTriggerClassName, e);
    } catch (IllegalArgumentException e) {
      throw new XACT_IncompatibleTriggerImplException(fqTriggerClassName, e);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new XACT_IncompatibleTriggerImplException(fqTriggerClassName, e);
    } catch (NoSuchMethodException e) {
      throw new XACT_IncompatibleTriggerImplException(fqTriggerClassName, e);
    }
    return el;
  }


  @SuppressWarnings("unchecked")
  private synchronized Class<StartParameter> getStartParameterClass() throws XACT_TriggerImplClassNotFoundException, XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException {
    if (cachedStartParameterClass == null) {
      Class<EventListener<?, ?>> c = getEventListenerClass();
      ParameterizedType t = (ParameterizedType) c.getGenericSuperclass();
      Type[] ts = t.getActualTypeArguments();
      cachedStartParameterClass = (Class<StartParameter>) ts[1]; //1, weil 2ter generic parameter in eventlistener
    }
    return cachedStartParameterClass;
  }


  public String getFQTriggerClassName() {
    return fqTriggerClassName;
  }


  public String getTriggerName() {
    return triggerName;
  }


  public String[] getSharedLibs() {
    return sharedLibs;
  }


  public String getDescription() {
    try {
      Class<EventListener<?, ?>> c = getEventListenerClass();
      EventListener<?, ?> el = c.getConstructor().newInstance();
      return el.getClassDescription();
    } catch (Exception e) {
      logger.debug("Could not get description for trigger " + triggerName, e);
      return null;
    }
  }


  public StartParameter getStartParameterInstance() throws XACT_IncompatibleTriggerImplException, XACT_TriggerImplClassNotFoundException, XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfTriggerImplNotFoundException {
    try {
      return getStartParameterClass().getConstructor().newInstance();
    } catch (Exception e) {
      throw new XACT_IncompatibleTriggerImplException(fqTriggerClassName, e);
    }
  }
  
  public String[][] getStartParameterDocumentation() {
    try {
      StartParameter startParameterInstance = getStartParameterInstance();
      if( startParameterInstance != null ) {
        return startParameterInstance.getParameterDescriptions();
      }
    } catch (Throwable e) {
      logger.debug("Could not get StartParameterInstance", e);
    }
    
    return new String[0][0];
  }


  public File[] getJarFiles() {
    return jarFiles;
  }


  public AdditionalDependencyContainer getAdditionalDependencies() throws Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException {
    File xmlDefinition = new File(XynaActivationTrigger.getTriggerXmlLocationByTriggerFqClassName(fqTriggerClassName, revision));
    if (xmlDefinition.exists()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Found information on additional dependencies for trigger " + triggerName);
      }
      Document d = XMLUtils.parse(xmlDefinition.getAbsolutePath());
      if (!d.getDocumentElement().getTagName().equals(XmlElements.TRIGGER)) {
        throw new XPRC_InvalidXmlMissingRequiredElementException("root", XmlElements.TRIGGER);
      }
      return new AdditionalDependencyContainer(d.getDocumentElement());
    }
    return null;
  }

  
  public Long getRevision() {
    return revision;
  }
  
  public List<StringParameter<?>> getEnhancedStartParameter() {
    try {
      StartParameter startParameterInstance = getStartParameterInstance();
      if(startParameterInstance instanceof EnhancedStartParameter ) {
        return ((EnhancedStartParameter)startParameterInstance).getAllStringParameters();
      }
    } catch (Throwable e) {
      logger.debug("Could not get StartParameterInstance", e);
    }
    
    return null;
  }


  /**
   * Instantiieren einer EventListenerInstance: dazu geh�rt Instantiieren des EventListener, der StartParameter und
   * m�glicherweise eine Konvertierung der �bergeben StartParameter in das neue Key-Value-Format.
   * @param instanceName
   * @param startParameters
   * @param description
   * @return
   * @throws XACT_TriggerImplClassNotFoundException
   * @throws XACT_IncompatibleTriggerImplException
   * @throws XACT_InvalidStartParameterException
   * @throws XFMG_SHARED_LIB_NOT_FOUND
   * @throws XACT_LibOfTriggerImplNotFoundException
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public EventListenerInstance instantiateEventListenerInstance(String instanceName, 
                                                                String[] startParameters,
                                                                String description,
                                                                long revisionOfTriggerInstance) 
                                                                    throws XACT_TriggerImplClassNotFoundException,
                                                                    XACT_IncompatibleTriggerImplException,
                                                                    XACT_InvalidStartParameterException,
                                                                    XFMG_SHARED_LIB_NOT_FOUND,
                                                                    XACT_LibOfTriggerImplNotFoundException {
    StartParameter startParameterInstance = getStartParameterInstance();
    if( startParameterInstance == null ) {
      throw new RuntimeException("Trigger ist not correctly initialized: startParameterInstance=null");
    }
    if (logger.isTraceEnabled()) {
      logger.trace("startparameter loaded by " + startParameterInstance.getClass().getClassLoader());
    }
    List<String> startParameterList;
    if (startParameters == null) {
      startParameterList = new ArrayList<String>();
    } else {
      startParameterList = Arrays.asList(startParameters);
    }
    
    StartParameter startParameter = null;
    
    if( ! (startParameterInstance instanceof EnhancedStartParameter ) ) {
      startParameter = startParameterInstance.build(startParameters);
    } else {
      EnhancedStartParameter espi = (EnhancedStartParameter)startParameterInstance;
      Pair<StartParameter, List<String>> res = buildStartParameter(espi, startParameterList);
      startParameter = res.getFirst();
      startParameterList = res.getSecond();
    }
       
    EventListenerInstance eli =
        new EventListenerInstance(instanceName, startParameterList, instantiateTrigger(instanceName, revisionOfTriggerInstance), startParameter, description, revisionOfTriggerInstance);
    
    return eli;
  }


  /**
   * Bau der StartParameter-Instanz, m�glicherweise eine Konvertierung der �bergeben StartParameter in das neue Key-Value-Format
   * @param espi
   * @param startParameterList
   * @return
   * @throws XACT_InvalidTriggerStartParameterValueException 
   * @throws XACT_InvalidStartParameterCountException 
   */
  private Pair<StartParameter, List<String>> buildStartParameter(EnhancedStartParameter espi,
                                                                 List<String> startParameterList) throws XACT_InvalidStartParameterCountException, XACT_InvalidTriggerStartParameterValueException {
    //Versuch einer FormatErkennung: nese Format, wenn mehr als die H�lfte aller Parameter ein "=" enthalten
    int cntEqualsSign = 0;
    for( String param : startParameterList ) {
      if( param.indexOf('=') > 0 ) {
        ++cntEqualsSign;
      }
    }
    boolean hasNewFormat = cntEqualsSign > startParameterList.size()/2;
    
    StartParameter startParameter = null;
    Pair<StartParameter, Exception> res = tryBuildStartParameter( espi, startParameterList, hasNewFormat );
    if( res.getFirst() == null ) {
      //StartParameter konnten nicht gebaut werden
      //nochmal versuchen mit negierter hasNewFormat-Erkennung
      Pair<StartParameter, Exception> resTry = tryBuildStartParameter( espi, startParameterList, ! hasNewFormat );
      if( resTry.getFirst() == null ) {
        //Auch so konnte StartParameter nicht erzeugt werden
        String format = hasNewFormat?"new":"old";
        logger.warn( "Could not understand "+(hasNewFormat?"new":"old")+"format-parameters "+startParameterList+", neither new nor old format");
        logger.warn( format +" format exception", res.getSecond() );
        logger.warn( "other format exception", resTry.getSecond() );
        Exception mainException = res.getSecond();
        String parameterName = null;
        if( mainException instanceof XACT_InvalidTriggerStartParameterValueException ) {
          throw (XACT_InvalidTriggerStartParameterValueException) mainException;
        } else if( mainException instanceof StringParameterParsingException ) {
          parameterName = ((StringParameterParsingException) mainException).getParameterName();
        } else {
          parameterName = "unknown";
        }
        throw new XACT_InvalidTriggerStartParameterValueException( parameterName, mainException );
      } else {
        //Formaterkennung war falsch, dies korrigieren
        hasNewFormat = ! hasNewFormat;
        startParameter = resTry.getFirst();
      }
    } else {
      startParameter = res.getFirst();
    }
    return Pair.of( startParameter, hasNewFormat ? startParameterList : espi.convertToNewParameters(startParameterList) );
  }

  private Pair<StartParameter, Exception> tryBuildStartParameter(EnhancedStartParameter espi, List<String> startParameters, boolean hasNewFormat) {
    StartParameter startParameter = null;
    Exception exception = null;
    List<StringParameter<?>> allParameters = espi.getAllStringParameters();
    try {
      List<String> params = null;
      if( hasNewFormat ) {
        params = startParameters;
      } else {
        params = espi.convertToNewParameters(startParameters);
      }
      Map<String, Object> paramMap = StringParameter.paramListToMap(allParameters, params);
      startParameter = espi.build(paramMap);
    } catch (Exception e) {
      exception = e;
    }
    return Pair.of(startParameter,exception);
  }

  public static TriggerInformation getTriggerInfo(Trigger trigger, TriggerStorable triggerStorable,
                                                  Collection<TriggerInstanceStorable> triggerInstances) {
    
    String triggerName = triggerStorable.getTriggerName();
    Long triggerRevision = triggerStorable.getRevision();
    
    List<TriggerInstanceInformation> triggerInstanceInfo = 
        getTriggerInstanceInformation(triggerInstances, trigger, triggerName, triggerRevision);
   
    AdditionalDependencyContainer additionalDeps = null;
    try {
      additionalDeps = trigger.getAdditionalDependencies();
    } catch (Exception  e) {
      throw new RuntimeException("Could not parse additional dependencies of trigger " + trigger.getTriggerName(), e);
    }
    RuntimeContext runtimeContext = getRuntimeContext(triggerRevision);
    return new TriggerInformation(triggerName,
                                  triggerStorable.getFqTriggerClassName(),
                                  trigger.getStartParameterDocumentation(),
                                  trigger.getEnhancedStartParameter(),
                                  trigger.getDescription(),
                                  triggerInstanceInfo,
                                  runtimeContext,
                                  triggerStorable.getStateAsEnum(),
                                  triggerStorable.getErrorCause(),
                                  triggerStorable.getSharedLibsAsArray(),
                                  additionalDeps);
  }

  private static RuntimeContext getRuntimeContext(Long revision) {
    try {
      return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                      .getRevisionManagement().getRuntimeContext(revision);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
      logger.warn("Could not find workspace or application name and version name for revision " + revision, e1);
      return new Application("not found", "N/A");
    }
  }


  private static List<TriggerInstanceInformation> getTriggerInstanceInformation(Collection<TriggerInstanceStorable> triggerInstances,
                                                                                Trigger trigger, String triggerName, Long triggerRevision) {
    List<TriggerInstanceInformation> triggerInstanceInfo = new ArrayList<TriggerInstanceInformation>();
    Set<Long> revisions = new HashSet<Long>();
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
        .getParentRevisionsRecursivly(triggerRevision, revisions);
    revisions.add(triggerRevision);
    for (TriggerInstanceStorable triggerInstanceStorable : triggerInstances) {
      if (triggerInstanceStorable.getTriggerName().equals(triggerName) && revisions.contains(triggerInstanceStorable.getRevision())) {
        triggerInstanceInfo.add(new TriggerInstanceInformation(triggerInstanceStorable.getTriggerInstanceName(),
                                                               triggerInstanceStorable.getTriggerName(),
                                                               triggerInstanceStorable.getDescription(),
                                                               triggerInstanceStorable.getStateAsEnum(),
                                                               triggerInstanceStorable.getStartParameters(),
                                                               triggerInstanceStorable.getStartParameter(),
                                                               triggerInstanceStorable.getErrorCause(),
                                                               triggerInstanceStorable.getRevision()));
      }
    }
    return triggerInstanceInfo;
  }

  
}

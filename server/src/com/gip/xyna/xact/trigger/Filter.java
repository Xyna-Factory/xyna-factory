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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.misc.ComparatorUtils;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xact.exceptions.XACT_DuplicateFilterDefinitionException;
import com.gip.xyna.xact.exceptions.XACT_FilterImplClassNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_IncompatibleFilterImplException;
import com.gip.xyna.xact.exceptions.XACT_LibOfFilterImplNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_WrongTriggerException;
import com.gip.xyna.xact.trigger.FilterInformation.FilterInstanceInformation;
import com.gip.xyna.xact.trigger.XynaActivationTrigger.XmlElements;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.FilterConfigurationParameter;
import com.gip.xyna.xfmg.exceptions.XFMG_FilterClassLoaderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.exceptions.XFMG_TriggerClassLoaderNotFoundException;
import com.gip.xyna.xfmg.xfctrl.classloading.FilterClassLoader;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlMissingRequiredElementException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.AdditionalDependencyContainer;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


public class Filter implements Serializable {

  private static final Logger logger = CentralFactoryLogging.getLogger(Filter.class);

  private static final long serialVersionUID = 1L;

  private final String name;
  private final File[] jarFiles;
  private final String fqFilterClassName;
  private final Trigger trigger;
  private final String[] sharedLibs;
  private final String description;
  private final Long revision;
  private final String triggerName; //f�r den fall, dass trigger null ist


  public Filter(String name, Long revision, File[] jarFiles, String fqFilterClassName, Trigger t, String triggerName, String[] sharedLibs,
                String description) {
    this.name = name;
    this.jarFiles = jarFiles;
    this.fqFilterClassName = fqFilterClassName;
    this.trigger = t;
    this.sharedLibs = sharedLibs;
    this.description = description;
    this.revision = revision;
    this.triggerName = triggerName;
  }

  public Filter(FilterStorable storable, Trigger trigger, String triggerName) {
    this.name = storable.getFilterName();
    this.jarFiles = storable.getJarFilesAsArray();
    this.fqFilterClassName = storable.getFqFilterClassName();
    this.trigger = trigger;
    this.sharedLibs = storable.getSharedLibsArray();
    this.description = storable.getDescription();
    this.revision = storable.getRevision();
    this.triggerName = triggerName;
  }

  public void removeClassLoader() {
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
        .removeFilterClassLoader(fqFilterClassName, revision);
    
    //zugeh�rige OutdatedFilterClassLoader entfernen
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
        .removeOutdatedFilterClassLoaders(fqFilterClassName, revision);
  }
  
  
  public void resetClassLoader() {
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                    .removeFilterClassLoader(fqFilterClassName, revision);
    try {
      getClassLoader(null);
    } catch (XFMG_TriggerClassLoaderNotFoundException e) {
      throw new RuntimeException(e); //FIXME sind die runtimeexceptions ok?
    } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
      throw new RuntimeException(e);
    } catch (XACT_LibOfFilterImplNotFoundException e) {
      throw new RuntimeException(e);
    }
  }


  private FilterClassLoader getClassLoader(Long parentRevision) throws XFMG_TriggerClassLoaderNotFoundException,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfFilterImplNotFoundException {

    FilterClassLoader fcl;
    if(parentRevision == null) {
      fcl = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                    .getClassLoaderDispatcher().getFilterClassLoaderLazyCreate(fqFilterClassName,
                                                                               trigger.getFQTriggerClassName(),
                                                                               sharedLibs, revision);
    } else {
      fcl = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                      .getClassLoaderDispatcher().getOutdatedFilterClassLoaderLazyCreate(fqFilterClassName, 
                                                      trigger.getFQTriggerClassName(), sharedLibs,
                                                      revision, parentRevision);
    }
    for (File f : jarFiles) {
      try {
        fcl.addJarFile(f.getPath());
      } catch (Ex_FileAccessException e) {
        if (parentRevision != null) { //OutdatedFilter
          //beim Erstellen des "ursrp�nglichen" Filters, muss das jar noch vorhanden gewesen sein
          throw new RuntimeException("Library of outdated filter could not be found", e);
        }
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
            .removeFilterClassLoader(fqFilterClassName, revision);
        throw new XACT_LibOfFilterImplNotFoundException(f.getAbsolutePath(), name);
      }
    }
    return fcl;
  }

  public void setHasBeenDeployed(Long parentRevision) throws XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfFilterImplNotFoundException {
    try {
      getClassLoader(parentRevision).setHasBeenDeployed();
    } catch (XFMG_TriggerClassLoaderNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public void validate(Filter oldFilter) throws XACT_WrongTriggerException, XACT_FilterImplClassNotFoundException,
      XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfFilterImplNotFoundException, XACT_DuplicateFilterDefinitionException {
    if (oldFilter == null) {
      //alter wird nicht �berschrieben. check, ob bereits gleicher fqclassname - filter existiert.
      try {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
            .getFilterClassLoader(fqFilterClassName, revision, false);
        throw new XACT_DuplicateFilterDefinitionException(fqFilterClassName);
      } catch (XFMG_FilterClassLoaderNotFoundException e) {
        //ok
      }
    }
    boolean hadError = true;
    try {
      resetClassLoader();

      FilterClassLoader fcl = getClassLoader(null);
      Class<ConnectionFilter<?>> c = (Class<ConnectionFilter<?>>) fcl.loadClass(fqFilterClassName);
      try {
        c.getGenericSuperclass(); //versucht die triggerconnection �ber den classloader zu laden
      } catch (TypeNotPresentException e) {
        throw new XACT_WrongTriggerException(trigger.getTriggerName(), e);
      }
      hadError = false;
      
      setHasBeenDeployed(null);
    } catch (ClassNotFoundException e) {
      throw new XACT_FilterImplClassNotFoundException(fqFilterClassName, e);
    } catch (XFMG_TriggerClassLoaderNotFoundException e) {
      throw new RuntimeException(e);
    } finally {
      if (hadError) {
        if(oldFilter != null) {
          oldFilter.removeClassLoader();
        }
        removeClassLoader();
      }
    }
  }


  protected ConnectionFilter<?> instantiateFilter(long filterInstanceRevision) throws XACT_FilterImplClassNotFoundException,
                  XACT_IncompatibleFilterImplException, XFMG_SHARED_LIB_NOT_FOUND,
                  XACT_LibOfFilterImplNotFoundException {
    return instantiateFilter(filterInstanceRevision, null);
  }
  
  
  protected ConnectionFilter<?> instantiateFilter(long filterInstanceRevision, Long parentRevision) throws XACT_FilterImplClassNotFoundException,
                  XACT_IncompatibleFilterImplException, XFMG_SHARED_LIB_NOT_FOUND,
                  XACT_LibOfFilterImplNotFoundException {
    Class<ConnectionFilter<?>> c;
    try {
      FilterClassLoader fcl = getClassLoader(parentRevision);
      c = (Class<ConnectionFilter<?>>) fcl.loadClass(fqFilterClassName);
    } catch (ClassNotFoundException e) {
      throw new XACT_FilterImplClassNotFoundException(fqFilterClassName, e);
    } catch (XFMG_TriggerClassLoaderNotFoundException e) {
      throw new RuntimeException(e);
    }
    ConnectionFilter<?> cf;
    try {
      cf = c.getConstructor().newInstance();
    } catch (IllegalAccessException e) {
      throw new XACT_IncompatibleFilterImplException(fqFilterClassName, e);
    } catch (InstantiationException e) {
      throw new XACT_IncompatibleFilterImplException(fqFilterClassName, e);
    } catch (IllegalArgumentException e) {
      throw new XACT_IncompatibleFilterImplException(fqFilterClassName, e);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new XACT_IncompatibleFilterImplException(fqFilterClassName, e);
    } catch (NoSuchMethodException e) {
      throw new XACT_IncompatibleFilterImplException(fqFilterClassName, e);
    }
    cf.setRevision(filterInstanceRevision);
    return cf;
  }


  public String[] getSharedLibs() {
    return sharedLibs;
  }


  public String getName() {
    return name;
  }


  public Trigger getTrigger() {
    return trigger;
  }

  public String getTriggerName() {
    return triggerName;
  }

  public String getDescription() {
    return description;
  }


  public String getFQFilterClassName() {
    return fqFilterClassName;
  }

  
  public File[] getJarFiles() {
    return jarFiles;
  }


  public AdditionalDependencyContainer getAdditionalDependencies() throws Ex_FileAccessException, XPRC_XmlParsingException,
      XPRC_InvalidXmlMissingRequiredElementException {
    return getAdditionalDependencies(fqFilterClassName, revision);
  }


  public static AdditionalDependencyContainer getAdditionalDependencies(String fqFilterClassName, long revision)
      throws Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InvalidXmlMissingRequiredElementException {
    File xmlDefinition = new File(XynaActivationTrigger.getFilterXmlLocationByFqFilterClassName(fqFilterClassName, revision));
    if (xmlDefinition.exists()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Found information on additional dependencies for filter " + fqFilterClassName);
      }
      Document d = XMLUtils.parse(xmlDefinition.getAbsolutePath());
      if (!d.getDocumentElement().getTagName().equals(XmlElements.FILTER)) {
        throw new XPRC_InvalidXmlMissingRequiredElementException("root", XmlElements.FILTER);
      }
      return new AdditionalDependencyContainer(d.getDocumentElement());
    }

    return null;
  }
  
  public Long getRevision() {
    return revision;
  }

  public List<StringParameter<?>> getConfigurationParameter(long revision) throws XACT_FilterImplClassNotFoundException, XACT_IncompatibleFilterImplException, XFMG_SHARED_LIB_NOT_FOUND, XACT_LibOfFilterImplNotFoundException {
    ConnectionFilter<?> cf = instantiateFilter(revision);
    FilterConfigurationParameter params = cf.createFilterConfigurationTemplate();
    if( params == null ) {
      return null;
    } else {
      return params.getAllStringParameters();
    }
  }

  public static FilterInformation getFilterInformation(FilterStorable filterStorable, 
      Collection<FilterInstanceStorable> filterInstances, XynaActivationTrigger xat) {
    List<FilterInstanceInformation> filterInstanceInformation = new ArrayList<>();
    for (FilterInstanceStorable filterInstanceStorable : filterInstances) {
     filterInstanceInformation.add(new FilterInstanceInformation(filterInstanceStorable));
    }
    Collections.sort(filterInstanceInformation, filterInstanceComparator);

    RuntimeContext runtimeContext;
    try {
      runtimeContext = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                      .getRevisionManagement().getRuntimeContext(filterStorable.getRevision());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
      logger.warn("Could not find workspace or application name and version name for revision " + filterStorable.getRevision(), e1);
      runtimeContext = new Application("not found", "N/A");
    }

    AdditionalDependencyContainer additionalDeps;
    try {
      additionalDeps = Filter.getAdditionalDependencies(filterStorable.getFqFilterClassName(), filterStorable.getRevision());
    } catch (Exception e) {
      throw new RuntimeException("Could not get additional dependencies of filter " + filterStorable.getFilterName(), e);
    }
    
    Filter filter = null;
    List<StringParameter<?>> configParameter = null;
    try {
      filter = xat.getFilter(filterStorable.getRevision(), filterStorable.getFilterName(), false);
      if (filter.getTrigger() != null) {
        configParameter = filter.getConfigurationParameter(filterStorable.getRevision());
      }
    } catch (Throwable e) {
      //noclassdeffounderror ist zb typischer fehler
      Department.handleThrowable(e);
      logger.warn("Could not get filter " + filterStorable.getFilterName() + " or configuration parameter.", e);
    }
    
    return new FilterInformation(filterStorable, filterInstanceInformation, runtimeContext, 
        additionalDeps, configParameter );
  }

  private static final Comparator<FilterInstanceInformation> filterInstanceComparator = new Comparator<FilterInstanceInformation>() {

    public int compare(FilterInstanceInformation o1, FilterInstanceInformation o2) {
      int comp = ComparatorUtils.compareIgnoreCaseNullAware(o1.getFilterInstanceName(), o2.getFilterInstanceName(), false );
      if( comp == 0 ) {
        comp = ComparatorUtils.compareNullAware(o1.getRevision(), o2.getRevision(), false);
      }
      return comp;
    }
    
  };



}

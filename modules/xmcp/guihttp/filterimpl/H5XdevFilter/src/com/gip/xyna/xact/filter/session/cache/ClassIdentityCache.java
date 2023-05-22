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

package com.gip.xyna.xact.filter.session.cache;

import java.util.Optional;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.LruCache;
import com.gip.xyna.xact.filter.H5XdevFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.exceptions.XFMG_ExceptionClassLoaderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_MDMObjectClassLoaderNotFoundException;
import com.gip.xyna.xfmg.exceptions.XFMG_WFClassLoaderNotFoundException;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.MDMClassLoaderXMLBase;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;

public abstract class ClassIdentityCache <T>{

  private static final Logger logger = CentralFactoryLogging.getLogger(ClassIdentityCache.class);
  protected static final Long INVALID_REVISION = -10l;
  
  protected LruCache<Class<? extends GeneralXynaObject>, T> actualCache;


  private ClassLoaderDispatcher cld;
  
  public ClassIdentityCache() {
    cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
    Integer max = H5XdevFilter.GENERATION_BASE_CACHE_SIZE.get();
    if (max == null || max <= 0) {
      max = H5XdevFilter.GENERATION_BASE_CACHE_SIZE.getDefaultValue();
    }
    actualCache = new LruCache<Class<? extends GeneralXynaObject>, T>(max);
  }


  public Optional<T> getFromCacheNoInsert(Class<? extends GeneralXynaObject> clazz) {
    T tsgb = getFromActualCache(clazz);

    Optional<T> result = null;
    result = tsgb == null ? Optional.empty() : Optional.of(tsgb);

    return result;
  }
  
  public Optional<T> getFromCacheNoInsert(String fqXmlName, Long revision) {
    if(logger.isDebugEnabled()) {
      logger.debug("cache request (no insert): " + fqXmlName + " in '" + revision + "'");
    }
    Class<? extends GeneralXynaObject> clazz = loadClass(fqXmlName, revision);
    T tsgb = getFromActualCache(clazz);
    
    Optional<T> result = null;
    result = tsgb == null ? Optional.empty() : Optional.of(tsgb);
      
    return result;
  }
  
  
  private T getFromActualCache(Class<? extends GeneralXynaObject> clazz) {
    T tsgb = null;
    if (clazz != null) {
      synchronized (actualCache) {
        tsgb = actualCache.get(clazz);
      }
    }
    return tsgb;
  }


  public T getFromCache(Class<? extends GeneralXynaObject> clazz) {
    T tsgb = getFromActualCache(clazz);
    
    if (tsgb == null) {  
      ClassLoader cl =  clazz.getClassLoader();
      String fqXmlName;
      Long revision;
      if(!(cl instanceof MDMClassLoaderXMLBase)) {
        fqXmlName = GenerationBase.getXmlNameForReservedClass(clazz);
        revision = INVALID_REVISION;
      } else {
        fqXmlName = ((MDMClassLoaderXMLBase)cl).getOriginalXmlName();
        revision = ((MDMClassLoaderXMLBase)cl).getRevision();
      }
      return addOrReplaceEntry(fqXmlName, revision, clazz); //object not in cache
    }
    
    return tsgb;
  }
  
  public T getFromCache(String fqXmlName, Long revision) {
    if(logger.isDebugEnabled()) {
      logger.debug("cache request: " + fqXmlName + " in '" + revision + "'");
    }
    T tsgb = null;
    Class<? extends GeneralXynaObject> clazz = loadClass(fqXmlName, revision);

    if (clazz != null) {
      synchronized (actualCache) {
        tsgb = actualCache.get(clazz);
      }
      if (tsgb == null) {
        return addOrReplaceEntry(fqXmlName, revision, clazz); //object not in cache
      }
    }
    
    return tsgb;
  }
  
  @SuppressWarnings("unchecked")
  private Class<? extends GeneralXynaObject> loadClass(String fqXmlName, Long revision){
    String fqClassName = determineFqClassName(fqXmlName);
    
    try {
      return (Class<? extends GeneralXynaObject>) cld.getMDMClassLoader(fqClassName, revision, true).loadClass(fqXmlName);
    } catch (XFMG_MDMObjectClassLoaderNotFoundException | ClassNotFoundException e) {
      try {
        return (Class<? extends GeneralXynaObject>) cld.getWFClassLoader(fqClassName, revision, true).loadClass(fqXmlName);
      } catch (XFMG_WFClassLoaderNotFoundException | ClassNotFoundException e2) {
        try {
          return (Class<? extends GeneralXynaObject>) cld.getExceptionClassLoader(fqClassName, revision, true).loadClass(fqXmlName);
        } catch (XFMG_ExceptionClassLoaderNotFoundException | ClassNotFoundException e1) {
        }
      }
    }
    

    return null;
  }

  private String determineFqClassName(String fqXmlName) {
    try {
      return GenerationBase.transformNameForJava(fqXmlName);
    } catch (XPRC_InvalidPackageNameException e) {
      return fqXmlName;
    }
  }
  
  protected abstract T addOrReplaceEntry(String fqXmlName, Long revision, Class<? extends GeneralXynaObject> clazz);
}

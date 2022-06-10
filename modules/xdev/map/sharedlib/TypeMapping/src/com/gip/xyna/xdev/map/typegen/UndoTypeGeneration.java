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
package com.gip.xyna.xdev.map.typegen;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.xdev.map.TypeMappingCache;
import com.gip.xyna.xdev.map.TypeMappingEntry;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import com.gip.xyna.xprc.xpce.WorkflowEngine;


/**
 *
 */
public class UndoTypeGeneration {

  private String targetId;
  private TypeMappingCache typeMappingCache;
  private Collection<TypeMappingEntry> typeMappingEntries;
  private List<String> xmomTypes;
  
  public UndoTypeGeneration(String targetId) {
    this.targetId = targetId;
  }
  
  public void readTypeMappingEntries() throws PersistenceLayerException {
    this.typeMappingCache = new TypeMappingCache();
    typeMappingEntries = typeMappingCache.readTypeMappingEntries(targetId);
  }

  public void deleteTypeMappingEntries() throws PersistenceLayerException {
    typeMappingCache.deleteAll(typeMappingEntries);
  }
  
  public List<String> extractXmomTypes() {
    
    HashSet<String> fqClassNames = new HashSet<String>();
    CollectionUtils.transform(typeMappingEntries, new TypeMappingEntryToFqClassName(), fqClassNames);
    List<Class<? extends XynaObject>> classes = CollectionUtils.transformAndSkipNull(fqClassNames, new FqClassNameToXynaObjectClass() );
    
    xmomTypes = CollectionUtils.transformAndSkipNull(classes, new XynaObjectClassToFqXmlName() );
    
    return xmomTypes;
  }
  
  public void deleteTypes() {
    WorkflowEngine workflowEngine = XynaFactory.getInstance().getProcessing().getWorkflowEngine();
    boolean disableChecks = true;
    boolean recursivlyUndeployIfDeployedAndDependenciesExist = true;
    boolean deleteDependencies = false;
   
    for( String originalFqName : xmomTypes ) {
      try {
        workflowEngine.deleteDatatype(originalFqName, disableChecks, 
                                    recursivlyUndeployIfDeployedAndDependenciesExist, deleteDependencies);
      } catch ( Exception e ) { 
        //XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidPackageNameException,
        //XPRC_InternalObjectMayNotBeUndeployedException, XPRC_MDMUndeploymentException, XPRC_ExclusiveDeploymentInProgress, XPRC_EXISTING_DEPENDENCY_ONUNDEPLOYMENT
        throw new RuntimeException(e); //FIXME
      }
    }

  }


  private static class TypeMappingEntryToFqClassName implements CollectionUtils.Transformation<TypeMappingEntry, String> {

    public String transform(TypeMappingEntry tme) {
      String fqClassName = tme.getValue();
      int idx = fqClassName.indexOf(':');
      if( idx < 0 )  {
        return fqClassName;
      } else {
        return fqClassName.substring(0,idx);
      }
    }
    
  }
  
  private static class FqClassNameToXynaObjectClass implements CollectionUtils.Transformation<String, Class<? extends XynaObject>> {
    
    private ClassLoaderDispatcher classLoaderDispatcher;
    private Long revision;

    public FqClassNameToXynaObjectClass() {
      ClassLoader cl = getClass().getClassLoader();
      if( cl instanceof ClassLoaderBase ) {
        revision = ((ClassLoaderBase)cl).getRevision();
      } else {
        revision = Long.valueOf(-1);
      }
      classLoaderDispatcher = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
    }
    
    public Class<? extends XynaObject> transform(String fqClassName) {
      if (fqClassName == null) {
        return null;
      }
      Class<? extends XynaObject> clazz;
      try {
        clazz = classLoaderDispatcher.loadMDMClass(fqClassName, false, null, null, revision);
        return clazz;
      } catch (ClassNotFoundException e) {
        return null;
      }
    }
      
  }
  
  private static class XynaObjectClassToFqXmlName implements CollectionUtils.Transformation<Class<? extends XynaObject>, String> {

    public String transform(Class<? extends XynaObject> clazz) {   
      XynaObjectAnnotation xoa = clazz.getAnnotation(XynaObjectAnnotation.class);
      if( xoa != null ) {
        return xoa.fqXmlName();
      } else {
        return null;
      }
    }
    
  }



}

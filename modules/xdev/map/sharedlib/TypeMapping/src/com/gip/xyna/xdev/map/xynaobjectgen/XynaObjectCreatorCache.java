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
package com.gip.xyna.xdev.map.xynaobjectgen;

import java.util.Collection;
import java.util.HashMap;

import org.w3c.dom.Node;

import com.gip.xyna.xdev.map.TypeMappingCache;
import com.gip.xyna.xdev.map.TypeMappingEntry;
import com.gip.xyna.xdev.map.exceptions.XynaObjectCreationException;
import com.gip.xyna.xdev.map.xynaobjectgen.XynaObjectCreator.XOCExceptionType;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;

/**
 *
 */
public class XynaObjectCreatorCache {


  public static class TypeMappingCacheForTarget {

    private TypeMappingCache typeMappingCache;
    private String targetId;

    public TypeMappingCacheForTarget(TypeMappingCache typeMappingCache, String targetId) {
      this.typeMappingCache = typeMappingCache;
      this.targetId = targetId;
    }

    public String lookup(String key) {
      return typeMappingCache.lookup(targetId, key);
    }

     public Class<?> lookupClass(ClassLoader classLoader, String typeName) {
      return typeMappingCache.lookupClass(classLoader, typeName);
    }

    public String lookupReverse(String value) {
      return typeMappingCache.lookupReverse(targetId, value);
    }

    public String getTargetId() {
      return targetId;
    }

  }


  private HashMap<String,XynaObjectCreator> xynaObjectCreators = new HashMap<String,XynaObjectCreator>();
  private TypeMappingCache typeMappingCache;
  private XOCStrategy xocStrategy;

  public XynaObjectCreatorCache() throws PersistenceLayerException {
    xynaObjectCreators = new HashMap<String,XynaObjectCreator>();
    typeMappingCache = new TypeMappingCache();
    xocStrategy = new XOCStrategy();
  }

  public XynaObjectCreatorCache( Collection<TypeMappingEntry> typeMappingEntries ) {
    xynaObjectCreators = new HashMap<String,XynaObjectCreator>();
    typeMappingCache = new TypeMappingCache(typeMappingEntries);
    xocStrategy = new XOCStrategy();
  }

  public void setXocStrategy(XOCStrategy xocStrategy) {
    this.xocStrategy = xocStrategy;
  }

  public XOCStrategy getXocStrategy() {
    return xocStrategy;
  }

  public TypeMappingCache getTypeMappingCache() {
    return typeMappingCache;
  }

  public void setTargetId(String targetId) {
    xocStrategy.setTargetId(targetId);
  }


  public void reloadCache() throws PersistenceLayerException {
    xynaObjectCreators.clear();
    typeMappingCache.reloadCache();
  }
  public void reloadCache( Collection<TypeMappingEntry> typeMappingEntries ) {
    xynaObjectCreators.clear();
    typeMappingCache.reloadCache(typeMappingEntries);
  }

  public XynaObjectCreator getXynaObjectCreator(String defaultTargetId, String defaultNamespace, Node node, String parentXynaObjectName) throws XynaObjectCreationException {
    String targetId = xocStrategy.getTargetId(node, defaultTargetId);
    String namespace = xocStrategy.getNamespace(node, defaultNamespace);
    if( targetId == null ) {
      throw new XynaObjectCreationException(node.getNodeName(), XOCExceptionType.Lookup.name(), "targetId->null" );
    }
    if( namespace == null ) {
      throw new XynaObjectCreationException(node.getNodeName(), XOCExceptionType.Lookup.name(), "namespace->null" );
    }

    //gleicher elementname könnte im xml in unterschiedlichen typen auftauchen
    //aber im gleichen parent XynaObject ist er unique
    //so wird der cache den gleichen xynaobjectcreator mehrfach enthalten - pech.
    String key = targetId+"_"+namespace+":" + parentXynaObjectName + ":" +node.getNodeName();
    XynaObjectCreator xoc = xynaObjectCreators.get(key);
    if( xoc == null ) {
      xoc = new XynaObjectCreator(targetId, namespace, this);
      xynaObjectCreators.put(key,xoc);
    }
    return xoc;
  }

  public XynaObjectCreator getXynaObjectCreator( Node node, String parentXynaObjectName) throws XynaObjectCreationException {
    return getXynaObjectCreator( null, null, node, parentXynaObjectName);
  }

  public XynaObject createXynaObject(Node node) throws XynaObjectCreationException {
    XynaObjectCreator xoc = getXynaObjectCreator(node, "root");
    XynaObject xo = xoc.createXynaObject(node);
    return xo;
  }



}

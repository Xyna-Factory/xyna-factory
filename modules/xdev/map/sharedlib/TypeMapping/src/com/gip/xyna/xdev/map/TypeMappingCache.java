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
package com.gip.xyna.xdev.map;



import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.gip.xyna.BijectiveMap;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;



public class TypeMappingCache {

  Map<String, BijectiveMap<String, String>> cache;


  public TypeMappingCache() throws PersistenceLayerException {
    ODSImpl.getInstance().registerStorable(TypeMappingEntry.class);
  }
  
  public TypeMappingCache(Collection<TypeMappingEntry> tmes) {
    cache = createCache(tmes);
  }
  
  public void reloadCache() throws PersistenceLayerException {
    cache = loadCache();
  }
  
  public void reloadCache(Collection<TypeMappingEntry> tmes) {
    cache = createCache(tmes);
  }

  /**
   * versucht eine klasse zu laden, deren name der value zu dem übergebenen key in der mapping-tabelle ist.
   * zum laden wird der übergebene classloader verwendet.
   */
  public Class<? extends XynaObject> lookupClass(ClassLoader classLoader, String targetId, String key) {
    String typeName = lookup(targetId, key);
    if (typeName == null) {
      return null;
    }
    return lookupClass(classLoader, typeName);
  }
  
  /**
   * versucht eine klasse zu laden, deren name der value zu dem übergebenen key in der mapping-tabelle ist.
   * zum laden wird der übergebene classloader verwendet.
   */
  public Class<? extends XynaObject> lookupClass(ClassLoader classLoader, String typeName) {
    if (typeName == null) {
      return null;
    }
    //FIXME lohnt sich es, das zu cachen?
    Class<?> clazz;
    try {
      clazz = classLoader.loadClass(typeName);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    if (!XynaObject.class.isAssignableFrom(clazz)) {
      throw new RuntimeException("invalid class name: " + typeName + " is not a xynaobject class.");
    }
    @SuppressWarnings("unchecked")
    Class<? extends XynaObject> clazzXO = (Class<? extends XynaObject>) clazz;
    return clazzXO;
  }

  
  

  public String lookup(String targetId, String key) {
    if (cache == null) {
      try {
        cache = loadCache();
      } catch (PersistenceLayerException e) {
        return null;
      }
    }
    BijectiveMap<String, String> targetMap = cache.get(targetId);
    if (targetMap == null) {
      throw new RuntimeException("no mapping known for targetId " + targetId);
    }
    return targetMap.get(key);
  }

  private Map<String, BijectiveMap<String, String>> loadCache() throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      Collection<TypeMappingEntry> l = con.loadCollection(TypeMappingEntry.class);
      return createCache(l);
    } finally {
      con.closeConnection();
    }
  }

  private Map<String, BijectiveMap<String, String>> createCache(Collection<TypeMappingEntry> tmes) {
    Map<String, BijectiveMap<String, String>> map = new ConcurrentHashMap<String, BijectiveMap<String, String>>();
    for (TypeMappingEntry tme : tmes) {
      BijectiveMap<String, String> existingMap = map.get(tme.getId());
      if (existingMap == null) {
        existingMap = new BijectiveMap<String, String>();
        map.put(tme.getId(), existingMap);
      }
      existingMap.put(tme.getKeyv(), tme.getValue());
    }
    return map;
  }

  public String lookupReverse(String targetId, String value) {
    if (cache == null) {
      try {
        cache = loadCache();
      } catch (PersistenceLayerException e) {
        return null;
      }
    }
    BijectiveMap<String, String> map = cache.get(targetId);
    if( map == null ) {
      throw new IllegalArgumentException("Unknown targetId \""+targetId+"\", known targetIds: "+cache.keySet() );
    }
    return map.getInverse(value);
  }


  private PreparedQuery<TypeMappingEntry> queryKeyId;
  private PreparedQuery<TypeMappingEntry> queryId;


  private PreparedQuery<TypeMappingEntry> getQueryKeyId(ODSConnection con) throws PersistenceLayerException {
    if (queryKeyId == null) {
      queryKeyId =
          con.prepareQuery(new Query<TypeMappingEntry>("select * from " + TypeMappingEntry.TABLENAME + " where "
              + TypeMappingEntry.COL_KEY + " = ? and " + TypeMappingEntry.COL_ID + " = ?", TypeMappingEntry.reader));
    }
    return queryKeyId;
  }
  
  private PreparedQuery<TypeMappingEntry> getQueryId(ODSConnection con) throws PersistenceLayerException {
    if (queryId == null) {
      queryId =
          con.prepareQuery(new Query<TypeMappingEntry>("select * from " + TypeMappingEntry.TABLENAME + " where "
              + TypeMappingEntry.COL_ID + " = ?", TypeMappingEntry.reader));
    }
    return queryId;
  }

  
  public void store(List<TypeMappingEntry> list) throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      for (TypeMappingEntry entry : list) {
        TypeMappingEntry e = con.queryOneRow(getQueryKeyId(con), new Parameter(entry.getKeyv(), entry.getId()));
        if (e != null) {
          entry.setPk(e.getPk());
        }
      }
      con.persistCollection(list);
      con.commit();
    } finally {
      con.closeConnection();
    }
    cache = null;
  }

  public Collection<TypeMappingEntry> readTypeMappingEntries(String id) throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      Collection<TypeMappingEntry> tmes = con.query( getQueryId(con),  new Parameter(id), -1); 
      return tmes;
    } finally {
      con.closeConnection();
    }
  }

  public void deleteAll(Collection<TypeMappingEntry> typeMappingEntries) throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      con.delete(typeMappingEntries);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }
  

  
}

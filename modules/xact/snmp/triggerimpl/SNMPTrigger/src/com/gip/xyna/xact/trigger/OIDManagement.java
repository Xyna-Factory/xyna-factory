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
package com.gip.xyna.xact.trigger;



import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPathImpl;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;



public class OIDManagement {


  private static final Logger logger = CentralFactoryLogging.getLogger(OIDManagement.class);

  private static OIDManagement _instance = new OIDManagement();

  public static PreparedQueryCache queryCache = new PreparedQueryCache(); // why would this be public and static?!
  public static final String SQL_GET_OID_FOR_NAME = "SELECT * FROM " + OIDNameMappingStorable.TABLE_NAME + " WHERE "
      + OIDNameMappingStorable.COL_OBJECT_NAME + " = ?";
  public static final String SQL_GET_OID_FOR_NAME_FOR_UPDATE = "SELECT * FROM " + OIDNameMappingStorable.TABLE_NAME
      + " WHERE " + OIDNameMappingStorable.COL_OBJECT_NAME + " = ? for update";


  private volatile boolean initialized = false;
  private volatile SortedMap<OID, String> oIdToNameMappingCache = new TreeMap<OID, String>();
  private ODS ods;


  private OIDManagement() {
  }


  private void init() throws PersistenceLayerException {

    ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    ods.registerStorable(OIDNameMappingStorable.class);

    Collection<OIDNameMappingStorable> oidMappings;

    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      oidMappings = con.loadCollection(OIDNameMappingStorable.class);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.info("Failed to close connection", e);
      }
    }

    for (OIDNameMappingStorable onm : oidMappings) {
      oIdToNameMappingCache.put(new OID(onm.getId()), onm.getObjectName());
    }
    initialized = true;

  }


  public static OIDManagement getInstance() {
    if (!_instance.initialized) {
      synchronized (_instance) {
        if (!_instance.initialized) {
          try {
            _instance.init();
          } catch (PersistenceLayerException e) {
            // just to be sure that this is logged since it is not clear who else will use this
            logger.info(null, e);
            throw new RuntimeException("Failed to initialize OID management", e);
          }
        }
      }
    }
    return _instance;
  }


  public String getNameForOid(OID oid) {
    String result = oIdToNameMappingCache.get(oid);
    if (result != null) {
      return result;
    } else {
      OIDNameMappingStorable onms = new OIDNameMappingStorable(oid.toString());
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        con.queryOneRow(onms);
      } catch (PersistenceLayerException e) {
        // FIXME exception handling??
        logger.warn("Failed to read OID mapping", e);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // FIXME exception handling??
        logger.warn("Failed to read OID mapping", e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e1) {
        }
      }

      return onms.getObjectName();
    }
  }


  public SortedMap<OID, String> getMapForScope(OID prefix) {
    int lastPortion = prefix.getIntIndex(prefix.length() - 1) + 1;
    OID suffix = prefix.subOid(0, prefix.length() - 1);
    suffix = suffix.append(lastPortion);
    SortedMap<OID, String> oids = oIdToNameMappingCache.subMap(prefix, suffix);
    return oids;
  }
  
  
  public void setFixedOIDForName(OID oid, String name) {

    String existingString = oIdToNameMappingCache.get(oid);
    if (existingString != null && existingString.equals(name)) {
      // the key and value remained the same, so just do nothing
      return;
    }

    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {

      // check whether the target name exists for a different OID
      // TODO make sure that there can't be any other OIDs being mapped to the same name
      String existingValue = oIdToNameMappingCache.get(oid);
      if (existingValue == null || !existingValue.equals(name)) {

        for (Entry<OID, String> e : oIdToNameMappingCache.entrySet()) {
          if (e.getValue().equals(name)) {
            // the OID for a statistic has changed, remove and reinsert it afterwards
            OIDNameMappingStorable onmsDelete = new OIDNameMappingStorable(e.getKey().toString());
            try {
              con.deleteOneRow(onmsDelete);
            } catch (PersistenceLayerException e1) {
              logger.warn("Failed to delete OID mapping", e1);
            }

            oIdToNameMappingCache.remove(e.getKey());
            break;
          }
        }

      }

      oIdToNameMappingCache.put(oid, name);

      OIDNameMappingStorable onmsInsert = new OIDNameMappingStorable(oid.toString());
      onmsInsert.setObjectName(name);

      try {
        con.persistObject(onmsInsert);
        con.commit();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to create OID mapping", e);
      }

    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e1) {
        logger.warn("Failed to close connection", e1);
      }
    }

  }


  public OID getOidForName(String namePrefix, String name, String nameSuffix, OID oidPrefix, OID oidSuffix) {

    // this is the full statistics path
    String fullName = namePrefix + name + nameSuffix;

    // check memory cache
    OID cachedEntry = findWithinMemoryCacheByFullNameAndOIDPrefix(fullName, oidPrefix);
    if (cachedEntry != null) {
      return cachedEntry;
    }

    // check persisted mappings since maybe another cluster-node already created an OID for that name
    cachedEntry = findWithinDBandUpdateMemoryCache(fullName);
    if (cachedEntry != null) {
      return cachedEntry;
    }

    // finally create a new entry
    // ignore the fact that this requires getting a new ODSConnection since the connections are pooled
    // anyway and the actual creation should happen quite rarely
    return createNewEntry(namePrefix, name, fullName, oidPrefix, oidSuffix);

  }


  private OID findWithinMemoryCacheByFullNameAndOIDPrefix(String fullName, OID oidPrefix) {
    for (Entry<OID, String> e : oIdToNameMappingCache.entrySet()) {
      if (e.getValue().equals(fullName)) {
        if (e.getKey().startsWith(oidPrefix)) {
          // mapping already known
          return e.getKey();
        } else {
          // FIXME we might support this by updating cache and warehouse
          logger.warn("Mapping for statistics path <" + fullName + "> changed, this is not supported.");
          return null;
        }
      }
    }
    return null;
  }


  private OID findWithinDBandUpdateMemoryCache(String fullName) {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      PreparedQuery<OIDNameMappingStorable> pq =
          (PreparedQuery<OIDNameMappingStorable>) queryCache
              .getQueryFromCache(SQL_GET_OID_FOR_NAME, con, new OIDNameMappingStorable().getReader());
      OIDNameMappingStorable oidMapping = con.queryOneRow(pq, new Parameter(fullName));
      if (oidMapping == null) {
        return null;
      }
      oIdToNameMappingCache.put(new OID(oidMapping.getId()), oidMapping.getObjectName());
      return new OID(oidMapping.getId());
    } catch (PersistenceLayerException e) {
      logger.warn("Failed to obtain OID mapping", e);
      return null;
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e1) {
      }
    }
  }


  private OID createNewEntry(String namePrefix, String name, String fullName, OID oidPrefix, OID oidSuffix) {

    int nextAppropriateID = findAppropriateId(namePrefix + name, oidPrefix);

    // generate new OID
    OID newOid = oidPrefix.append(nextAppropriateID).append(oidSuffix);
    oIdToNameMappingCache.put(newOid, fullName);
    OIDNameMappingStorable stor = new OIDNameMappingStorable(newOid.toString());
    stor.setObjectName(fullName);

    // persist the new object and handle the case in which a different cluster node has already created that entry
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.persistObject(stor);
      con.commit();
    } catch (PersistenceLayerException e) {
      // check whether the entry has just been created by the other cluster node
      try {
        PreparedQuery<OIDNameMappingStorable> pq = (PreparedQuery<OIDNameMappingStorable>) queryCache
            .getQueryFromCache(SQL_GET_OID_FOR_NAME_FOR_UPDATE, con, new OIDNameMappingStorable().getReader());
        OIDNameMappingStorable foreignMapping = con.queryOneRow(pq, new Parameter(fullName));
        
        if (foreignMapping != null) { // case 1: entry exists, exception was probably due to uniqueness of objectname
          if (!foreignMapping.getId().equals(newOid.toString())) {
            // entry has been created with different OID
            oIdToNameMappingCache.remove(newOid);
            newOid = new OID(foreignMapping.toString());
          }
        } else { // case 2: entry does not exist, suspecting warehouse connection problems
          logger.warn("Failed to store OID mapping. OIDs might be different after next factory "
              + "startup or on other cluster nodes.", e);
        }
      } catch (PersistenceLayerException e1) {
        // cannot even check whether the entry exists
        logger.warn("Failed to store OID mapping. OIDs might be different after next factory "
            + "startup or on other cluster nodes.", e);
      }
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e1) {
      }
    }

    return newOid;
  }


  private int findAppropriateId(String namePrefixPlusName, OID oidPrefix) {

    SortedSet<Integer> usedIDs = new TreeSet<Integer>();
    StatisticsPath newPathPrefixAndName = StatisticsPathImpl.fromEscapedString(namePrefixPlusName);

    for (Entry<OID, String> e : oIdToNameMappingCache.entrySet()) {
      OID nextOid = e.getKey();

      if (nextOid.startsWith(oidPrefix)) {

        int id = nextOid.getIntIndex(oidPrefix.length());

        String nextName = e.getValue();
        StatisticsPath nextPath = StatisticsPathImpl.fromEscapedString(nextName);
        int indexToMatch = newPathPrefixAndName.length() - 1;
        if (nextPath.length() > indexToMatch) {
          String newColumnName = newPathPrefixAndName.getPathPart(indexToMatch).getPartName();
          String existingColumnName = nextPath.getPathPart(indexToMatch).getPartName();
          if (newColumnName.equals(existingColumnName)) {
            return id;
          }
        }

        usedIDs.add(id);
      }
    }

    int previousUsedId = -1;
    for (int nextUsedId: usedIDs) {
      if (nextUsedId - previousUsedId > 1 && previousUsedId != -1) {
        return previousUsedId + 1;
      }
      previousUsedId = nextUsedId;
    }

    return previousUsedId + 1;

  }


  @Deprecated
  public OID getOidForName(OID scope, String name) {
    if (oIdToNameMappingCache.containsValue(name)) {
      for (Entry<OID, String> e : oIdToNameMappingCache.entrySet()) {
        if (e.getValue().equals(name) && e.getKey().startsWith(scope)) {
          return e.getKey();
        }
      }

      return null;
    } else {
      int largestID = 0;

      for (Entry<OID, String> e : oIdToNameMappingCache.entrySet()) {
        OID oid = e.getKey();

        if (oid.startsWith(scope)) {
          int id = oid.getIntIndex(oid.length() - 1);

          if (id > largestID)
            largestID = id;
        }
      }

      // look into the database - perhaps some other cluster-node already created an OID for that name
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      OIDNameMappingStorable oidMapping = null;

      try {
        PreparedQuery<OIDNameMappingStorable> pq =
            (PreparedQuery<OIDNameMappingStorable>) queryCache.getQueryFromCache(SQL_GET_OID_FOR_NAME, con,
                                                                                 new OIDNameMappingStorable()
                                                                                     .getReader());
        Parameter sqlParamName = new Parameter(name);
        oidMapping = con.queryOneRow(pq, sqlParamName);
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to read OID mapping", e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e1) {
          logger.warn("Failed to close connection", e1);
        }
      }

      if (oidMapping != null) {
        oIdToNameMappingCache.put(new OID(oidMapping.getId()), oidMapping.getObjectName());
        return new OID(oidMapping.getId());
      } else {
        // generate new OID
        OID newOid = scope.append(largestID + 1);
        oIdToNameMappingCache.put(newOid, name);
        OIDNameMappingStorable stor = new OIDNameMappingStorable(newOid.toString());
        stor.setObjectName(name);

        con = ods.openConnection(ODSConnectionType.HISTORY);
        try {
          con.persistObject(stor);
          con.commit();
        } catch (PersistenceLayerException e) {
          logger.warn("Failed to store OID mapping", e);
        } finally {
          try {
            con.closeConnection();
          } catch (PersistenceLayerException e1) {
            logger.warn("Failed to close connection", e1);
          }
        }

        return newOid;
      }
    }
  }


  private AtomicBoolean locked = new AtomicBoolean(false);
  public boolean getOidManagementLock() {
    return locked.compareAndSet(false, true);
  }
  public void returnOidManagementLock() {
    if (!locked.get()) {
      throw new RuntimeException();
    }
    locked.set(false);
  }

}

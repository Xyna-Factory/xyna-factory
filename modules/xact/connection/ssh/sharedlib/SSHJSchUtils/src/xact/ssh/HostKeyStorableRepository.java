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
package xact.ssh;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.exceptions.XNWH_NoPersistenceLayerConfiguredForTableException;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.UserInfo;



public class HostKeyStorableRepository implements XynaHostKeyRepository {
  
  private final Logger logger = CentralFactoryLogging.getLogger(HostKeyStorableRepository.class);

  private final static PreparedQueryCache queryCache = new PreparedQueryCache();
  private final static ReadWriteLock hostLock = new ReentrantReadWriteLock();
  private final static ODS ods = ODSImpl.getInstance();
  
  private final static String directHostKeyQueryString = "SELECT * FROM " + HostKeyStorable.TABLE_NAME + " WHERE " + HostKeyStorable.COL_NAME + " = ?" +
                                                                                                  " AND " + HostKeyStorable.COL_FUZZY + " = 'false'" + 
                                                                                                  " AND " + HostKeyStorable.COL_HASHED + " = 'false'";
  
  private final static String directHostKeyQueryString_possibleList = "SELECT * FROM " + HostKeyStorable.TABLE_NAME + " WHERE " + HostKeyStorable.COL_NAME + " LIKE ?" +
                                                                                                  " AND " + HostKeyStorable.COL_FUZZY + " = 'false'" + 
                                                                                                  " AND " + HostKeyStorable.COL_HASHED + " = 'false'";
  
  private final static String hashedOrFuzzyHostKeyQueryString = "SELECT * FROM " + HostKeyStorable.TABLE_NAME + " WHERE " + HostKeyStorable.COL_HASHED + " = 'true'" + 
                                                                                                  " OR " + HostKeyStorable.COL_FUZZY + " = 'true'";
  
  private final static String loadAllQueryString = "SELECT * FROM " + HostKeyStorable.TABLE_NAME;
  
  private final Set<SupportedHostNameFeature> features;
  private final Set<SupportedHostNameFeature> notSupported;
  
  
  // not a Singleton because JSch-lib synchronizes on it in cases we'd like it not to
  public HostKeyStorableRepository() {
    this(SupportedHostNameFeature.all());
  }
  
  public HostKeyStorableRepository(Set<SupportedHostNameFeature> features) {
    this.features = features;
    this.notSupported = SupportedHostNameFeature.inverse(features);
  }
    
  
  public void add(HostKey hostkey, UserInfo ui) {
    hostLock.writeLock().lock();
    try {
      for (SupportedHostNameFeature noSup : notSupported) {
        if (noSup.accept(hostkey)) {
          logger.debug("HostKey feature '" + noSup + "' is not supported, key will not be added!");
          return;
        }
      }
      CheckResult check = CheckResult.getByNumericRepresentation(check(hostkey.getHost(), JSchUtil.base64StringToPublicKeyBlob(hostkey.getKey())));
      switch (check) {
        case CHANGED :
          if (ui != null) {
            ui.showMessage("Adding already contained host (" + hostkey.getHost() + ") with different key, adding anyway.");
          }
          //fall through
        case NOT_INCLUDED :
          HostKeyStorable storable = hostKeyToStorable(hostkey);
          ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
          try {
            try {
              con.persistObject(storable);
              con.commit();
            } catch (PersistenceLayerException e) {
              throw new RuntimeException("Error storing hostKey",e);
            }
          } finally {
            try {
              con.closeConnection();
            } catch (PersistenceLayerException e) {
              logger.debug("Error while trying to close connection",e);
            }
          }
          break;
        case OK :
          // ntbd
          break;
      }
    } finally {
      hostLock.writeLock().unlock();
    }
  }

  public int check(String host, byte[] key) {
    hostLock.readLock().lock();
    try {
      // direct lookup against !fuzzy & !hashed with [<host>]:<port> and without 
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        String publickey = JSchUtil.publicKeyBlobTobase64String(key);
        EncryptionType type = JSchUtil.getKeyType(key);
        boolean atLeastOneChanged = false;
        CheckResult result = findDirectMatch(host, publickey, type, con);
        switch (result) {
          case OK :
            return CheckResult.OK.getNumericRepresentation();
          case CHANGED :
            atLeastOneChanged = true;
          case NOT_INCLUDED :
            //ntbd
        }
        if (host.startsWith("[") && host.contains("]:")) {
          String hostWithoutSpecialPort = host.substring(1, host.indexOf("]:"));
          result = findDirectMatch(hostWithoutSpecialPort, publickey, type, con);
          switch (result) {
            case OK :
              return CheckResult.OK.getNumericRepresentation();
            case CHANGED :
              atLeastOneChanged = true;
            case NOT_INCLUDED :
              //ntbd
          }
        }
        if (features.contains(SupportedHostNameFeature.FUZZY) ||
            features.contains(SupportedHostNameFeature.HASHED)) {
          // check against all hashed and fuzzy
          FactoryWarehouseCursor<HostKeyStorable> cursor = con.getCursor(hashedOrFuzzyHostKeyQueryString, Parameter.EMPTY_PARAMETER, HostKeyStorable.reader, 100, queryCache);
          Collection<HostKeyStorable> keys = cursor.getRemainingCacheOrNextIfEmpty();
          while (keys != null && keys.size() > 0) {
            for (HostKeyStorable hostkey : keys) {
              result = checkHostKey(hostkey, host, publickey, type);
              switch (result) {
                case OK :
                  return CheckResult.OK.getNumericRepresentation();
                case CHANGED :
                  atLeastOneChanged = true;
                  break;
                case NOT_INCLUDED :
                  //ntbd
              }
            }
            keys = cursor.getRemainingCacheOrNextIfEmpty();
          }
        }
        if (atLeastOneChanged) {
          return CheckResult.CHANGED.getNumericRepresentation();
        } else {
          return CheckResult.NOT_INCLUDED.getNumericRepresentation();
        }
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("",e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.debug("Error while trying to close connection",e);
        }
      }
    } finally {
      hostLock.readLock().unlock();
    }
  }
  
  
  public CheckResult findDirectMatch(String host, String key, EncryptionType type, ODSConnection con) throws PersistenceLayerException {
    Collection<HostKeyStorable> hits = queryDirectMatch(host, con);
    boolean atLeastOneChanged = false;
    for (HostKeyStorable hostKeyStorable : hits) {
      CheckResult result = checkHostKey(hostKeyStorable, host, key, type);
      switch (result) {
        case OK :
          return CheckResult.OK;
        case CHANGED :
          atLeastOneChanged = true;
          break;
        case NOT_INCLUDED :
        default :
          break;
      }
    }
    if (atLeastOneChanged) {
      return CheckResult.CHANGED;
    } else {
      return CheckResult.NOT_INCLUDED;
    }
  }
  
  
  private Collection<HostKeyStorable> queryDirectMatch(String host, ODSConnection con) throws PersistenceLayerException {
    String query;
    Parameter params;
    if (features.contains(SupportedHostNameFeature.LIST)) {
      query = directHostKeyQueryString_possibleList;
      params = new Parameter("%\"" + host + "\"%");
    } else {
      query = directHostKeyQueryString;
      params = new Parameter(host);
    }
    PreparedQuery<HostKeyStorable> findHostKeyQuery = queryCache.getQueryFromCache(query, con, HostKeyStorable.reader);
    return con.query(findHostKeyQuery, params, -1);
  }
  

  public HostKey[] getHostKey() {
    // not called from infrastructure, might fail for huge data
    // could be load by cursor but does'nt prevent us from returning a huge HostKey[]
    ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      Collection<HostKeyStorable> keys = con.loadCollection(HostKeyStorable.class);
      ArrayList<HostKey> hks = new ArrayList<HostKey>();
      for (HostKeyStorable key : keys) {
        hks.add(storableToHostKey(key));
      }
      return hks.toArray(new HostKey[hks.size()]);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("",e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.debug("Error while trying to close connection",e);
      }
    }
  }

  public HostKey[] getHostKey(String host) {
    return getHostKey(host, null);
  }
  
  public HostKey[] getHostKey(String host, String type) {
    hostLock.readLock().lock();
    try {
      EncryptionType encryptionType = (type != null) ? EncryptionType.getBySshStringRepresentation(type) : null;
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {

        List<HostKey> result = getDirectMatches(con, host, encryptionType);
        if (host.startsWith("[") && host.contains("]:")) {
          String hostWithoutSpecialPort = host.substring(1, host.indexOf("]:"));
          result.addAll(getDirectMatches(con, hostWithoutSpecialPort, encryptionType));
        }

        if (features.contains(SupportedHostNameFeature.FUZZY) ||
            features.contains(SupportedHostNameFeature.HASHED)) {
          FactoryWarehouseCursor<HostKeyStorable> cursor =
              con.getCursor(hashedOrFuzzyHostKeyQueryString, Parameter.EMPTY_PARAMETER, HostKeyStorable.reader, 100, queryCache);
          try {
            Collection<HostKeyStorable> keys = cursor.getRemainingCacheOrNextIfEmpty();
            while (keys != null && keys.size() > 0) {
              for (HostKeyStorable hostkey : keys) {
                if (isMatched(hostkey, host)) {
                  if (encryptionType == null ||
                      hostkey.getType().equals(encryptionType.getStringRepresentation()))
                    result.add(storableToHostKey(hostkey));
                  }
              }
              keys = cursor.getRemainingCacheOrNextIfEmpty();
            }
          } finally {
            cursor.close();
          }
        }
        return result.toArray(new HostKey[result.size()]);
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("", e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.debug("Error while trying to close connection", e);
        }
      }
    } finally {
      hostLock.readLock().unlock();
    }
  }


  private List<HostKey> getDirectMatches(ODSConnection con, String host, EncryptionType encryptionType) throws PersistenceLayerException {
    List<HostKey> result = new ArrayList<HostKey>();
    for (HostKeyStorable hostkey : queryDirectMatch(host, con)) {
      if (isMatched(hostkey, host)) {
        if (encryptionType == null || 
            hostkey.getType().equals(encryptionType.getStringRepresentation()))
          result.add(storableToHostKey(hostkey));
        }
    }
    return result;
  }


  public String getKnownHostsRepositoryID() {
    return "HostKeyStorableRepository@"+System.identityHashCode(this);
  }

  public void remove(String host, String type) {
    this.remove(host, type, null);
  }

  public void remove(String host, String type, byte[] key) {
    String publickey = null;
    if (key != null) {
      publickey = JSchUtil.publicKeyBlobTobase64String(key);
    }
    hostLock.writeLock().lock();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        List<HostKeyStorable> toDelete = new ArrayList<HostKeyStorable>();
        List<HostKeyStorable> toPersist = new ArrayList<HostKeyStorable>();
        
        for (HostKeyStorable hostkey : queryDirectMatch(host, con)) {
          checkDeletion(hostkey, toPersist, toDelete, host, type, publickey);
        }
        
        if (features.contains(SupportedHostNameFeature.FUZZY) ||
            features.contains(SupportedHostNameFeature.HASHED)) {
          FactoryWarehouseCursor<HostKeyStorable> cursor = con.getCursor(hashedOrFuzzyHostKeyQueryString, Parameter.EMPTY_PARAMETER, HostKeyStorable.reader, 100, queryCache);
          Collection<HostKeyStorable> keys = cursor.getRemainingCacheOrNextIfEmpty();
          while (keys != null && keys.size() > 0) {
            for (HostKeyStorable hostkey : keys) {
              checkDeletion(hostkey, toPersist, toDelete, host, type, publickey);
            }
            keys = cursor.getRemainingCacheOrNextIfEmpty();
          }
        }
        con.delete(toDelete);
        con.persistCollection(toPersist);
        con.commit();
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("",e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.debug("Error while trying to close connection",e);
        }
      }
    } finally {
      hostLock.writeLock().unlock();
    }
  }
  
  
  private void checkDeletion(HostKeyStorable hostkey, List<HostKeyStorable> toPersist, List<HostKeyStorable> toDelete, String host, String type, String publickey) {
    if (isHostNameMatched(hostkey, host) &&
        (type == null || type.equals(hostkey.getType())) &&
        (publickey == null || publickey.equals(hostkey.getPublickey()))) {
       if (hostkey.isHostNameList()) {
         hostkey.removeFromHostNameList(host);
         toPersist.add(hostkey);
       } else {
         toDelete.add(hostkey);
       }
     }
  }


  public void exportKnownHost(String host, String type, String filenameKnownHosts) {
    hostLock.readLock().lock();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        FactoryWarehouseCursor<HostKeyStorable> cursor = con.getCursor(loadAllQueryString, Parameter.EMPTY_PARAMETER, HostKeyStorable.reader, 100, queryCache);
        Collection<HostKeyStorable> keys = cursor.getRemainingCacheOrNextIfEmpty();
        while (keys != null && keys.size() > 0) {
          Collection<HostKey> matches = new ArrayList<HostKey>();
          for (HostKeyStorable hostkey : keys) {
            if (host == null || isHostNameMatched(hostkey, host) &&
               (type == null || type.equals(hostkey.getType()))) {
              matches.add(storableToHostKey(hostkey));
            }
          }
          if (matches.size() > 0) {
            JSchUtil.exportKnownHosts(matches.toArray(new HostKey[matches.size()]), filenameKnownHosts);
          }
          keys = cursor.getRemainingCacheOrNextIfEmpty();
        }
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("",e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.debug("Error while trying to close connection",e);
        }
      }
    } finally {
      hostLock.readLock().unlock();
    }
  }

  
  public void importKnownHosts(String filenameKnownHosts) {
    if (!new File(filenameKnownHosts).exists()) {
      throw new IllegalArgumentException("Hostfile '" + filenameKnownHosts + "' does not exist.");
    }
    List<HostKey> keys = JSchUtil.importKnownHosts(filenameKnownHosts);
    for (HostKey hostKey : keys) {
      add(hostKey, null);
    }
  }
  
  
  private final static HostKeyStorable hostKeyToStorable(HostKey key) {
    return new HostKeyStorable(key.getHost(), EncryptionType.getBySshStringRepresentation(key.getType()).getStringRepresentation(), key.getKey(), JSchUtil.isHostKeyHashed(key), key.getComment());
  }
  
  private final static HostKey storableToHostKey(HostKeyStorable key) {
    return JSchUtil.instantiateHashedHostKey(key.getName(), EncryptionType.getByStringRepresentation(key.getType()), key.getPublickey(), null, false, key.getComment());
  }

  
  public void init() {
    try {
      ods.registerStorable(HostKeyStorable.class);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("", e);
    }
    try {
      if (!ods.isSamePhysicalTable(HostKeyStorable.TABLE_NAME, ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY)) {
        ods.replace(HostKeyStorable.class, ODSConnectionType.HISTORY, ODSConnectionType.DEFAULT);
      }
    } catch (XNWH_NoPersistenceLayerConfiguredForTableException e) {
      throw new RuntimeException("Could not initialize HostKeyStorableRepository.", e);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Could not initialize HostKeyStorableRepository.", e);
    }
  }

  public void shutdown() {
    try {
      if (!ods.isSamePhysicalTable(HostKeyStorable.TABLE_NAME, ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY)) {
        ods.replace(HostKeyStorable.class, ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY);
      }
    } catch (XNWH_NoPersistenceLayerConfiguredForTableException e) {
      logger.debug("Error while trying to transfer data from DEFAULT to HISTORY", e);
    } catch (PersistenceLayerException e) {
      logger.debug("Error while trying to transfer data from DEFAULT to HISTORY", e);
    }
  }
  
  
  private CheckResult checkHostKey(HostKeyStorable key, String hostname, String publickey, EncryptionType type) {
    if (isHostNameMatched(key, hostname) &&
        key.getType().equals(type.getStringRepresentation())) {
      if (key.getPublickey().equals(publickey)) {
        return CheckResult.OK;
      } else {
        return CheckResult.CHANGED; 
      }
    } else {
      return CheckResult.NOT_INCLUDED;
    }
  }
  
    
    
    /*
    Hostnames is a comma-separated list of patterns (`*' and `?' act as
   wildcards); each pattern in turn is matched against the canonical host
   name (when authenticating a client) or against the user-supplied name
   (when authenticating a server).  A pattern may also be preceded by `!' to
   indicate negation: if the host name matches a negated pattern, it is not
   accepted (by that line) even if it matched another pattern on the line.
   A hostname or address may optionally be enclosed within `[' and `]'
   brackets then followed by `:' and a non-standard port number.
   */
    public boolean isHostNameMatched(HostKeyStorable knownHost, String hostname) {
      List<String> hostNamesToCheck = new ArrayList<String>();
      hostNamesToCheck.add(hostname);
      if (hostname.startsWith("[") && hostname.contains("]:")) {
        hostNamesToCheck.add(hostname.substring(1, hostname.indexOf("]:")));
      }
      for (String hostnameToCheck : hostNamesToCheck) {
        if (knownHost.isFuzzy() && 
            features.contains(SupportedHostNameFeature.FUZZY)) {
          for (Pattern pattern : knownHost.getFuzzyPatterns()) {
            if (pattern.matcher(hostnameToCheck).matches()) {
              return true;
            }
          }
        } else if (isMatched(knownHost, hostnameToCheck)) {
          return true;
        } else if (hostnameToCheck.equals(knownHost.getName())) {
          return true;
        }
      }
      
      return false;
    }
    
    
    public boolean isMatched(HostKeyStorable knownHost, String hostname) {
      HostKey hostkey = storableToHostKey(knownHost);
      for (SupportedHostNameFeature noSup : notSupported) {
        if (noSup.accept(hostkey)) {
          logger.debug("HostKey feature '" + noSup + "' is not supported, key will not be matched!");
          return false;
        }
      }
      return JSchUtil.isHostNameMatched(hostkey, hostname);
    }

}
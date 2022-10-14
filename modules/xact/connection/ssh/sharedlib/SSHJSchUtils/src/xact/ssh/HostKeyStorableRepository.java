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
package xact.ssh;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.Constants;
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

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts.EntryFactory;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts.HostEntry;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts.KnownHostEntry;



public class HostKeyStorableRepository implements XynaHostKeyRepository {
  
  private final Logger logger = CentralFactoryLogging.getLogger(HostKeyStorableRepository.class);

  private final static PreparedQueryCache queryCache = new PreparedQueryCache();
  private final static ReadWriteLock hostLock = new ReentrantReadWriteLock();
  private final static ODS ods = ODSImpl.getInstance();
  private final static int DEFAULT_PORT = 22;
  
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
  
  
  public void add(HostKeyStorable hostkey) {
    hostLock.writeLock().lock();
    try {
      for (SupportedHostNameFeature noSup : notSupported) {
        if (noSup.accept(hostkey)) {
          logger.debug("HostKey feature '" + noSup + "' is not supported, key will not be added!");
          return;
        }
      }
      PublicKey key;
      try {
        key = SSHUtil.extractPublicKey(hostkey);
      } catch (Throwable t) {
        throw new RuntimeException("Error adding hostKey", t);
      }
      // name might already contain a port but passing DEFAULT_PORT leads to no adjustment of the hostname, so there is no need to extract the port and pass a modified hostname & port
      boolean verified = verify(hostkey.getName(), DEFAULT_PORT, key);
      if (!verified) { // TODO here or during handling-hook?
        ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
        try {
          try {
            con.persistObject(hostkey);
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
      }
    } finally {
      hostLock.writeLock().unlock();
    }
  }

  
  public boolean verify(String hostname, int port, PublicKey key) {
    hostLock.readLock().lock();
    try {
      // direct lookup against !fuzzy & !hashed with [<host>]:<port> and without 
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        EncryptionType type = EncryptionType.getByStringRepresentation(key.getAlgorithm());
        boolean atLeastOneChanged = false;
        CheckResult result = findDirectMatch(hostname, SSHUtil.encodePublicKey(key), type, con);
        switch (result) {
          case OK :
            return true;
          case CHANGED :
            atLeastOneChanged = true;
          case NOT_INCLUDED :
            //ntbd
        }
        if (hostname.startsWith("[") && hostname.contains("]:")) {
          String hostWithoutSpecialPort = hostname.substring(1, hostname.indexOf("]:"));
          result = findDirectMatch(hostWithoutSpecialPort, SSHUtil.encodePublicKey(key), type, con);
          switch (result) {
            case OK :
              return true;
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
              result = checkHostKey(hostkey, hostname, SSHUtil.encodePublicKey(key), type);
              switch (result) {
                case OK :
                  return true;
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
          return true;
        } else {
          return false;
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
  
  
  public List<String> findExistingAlgorithms(String hostname, int port) {
    hostLock.readLock().lock();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        List<String> matches = new ArrayList<String>();
        FactoryWarehouseCursor<HostKeyStorable> cursor = con.getCursor(loadAllQueryString, Parameter.EMPTY_PARAMETER, HostKeyStorable.reader, 100, queryCache);
        Collection<HostKeyStorable> keys = cursor.getRemainingCacheOrNextIfEmpty();
        while (keys != null && keys.size() > 0) {
          for (HostKeyStorable hostKeyStorable : keys) {
            // TODO include port?
            //        as fallback if the match did not work?
            if (isMatched(hostKeyStorable, hostname)) {
              matches.add(hostKeyStorable.getType()); // this ist the 'ssh-rsa'-type, should it be 'RSA' ?
            }
          }
        }
        return matches;
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("TODO",e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          throw new RuntimeException("TODO",e);
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
  


  public HostKeyStorable[] getHostKey(String host) {
    return getHostKey(host, null);
  }
  
  public HostKeyStorable[] getHostKey(String host, String type) {
    hostLock.readLock().lock();
    try {
      EncryptionType encryptionType = (type != null) ? EncryptionType.getBySshStringRepresentation(type) : null;
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {

        List<HostKeyStorable> result = getDirectMatches(con, host, encryptionType);
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
                    result.add(hostkey);
                  }
              }
              keys = cursor.getRemainingCacheOrNextIfEmpty();
            }
          } finally {
            cursor.close();
          }
        }
        return result.toArray(new HostKeyStorable[result.size()]);
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


  private List<HostKeyStorable> getDirectMatches(ODSConnection con, String host, EncryptionType encryptionType) throws PersistenceLayerException {
    List<HostKeyStorable> result = new ArrayList<HostKeyStorable>();
    for (HostKeyStorable hostkey : queryDirectMatch(host, con)) {
      if (isMatched(hostkey, host)) {
        if (encryptionType == null || 
            hostkey.getType().equals(encryptionType.getStringRepresentation()))
          result.add(hostkey);
        }
    }
    return result;
  }


  public String getKnownHostsRepositoryID() {
    return "HostKeyStorableRepository@"+System.identityHashCode(this);
  }


  public boolean remove(String host, String type) { // TODO Optional<EncryptionType> ?
    hostLock.writeLock().lock();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        List<HostKeyStorable> toDelete = new ArrayList<HostKeyStorable>();
        List<HostKeyStorable> toPersist = new ArrayList<HostKeyStorable>();
        
        for (HostKeyStorable hostkey : queryDirectMatch(host, con)) {
          checkDeletion(hostkey, toPersist, toDelete, host, type);
        }
        
        if (features.contains(SupportedHostNameFeature.FUZZY) ||
            features.contains(SupportedHostNameFeature.HASHED)) {
          FactoryWarehouseCursor<HostKeyStorable> cursor = con.getCursor(hashedOrFuzzyHostKeyQueryString, Parameter.EMPTY_PARAMETER, HostKeyStorable.reader, 100, queryCache);
          Collection<HostKeyStorable> keys = cursor.getRemainingCacheOrNextIfEmpty();
          while (keys != null && keys.size() > 0) {
            for (HostKeyStorable hostkey : keys) {
              checkDeletion(hostkey, toPersist, toDelete, host, type);
            }
            keys = cursor.getRemainingCacheOrNextIfEmpty();
          }
        }
        con.delete(toDelete);
        con.persistCollection(toPersist);
        con.commit();
        return toDelete.size() > 0 || toPersist.size() > 0;
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
  
  
  private void checkDeletion(HostKeyStorable hostkey, List<HostKeyStorable> toPersist, List<HostKeyStorable> toDelete, String host, String type) {
    if (isMatched(hostkey, host) &&
        (type == null || type.equals(hostkey.getType()))) {
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
          Collection<HostKeyStorable> matches = new ArrayList<HostKeyStorable>();
          for (HostKeyStorable hostkey : keys) {
            if (host == null || isMatched(hostkey, host) &&
               (type == null || type.equals(hostkey.getType()))) {
              matches.add(hostkey);
            }
          }
          if (matches.size() > 0) {
            try (FileOutputStream fos = new FileOutputStream(new File(filenameKnownHosts))) {
              for (HostKeyStorable hostKeyStorable : matches) {
                fos.write(hostKeyToHostEntryLine(hostKeyStorable).getBytes(Charset.defaultCharset()));
                fos.write(Constants.LINE_SEPARATOR.getBytes(Charset.defaultCharset()));
              }
            }
          }
          keys = cursor.getRemainingCacheOrNextIfEmpty();
        }
      } catch (Exception e) {
        throw new RuntimeException("TODO",e);
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

    OpenSSHKnownHosts openSSHKnownHosts;
    try {
      openSSHKnownHosts = new OpenSSHKnownHosts(new File(filenameKnownHosts));
    } catch (IOException e) {
      throw new RuntimeException("File not parsable.", e);
    }
    for (KnownHostEntry host : openSSHKnownHosts.entries()) {
      if (host instanceof HostEntry) {
        add(SSHJReflection.createStorableFromHostEntry((HostEntry) host));
      }
    }
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
    if (isMatched(key, hostname) &&
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

  public boolean isMatched(HostKeyStorable knownHost, String hostname) {
    HostEntry hostkey = storableToHostEntry(knownHost);
    for (SupportedHostNameFeature noSup : notSupported) {
      if (noSup.accept(knownHost)) {
        logger.debug("HostKey feature '" + noSup + "' is not supported, key will not be matched!");
        return false;
      }
    }
    try {
      return hostkey.appliesTo(hostname);
    } catch (IOException e) {
      throw new RuntimeException("Host '" + knownHost.getName() + "' could not be converted to HostEntry.", e);
    }
  }
    
    
    private static String hostKeyToHostEntryLine(HostKeyStorable knownHost) {
      StringBuilder sb = new StringBuilder();
      sb.append(knownHost.getName())
        .append(' ')
        .append(knownHost.getType())
        .append(' ')
        .append(knownHost.getPublickey()); // TODO decode?
      return sb.toString();
    }

    private static HostEntry storableToHostEntry(HostKeyStorable knownHost) {
      InputStream entry = new ByteArrayInputStream(hostKeyToHostEntryLine(knownHost).getBytes(Charset.defaultCharset()));
      try {
        OpenSSHKnownHosts hosts = new OpenSSHKnownHosts(new InputStreamReader(entry, Charset.defaultCharset()));
        assert hosts.entries().size() == 1;
        for (KnownHostEntry hostEntry : hosts.entries()) {
          if (hostEntry instanceof HostEntry) {
            return (HostEntry) hostEntry;
          }
        }
      } catch (IOException e) {
        // ntbd
      }
      throw new RuntimeException("Host '" + knownHost.getName() + "' could not be converted to HostEntry.");
    }


    
    


}
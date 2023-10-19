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
package xact.ssh;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
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
import net.schmizz.sshj.common.SSHException;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts.EntryFactory;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts.HostEntry;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts.KnownHostEntry;

import java.util.*;

public class HostKeyStorableRepository implements XynaHostKeyRepository {
  
  //private final Logger logger = CentralFactoryLogging.getLogger(HostKeyStorableRepository.class);
  private final static Logger logger = CentralFactoryLogging.getLogger(HostKeyStorableRepository.class);

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
  
  private String getStringFromHostKeyStorable(HostKeyStorable hostkey) {
    String comment = "";
    if (hostkey.getComment().length()>0) {
        comment = hostkey.getComment();
    };
    EncryptionType encrytype = EncryptionType.getByStringRepresentation(hostkey.getType());
    String encrystring = encrytype.getSshStringRepresentation();
    String response = hostkey.getName() + " " + encrystring + " " + hostkey.getPublickey() + " " + comment;
    return response;
  }
  
  public void add(HostKeyStorable hostkey) {
    
    hostkey = addAdjusted(hostkey);
    
    String HostKeyStorableString = getStringFromHostKeyStorable(hostkey);
    
    hostLock.writeLock().lock();
    try {
      for (SupportedHostNameFeature noSup : notSupported) {
        if (noSup.accept(hostkey)) {
          logger.debug("HostKey feature '" + noSup + "' is not supported, key will not be added!");
          return;
        }
      }
      
      try {
        SSHClient ssh = new SSHClient();
        InputStream entry = new ByteArrayInputStream(HostKeyStorableString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        ssh.addHostKeyVerifier(new OpenSSHKnownHosts(new InputStreamReader(entry,"UTF-8")));
      } catch (Throwable t) {
        throw new RuntimeException("Error adding hostKey", t);
      }
      
      //PublicKey key;
      //try {
      //  key = SSHUtil.extractPublicKey(hostkey);
      //} catch (Throwable t) {
      //  throw new RuntimeException("Error adding hostKey", t);
      //}
      //name might already contain a port but passing DEFAULT_PORT leads to no adjustment of the hostname, so there is no need to extract the port and pass a modified hostname & port
      //boolean verified = verify(hostkey.getName(), DEFAULT_PORT, key);
      
      boolean verified = verifyIntern(hostkey.getName(), DEFAULT_PORT, hostkey.getPublickey(), EncryptionType.getByStringRepresentation(hostkey.getType()), false);
      
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
  
  private HostKeyStorable addAdjusted(HostKeyStorable hostkey) {
    HostKeyStorable response = hostkey;
    String rawpublickey = hostkey.getPublickey();
    
    String typ= "";
    String publickey= "";
    boolean valid = false;
    
    String[] subelements = rawpublickey.trim().split("\\s+");
    if (subelements.length==1) {
      try {
        byte[] keyBytes = net.schmizz.sshj.common.Base64.decode(hostkey.getPublickey().trim());
        java.security.PublicKey key = new net.schmizz.sshj.common.Buffer.PlainBuffer(keyBytes).readPublicKey();
        EncryptionType encryType = EncryptionType.getByStringRepresentation(key.getAlgorithm());
        publickey=hostkey.getPublickey().trim();
        typ=encryType.getStringRepresentation();
        valid = true;
        logger.debug("SSH App: HostKeyStorableRepository - addAdjusted: Key successfully adjusted (only key)");
      //} catch (IOException e) {
      } catch (Exception e) {
        logger.debug("SSH App: HostKeyStorableRepository - addAdjusted: Key adjustment (only key) failed",e);
        //Unexpected format: PEM RSA for public key
        try {
          byte[] encoded = java.util.Base64.getDecoder().decode(hostkey.getPublickey().trim());
          java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
          java.security.spec.X509EncodedKeySpec keySpec = new java.security.spec.X509EncodedKeySpec(encoded);
          java.security.PublicKey tempPublickey = keyFactory.generatePublic(keySpec);
          
          byte[] b = new net.schmizz.sshj.common.Buffer.PlainBuffer().putPublicKey(tempPublickey).getCompactData();
          //String publicKeyString = xact.ssh.EncryptionType.RSA.getSshStringRepresentation() + " " + Base64.getEncoder().encodeToString(b);
          String publicKeyString = Base64.getEncoder().encodeToString(b);
          
          EncryptionType encryType = EncryptionType.getByStringRepresentation(tempPublickey.getAlgorithm());
          publickey=publicKeyString.trim();
          typ=encryType.getStringRepresentation();
          valid = true;
          logger.debug("SSH App: HostKeyStorableRepository - addAdjusted: Key successfully adjusted (PEM RSA)");
        //} catch (NoSuchAlgorithmException e2) {
        //} catch (InvalidKeySpecException e2) {};
        } catch (Exception e2) {
          logger.debug("SSH App: HostKeyStorableRepository - addAdjusted: Key adjustment (PEM RSA) failed",e2);
        };
      }
    } else {
      for(int counter=0; counter<subelements.length; counter++) {
        if (subelements[counter].trim().equalsIgnoreCase("ssh-rsa") || subelements[counter].trim().equalsIgnoreCase("ssh-dss")) {
          if ((counter+1)<subelements.length) {
            publickey=subelements[counter+1].trim();
            EncryptionType encryType=EncryptionType.getBySshStringRepresentation(subelements[counter].trim());
            typ=encryType.getStringRepresentation();
            valid = true;
            logger.debug("SSH App: HostKeyStorableRepository - addAdjusted: Key adjusted (standard format)");
          }
        }
      }
    }
    
    if (valid) {
      response.setPublickey(publickey);
      if ((response.getType()==null) || (response.getType().isBlank()) || response.getType().equalsIgnoreCase(EncryptionType.UNKNOWN.getStringRepresentation())) {
        response.setType(typ);
      }
    } else {
      logger.warn("Key adjustment failed");
    }
    
    return response;
  }
  
  public boolean verify(String hostname, int port, PublicKey key) {
    EncryptionType type = EncryptionType.getByStringRepresentation(key.getAlgorithm());
    PublicKey sshjpub=key;
    byte[] b = new net.schmizz.sshj.common.Buffer.PlainBuffer().putPublicKey(sshjpub).getCompactData();
    String publicstring = Base64.getEncoder().encodeToString(b);
    //logger.debug("SSH App: HostKeyStorableRepository - verify: " + HostKeyAliasMapping.persist(hostname) + " " + key.getAlgorithm()+" "+publicstring);
    
    boolean responseIntern = false;
    boolean speedResponseIntern = speedVerifyIntern(hostname, port, publicstring, type, true);
    if (speedResponseIntern) {
      responseIntern = true;
    } else {
      responseIntern = verifyIntern(hostname, port, publicstring, type, true);
    }
    
    //This may also fit e.g. in verifyIntern or findDirectMatch
    if (responseIntern==false && HostKeyAliasMapping.persist(hostname)) {
      KeyType hostkeyType = KeyType.fromKey(key);
      EncryptionType encryType = EncryptionType.UNKNOWN;
      if (hostkeyType.equals(KeyType.DSA) || hostkeyType.equals(KeyType.ECDSA521)) {
      //if (hostkeyType.equals(KeyType.DSA)) {
        encryType = EncryptionType.DSA;
      }
      if (hostkeyType.equals(KeyType.RSA)) {
        encryType = EncryptionType.RSA;
      }
      //encryType = EncryptionType.DSA;
      //logger.debug("SSH App: HostKeyStorableRepository - verify and add: "+HostKeyAliasMapping.convertHostname(hostname));
      add(createKnownHost(HostKeyAliasMapping.convertHostname(hostname), encryType, publicstring, ""));
      responseIntern = true;
    }
    
    //logger.debug("SSH App: HostKeyStorableRepository - verify responseIntern: "+responseIntern);
    return responseIntern;
  }
  
  
  public boolean speedVerifyIntern(String hostname, int port, String publickey, EncryptionType encrytype, boolean strictly) {
      try {
        EncryptionType type = encrytype; //EncryptionType.getByStringRepresentation(key.getAlgorithm());
        boolean atLeastOneChanged = false;
        CheckResult result = speedFindDirectMatch(HostKeyAliasMapping.convertHostname(hostname), publickey, type);
        switch (result) {
          case OK :
            return true;
          case CHANGED :
            atLeastOneChanged = true;
          case NOT_INCLUDED :
            //ntbd
        }
        if (atLeastOneChanged) {
          if (strictly) {
            return false;
          } else {
            return true;
          }
        } else {
          return false;
        }
      } catch (Exception e) {
        return false;
      } 
  }
  
  
  public void injectHostKey(String hostalias) {
    hostLock.readLock().lock();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        Collection<HostKeyStorable> hits = queryDirectMatch(hostalias, con);
        HostKeyHashMap.putHostKeyCollection(hostalias, hits);
      } catch (PersistenceLayerException e) {
        logger.warn("PersistenceLayerException",e);
        //throw new RuntimeException("",e);
      } catch (Exception e) {
        logger.warn("Exception in injectHostKey",e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.debug("Error while trying to close connection",e);
        }
      }
    } catch (Exception e0) {
      logger.warn("Exception in injectHostKey",e0);
    } finally {
      hostLock.readLock().unlock();
    }
  }
  
  
  public CheckResult speedFindDirectMatch(String hostalias, String key, EncryptionType type) {
    try {
      Collection<HostKeyStorable> hits = HostKeyHashMap.getHostKeyCollection(hostalias);
      if (hits.isEmpty()) {
        logger.debug("SSH App: HostKeyStorableRepository - speedFindDirectMatch: No hits");
        return CheckResult.NOT_INCLUDED;
      };
      //logger.debug("SSH App: HostKeyStorableRepository - speedFindDirectMatch hits.size(): "+hits.size());
      boolean atLeastOneChanged = false;
      for (HostKeyStorable hostKeyStorable : hits) {
        CheckResult result = checkHostKey(hostKeyStorable, hostalias, key, type);
        //logger.debug("SSH App: HostKeyStorableRepository - findDirectMatch result.toString(): "+result.toString());
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
    } catch (Exception e) {
      logger.warn("Exception in speedFindDirectMatch",e);
      return CheckResult.NOT_INCLUDED;
    }
  }
  
  
  
  public boolean verifyIntern(String hostname, int port, String publickey, EncryptionType encrytype, boolean strictly) {
    hostLock.readLock().lock();
    try {
      // direct lookup against !fuzzy & !hashed with [<host>]:<port> and without 
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        EncryptionType type = encrytype; //EncryptionType.getByStringRepresentation(key.getAlgorithm());
        boolean atLeastOneChanged = false;
        CheckResult result = findDirectMatch(HostKeyAliasMapping.convertHostname(hostname), publickey, type, con);
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
          result = findDirectMatch(HostKeyAliasMapping.convertHostname(hostWithoutSpecialPort), publickey, type, con);
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
          FactoryWarehouseCursor<HostKeyStorable> cursor = con.getCursor(hashedOrFuzzyHostKeyQueryString, Parameter.EMPTY_PARAMETER, HostKeyStorable.reader, 1000, queryCache);
          Collection<HostKeyStorable> keys = cursor.getRemainingCacheOrNextIfEmpty();
          while (keys != null && keys.size() > 0) {
            for (HostKeyStorable hostkey : keys) {
              result = checkHostKey(hostkey, hostname, publickey, type);
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
          if (strictly) {
            return false;
          } else {
            return true;
          }
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
  
  
  public List<String> speedFindExistingAlgorithms(String hostname, int port) {
    String hostalias=HostKeyAliasMapping.convertHostname(hostname);
    Collection<HostKeyStorable> hits = HostKeyHashMap.getHostKeyCollection(hostalias);
    List<String> matches = new ArrayList<String>();
    if (!hits.isEmpty()) {
      for (HostKeyStorable hostKeyStorable : hits) {
        if (isMatched(hostKeyStorable, hostalias)) {
          logger.debug("SSH App: HostKeyStorableRepository - findExistingAlgorithms hostKeyStorable.getType(): "+hostKeyStorable.getType());
          KeyType keytype = EncryptionType.getKeyType(hostKeyStorable.getType());
          matches.add(keytype.toString());
        }
      }
    } else {
      logger.debug("SSH App: HostKeyStorableRepository - speedFindExistingAlgorithms: No hits");
    }
    return matches;
  }
  
  public List<String> findExistingAlgorithms(String hostname, int port) {
    List<String> speedReply=speedFindExistingAlgorithms(hostname, port);
    if (!speedReply.isEmpty()) {
      return speedReply;
    }
    hostLock.readLock().lock();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        List<String> matches = new ArrayList<String>();
        FactoryWarehouseCursor<HostKeyStorable> cursor = con.getCursor(loadAllQueryString, Parameter.EMPTY_PARAMETER, HostKeyStorable.reader, 1000, queryCache);
        Collection<HostKeyStorable> keys = cursor.getRemainingCacheOrNextIfEmpty();
        //logger.debug("SSH App: HostKeyStorableRepository - findExistingAlgorithms keys.size(): "+keys.size());
        while (keys != null && keys.size() > 0) {
          Collection<HostKeyStorable> keyCollection=keys;
          for (HostKeyStorable hostKeyStorable : keyCollection) {
            // TODO include port?
            //        as fallback if the match did not work?
            if (isMatched(hostKeyStorable, HostKeyAliasMapping.convertHostname(hostname))) {
              logger.debug("SSH App: HostKeyStorableRepository - findExistingAlgorithms hostKeyStorable.getType(): "+hostKeyStorable.getType());
              //In OpenSSHKnownHosts KeyType.toString!
              //Otherwise return Collections.emptyList();
              KeyType keytype = EncryptionType.getKeyType(hostKeyStorable.getType());
              matches.add(keytype.toString());
            }
          }
          keys = cursor.getRemainingCacheOrNextIfEmpty();
        }
        return matches;
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("",e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          throw new RuntimeException("Error while trying to close connection",e);
        }
      }
    } finally {
      hostLock.readLock().unlock();
    }
  }
  
  
  public CheckResult findDirectMatch(String host, String key, EncryptionType type, ODSConnection con) throws PersistenceLayerException {
    Collection<HostKeyStorable> hits = queryDirectMatch(host, con);
    //logger.debug("SSH App: HostKeyStorableRepository - findDirectMatch hits.size(): "+hits.size());
    boolean atLeastOneChanged = false;
    for (HostKeyStorable hostKeyStorable : hits) {
      CheckResult result = checkHostKey(hostKeyStorable, host, key, type);
      //logger.debug("SSH App: HostKeyStorableRepository - findDirectMatch result.toString(): "+result.toString());
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
  
/*
  public HostKeyStorable[] getHostKey(String host) {
    return getHostKey(host, null);
  }
  
  public HostKeyStorable[] getHostKey(String host, String type) {
    //host = HostKeyAliasMapping.convertHostname(host);
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
*/

/*
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
*/

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
          FactoryWarehouseCursor<HostKeyStorable> cursor = con.getCursor(hashedOrFuzzyHostKeyQueryString, Parameter.EMPTY_PARAMETER, HostKeyStorable.reader, 1000, queryCache);
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
        FactoryWarehouseCursor<HostKeyStorable> cursor = con.getCursor(loadAllQueryString, Parameter.EMPTY_PARAMETER, HostKeyStorable.reader, 1000, queryCache);
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
        throw new RuntimeException("Error in exportKnownHost",e);
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
    
    //Test of validity
    OpenSSHKnownHosts openSSHKnownHosts;
    try {
      openSSHKnownHosts = new OpenSSHKnownHosts(new File(filenameKnownHosts));
    } catch (IOException e) {
      throw new RuntimeException("File not parsable.", e);
    }
    
    //After validity test assume valid file
    FileInputStream instream = null;
    try {
        instream = new FileInputStream(filenameKnownHosts);
        BufferedReader bufread = new BufferedReader(new InputStreamReader(instream));
        
        String line;
        while( (line = bufread.readLine()) != null) {
                String[] subelements = line.trim().split("\\s+");
                
                String name= "";
                String typ= "";
                String publickey= "";
                String comment = "";
                if (subelements.length>2) {
                    name=subelements[0];
                    typ=subelements[1];
                    publickey=subelements[2];
                    if (subelements.length>3) {
                        comment =subelements[3];
                    }
                }
                boolean hashed = false;
                EncryptionType encry = EncryptionType.getBySshStringRepresentation(typ.trim());
                String type_store = encry.getStringRepresentation();
                
                //logger.debug("SSH App - HostKeyStorableRepository importKnownHosts: "+name+" "+typ+" "+publickey);
                
                //Add of hostkey contains again validity tests
                HostKeyStorable hostkey = new HostKeyStorable(name,type_store,publickey,hashed,comment);
                add(hostkey);
        }
      } catch (IOException e) {
        throw new RuntimeException("File not parsable.", e);
      } finally {
      try {
          instream.close();
          } catch (IOException e) {
            throw new RuntimeException("File not parsable.", e);
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
    if (isMatched(key, hostname)) {
      if (key.getType().equals(type.getStringRepresentation()) && (key.getPublickey().equals(publickey))) {
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
    if (!(hostkey != null)) {
        return false;
    }
    for (SupportedHostNameFeature noSup : notSupported) {
      if (noSup.accept(knownHost)) {
        logger.debug("HostKey feature '" + noSup + "' is not supported, key will not be matched!");
        return false;
      }
    }
    try {
      //logger.debug("SSH App: HostKeyStorableRepository - isMatched: "+hostname+" : " + knownHost.getName() + " : " + hostkey.appliesTo(hostname));
      return hostkey.appliesTo(hostname);
    } catch (IOException e) {
      throw new RuntimeException("Host '" + knownHost.getName() + "' could not be converted to HostEntry.", e);
    }
  }
  
  private static String hostKeyToHostEntryLine(HostKeyStorable knownHost) {
      StringBuilder sb = new StringBuilder();
      sb.append(knownHost.getName());
      sb.append(' ');
      EncryptionType encry = EncryptionType.getByStringRepresentation(knownHost.getType());
      sb.append(encry.getSshStringRepresentation());
      sb.append(' ');
      sb.append(knownHost.getPublickey());
      return sb.toString();
  }

  private static HostEntry storableToHostEntry(HostKeyStorable knownHost) {
      String hostkey1 = null;
      hostkey1 = hostKeyToHostEntryLine(knownHost);
      
      //Workaround for bad database entries!!!
      if ((knownHost.getType().equalsIgnoreCase("UNKNOWN"))){
          return null;
      }
      //String hostkeytype1 = EncryptionType.getBySshStringRepresentation(knownHost.getType()).getStringRepresentation();
      //logger.debug("SSH App: HostKeyStorableRepository - storableToHostEntry: "+hostkey1);
      //InputStream entry = new ByteArrayInputStream(hostKeyToHostEntryLine(knownHost).getBytes(Charset.defaultCharset()));
      
      //InputStream entry = new ByteArrayInputStream(hostkey1.getBytes(Charset.defaultCharset()));
      InputStream entry = new ByteArrayInputStream(hostkey1.getBytes(StandardCharsets.UTF_8));
      try {
        //OpenSSHKnownHosts hosts = new OpenSSHKnownHosts(new InputStreamReader(entry, Charset.defaultCharset()));
        OpenSSHKnownHosts hosts = new OpenSSHKnownHosts(new InputStreamReader(entry, StandardCharsets.UTF_8));
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

    public static HostKeyStorable createKnownHost(String hostname, EncryptionType type, String publickey, String comment) {
        // TODO String value = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty("xact.ssh.hashHostKeys");
        //      hash if configured?
        boolean isHashed = hostname.startsWith("|1|");
        return new HostKeyStorable(hostname, type.getStringRepresentation(), publickey, isHashed, comment);
      }
    
}
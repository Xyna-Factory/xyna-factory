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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.exceptions.XNWH_NoPersistenceLayerConfiguredForTableException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;

import net.schmizz.sshj.Config;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.Factory;
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.common.SSHException;
import net.schmizz.sshj.userauth.keyprovider.FileKeyProvider;
import net.schmizz.sshj.userauth.keyprovider.KeyFormat;
import net.schmizz.sshj.userauth.keyprovider.KeyPairWrapper;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.keyprovider.KeyProviderUtil;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.PasswordUtils;
import net.schmizz.sshj.userauth.password.Resource;

public class IdentityStorableRepository implements XynaIdentityRepository {
  
  private final static Logger logger = CentralFactoryLogging.getLogger(IdentityStorableRepository.class);
  
  private final static ODS ods = ODSImpl.getInstance();
  private final static ReadWriteLock identityLock = new ReentrantReadWriteLock();
  private final Config config;
  private final PassphraseStore passphraseStore;
  
  
  public IdentityStorableRepository(Config config) {
    this.config = config;
    passphraseStore = new SecureStorablePassphraseStore();
  }
  
  
  public IdentityStorable add(Optional<String> name, EncryptionType keyType, byte[] privateKey, byte[] publicKey, Optional<String> passphrase) {
    byte[] adjustedPrivateKey = privateKey;
    
    //Legacy from JSCH implementation
    boolean success = true;
    
    IdentityStorable newIdentity = keyPairToStorable(name, keyType, adjustedPrivateKey, publicKey);
    identityLock.writeLock().lock();
    try {
      Collection<IdentityStorable> identities;
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        identities = con.loadCollection(IdentityStorable.class);
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("Could not load identities", e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.debug("Error while trying to close connection",e);
        }
      }
      for (IdentityStorable identityStorable : identities) {
        // TODO does not check for name collisions, should it?
        CheckResult result = check(identityStorable, newIdentity);
        if (result == CheckResult.OK) {
          return null; // already contained
        }
      }
      //add as new
      con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        con.persistObject(newIdentity);
        con.commit();
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("", e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.debug("Error while trying to close connection",e);
        }
      }
    } finally {
      identityLock.writeLock().unlock();
      if (passphrase.isPresent() && success) {
        passphraseStore.store(newIdentity.getName(), passphrase.get());
      }
    }
    return newIdentity;
  }
  
  
  public Collection<IdentityStorable> removeKey(EncryptionType type, Optional<String> publickey) {
    identityLock.writeLock().lock();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        Collection<IdentityStorable> identities = con.loadCollection(IdentityStorable.class);
        Collection<IdentityStorable> toDelete = identities.stream().filter(i -> i.getType().equals(type.getStringRepresentation())).collect(Collectors.toList());
        if (toDelete.isEmpty()) {
          return Collections.emptyList();
        } else {
          if (publickey.isPresent()) {
            //logger.debug("SSH App: IdentityStorableRepository - removeKey publicKey: "+publickey.get());
            //for (IdentityStorable identity : toDelete) {
            //    logger.debug("SSH App: IdentityStorableRepository - removeKey publicKeyList: "+getPublicKeyString(identity));
            //    if (getPublicKeyString(identity).trim().equals(publickey.get().trim())) {logger.debug("SSH App: IdentityStorableRepository - removeKey publicKeyList: "+"Key found");};
            //}
            toDelete = toDelete.stream().filter(i -> getPublicKeyString(i).trim().equals(publickey.get().trim())).collect(Collectors.toList());
            //logger.debug("SSH App: IdentityStorableRepository - removeKey toDelete.size(): "+toDelete.size());
          }
          con.delete(toDelete);
          for (IdentityStorable identity : toDelete) {
            passphraseStore.remove(identity.getName());
          }
          con.commit();
          return toDelete;
        }
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("Could not load identities", e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.debug("Error while trying to close connection",e);
        }
      }
    } finally {
      identityLock.writeLock().unlock();
    }
  }


  public void clearAll() {
    identityLock.writeLock().lock();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        con.deleteAll(IdentityStorable.class);
        con.commit();
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("Could not load identities", e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.debug("Error while trying to close connection",e);
        }
      }
    } finally {
      identityLock.writeLock().unlock();
    }
  }
  
  public CheckResult check(IdentityStorable one, IdentityStorable another) {
    if (one.getType().equals(another.getType())) {
      if (Arrays.equals(one.getPublickey(), another.getPublickey()) &&
          Arrays.equals(one.getPrivatekey(), another.getPrivatekey())) {
        return CheckResult.OK;
      } else {
        return CheckResult.CHANGED;
      }
    } else {
      return CheckResult.NOT_INCLUDED;
    }
  }

  public void init() {
    try {
      ods.registerStorable(IdentityStorable.class);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("", e);
    }
    try {
      if (!ods.isSamePhysicalTable(IdentityStorable.TABLE_NAME, ODSConnectionType.HISTORY, ODSConnectionType.DEFAULT)) {
        ods.replace(IdentityStorable.class, ODSConnectionType.HISTORY, ODSConnectionType.DEFAULT);
      }
    } catch (XNWH_NoPersistenceLayerConfiguredForTableException e) {
      throw new RuntimeException("Could not initialize IdentityStorableRepository.", e);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Could not initialize IdentityStorableRepository.", e);
    }
  }
  
  public void shutdown() {
    try {
      if (!ods.isSamePhysicalTable(IdentityStorable.TABLE_NAME, ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY)) {
        ods.replace(IdentityStorable.class, ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY);
      }
    } catch (XNWH_NoPersistenceLayerConfiguredForTableException e) {
      logger.debug("Error while trying to transfer data from DEFAULT to HISTORY", e);
    } catch (PersistenceLayerException e) {
      logger.debug("Error while trying to transfer data from DEFAULT to HISTORY", e);
    }
  }
  
  
  public Collection<IdentityStorable> getAllIdentities() {

    identityLock.readLock().lock();
    try {
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      try {
        Collection<IdentityStorable> identities = con.loadCollection(IdentityStorable.class);
        //logger.debug("SSH App: IdentityStorableRepository - getAllIdentities identities.size(): "+identities.size());
        Collection<IdentityStorable> adjustedIdentities = new ArrayList<>();
        
        //If adjustments should be necessary, please implement them here.
        for (IdentityStorable identity : identities) {
          IdentityStorable adjustedIdentity = new IdentityStorable();
          adjustedIdentity.setId(identity.getId());
          adjustedIdentity.setName(identity.getName());
          adjustedIdentity.setType(identity.getType());
          adjustedIdentity.setPublickey(identity.getPublickey());
          adjustedIdentity.setPrivatekey(identity.getPrivatekey());
          
          //String privateKeyC = null;
          //String publicKeyC = null;
          //try {
          //    privateKeyC = new String(identity.getPrivatekey(),"UTF-8");
          //    publicKeyC = new String(identity.getPublickey(),"UTF-8");
          //} catch(Exception e) {};
          //logger.debug("SSH App: IdentityStorableRepository - getAllIdentities privateKey: "+privateKeyC);
          //logger.debug("SSH App: IdentityStorableRepository - getAllIdentities publicKey: "+publicKeyC);
          
          adjustedIdentities.add(adjustedIdentity);
        }
        return adjustedIdentities;
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("", e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.debug("Error while trying to close connection",e);
        }
      }
    } finally {
      identityLock.readLock().unlock();
    }
  }


  public List<KeyProvider> getAllKeys() {
    Collection<IdentityStorable> storables = getAllIdentities();
    List<KeyProvider> result = new ArrayList<KeyProvider>();
    for (IdentityStorable identity : storables) {
      result.add(storableToKeyProvider(identity));
    }
    //logger.debug("SSH App: IdentityStorableRepository - getAllKeys result.size(): "+result.size());
    return result;
  }


  @Override
  public List<KeyProvider> getKey(String name, Optional<String> type) {
    identityLock.readLock().lock();
    try {
      Collection<IdentityStorable> storables = getAllIdentities();
      List<KeyProvider> result = new ArrayList<KeyProvider>();
      for (IdentityStorable identity : storables) {
        //logger.debug("SSH App: IdentityStorableRepository - getKey identity.getName() : "+identity.getName());
        if (name != null) {
          //logger.debug("SSH App: IdentityStorableRepository - getKey name: "+name);
          if (identity.getName().equalsIgnoreCase(name)) {
            if (type.isPresent()) {
              //logger.debug("SSH App: IdentityStorableRepository - getKey type.get(): "+type.get());
              //logger.debug("SSH App: IdentityStorableRepository - getKey: "+identity.getType());
              if (type.get().equals(identity.getType())) {
                result.add(storableToKeyProvider(identity));
              }
            } else {
              result.add(storableToKeyProvider(identity));
            }
          }
        } else {
          if (type.isPresent()) {
            //logger.debug("SSH App: IdentityStorableRepository - getKey type.get(): "+type.get());
            //logger.debug("SSH App: IdentityStorableRepository - getKey identity.getType(): "+identity.getType());
            if (type.get().equals(identity.getType())) {
              result.add(storableToKeyProvider(identity));
            }
          } else {
            result.add(storableToKeyProvider(identity));
          }
        }
      }
      //logger.debug("SSH App: IdentityStorableRepository - getKey result.size(): "+result.size());
      return result;
    } finally {
      identityLock.readLock().unlock();
    }
  }
  
  
  public KeyProvider storableToKeyProvider(IdentityStorable identity) {
    try {
        String privateKey = new String(identity.getPrivatekey(),"UTF-8");
        String publicKey = new String(identity.getPublickey(),"UTF-8");
        KeyFormat format = KeyProviderUtil.detectKeyFileFormat(privateKey, identity.getPublickey() != null);
        FileKeyProvider fkp = Factory.Named.Util.create(config.getFileKeyProviderFactories(), format.toString());
        if (hasPassphrase(identity)) {
            String passphraseString = passphraseStore.retrieve(identity.getName());
            fkp.init(privateKey, publicKey, new PasswordFinder() {
                
                public boolean shouldRetry(Resource<?> ressource) {
                    return false;
                  }
                  
                  public char[] reqPassword(Resource<?> ressource) {
                    return passphraseString.toCharArray();
                    
                  }
            });
            
        } else {
            fkp.init(privateKey, publicKey);
        }
        return new KeyPairWrapper(fkp.getPublic(), fkp.getPrivate());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
  }
  
  private static IdentityStorable keyPairToStorable(Optional<String> name, EncryptionType format, byte[] privateKey, byte[] publicKey) {
     return new IdentityStorable(name.orElse(generateName(publicKey)),
                                format.getStringRepresentation(),
                                privateKey,
                                publicKey);
  }

  private static String generateName(byte[] publicKey) {
    try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(publicKey);
        String sha256hex = new String(Hex.encode(hash));
        return "Identity@" + sha256hex;
    } catch (Exception e) {
        SecureRandom random = new SecureRandom();
        return "GeneratedKey_"+System.currentTimeMillis()+"."+random.nextLong();
    }
  }

  private String getPublicKeyString(IdentityStorable identity) {
    try {
      String response = new String(identity.getPublickey(),"UTF-8");
      return response;
    } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
    }
  }

  private boolean hasPassphrase(IdentityStorable identity) {
    String passphrase = passphraseStore.retrieve(identity.getName());
    return passphrase != null &&
          !passphrase.isEmpty();
  }

}

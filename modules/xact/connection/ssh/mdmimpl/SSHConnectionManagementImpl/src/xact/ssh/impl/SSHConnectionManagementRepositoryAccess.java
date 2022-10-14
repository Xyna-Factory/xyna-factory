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
package xact.ssh.impl;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;

import xact.ssh.EncryptionType;
import xact.ssh.HostKeyStorableRepository;
import xact.ssh.IdentityStorable;
import xact.ssh.IdentityStorableRepository;
import xact.ssh.SSHConnectionManagement;
import xact.ssh.SSHUtil;
import xact.ssh.XynaHostKeyRepository;
import xact.ssh.XynaIdentityRepository;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;


public class SSHConnectionManagementRepositoryAccess {
  
  private final static String HASHHOSTKEYS_PROPERTY_NAME = "xact.ssh.hashhostkeys";
  
  private static XynaHostKeyRepository hostKeyRepo;
  private static XynaIdentityRepository identityRepo;
  private static SSHClient client;
  private static final Logger logger = CentralFactoryLogging.getLogger(SSHConnectionManagementRepositoryAccess.class);
  
  public static void init() {
    hostKeyRepo = new HostKeyStorableRepository();
    hostKeyRepo.init();
    identityRepo = new IdentityStorableRepository(client.getTransport().getConfig());
    identityRepo.init();
    
    XynaFactory
        .getInstance()
        .getFactoryManagementPortal()
        .getXynaFactoryControl()
        .getDependencyRegister()
        .addDependency(DependencySourceType.XYNAPROPERTY, HASHHOSTKEYS_PROPERTY_NAME, DependencySourceType.DATATYPE,
                       SSHConnectionManagement.class.getName(), getRevision());
  }


  private static Long getRevision() {
    if (SSHConnectionManagementRepositoryAccess.class.getClassLoader() instanceof ClassLoaderBase) {
      return ((ClassLoaderBase) SSHConnectionManagementRepositoryAccess.class.getClassLoader()).getRevision();
    }
    logger.warn(SSHConnectionManagementRepositoryAccess.class.getName() + " not loaded by ClassLoaderBase.");
    return VersionManagement.REVISION_WORKINGSET;
  }


  public static void shutdown() {
    hostKeyRepo.shutdown();
    identityRepo.shutdown();
    XynaFactory
        .getInstance()
        .getFactoryManagementPortal()
        .getXynaFactoryControl()
        .getDependencyRegister()
        .removeDependency(DependencySourceType.XYNAPROPERTY, HASHHOSTKEYS_PROPERTY_NAME, DependencySourceType.DATATYPE,
                          SSHConnectionManagement.class.getName(), getRevision());
  }

  
  public static void addKnownHost(String hostname, EncryptionType type, String publickey, String comment) {
    hostKeyRepo.add(SSHUtil.createKnownHost(hostname, type, publickey, comment));
  }
  
  
  public static void exportKnownHost(String hostname, EncryptionType type, String keyFileName) {
    hostKeyRepo.exportKnownHost(hostname, type.getStringRepresentation(), keyFileName);
  }
  
  
  public static void generateKeyPair(EncryptionType type, Integer keysize, String passphrase, boolean overwriteExisting) {
    int size = 1024;
    if (keysize != null) {
      size = keysize.intValue();
    }
    KeyPairGenerator keyGen;
    try {
      keyGen = KeyPairGenerator.getInstance(type.getStringRepresentation());
      keyGen.initialize(size);
      KeyPair pair = keyGen.generateKeyPair();
      
      byte[] publicKeyBlob = pair.getPublic().getEncoded();
      byte[] privateKeyBlob = pair.getPrivate().getEncoded();
      if (overwriteExisting) {
        identityRepo.clearAll();
      }
      add(Optional.empty(),
          EncryptionType.getBySshStringRepresentation(pair.getPrivate().getFormat()), 
          privateKeyBlob, 
          publicKeyBlob, 
          Optional.ofNullable(passphrase));
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private static void add(Optional<String> name, EncryptionType type, byte[] privateKey, byte[] publicKey, Optional<String> passphrase) {
    identityRepo.add(name, type, privateKey, publicKey, passphrase);
  }
  

  
  
  public static List<String> getPublicKey(EncryptionType encryptionType) {
    try {
      List<String> keys = new ArrayList<String>();
      for (KeyProvider identity : identityRepo.getAllKeys()) {
        if (encryptionType == null || 
            encryptionType == EncryptionType.UNKNOWN || 
            encryptionType.getSshStringRepresentation().equals(identity.getType().toString())) {
          keys.add(SSHUtil.encodePublicKey(identity.getPublic()));
          // TODO not the same format as before? see below
          // keys.add(JSchUtil.byte2str(type) + " " +JSchUtil.byte2str(encodedBytes));
        }
      }
      return keys;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  
  public static void importKnownHosts(String keyFileName) {
    hostKeyRepo.importKnownHosts(keyFileName);
  }
  
  
  public static void removeKnownHost(String hostname, String publicKey, EncryptionType type) {
    hostKeyRepo.remove(hostname, type != null ? type.getStringRepresentation() : null);
  }
  
  
  public static void addKeyFiles(String publicfilename, String privatefilename, String passphrase) {
    
    try (SSHClient client = new SSHClient()) {
      KeyProvider provider = client.loadKeys(privatefilename, passphrase);
      add(Optional.of(generateIdentityName(privatefilename)),
          EncryptionType.getBySshStringRepresentation(provider.getType().toString()),
          provider.getPrivate().getEncoded(),
          provider.getPrivate().getEncoded(),
          Optional.ofNullable(passphrase));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private static String generateIdentityName(String privatefilename) {
    return new File(privatefilename).getName()+"_"+System.currentTimeMillis();
  }


  public static void addKeyPair(String privatekey, String publickey, String passphrase) {
    
    try (SSHClient client = new SSHClient()) {
      KeyProvider provider = client.loadKeys(privatekey, publickey, new PasswordFinder() {
        
        public boolean shouldRetry(Resource<?> ressource) {
          return false;
        }
        
        public char[] reqPassword(Resource<?> ressource) {
          return passphrase.toCharArray();
        }
      });
      add(Optional.empty(),
          EncryptionType.getBySshStringRepresentation(provider.getType().toString()),
          provider.getPrivate().getEncoded(),
          provider.getPrivate().getEncoded(),
          Optional.ofNullable(passphrase));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
    
  
  /**
   * Löscht die Identities vom angegeben type und publickey. Wird keiner der beiden
   * Parameter angeben wird das ganze Archiv geleert.
   * @param type
   * @param publickey
   * @throws IllegalArgumentException falls der EncryptionType unbekannt ist (type == UNKOWN)
   */
  public static void removeIdentity(EncryptionType type, String publickey) {
    if (type == null) {
      identityRepo.clearAll();
    }
    if (type == EncryptionType.UNKNOWN) {
      throw new IllegalArgumentException("EncryptionType not supported");
    }
    identityRepo.removeKey(type, Optional.ofNullable(publickey));
  }
  
  
}

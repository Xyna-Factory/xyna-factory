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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;

import xact.ssh.EncryptionType;
import xact.ssh.HostKeyStorableRepository;
import xact.ssh.IdentityStorableRepository;
import xact.ssh.JSchUtil;
import xact.ssh.PassphraseStore;
import xact.ssh.SSHConnectionManagement;
import xact.ssh.SecureStorablePassphraseStore;
import xact.ssh.XynaHostKeyRepository;
import xact.ssh.XynaIdentityRepository;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;
import com.jcraft.jsch.Buffer;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.Identity;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;


public class SSHConnectionManagementRepositoryAccess {
  
  private final static String HASHHOSTKEYS_PROPERTY_NAME = "xact.ssh.hashhostkeys";
  
  private static XynaHostKeyRepository hostKeyRepo;
  private static XynaIdentityRepository identityRepo;
  private static PassphraseStore passphraseStore;
  private static JSch jsch;
  private static final Logger logger = CentralFactoryLogging.getLogger(SSHConnectionManagementRepositoryAccess.class);
  
  public static void init() {
    jsch = new JSch();
    hostKeyRepo = new HostKeyStorableRepository();
    hostKeyRepo.init();
    identityRepo = new IdentityStorableRepository();
    identityRepo.init();
    passphraseStore = new SecureStorablePassphraseStore();
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
    if (type == null || type == EncryptionType.UNKNOWN) {
      type = JSchUtil.getKeyType(publickey);
    }
    String value = XynaFactory.getPortalInstance().getFactoryManagementPortal().getProperty("xact.ssh.hashHostKeys");
    HostKey key = JSchUtil.instantiateHashedHostKey(hostname, type, publickey, jsch, Boolean.valueOf(value), comment);
    hostKeyRepo.add(key, null);
  }
  
  
  public static void exportKnownHost(String hostname, EncryptionType type, String keyFileName) {
    hostKeyRepo.exportKnownHost(hostname, type.getStringRepresentation(), keyFileName);
  }
  
  
  public static void generateKeyPair(EncryptionType type, Integer keysize, String passphrase, boolean overwriteExisting) {
    int size = 1024;
    if (keysize != null) {
      size = keysize.intValue();
    }
    com.jcraft.jsch.KeyPair pair;
    try {
      pair = com.jcraft.jsch.KeyPair.genKeyPair(jsch, type.getNumericRepresentation(), size);
    } catch (JSchException e) {
      throw new RuntimeException("", e);
    }
    if (passphrase != null) {
      pair.setPassphrase(passphrase);
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    pair.writePrivateKey(baos);
    byte[] privateKeyBlob = baos.toByteArray();
    baos = new ByteArrayOutputStream();
    pair.writePublicKey(baos, "");
    byte[] publicKeyBlob = baos.toByteArray();
    if (overwriteExisting) {
      Vector<Identity> all = identityRepo.getIdentities();
      for (Identity identity : all) {
        passphraseStore.remove(identity.getName());
      }
      identityRepo.removeAll();
    }
    Identity identity = identityRepo.tryAdd(privateKeyBlob, publicKeyBlob);
    if (identity != null) {
      passphraseStore.store(identity.getName(), passphrase);
    }
  }
  
  
  public static List<String> getPublicKey(EncryptionType encryptionType) {
    List<String> keys = new ArrayList<String>();
    for (Object identity : identityRepo.getIdentities()) {
      Identity id = (Identity) identity;
      if (encryptionType == null || encryptionType == EncryptionType.UNKNOWN || encryptionType.getSshStringRepresentation().equals(id.getAlgName())) {
        if (id.isEncrypted()) {
          try {
            id.setPassphrase(JSchUtil.str2byte(passphraseStore.retrieve(id.getName())));
          } catch (JSchException e) {
            throw new RuntimeException("",e);
          }
        }
        byte[] publicKeyBlob = id.getPublicKeyBlob();
        byte[] encodedBytes = JSchUtil.toBase64(publicKeyBlob, 0, publicKeyBlob.length);
        Buffer buf =new Buffer(publicKeyBlob);
        byte[] type = buf.getString();
        keys.add(JSchUtil.byte2str(type) + " " +JSchUtil.byte2str(encodedBytes));
    
      }
    }
    return keys;
  }
  
  
  public static void importKnownHosts(String keyFileName) {
    hostKeyRepo.importKnownHosts(keyFileName);
  }
  
  
  public static void removeKnownHost(String hostname, String publickey, EncryptionType type) {
    byte[] publicKeyBlob = null;
    if (publickey != null) {
      publicKeyBlob = JSchUtil.base64StringToPublicKeyBlob(publickey);
    }
    hostKeyRepo.remove(hostname, type == null ? null : type.getStringRepresentation(), publicKeyBlob);
  }
  
  
  public static void addKeyFiles(String publicfilename, String privatefilename, String passphrase) {
    com.jcraft.jsch.KeyPair key; // TODO ...we don't use publicfilename, we could at least validate
    try {
      key = com.jcraft.jsch.KeyPair.load(jsch, privatefilename, publicfilename);
    } catch (JSchException e) {
      throw new RuntimeException("",e);
    }
    if (key.isEncrypted()) {
      if (passphrase == null) {
        throw new IllegalArgumentException("Key is encrypted and no passphrase is given");
      } else {
        key.setPassphrase(passphrase);
        key.decrypt(passphrase);
      }
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    key.writePrivateKey(baos);
    byte[] identity = baos.toByteArray();
    baos = new ByteArrayOutputStream();
    key.writePublicKey(baos, "");
    byte[] publicKey = baos.toByteArray();
    Identity generatedId = identityRepo.tryAdd(identity, publicKey);
    if (generatedId != null) {
      passphraseStore.store(generatedId.getName(), passphrase);
    }
  }
  
  
  public static void addKeyPair(String privatekey, String publickey, String passphrase) {
    byte[] privateKeyBytes = JSchUtil.str2byte(privatekey);
    byte[] base64KeyBytes = JSchUtil.str2byte(publickey);
    byte[] decodedBytes = JSchUtil.fromBase64(base64KeyBytes, 0, base64KeyBytes.length);
    Identity identity = identityRepo.tryAdd(privateKeyBytes, decodedBytes);
    if (identity != null) {
      passphraseStore.store(identity.getName(), passphrase);
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
    if (type == EncryptionType.UNKNOWN) {
      throw new IllegalArgumentException("EncryptionType not supported");
    }
    Vector<Identity> all = identityRepo.getIdentities();
    Collection<Pair<String, byte[]>> fittingIdentities = new ArrayList<Pair<String, byte[]>>();
    for (Identity identity : all) {
      if (type == null || type.getSshStringRepresentation().equals(identity.getAlgName())) {
        byte[] privateKeyBlob = JSchUtil.exractPrivateKey(identity);
        if (publickey == null) {
          fittingIdentities.add(new Pair<String, byte[]>(identity.getName(), privateKeyBlob));
        } else {
          Pair<String, byte[]> id = new Pair<String, byte[]>(identity.getName(), privateKeyBlob);
          if (identity.isEncrypted()) {
            try {
              identity.setPassphrase(JSchUtil.str2byte(passphraseStore.retrieve(identity.getName())));
            } catch (JSchException e) {
              throw new RuntimeException("",e);
            }
          }
          byte[] publicKeyBlob = identity.getPublicKeyBlob();
          byte[] encodedBytes = JSchUtil.toBase64(publicKeyBlob, 0, publicKeyBlob.length);
          String identityPublicKey = JSchUtil.byte2str(encodedBytes);
          if (identityPublicKey.equals(publickey)) {
            fittingIdentities.add(id);
          }
        }
      }
    }
    for (Pair<String, byte[]> identity : fittingIdentities) {
      passphraseStore.remove(identity.getFirst());
      identityRepo.remove(identity.getSecond());
    }
  }
  
  
}

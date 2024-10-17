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
package xact.ssh.impl;



import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
//import xact.ssh.SSHUtil;
import xact.ssh.XynaHostKeyRepository;
import xact.ssh.XynaIdentityRepository;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement;

import net.schmizz.sshj.Config;
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

    client = new SSHClient();

    hostKeyRepo = new HostKeyStorableRepository();
    hostKeyRepo.init();
    identityRepo = new IdentityStorableRepository(client.getTransport().getConfig());
    identityRepo.init();

    XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getDependencyRegister()
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
    XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getDependencyRegister()
        .removeDependency(DependencySourceType.XYNAPROPERTY, HASHHOSTKEYS_PROPERTY_NAME, DependencySourceType.DATATYPE,
                          SSHConnectionManagement.class.getName(), getRevision());
  }


  public static void addKnownHost(String hostname, EncryptionType type, String publickey, String comment) {
    if (type==null) {type = EncryptionType.UNKNOWN;};
    hostKeyRepo.add(HostKeyStorableRepository.createKnownHost(hostname, type, publickey, comment));
  }


  public static void exportKnownHost(String hostname, EncryptionType type, String keyFileName) {
    hostKeyRepo.exportKnownHost(hostname, type.getStringRepresentation(), keyFileName);
  }

  public static void generateKeyPair(EncryptionType type, Integer keysize, String passphrase, boolean overwriteExisting) {
    try {
      if (type.getStringRepresentation().equalsIgnoreCase("RSA") | type.getStringRepresentation().equalsIgnoreCase("DSA")) {
        xact.ssh.generatekeypackage.ExtdKeyGeneration.generateKeyPair(keysize, passphrase, overwriteExisting, type, identityRepo);
      } else {
        logger.warn("EncryptionType not supported: " + type.getStringRepresentation());
        NoSuchAlgorithmException e = new NoSuchAlgorithmException();
        throw new RuntimeException(e);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private static void add(Optional<String> name, EncryptionType type, byte[] privateKey, byte[] publicKey, Optional<String> passphrase) {
    identityRepo.add(name, type, privateKey, publicKey, passphrase);
  }


  public static List<String> getPublicKey(EncryptionType encryptionType) {
    try {
      List<String> keys = new ArrayList<String>();

      Collection<IdentityStorable> storables = identityRepo.getAllIdentities();
      for (IdentityStorable identity : storables) {
        KeyProvider keyproviderIdentity = identityRepo.storableToKeyProvider(identity);
        if (encryptionType == null || encryptionType == EncryptionType.UNKNOWN
            || encryptionType.getSshStringRepresentation().equals(keyproviderIdentity.getType().toString())) {
          String publicKey = new String(identity.getPublickey(), "UTF-8");
          keys.add(publicKey.trim());
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

    //Format compatible with JSCH entries
    File privatefile = new File(privatefilename);
    File publicfile = new File(publicfilename);
    try {
      byte[] byteprivate = Files.readAllBytes(privatefile.toPath());
      byte[] bytepublic = Files.readAllBytes(publicfile.toPath());
      String stringprivate = new String(byteprivate, "UTF-8");
      String stringpublic = new String(bytepublic, "UTF-8");

      addKeyPair(stringprivate, stringpublic, passphrase);
      
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void addKeyPair(String privatekey, String publickey, String passphrase) {

    String alias = null;
    
    String adjustedPublickey = adjustPublickey(publickey);
    
    try (SSHClient client = new SSHClient()) {
      KeyProvider provider = client.loadKeys(privatekey, adjustedPublickey, new PasswordFinder() {

        public boolean shouldRetry(Resource<?> ressource) {
          return false;
        }


        public char[] reqPassword(Resource<?> ressource) {
          return passphrase.toCharArray();
        }
      });

      //Format compatible with JSCH entries
      byte[] byteprivate = privatekey.getBytes(StandardCharsets.UTF_8);
      byte[] bytepublic = adjustedPublickey.getBytes(StandardCharsets.UTF_8);
      
      add(Optional.ofNullable(alias), EncryptionType.getBySshStringRepresentation(provider.getType().toString()), bytepublic, byteprivate,
          Optional.ofNullable(passphrase));

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String adjustPublickey(String publickey) {
    //Format: [ssh-...] [Key] [Optional]
    //Adjust: [ssh-...] [Key]
    String adjustedPublickey= publickey;
    String[] subelements = publickey.trim().split("\\s+");
    if (subelements.length<2) {
      adjustedPublickey = publickey.trim();
    } else {
      adjustedPublickey = subelements[0] + " " + subelements[1];
    }
    return adjustedPublickey;
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

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
package xact.ldap.generation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;

import xact.ldap.generation.cli.generated.OverallInformationProvider;
import xact.ldap.dictionary.LDAPAttributeTypeDictionaryEntry;
import xact.ldap.dictionary.LDAPDictionaryProvider;
import xact.ldap.dictionary.LDAPObjectClassDictionaryEntry;
import xact.ldap.dictionary.LDAPSchemaDictionary;
import xact.ldap.dictionary.ObjectClassType;
import base.Credentials;
import base.Host;
import base.Port;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.map.TypeMappingCache;
import com.gip.xyna.xdev.map.TypeMappingEntry;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.DeploymentTask;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.SSLKeyAndTruststoreParameter;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.SSLKeystoreParameter;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.SSLParameter;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.TrustEveryone;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xmcp.xfcli.AXynaCommand;
import com.gip.xyna.xmcp.xfcli.CLIRegistry;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Datatype;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomGenerator;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Variable;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPJSSESecureSocketFactory;
import com.novell.ldap.LDAPSchema;
import com.novell.ldap.LDAPSocketFactory;



public class LDAPManagementImpl implements DeploymentTask {

  private final static Logger logger = CentralFactoryLogging.getLogger(LDAPManagementImpl.class);
  private final static String TYPEMAPPING_COMPONENT_IDENTIFIER = "ldapobjectclassmap";

  private static ODS ods;
  private static IDGenerator idGenerator;
  private static LDAPDictionaryProvider dictionaryProvider;
  private static TypeMappingCache mappingLDAPNameToFqXynaObjectName;
  private static List<String> staticAdditionalDatatypeDependencies;
  private static List<String> staticAdditionalExceptionTypeDependencies;


  protected LDAPManagementImpl() {
  }

  public void onDeployment() {
    idGenerator = XynaFactory.getInstance().getIDGenerator();
    ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    try {
      ods.registerStorable(LDAPObjectClassDictionaryEntry.class);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Failed to register LDAPObjectClassDictionaryEntry.",e);
    }
    try {
      mappingLDAPNameToFqXynaObjectName = new TypeMappingCache();
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Could not instantiate TypeMapping!", e);
    }
    dictionaryProvider = LDAPDictionaryProvider.getInstance();
    if (dictionaryProvider.getDictionary() == null) {
      ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        try {
          LDAPSchemaDictionary dictionary = new LDAPSchemaDictionary(con);
          dictionaryProvider.setDictionary(dictionary);
        } catch (PersistenceLayerException e) {
          logger.error("Could not restore dictionary entries!",e);
          throw new RuntimeException("Could not restore dictionary entries!",e);
        }
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.warn("Failed to close connection.",e);
        }
      }
    }
    staticAdditionalDatatypeDependencies = new ArrayList<String>();
    staticAdditionalDatatypeDependencies.add("xact.ldap.LDAPAbstractObjectclass");
    staticAdditionalDatatypeDependencies.add("xact.ldap.LDAPAttribute");
    staticAdditionalDatatypeDependencies.add("xact.ldap.LDAPAuxiliaryObjectclass");
    staticAdditionalDatatypeDependencies.add("xact.ldap.LDAPModification");
    staticAdditionalDatatypeDependencies.add("xact.ldap.LDAPModificationAdd");
    staticAdditionalDatatypeDependencies.add("xact.ldap.LDAPModificationDelete");
    staticAdditionalDatatypeDependencies.add("xact.ldap.LDAPModificationReplace");
    staticAdditionalDatatypeDependencies.add("xact.ldap.LDAPModificationOperation");
    staticAdditionalDatatypeDependencies.add("xact.ldap.LDAPMultiValueAttribute");
    staticAdditionalDatatypeDependencies.add("xact.ldap.LDAPSingleValueAttribute");
    staticAdditionalDatatypeDependencies.add("xact.ldap.LDAPStructuralObjectclass");
    staticAdditionalDatatypeDependencies.add("xact.ldap.SearchFilterDepthScope");
    staticAdditionalDatatypeDependencies.add("xact.ldap.SearchResultScope");
    staticAdditionalDatatypeDependencies.add("xact.ldap.SearchRootScope");
    staticAdditionalDatatypeDependencies.add("xact.ldap.SSLParameter");
    staticAdditionalDatatypeDependencies.add("xact.ldap.TrustEveryone");
    staticAdditionalDatatypeDependencies.add("xact.ldap.SSLKeystoreParameter");
    staticAdditionalDatatypeDependencies.add("xact.ldap.LDAPCompareMatch");
    staticAdditionalDatatypeDependencies.add("xact.ldap.LDAPCompareResult");
    staticAdditionalDatatypeDependencies.add("xact.ldap.LDAPCompareMiss");
    staticAdditionalExceptionTypeDependencies = new ArrayList<String>();
    staticAdditionalExceptionTypeDependencies.add("xact.ldap.exceptions.SizeLimitExceeded");
    staticAdditionalExceptionTypeDependencies.add("xact.ldap.exceptions.ObjectDoesAlreadyExist");
    staticAdditionalExceptionTypeDependencies.add("xact.ldap.exceptions.ObjectClassViolation");
    staticAdditionalExceptionTypeDependencies.add("xact.ldap.exceptions.NotAllowedOnNonLeaf");
    staticAdditionalExceptionTypeDependencies.add("xact.ldap.exceptions.NoSuchObject");
    staticAdditionalExceptionTypeDependencies.add("xact.ldap.exceptions.NamingViolation");
    staticAdditionalExceptionTypeDependencies.add("xact.ldap.exceptions.InvalidDNSyntax");
    staticAdditionalExceptionTypeDependencies.add("xact.ldap.exceptions.InsufficientAccessRights");
    staticAdditionalExceptionTypeDependencies.add("xact.ldap.exceptions.ExtendedOperationNotSupported");
    staticAdditionalExceptionTypeDependencies.add("xact.ldap.exceptions.ControlNotSupported");
    staticAdditionalExceptionTypeDependencies.add("xact.ldap.exceptions.ConnectionFault");
    staticAdditionalExceptionTypeDependencies.add("xact.ldap.exceptions.AuthenticationFault");
    
    try {
      List<Class<? extends AXynaCommand>> commands = OverallInformationProvider.getCommands();
      for (Class<? extends AXynaCommand> command : commands) {
        CLIRegistry.getInstance().registerCLICommand(command);
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("could not register cli commands.", e);
    }
  }

  public void onUndeployment() {
    try {
      List<Class<? extends AXynaCommand>> commands = OverallInformationProvider.getCommands();
      for (Class<? extends AXynaCommand> command : commands) {
        CLIRegistry.getInstance().unregisterCLICommand(command);
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("could not register cli commands.", e);
    }
  }

  public static void reloadLDAPSchemaAndRegenerateArtifacts(Host host, Port port, Credentials credentials, SSLParameter sslParameter) {
    LDAPConnection con = createConnection(sslParameter);
    try {
      con.connect(host.getHostname(), port.getValue());
    } catch (LDAPException e) {
      // throw XynaException:LdapConnectException
      throw new RuntimeException("",e);
    }

    try {
      con.bind(LDAPConnection.LDAP_V3, credentials.getUsername(), credentials.getPassword());
    } catch (LDAPException e) {
      // throw XynaException:LdapAuthenticationException
      throw new RuntimeException(e);
    }

    LDAPSchema schemaDef;
    try {
      schemaDef = con.fetchSchema(con.getSchemaDN());
    } catch (LDAPException e) {
      throw new RuntimeException("",e);
    }
    // TODO do we need to recreate?
    //LDAPSchemaDictionary dictionary = new LDAPSchemaDictionary(schemaDef);
    LDAPSchemaDictionary dictionary = dictionaryProvider.getDictionary();
    dictionary.rebuildFromSchema(schemaDef);
    ODSConnection odsCon = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      dictionary.persistDicitionary(odsCon);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Could not persist Dictionary!", e);
    } finally {
      try {
        odsCon.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection.",e);
      }
    }
    //dictionaryProvider.setDictionary(dictionary); // TODO invalidate during regeneration or disable a cap to prevent wfs from using it?

    XmomGenerator batch = 
      XmomGenerator.with(RevisionManagement.REVISION_DEFAULT_WORKSPACE, true, DeploymentMode.codeChanged, false, WorkflowProtectionMode.BREAK_ON_INTERFACE_CHANGES);

    List<TypeMappingEntry> typeMappings = new ArrayList<TypeMappingEntry>();
    Set<LDAPObjectClassDictionaryEntry> objectClasses = dictionary.getAllObjectClasses();
    for (LDAPObjectClassDictionaryEntry ldapObjectClassDictionaryEntry : objectClasses) {
      Datatype datatype = generateDatytypeFromDictionaryEntry(ldapObjectClassDictionaryEntry);
      batch.add(datatype);
      typeMappings.add(generateMappingEntry(ldapObjectClassDictionaryEntry.getProminentName(), datatype.getFQTypeName()));
    }

    // TODO really? shouldn't we keep those around for a purge now?
    //cleanupTypeMappings();

    try {
      mappingLDAPNameToFqXynaObjectName.store(typeMappings);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Could not persist TypeMapping!", e);
    }

    try {
      batch.save();
      batch.deploy();
    } catch (XynaException e) {
      throw new RuntimeException("",e);
    }
  }
  

  public static void reloadLDAPSchemaAndRegenerateArtifacts(Host host, Port port, Credentials credentials) {
    reloadLDAPSchemaAndRegenerateArtifacts(host, port, credentials, null);
  }
  
  
  // TODO code duplication with LDAPServices, move to sharedLib?
  private static LDAPConnection createConnection(SSLParameter sslParams) {
    if (sslParams == null) {
      return new LDAPConnection();
    } else {
      LDAPSocketFactory sf = null;
      if (sslParams instanceof TrustEveryone) {
        sf = buildTrustAllSocketFactory();
      } else if (sslParams instanceof SSLKeystoreParameter ||
                 sslParams instanceof SSLKeyAndTruststoreParameter) {
        SSLKeystoreParameter keyStoreParams;
        SSLKeystoreParameter trustStoreParams;
        if (sslParams instanceof SSLKeystoreParameter) {
          keyStoreParams = (SSLKeystoreParameter) sslParams;
          trustStoreParams = (SSLKeystoreParameter) sslParams;
        } else {
          SSLKeyAndTruststoreParameter keyAndTrust = (SSLKeyAndTruststoreParameter) sslParams;
          keyStoreParams = keyAndTrust.getSSLKeystore();
          trustStoreParams = keyAndTrust.getSSLTruststore();
        }
        
        SSLContext context;
        try {
          if (sslParams instanceof SSLKeystoreParameter && 
              (keyStoreParams.getType() == null || keyStoreParams.getType().equalsIgnoreCase("default"))) {
            // SSLContext.getDefault() once java 1.6
            context = SSLContext.getInstance("Default");
          } else { // TODO cache keystore by params? ks.load might cost us
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            char[] keyStorePassphrase = keyStoreParams.getPassphrase().toCharArray();
            kmf.init(buildKeyStore(keyStoreParams), keyStorePassphrase);
            
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(buildKeyStore(trustStoreParams));
            
            context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
          }
        } catch (Throwable t) {
          Department.handleThrowable(t);
          throw new RuntimeException("Unexpected failure",t);
        }
        sf = new LDAPJSSESecureSocketFactory(context.getSocketFactory());
      } else {
        throw new IllegalArgumentException("Unknown SSLParameter-Type: " + (sslParams == null ? "null" : sslParams.getClass().getName()));
      }
      return new LDAPConnection(sf);
    }
  }
  
  
  private static KeyStore buildKeyStore(SSLKeystoreParameter keyStoreParams) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException {
    char[] passphrase = keyStoreParams.getPassphrase().toCharArray();
    KeyStore ks = KeyStore.getInstance(keyStoreParams.getType());
    ks.load(new FileInputStream(keyStoreParams.getPath()), passphrase);
    return ks;
  }
  
  
  /**
   * erzeugt LDAPSocketFactory, die allen Zertifikaten blind vertraut;
   * WARNUNG: Verwendung ist sicherheitstechnisch sehr bedenklich
   */
  public static LDAPSocketFactory buildTrustAllSocketFactory() {
    TrustManager[] trustAllCerts = new TrustManager[] {
          new X509TrustManager() {
              public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                  return null;
              }
              public void checkClientTrusted(
                  java.security.cert.X509Certificate[] certs, String authType) {
                  }
              public void checkServerTrusted(
                  java.security.cert.X509Certificate[] certs, String authType) {
              }
          }
      };
    try {
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      return new LDAPJSSESecureSocketFactory(sc.getSocketFactory());
    }
    catch (GeneralSecurityException e) {
      //do nothing
    }
    return null;
  }

  
  // TODO enrich with TypeMapping-Info
  public static void listGeneration(OutputStream out, boolean verbose) {
    Set<LDAPObjectClassDictionaryEntry> allObjectClasses = dictionaryProvider.getDictionary().getAllObjectClasses();
    String format = " %-" + 40 + "s %-" + 40 + "s %-" + 35 + "s  %-" + 25 + "s";
    String attribformat = "     %-" + 40 + "s %-" + 40 + "s %-" + 20 + "s  %-" + 15 + "s";
    for (LDAPObjectClassDictionaryEntry ldapEntry : allObjectClasses) {
      writeLineToCommandLine(out, String.format(format, LDAPSchemaDictionary.generateValidNameForClass(ldapEntry.getProminentName()),
                                                ldapEntry.getProminentName(), ldapEntry.getOid(),
                                                (ldapEntry.getLDAPSuperclasses().size() > 0 ? ldapEntry.getLDAPSuperclasses().get(0).getProminentName() : "-")));
      if (verbose) {
        for (LDAPAttributeTypeDictionaryEntry ldapAttribEntry : ldapEntry.getLDAPAttributes()) {
          writeLineToCommandLine(out, String.format(attribformat, LDAPSchemaDictionary.generateValidNameForAttribute(ldapAttribEntry.getProminentName()), 
                                                    ldapAttribEntry.getProminentName(), ldapAttribEntry.getOid(), ldapAttribEntry.getSyntax().getCorrespondingJavaClass().getSimpleName()));
        }
      }
    }
  }
  
  
  public static void purgeGeneration() throws XynaException {
    LDAPSchemaDictionary dictionary = dictionaryProvider.getDictionary();
    Set<LDAPObjectClassDictionaryEntry> allObjectClasses = dictionary.getAllObjectClasses();
    for (LDAPObjectClassDictionaryEntry ldapEntry : allObjectClasses) {
      String xynaObjectName = mappingLDAPNameToFqXynaObjectName.lookup(TYPEMAPPING_COMPONENT_IDENTIFIER, ldapEntry.getProminentName());
      if (xynaObjectName != null) {
        ((XynaMultiChannelPortal)XynaFactory.getInstance().getXynaMultiChannelPortal()).deleteDatatype(xynaObjectName, true, false, RevisionManagement.REVISION_DEFAULT_WORKSPACE);
      }
    }
    ODSConnection odsCon = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      odsCon.deleteAll(LDAPObjectClassDictionaryEntry.class);
      odsCon.commit();
    } finally {
      odsCon.closeConnection();
    }
    dictionary.clear();
    cleanupTypeMappings();
  }
  
  
  private static void writeLineToCommandLine(OutputStream outputstream, Object... s) {
    if (s == null) {
      return;
    }
    if (s.length == 1) {
      writeToCommandLine(outputstream, s[0], "\n");
    } else {
      StringBuilder sb = new StringBuilder();
      for (Object part : s) {
        if (part == null) {
          sb.append("null");
        } else {
          if (part instanceof String) {
            sb.append((String) part);
          } else {
            sb.append(part);
          }
        }
      }
      sb.append("\n");
      writeToCommandLine(outputstream, sb.toString());
    }
  }
  
  
  private static void writeToCommandLine(OutputStream outputstream, Object... s) {
    try {
      if (s == null) {
        return;
      }
      if (s.length == 1) {
        if (s[0] == null) {
          outputstream.write("null".getBytes(Constants.DEFAULT_ENCODING));
        } else {
          outputstream.write(s[0].toString().getBytes(Constants.DEFAULT_ENCODING));
        }
      } else {
        StringBuilder sb = new StringBuilder();
        for (Object part : s) {
          if (part == null) {
            sb.append("null");
          } else {
            if (part instanceof String) {
              sb.append((String) part);
            } else {
              sb.append(part);
            }
          }
        }
        outputstream.write(sb.toString().getBytes(Constants.DEFAULT_ENCODING));
      }
    } catch (IOException e) {
      throw new RuntimeException("Unexpected exception while writing to stream.", e);
    }
  }


  private final static String xmlPathPrefix = "xact.ldap.generation.";

  
  private static Datatype generateDatytypeFromDictionaryEntry(LDAPObjectClassDictionaryEntry entry) {
    return Datatype.derived(createXmomType(entry), findAppropriateSuperclass(entry), generateVariables(entry));
  }

  
  private static XmomType findAppropriateSuperclass(LDAPObjectClassDictionaryEntry entry) {
    if (entry.getSuperclasses() != null) {
      for (LDAPObjectClassDictionaryEntry superclass : entry.getLDAPSuperclasses()) {
        if (!superclass.getNames().get(0).equalsIgnoreCase("top")) {
          return createXmomType(superclass);
        }
      }
    }
    // get modeled base class instead of top
    ObjectClassType type = ObjectClassType.getTypeByIdentifier(entry.getObjectclasstype()); // TODO this might fail if there actually is more inheritance from ABSTRACT
    return type.getXmomType();
  }
  
  
  private static XmomType createXmomType(LDAPObjectClassDictionaryEntry entry) {
    String name = LDAPSchemaDictionary.generateValidNameForClass(entry.getProminentName());
    String path = xmlPathPrefix + entry.getOidRealm().getName();
    return new XmomType(path, name, entry.getProminentName());
  }


  private static List<Variable> generateVariables(LDAPObjectClassDictionaryEntry entry) {
    List<Variable> variables = new ArrayList<Variable>();
    for (LDAPAttributeTypeDictionaryEntry attribute : entry.getLDAPAttributes()) {
      String name = LDAPSchemaDictionary.generateValidNameForAttribute(attribute.getProminentName());
      variables.add(Variable.simple(name, attribute.getProminentName(), attribute.getSyntax().getCorrespondingJavaClass(), attribute.isList()));
    }
    return variables;
  }


  private static TypeMappingEntry generateMappingEntry(String ldapName, String fqXmlName) {
    long id = idGenerator.getUniqueId();
    String xynaObjectName;
    try {
      xynaObjectName = GenerationBase.transformNameForJava(fqXmlName);
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException("Invalid name for code generation: " + fqXmlName);
    }
    return new TypeMappingEntry(id, TYPEMAPPING_COMPONENT_IDENTIFIER, ldapName, xynaObjectName);
  }


  private static void cleanupTypeMappings() {
    try {
      mappingLDAPNameToFqXynaObjectName.deleteAll(mappingLDAPNameToFqXynaObjectName.readTypeMappingEntries(TYPEMAPPING_COMPONENT_IDENTIFIER));
    } catch (PersistenceLayerException e) {
      logger.warn("Failed to cleanup TypeMappings!", e);
    }
  }



}

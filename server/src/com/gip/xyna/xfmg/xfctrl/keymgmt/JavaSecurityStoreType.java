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
package com.gip.xyna.xfmg.xfctrl.keymgmt;

import java.io.File;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xfmg.exceptions.XFMG_KeyStoreConversionError;
import com.gip.xyna.xfmg.exceptions.XFMG_KeyStoreImportError;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.PluginDescription.PluginType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.securestorage.SecureStorage;


public class JavaSecurityStoreType implements KeyStoreType<JavaSecurityKeyStore> {

  private final static Logger logger = CentralFactoryLogging.getLogger(JavaSecurityStoreType.class);
  
  public final static String NAME = "java.security";
  public final static String OLD_NAME = "ExternalFile";
  private final static String VERSION = "1.0.0";
  private final static String DESCRIPTION = "java.security.KeyStore backed by external file";
  private final static KeyStoreCapability[] CAPABILITIES =
                   new KeyStoreCapability[] { /*KeyStoreCapability.EXPORT*/ };
  public static final StringParameter<String> PASSPHRASE = 
                  StringParameter.typeString("passphrase")
                                 .label("Passphrase")
                                 .documentation(Documentation
                                                .en("Passphrase of key store file")
                                                .de("Passwort der Schlüsseldatei")
                                                .build())
                                 .optional().build();
  
  public static final StringParameter<String> FILE_TYPE = 
                  StringParameter.typeString("filetype")
                                 .label("File type")
                                 .documentation(Documentation
                                                .en("The type of key file given")
                                                .de("Formatsangabe für die Schlüsseldatei")
                                                .build())
                                 .defaultValue(java.security.KeyStore.getDefaultType()).build();
                  
  
  public static final List<StringParameter<?>> importParameters = StringParameter.asList( PASSPHRASE, FILE_TYPE );
  public static final List<StringParameter<?>> conversionParameters = StringParameter.asList( PASSPHRASE );
  
  public KeyStoreTypeIdentifier getTypeIdentifier() {
    return new KeyStoreTypeIdentifier(NAME, VERSION);
  }


  public Set<KeyStoreCapability> getCapabilities() {
    return new HashSet<KeyStoreType.KeyStoreCapability>(Arrays.asList(CAPABILITIES));
  }


  public KeyStoreStorable importKeyStore(String name, File file, Map<String, Object> parsedParams) throws XFMG_KeyStoreImportError {
    String passphrase = PASSPHRASE.getFromMapAsString(parsedParams); 
    if (passphrase != null) {
      try {
        XynaFactory.getInstance().getXynaMultiChannelPortal().getSecureStorage().store(NAME, name, passphrase);
        parsedParams.remove(PASSPHRASE.getName());
      } catch (PersistenceLayerException e) {
        throw new XFMG_KeyStoreImportError(e);
      }
    }
    return new KeyStoreStorable(name, this, file.getAbsolutePath(), parsedParams);
  }
  
  
  public void removeKeyStore(String name) {
    try {
      SecureStorage secStore = XynaFactory.getInstance().getXynaMultiChannelPortal().getSecureStorage();
      if (!secStore.remove(NAME, name)) {
        secStore.remove(OLD_NAME, name);  
      }
    } catch (PersistenceLayerException e) {
      // TODO throw specific exception?
      logger.warn("Failed to delete from store", e);
    }
  }
  
  static String getPassphrase(String name) {
    SecureStorage secStore = XynaFactory.getInstance().getXynaMultiChannelPortal().getSecureStorage();
    Object passphrase = secStore.retrieve(NAME, name);
    if (passphrase == null) {
      passphrase = secStore.retrieve(OLD_NAME, name);
      if (passphrase != null) {
        try {
          secStore.remove(OLD_NAME, name);
          secStore.store(NAME, name, (String)passphrase);
        } catch (PersistenceLayerException e) {
          logger.debug("Error during lazy keyStore update",e);
        }
      }
    }
    return (String)passphrase;
  }
  
  @Override
  public JavaSecurityKeyStore createKeyStore(KeyStoreStorable parameter) {
    return new JavaSecurityKeyStore(parameter);
  }



  public PluginDescription getTypeDescription() {
    PluginDescription pluginDescription;
    pluginDescription = PluginDescription.create(PluginType.keystoretype).
                    name(NAME).
                    label(NAME + " " + VERSION).
                    description(DESCRIPTION).
                    parameters(PluginDescription.ParameterUsage.Create, importParameters).
                    parameters(PluginDescription.ParameterUsage.Configure, conversionParameters).
                    build();
    return pluginDescription;
  }


  @SuppressWarnings("rawtypes")
  public Set<ConversionCapability> getConversions() {
    Set<ConversionCapability> set = new HashSet<ConversionCapability>();
    set.add(new KeyStoreConversion());
    set.add(new KeyManagerFactoryConversion());
    set.add(new TrustManagerFactoryConversion());
    return set;
  }

  
  private static class KeyStoreConversion implements ConversionCapability<JavaSecurityKeyStore, KeyStore> {

    public KeyStore convert(JavaSecurityKeyStore ks, Map<String, Object> parsedParams) throws XFMG_KeyStoreConversionError {
      try {
        return ks.convert(parsedParams);
      } catch (Exception e) {
        throw new XFMG_KeyStoreConversionError(ks.getName(), KeyStore.class.getName(), e);
      }
    }

    public Class<KeyStore> getTargetClass() {
      return KeyStore.class;
    }
    
  }
  
  
  private static class KeyManagerFactoryConversion implements ConversionCapability<JavaSecurityKeyStore, KeyManagerFactory> {

    public KeyManagerFactory convert(JavaSecurityKeyStore ks, Map<String, Object> parsedParams) throws XFMG_KeyStoreConversionError {
      try {
        KeyStore keystore = ks.convert(parsedParams);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        String passphrase = getPassphrase(ks.getName());
        char[] chars = null;
        if (passphrase != null && 
            passphrase.length() > 0) {
          chars = passphrase.toCharArray();
        }
        kmf.init(keystore, chars);
        return kmf;
      } catch (Exception e) {
        throw new XFMG_KeyStoreConversionError(ks.getName(), KeyManagerFactory.class.getName(), e);
      }
    }

    public Class<KeyManagerFactory> getTargetClass() {
      return KeyManagerFactory.class;
    }
    
  }
  
  
  private static class TrustManagerFactoryConversion implements ConversionCapability<JavaSecurityKeyStore, TrustManagerFactory> {

    public TrustManagerFactory convert(JavaSecurityKeyStore ks, Map<String, Object> parsedParams) throws XFMG_KeyStoreConversionError {
      try {
        KeyStore ts = ks.convert(parsedParams);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);
        return tmf;
      } catch (Exception e) {
        throw new XFMG_KeyStoreConversionError(ks.getName(), TrustManagerFactory.class.getName(), e);
      }
    }

    public Class<TrustManagerFactory> getTargetClass() {
      return TrustManagerFactory.class;
    }
    
  }



}

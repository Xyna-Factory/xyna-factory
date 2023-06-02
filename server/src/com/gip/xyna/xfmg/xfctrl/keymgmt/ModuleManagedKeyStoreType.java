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
package com.gip.xyna.xfmg.xfctrl.keymgmt;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xfmg.exceptions.XFMG_KeyStoreImportError;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.PluginDescription.PluginType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.securestorage.SecureStorage;


public class ModuleManagedKeyStoreType implements KeyStoreType<ModuleManagedKeyStore> {

  private final static Logger logger = CentralFactoryLogging.getLogger(ModuleManagedKeyStoreType.class);
  
  public final static String NAME = "ModuleManaged";
  private final static String VERSION = "1.0.0";
  private final static String DESCRIPTION = "Externally managed KeyStore backed by external file";
  private final static KeyStoreCapability[] CAPABILITIES =
                   new KeyStoreCapability[] {};
  public static final StringParameter<String> PASSPHRASE = 
                  StringParameter.typeString("passphrase")
                                 .label("Passphrase")
                                 .documentation(Documentation
                                                .en("Passphrase of key store file")
                                                .de("Passwort der Schlüsseldatei")
                                                .build())
                                 .optional().build();
  
  
  public static final List<StringParameter<?>> importParameters = StringParameter.asList( PASSPHRASE );

  
  public KeyStoreTypeIdentifier getTypeIdentifier() {
    return new KeyStoreTypeIdentifier(NAME, VERSION);
  }


  public Set<KeyStoreCapability> getCapabilities() {
    return new HashSet<KeyStoreType.KeyStoreCapability>(Arrays.asList(CAPABILITIES));
  }


  public PluginDescription getTypeDescription() {
    PluginDescription pluginDescription;
    pluginDescription = PluginDescription.create(PluginType.keystoretype).
                    name(NAME).
                    label(NAME + " " + VERSION).
                    description(DESCRIPTION).
                    parameters(PluginDescription.ParameterUsage.Create, importParameters).
                    //parameters(PluginDescription.ParameterUsage.Configure, conversionParameters).
                    build();
    return pluginDescription;
  }


  @SuppressWarnings("rawtypes")
  public Set<ConversionCapability> getConversions() {
    return Collections.emptySet();
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
      XynaFactory.getInstance().getXynaMultiChannelPortal().getSecureStorage().remove(NAME, name);
    } catch (PersistenceLayerException e) {
      // TODO throw specific exception?
      logger.warn("Failed to delete from store", e);
    }
  }


  public ModuleManagedKeyStore createKeyStore(KeyStoreStorable parameter) {
    SecureStorage secStore = XynaFactory.getInstance().getXynaMultiChannelPortal().getSecureStorage();
    String passphrase;
    Serializable retrieved = secStore.retrieve(NAME, parameter.getName());
    if (retrieved == null) {
      passphrase = null;
    } else {
      passphrase = retrieved.toString();
    }
    return new ModuleManagedKeyStore(parameter, passphrase);
  }


}

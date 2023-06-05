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
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.utils.misc.StringParameter.Unmatched;
import com.gip.xyna.utils.misc.StringParameter.Unparseable;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateKeyStoreName;
import com.gip.xyna.xfmg.exceptions.XFMG_KeyStoreConversionError;
import com.gip.xyna.xfmg.exceptions.XFMG_KeyStoreImportError;
import com.gip.xyna.xfmg.exceptions.XFMG_KeyStoreValidationError;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownKeyStore;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownKeyStoreType;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyStoreType.ConversionCapability;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyStoreType.KeyStoreCapability;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class KeyManagement extends FunctionGroup {

  public static final String DEFAULT_NAME = "KeyManagement";
  
  private ConcurrentMap<KeyStoreTypeIdentifier, KeyStoreType<?>> registeredTypes;
  
  public KeyManagement() throws XynaException {
    super();
  }

  
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  protected void init() throws XynaException {
    registeredTypes = new ConcurrentHashMap<KeyStoreTypeIdentifier, KeyStoreType<? extends KeyStore>> ();
    //keyStores = new ConcurrentHashMap<String, KeyStoreType<? extends KeyStore>>();
    
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(KeyManagement.class,"KeyManagement.initExternalFileType").
      after(PersistenceLayerInstances.class).
      execAsync(new Runnable() { public void run() { initExternalFileType(); }});
    
  }


  protected void shutdown() throws XynaException {

  }
  
  private void initExternalFileType() {
    try {
      registerKeyStoreType(new JavaSecurityStoreType());
      registerKeyStoreType(new ModuleManagedKeyStoreType());
      ODSImpl.getInstance().registerStorable(KeyStoreStorable.class);
    } catch (PersistenceLayerException e) {
      logger.warn("Failed to register ExternalFileStoreType",e);
    }
  }
  
  /*private void addKeyStore(KeyStoreType<? extends KeyStore> type, KeyStore keyStore) throws XFMG_DuplicateKeyStoreName {
    KeyStoreType<? extends KeyStore> previousEntry = keyStores.putIfAbsent(keyStore.getName(), type);
    if (previousEntry != null) {
      throw new XFMG_DuplicateKeyStoreName(keyStore.getName());
    }
  }*/
  
  public void registerKeyStoreType(KeyStoreType<?> type) {
    // TODO put if absent, throw on duplicated (once we have more then 1 type)
    registeredTypes.put(type.getTypeIdentifier(), type);
    /*Collection<? extends KeyStore> stores = type.getKeyStores();
    for (KeyStore keyStore : stores) {
      try {
        addKeyStore(type, keyStore);
      } catch (XFMG_DuplicateKeyStoreName e) {
        logger.warn("Duplicated KeyStoreName during type registration of " + type.getClass().getName(), e);
      }
    }*/
  }
  
  public Collection<KeyStoreType<?>> getRegisteredKeyStoreTypes() {
    return registeredTypes.values();
  }
  
  public KeyStoreType<? extends KeyStore> getRegisteredKeyStoreType(KeyStoreTypeIdentifier ksti) throws XFMG_UnknownKeyStoreType {
    KeyStoreType<? extends KeyStore> type = registeredTypes.get(ksti);
    if (type == null) {
      throw new XFMG_UnknownKeyStoreType(ksti.toString());
    }
    return type;
  }
  

  @SuppressWarnings("unchecked")
  public <K extends KeyStore> K importKeyStore(KeyStoreTypeIdentifier ksti, String name, File file, Map<String, String> params) throws XFMG_KeyStoreImportError, XFMG_UnknownKeyStoreType, XFMG_DuplicateKeyStoreName {
    if (!file.exists()) {
      throw new XFMG_KeyStoreImportError(new FileNotFoundException(file.getAbsolutePath()));
    }
    
    KeyStoreType<K> kst = (KeyStoreType<K>) getRegisteredKeyStoreType(ksti);
    Map<String, Object> parsedParams;
    try {
      parsedParams = parseParams(params, kst, ParameterUsage.Create);
    } catch (StringParameterParsingException e) {
      throw new XFMG_KeyStoreImportError(e);
    }
    
    KeyStoreStorable imported = kst.importKeyStore(name, file, parsedParams);
    
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      if (con.containsObject(imported)) {
        throw new XFMG_DuplicateKeyStoreName(name);
      }
      con.persistObject(imported);
      con.commit();
    } catch (PersistenceLayerException e) {
      kst.removeKeyStore(name);
      throw new XFMG_KeyStoreImportError(e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.debug("Failed to close connection", e);
      }
    }

    K keyStore = kst.createKeyStore(imported);
    try {
      testKeyStore(kst, keyStore, parsedParams);
    } catch (XFMG_KeyStoreValidationError e) {
      kst.removeKeyStore(name);
      try {
        removeKeyStore(name, kst);
      } catch (XFMG_UnknownKeyStore ee) {
        logger.debug("Error while trying to rollback keyStore creation", ee);
      }
      throw new XFMG_KeyStoreImportError(e);
    }
    return keyStore;
  }
  
  
  private Map<String, Object> parseParams(Map<String, String> params, KeyStoreType<?> kst, ParameterUsage usage) throws StringParameterParsingException {
    List<StringParameter<?>> description = kst.getTypeDescription().getParameters(usage);
    return StringParameter.parse(params)
                          .unmatchedKey(Unmatched.Error)
                          .unparseableValue(Unparseable.Ignore)
                          .with(description);
  }


  @SuppressWarnings({"unchecked", "rawtypes"})
  private <K extends KeyStore> void testKeyStore(KeyStoreType<K> kst, K imported, Map<String, Object> parsedParams) throws XFMG_KeyStoreValidationError {
    for (ConversionCapability cc : kst.getConversions()) {
      try {
        cc.convert(imported, parsedParams);
      } catch (Exception e) {
        throw new XFMG_KeyStoreValidationError(e);
      }
    }
  }
  
  public <K extends KeyStore> K getKeyStore(String keyStoreName) throws XFMG_UnknownKeyStore, XFMG_UnknownKeyStoreType {
    KeyStoreStorable kss = getKeyStoreStorable(keyStoreName);
    KeyStoreTypeIdentifier ksti = KeyStoreTypeIdentifier.with(kss.getType(), kss.getVersion());
    @SuppressWarnings("unchecked")
    KeyStoreType<K> kst = (KeyStoreType<K>) registeredTypes.get(ksti);
    K ks = kst.createKeyStore(kss);
    if (ks == null) {
      throw new XFMG_UnknownKeyStore(kst.toString());
    }
    return ks;
  }
  
  private KeyStoreStorable getKeyStoreStorable(String keyStoreName) throws XFMG_UnknownKeyStore, XFMG_UnknownKeyStoreType {
    KeyStoreStorable kss = new KeyStoreStorable(keyStoreName);
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      try {
        con.queryOneRow(kss);
      } catch (PersistenceLayerException e) {
        // TODO throw specific exception?
        logger.warn("Failed to query store", e);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new XFMG_UnknownKeyStore(keyStoreName);
      }
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.debug("Failed to close connection", e);
      }
    }
    return kss;
  }

  @SuppressWarnings("unchecked")
  private <K extends KeyStore> KeyStoreType<K> getKeyStoreType(String keyStoreName) throws XFMG_UnknownKeyStore, XFMG_UnknownKeyStoreType {
    KeyStoreStorable kss = getKeyStoreStorable(keyStoreName);
    if (kss == null) {
      throw new XFMG_UnknownKeyStore(keyStoreName);
    }
    KeyStoreTypeIdentifier ksti = KeyStoreTypeIdentifier.with(kss.getType(), kss.getVersion());
    return (KeyStoreType<K>) registeredTypes.get(ksti);
  }
  
  @SuppressWarnings({"unchecked", "rawtypes"})
  public <C> C getKeyStore(String keyStoreName, Class<C> conversionTarget, Map<String, String> params) throws StringParameterParsingException, XFMG_KeyStoreConversionError, XFMG_UnknownKeyStoreType, XFMG_UnknownKeyStore {
    KeyStoreType<?> kst = getKeyStoreType(keyStoreName);
    KeyStore ks = getKeyStore(keyStoreName);
    for (ConversionCapability cc : kst.getConversions()) {
      if (conversionTarget.isAssignableFrom(cc.getTargetClass())) {
        C conversion;
        try {
          conversion = conversionTarget.cast(cc.convert(ks, parseParams(params, kst, ParameterUsage.Configure)));
        } catch (RuntimeException e) {
          throw new XFMG_KeyStoreConversionError(keyStoreName, conversionTarget.getName(), e);
        }
        if (conversion != null) {
          return conversion;
        }
      }
    }
    return null;
  }
  
  
  public void removeKeyStore(String keyStoreName) throws XFMG_UnknownKeyStoreType {
    try {
      KeyStoreType<?> kst = getKeyStoreType(keyStoreName);
      removeKeyStore(keyStoreName, kst);
    } catch (XFMG_UnknownKeyStore e) {
      // no keyStore -> nothing to do
    } // TODO catch XFMG_UnknownKeyStoreType as well and delete the storable in that case? We would skip a potantial type specific cleanup
  }
  
  private void removeKeyStore(String keyStoreName, KeyStoreType<?> kst) throws XFMG_UnknownKeyStore, XFMG_UnknownKeyStoreType {
    KeyStoreStorable kss = getKeyStoreStorable(keyStoreName);
    kst.removeKeyStore(keyStoreName);
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      con.deleteOneRow(kss);
      con.commit();
    } catch (PersistenceLayerException e) {
      logger.debug("Failed to remove keyStore",e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.debug("Error while trying to close connection",e);
      }
    }
  }
  
  public Collection<KeyStoreInformation> listKeyStores() throws PersistenceLayerException {
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
        return con.loadCollection(KeyStoreStorable.class).stream().map(KeyStoreInformation::new)
                                                                  .collect(Collectors.toUnmodifiableList());
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.debug("Failed to close connection", e);
      }
    }
  }
  
  private static boolean checkCapability(KeyStoreType<?> kst, KeyStoreCapability capability) {
    return kst.getCapabilities().contains(capability);
  }
  
  

}

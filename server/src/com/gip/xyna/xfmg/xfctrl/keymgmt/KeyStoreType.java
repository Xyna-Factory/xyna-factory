/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.xfmg.exceptions.XFMG_KeyStoreConversionError;
import com.gip.xyna.xfmg.exceptions.XFMG_KeyStoreImportError;
import com.gip.xyna.xmcp.PluginDescription;

public interface KeyStoreType<K extends KeyStore> {
  
  public KeyStoreTypeIdentifier getTypeIdentifier();
  
  public Set<KeyStoreCapability> getCapabilities();
  
  @SuppressWarnings("rawtypes")
  public Set<ConversionCapability> getConversions();
  
  //public Collection<K> getKeyStores();
  
  //public K getKeyStore(String name);
  
  public void removeKeyStore(String name);
  
  //public K importKeyStore(String name, File file, Map<String, Object> params) throws XFMG_KeyStoreImportError;
  
  public KeyStoreStorable importKeyStore(String name, File file, Map<String, Object> params) throws XFMG_KeyStoreImportError;
  
  public K createKeyStore(KeyStoreStorable parameter);
  
  public PluginDescription getTypeDescription();
  
  
  public static enum KeyStoreCapability { 
    IN_STORE_MANIPULATION,
    EXPORT;
  };
    
    
  
  public static interface ConversionCapability<K extends KeyStore, C> {
    
    public C convert(K ks, Map<String, Object> parsedParams) throws XFMG_KeyStoreConversionError;
    
    public Class<C> getTargetClass();
    
  }
  
  
  
}

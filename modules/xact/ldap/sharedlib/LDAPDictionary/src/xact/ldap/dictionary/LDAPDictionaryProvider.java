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
package xact.ldap.dictionary;


import java.util.concurrent.atomic.AtomicReference;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class LDAPDictionaryProvider {
  
  private static AtomicReference<LDAPDictionaryProvider> instance = new AtomicReference<LDAPDictionaryProvider>(null);
  
  private volatile LDAPSchemaDictionary dictionary;
  
  private LDAPDictionaryProvider() {
    
  }
  
  public static LDAPDictionaryProvider getInstance() {
    LDAPDictionaryProvider provider = instance.get();
    if (provider == null) {
      provider = new LDAPDictionaryProvider();
      instance.compareAndSet(null, provider);
      provider = instance.get();
    }
    return provider;
  }
  
  
  public LDAPSchemaDictionary getDictionary() {
    if (dictionary == null) {
      synchronized (LDAPDictionaryProvider.class) {
        if (dictionary == null) {
          ODS ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
          try {
            ods.registerStorable(LDAPObjectClassDictionaryEntry.class);
          } catch (PersistenceLayerException e) {
            throw new RuntimeException("Could not restore LDAPSchemaDictionary from persistence",e);
          }
          ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
          try {
            dictionary = new LDAPSchemaDictionary(con);
          } catch (PersistenceLayerException e) {
            throw new RuntimeException("Could not restore LDAPSchemaDictionary from persistence",e);
          } finally {
            try {
              con.closeConnection();
            } catch (PersistenceLayerException e) {
              // TODO log
            }
          }
        }
      }
    }
    return dictionary;
  }
  
  public void setDictionary(LDAPSchemaDictionary dictionary) {
    this.dictionary = dictionary;
  }
  

}

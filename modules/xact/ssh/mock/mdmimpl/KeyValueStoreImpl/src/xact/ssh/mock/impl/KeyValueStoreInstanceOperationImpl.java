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
package xact.ssh.mock.impl;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import xact.ssh.mock.KeyValuePair;
import xact.ssh.mock.KeyValueStore;
import xact.ssh.mock.KeyValueStoreInstanceOperation;
import xact.ssh.mock.KeyValueStoreSuperProxy;


public class KeyValueStoreInstanceOperationImpl extends KeyValueStoreSuperProxy implements KeyValueStoreInstanceOperation {

  private static final long serialVersionUID = 1L;
  
  private Map<String,String> keyValueMap = new ConcurrentHashMap<String,String>();

  public KeyValueStoreInstanceOperationImpl(KeyValueStore instanceVar) {
    super(instanceVar);
  }

  @Override
  public KeyValuePair getKeyValue(KeyValuePair keyValuePair) {
    String key = keyValuePair.getKey();
    String value = keyValueMap.get(key);
    return new KeyValuePair(key,value);
  }

  @Override
  public KeyValuePair putKeyValue(KeyValuePair keyValuePair) {
    String key = keyValuePair.getKey();
    String value = keyValuePair.getValue();
    String old = keyValueMap.put(key, value);
    return new KeyValuePair(key,old);
  }
  
  @Override
  public void removeKeyValue(KeyValuePair keyValuePair) {
    String key = keyValuePair.getKey();
    keyValueMap.remove(key);
  }
  
  @Override
  public String getKeyValueString(String key) {
    return keyValueMap.get(key);
  }
  
  @Override
  public String putKeyValueString(String key, String value) {
    return keyValueMap.put(key, value);
  }
  
  @Override
  public void removeKeyValueString(String key) {
    keyValueMap.remove(key);
  }

  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
  }

  @Override
  public List<? extends KeyValuePair> listAllKeyValuePairs() {
    List<KeyValuePair> list = new ArrayList<KeyValuePair>();
    for( Map.Entry<String,String> entry : keyValueMap.entrySet() ) {
      list.add( new KeyValuePair(entry.getKey(), entry.getValue() ));
    }
    return list;
  }

}

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

package com.gip.xyna.xact.filter.session.cache;



import java.util.Set;

import com.gip.xyna.xact.filter.util.xo.IClassIdentityCache;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;



public class JsonBuilderCache implements IClassIdentityCache<Set<String>> {

  private ClassIdentityCache<Set<String>> internalCache = new ClassIdentityCache<Set<String>>() {
    
    @Override
    protected Set<String> addOrReplaceEntry(String fqXmlName, Long revision, Class<? extends GeneralXynaObject> clazz) {
      synchronized (actualCache) {
        actualCache.put(clazz, null); //only sets key, value will be placed separately (-> insertIntoCache)
      }
      return null;
    }
  };
  

  @Override
  public void insertIntoCache(Class<? extends GeneralXynaObject> clazz, Set<String> object) {
    synchronized (internalCache.actualCache) {
      internalCache.actualCache.put(clazz, object);
    }
  }

  @Override
  public Set<String> getFromCache(Class<? extends GeneralXynaObject> clazz) {
    return internalCache.getFromCache(clazz);
  }




}

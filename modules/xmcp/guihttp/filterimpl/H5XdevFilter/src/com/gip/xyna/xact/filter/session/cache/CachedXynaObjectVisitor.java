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

package com.gip.xyna.xact.filter.session.cache;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.gip.xyna.xact.filter.util.xo.XynaObjectVisitor;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;

public class CachedXynaObjectVisitor extends XynaObjectVisitor {

  private JsonVisitorCache cache;
  
  public CachedXynaObjectVisitor(JsonVisitorCache cache) {
    this.cache = cache;
  }
  
  @Override
  protected Field oFindField(Class<? extends GeneralXynaObject> clazz, String label) {
    
    Map<String, Field> map = cache.getFromCache(clazz);
    if(map == null) {
      map = new ConcurrentHashMap<String, Field>();
      cache.insertIntoCache(clazz, map);
      map = cache.getFromCache(clazz);
      Field field = super.findField(clazz, label);
      map.put(label, field);
    }
    
    Field result = map.get(label);
    
    if (result == null) {
      result = super.findField(clazz, label);
      if (result != null) {
        map.put(label, result);
      }
    }

    return result;
  }
}

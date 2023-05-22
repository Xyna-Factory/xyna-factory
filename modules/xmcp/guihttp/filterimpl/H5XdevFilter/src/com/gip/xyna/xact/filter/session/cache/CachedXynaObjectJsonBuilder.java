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

import java.util.Set;

import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.xact.filter.util.xo.XynaObjectJsonBuilder;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;

public class CachedXynaObjectJsonBuilder extends XynaObjectJsonBuilder{

  private JsonBuilderCache cache;
  
  public CachedXynaObjectJsonBuilder(RuntimeContext rc, RuntimeContext[] backupRCs, JsonBuilder builder, JsonBuilderCache bCache, JsonVisitorCache vCache) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    super(rc, backupRCs, builder);
    this.cache = bCache;
    this.visitor = new CachedXynaObjectVisitor(vCache);
    
  }
  
  public CachedXynaObjectJsonBuilder(long revision, long[] backupRevisions, JsonBuilder builder, JsonBuilderCache bCache, JsonVisitorCache vCache) {
    super(revision, backupRevisions, builder);
    this.cache = bCache;
    this.visitor = new CachedXynaObjectVisitor(vCache);
  }

  public CachedXynaObjectJsonBuilder(long revision, JsonBuilder jsonBuilder, JsonBuilderCache bCache, JsonVisitorCache vCache) {
    super(revision, jsonBuilder);
    this.cache = bCache;
    this.visitor = new CachedXynaObjectVisitor(vCache);
  }
  
  
  @SuppressWarnings("unchecked")
  @Override
  protected Set<String> getVariableNames(GeneralXynaObject gxo) {
    Class<GeneralXynaObject> clazz = (Class<GeneralXynaObject>) gxo.getClass();
    Set<String> result = cache.getFromCache(clazz);
    if(result == null) {
      result = super.getVariableNames(gxo);
      cache.insertIntoCache(clazz, result);
    }
    
    return result;
  }
}

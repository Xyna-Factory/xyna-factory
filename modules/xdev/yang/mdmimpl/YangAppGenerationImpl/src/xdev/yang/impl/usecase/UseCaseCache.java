/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xdev.yang.impl.usecase;

import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.yangcentral.yangkit.model.api.stmt.Module;

import com.gip.xyna.utils.collections.LruCache;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;

import xmcp.yang.LoadYangAssignmentsData;


public class UseCaseCache {
  
  public static final XynaPropertyInt PROP_USECASE_CACHE_SIZE = new XynaPropertyInt("xdev.yang.UseCaseCache.size", 2)
    .setDefaultDocumentation(DocumentationLanguage.EN,
    "Number of recently loaded yang usescases for which xyna factory keeps the parsed data in cache (0 = no cache used)")
    .setDefaultDocumentation(DocumentationLanguage.DE,
    "Anzahl der zuletzt geladenen Yang UseCases für welche die Xyna Factory geparste Daten im Cache behält (0 = kein cache wird verwendet) ");
  
  private static Logger _logger = Logger.getLogger(UseCaseCache.class);
  private static UseCaseCache _instance = null;
  private LruCache<UseCaseCacheId, List<Module>> _cache;
  private int _lastCacheSize = 0;
  
  
  private UseCaseCache() {
    int size = readCurrentSize();
    refresh(size);
  }
  
  synchronized public void put(LoadYangAssignmentsData data, List<Module> modules) {
    refreshIfNecessary();
    UseCaseCacheId id = new UseCaseCacheId(data);
    _cache.put(id, modules);
  }
  
  synchronized public Optional<List<Module>> get(LoadYangAssignmentsData data) {
    refreshIfNecessary();
    UseCaseCacheId id = new UseCaseCacheId(data);
    return Optional.ofNullable(_cache.get(id));
  }
  
  private int readCurrentSize() {
    Integer size = PROP_USECASE_CACHE_SIZE.get();
    if (size == null) { return 0; }
    return size;
  }
  
  private void refreshIfNecessary() {
    int size = readCurrentSize();
    if (size == _lastCacheSize) { return; }
    if (size == 0) {
      _cache.clear();
    }
    refresh(size);
  }
  
  private void refresh(int size) {
    _logger.debug("Refreshing useCase cache, size = " + size);
    _lastCacheSize = size;
    _cache = new LruCache<UseCaseCacheId, List<Module>>(size);
  }
  
  
  public static UseCaseCache getInstance() {
    synchronized (UseCaseCache.class) {
      if (_instance == null) {
        _instance = new UseCaseCache();
      }
    }
    return _instance;
  }
  
}

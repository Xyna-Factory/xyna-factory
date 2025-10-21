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

package xdev.yang.impl.operation;

import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.yangcentral.yangkit.model.api.stmt.Module;

import com.gip.xyna.utils.collections.LruCache;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;


public class OperationCache {
  
  public static final XynaPropertyInt PROP_OPERATION_CACHE_SIZE = new XynaPropertyInt("xdev.yang.OperationCache.size", 2)
    .setDefaultDocumentation(DocumentationLanguage.EN,
    "Number of recently loaded yang usescases for which xyna factory keeps the parsed data in cache (0 = no cache used)")
    .setDefaultDocumentation(DocumentationLanguage.DE,
    "Anzahl der zuletzt geladenen Yang Operations für welche die Xyna Factory geparste Daten im Cache behält (0 = kein cache wird verwendet) ");
  
  private static Logger _logger = Logger.getLogger(OperationCache.class);
  private static OperationCache _instance = null;
  private LruCache<OperationCacheId, List<Module>> _cache;
  private int _lastCacheSize = 0;
  
  
  private OperationCache() {
    int size = readCurrentSize();
    refresh(size);
  }
  
  synchronized public void put(OperationCacheId id, List<Module> modules) {
    refreshIfNecessary();
    _cache.put(id, modules);
  }
  
  synchronized public Optional<List<Module>> get(OperationCacheId id) {
    refreshIfNecessary();
    return Optional.ofNullable(_cache.get(id));
  }
  
  private int readCurrentSize() {
    Integer size = PROP_OPERATION_CACHE_SIZE.get();
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
    _logger.debug("Refreshing operation cache, size = " + size);
    _lastCacheSize = size;
    _cache = new LruCache<OperationCacheId, List<Module>>(size);
  }
  
  
  public static OperationCache getInstance() {
    synchronized (OperationCache.class) {
      if (_instance == null) {
        _instance = new OperationCache();
      }
    }
    return _instance;
  }
  
}

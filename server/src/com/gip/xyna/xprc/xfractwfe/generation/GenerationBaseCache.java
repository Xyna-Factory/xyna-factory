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
package com.gip.xyna.xprc.xfractwfe.generation;



import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport;
import com.gip.xyna.utils.collections.ObjectWithRemovalSupport;



public class GenerationBaseCache {

  private static final Logger logger = CentralFactoryLogging.getLogger(GenerationBaseCache.class);

  
  public static class InnerMap extends ObjectWithRemovalSupport  {
    private final ConcurrentMap<String, GenerationBase> map = new ConcurrentHashMap<String, GenerationBase>(32, 0.75f, 2);

    @Override
    protected boolean shouldBeDeleted() {
      return map.size() == 0;
    }
  }
  
  private final ConcurrentMapWithObjectRemovalSupport<Long, InnerMap> cache =
      new ConcurrentMapWithObjectRemovalSupport<Long, InnerMap>(16, 0.75f, 4) {

        private static final long serialVersionUID = -2078069093469194752L;

        @Override
        public InnerMap createValue(Long key) {
          return createInnerMap(key);
        }
    
  };
  
  public InnerMap createInnerMap(long revision) {
    return new InnerMap();
  }

  //Achtung: hier passiert kein cleanup. Auf eigene Gefahr aufrufen
  public InnerMap getOrCreateInnerMap(long revision) {
    return cache.lazyCreateGet(revision);
  }

  public void printCacheContent() {
    if (cache.size() > 0) {
      logger.warn("cache content (size=" + size() + "):");
      for (Entry<Long, InnerMap> e1 : cache.entrySet()) {
        logger.warn("revision = " + e1.getKey());
        for (Entry<String, GenerationBase> e : e1.getValue().map.entrySet()) {
          logger.warn("  " + e.getKey() + " -> " + e.getValue());
        }
      }
    }
  }


  public GenerationBase getFromCache(String fqXmlName, Long revision) {
    GenerationBase gb = cache.lazyCreateGet(revision).map.get(fqXmlName);
    cache.cleanup(revision);
    return gb;
  }


  public void insertIntoCache(GenerationBase gb) {
    cache.lazyCreateGet(gb.revision).map.put(gb.getOriginalFqName(), gb);
    cache.cleanup(gb.revision);
  }



  public int getNumberOfRevisions() {
    return cache.keySet().size();
  }


  public int size() {
    int size = 0;
    for (InnerMap m : cache.values()) {
      size += m.map.size();
    }
    return size;
  }


  public void clear() {
    cache.clear();
  }


  public boolean remove(GenerationBase gb) {
     boolean b = cache.lazyCreateGet(gb.revision).map.remove(gb.getOriginalFqName()) != null;
     cache.cleanup(gb.revision);
     return b;
  }


  public void replaceInCache(GenerationBase oldGB, GenerationBase newGB) {
    cache.lazyCreateGet(oldGB.revision).map.replace(oldGB.getOriginalFqName(), oldGB, newGB);
    cache.cleanup(oldGB.revision);
  }


  /**
   * objekt kann auch in abhängiger revision definiert sein
   */
  public GenerationBase getFromCacheInCorrectRevision(String fqXmlName, Long revision) {
    GenerationBase gb = getFromCache(fqXmlName, revision);
    if (gb == null) {
      revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getRevisionDefiningXMOMObjectOrParent(fqXmlName, revision);
      gb = getFromCache(fqXmlName, revision);
    }
    return gb;
  }


  public Collection<GenerationBase> values(long revision) {
    Collection<GenerationBase> c = cache.lazyCreateGet(revision).map.values();
    cache.cleanup(revision);
    return c;
  }


  public Collection<Long> revisions() {
    return cache.keySet();
  }


  public void removeRevision(long revision) {
    cache.remove(revision);
  }

}

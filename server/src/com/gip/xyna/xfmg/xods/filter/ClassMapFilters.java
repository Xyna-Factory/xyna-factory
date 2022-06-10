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
package com.gip.xyna.xfmg.xods.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xods.filter.ClassMapFilter.FilterElement;

public class ClassMapFilters {
  
  private static volatile ClassMapFilters instance;
  
  private ConcurrentMap<String, Map<String, StringMapper<?>>> registeredMapper;
  private Map<String, Class<? extends Filter>> registeredFilter;

  
  private ClassMapFilters() {
    registeredMapper = new ConcurrentHashMap<String, Map<String, StringMapper<?>>>();
    registeredFilter = new HashMap<String, Class<? extends Filter>>();
    for (Filter defaultFilter : new Filter[] {new Filter.BlackListFilter(), new Filter.WhiteListFilter(), new Filter.RegExpFilter()}) {
      registeredFilter.put(defaultFilter.getIdentifier(), defaultFilter.getClass());
    }
  }
  
  
  public static ClassMapFilters getInstance() {
    if (instance == null) {
      synchronized (ClassMapFilters.class) {
        if (instance == null) {
          instance = new ClassMapFilters();
        }
      }
    }
    return instance;
  }
  
  
  public void registerMapper(String clazz, StringMapper<?> adapter) {
    Map<String, StringMapper<?>> mappers = registeredMapper.get(clazz);
    if (mappers == null) {
      registeredMapper.putIfAbsent(clazz, new HashMap<String, StringMapper<?>>());
      mappers = registeredMapper.get(clazz);
    }
    mappers.put(adapter.getIdentifier(), adapter);
  }
  
  
  protected Filter instantiateFilter(String filtername, String... params) {
    Class<? extends Filter> clazz = registeredFilter.get(filtername);
    if (clazz != null) {
      try {
        Filter filter = clazz.getConstructor().newInstance();
        filter.initialize(params);
        return filter;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new IllegalArgumentException("Filter " + filtername + " is unknown!");
    }
  }
  
  
  protected boolean isClassKnown(String clazz) {
    return registeredMapper.containsKey(clazz);
  }
  
  
  protected boolean isMapperKnown(String clazz, String mapper) {
    if (isClassKnown(clazz)) {
      return registeredMapper.get(clazz).containsKey(mapper);
    } else {
      return false;
    }
  }
  
  
  protected StringMapper<?> getMapper(String clazz, String mapper) {
    if (isMapperKnown(clazz, mapper)) {
      return registeredMapper.get(clazz).get(mapper);
    } else {
      return null;
    }
  }
  
  
  public enum TerminalStreamOperation {
    anyMatch {
      @Override
      public <E> boolean evaluate(List<FilterElement<E>> elements, E element) {
        for (FilterElement<E> filterElement : elements) {
          String value = filterElement.getMapper().map(element);
          for (Filter filter : filterElement.getFilters()) {
            if (filter.accept(value)) {
              return true;
            }
          }
        }
        return false;
      }
    },
    allMatch {
      @Override
      public <E> boolean evaluate(List<FilterElement<E>> elements, E element) {
        for (FilterElement<E> filterElement : elements) {
          String value = filterElement.getMapper().map(element);
          for (Filter filter : filterElement.getFilters()) {
            if (!filter.accept(value)) {
              return false;
            }
          }
        }
        return true;
      }
    },
    noneMatch {
      @Override
      public <E> boolean evaluate(List<FilterElement<E>> elements, E element) {
        for (FilterElement<E> filterElement : elements) {
          String value = filterElement.getMapper().map(element);
          for (Filter filter : filterElement.getFilters()) {
            if (filter.accept(value)) {
              return false;
            }
          }
        }
        return true;
      }
    };
    
    public abstract <E> boolean evaluate(List<FilterElement<E>> elements, E element);
  }
  
  
  public List<Pair<String, Set<String>>> listClassesAndFilters() {
    List<Pair<String, Set<String>>> classesAndFilters = new ArrayList<Pair<String,Set<String>>>();
    for (Entry<String, Map<String, StringMapper<?>>> entry : registeredMapper.entrySet()) {
      classesAndFilters.add(Pair.of(entry.getKey(), entry.getValue().keySet()));
    }
    return classesAndFilters;
  }
  
  
  public Set<String> listFilterIdentifier() {
    return registeredFilter.keySet();
  }
  
}

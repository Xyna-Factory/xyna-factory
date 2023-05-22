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
package com.gip.xyna.utils.collections;



import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;



/**
 * M zu N map.
 * jeder key m kann mehrere values n zugeordnet haben. man kann in beide richtungen objekte suchen,
 * also alle values zu einem key ermitteln {@link #getValues(Object)} und alle keys, die einen value 
 * zugeordnet haben {@link #getKeys(Object)}.
 * 
 */
public class MtoNMapping<U, V> {

  private static class InnerSet<T> implements Iterable<T> {

    private final ConcurrentMap<T, Boolean> map = new ConcurrentHashMap<T, Boolean>();
    private final AtomicInteger addDeleteSafety = new AtomicInteger(0);
    private final AtomicLong modCnt = new AtomicLong(0);


    public void add(T t) {
      if (addDeleteSafety.get() <= 0) {
        throw new RuntimeException();
      }
      map.put(t, true);
      modCnt.incrementAndGet();
    }


    public boolean remove(T t) {
      modCnt.incrementAndGet();
      return map.remove(t) != null;
    }


    public Iterator<T> iterator() {
      return map.keySet().iterator();
    }


    public int size() {
      return map.size();
    }


    /**
     * @return true, falls set nicht entfernt werden soll. es wird sichergestellt, dass es nicht entfernt wird
     */
    // >=0 -> ++ return true,  <0 -> return false
    public boolean prepareToAdd() {
      int oldVal = addDeleteSafety.get();
      while (oldVal >= 0) {
        if (!addDeleteSafety.compareAndSet(oldVal, oldVal + 1)) {
          oldVal = addDeleteSafety.get();
        } else {
          return true;
        }
      }
      return false;
    }


    /**
     * @return true, falls set leer ist und entfernt werden kann. es wird sichergestellt, dass nach dem entfernen nichts mehr geaddet wird
     */
    // 0 -> -1 return true, != 0 -> return false
    public boolean prepareToDelete() {
      int oldVal = addDeleteSafety.get();
      while (oldVal == 0) {
        if (!addDeleteSafety.compareAndSet(0, -1)) {
          oldVal = addDeleteSafety.get();
        } else {
          return true;
        }
      }
      return false;
    }


    /**
     * set freigeben f�r l�schen etc
     */
    public void addComplete() {
      addDeleteSafety.decrementAndGet();
    }


    public void unPrepareToDelete() {
      if (addDeleteSafety.get() != -1) {
        throw new RuntimeException();
      }
      addDeleteSafety.set(0);
    }


    public Set<T> asSet() {
      return map.keySet();
    }


    @Override
    public String toString() {
      return map.keySet().toString();
    }

  }


  private final ConcurrentMap<U, InnerSet<V>> keyMap = new ConcurrentHashMap<U, InnerSet<V>>();
  private final ConcurrentMap<V, InnerSet<U>> valueMap = new ConcurrentHashMap<V, InnerSet<U>>();
  /*
   * FIXME ohne lock auskommen?
   * code ist im prinzip darauf vorbereitet.
   * problem: operationen �ber beide maps hinweg sind nicht atomar, beim remove sieht man vielleicht nur die h�lfte des adds oder sowas.
   * das f�hrt dann zu inkonsistenzen.
   * TODO ConcurrentMapWithObjectRemovalSupport verwenden
   */
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();


  /**
   * @return alle keys, die dem value zugeordnet sind
   */
  public Set<U> getKeys(V val) {
    lock.readLock().lock();
    try {
      InnerSet<U> innerSet = valueMap.get(val);
      if (innerSet == null) {
        return null;
      }
      return new HashSet<U>(innerSet.asSet());
    } finally {
      lock.readLock().unlock();
    }
  }


  /**
   * @return alle values, die dem key zugeordnet sind
   */
  public Set<V> getValues(U key) {
    lock.readLock().lock();
    try {
      InnerSet<V> innerSet = keyMap.get(key);
      if (innerSet == null) {
        return null;
      }
      return new HashSet<V>(innerSet.asSet());
    } finally {
      lock.readLock().unlock();
    }
  }
  
  public Set<V> getValuesUnsafe(U key) {
    InnerSet<V> innerSet = keyMap.get(key);
    if (innerSet == null) {
      return null;
    }
    return innerSet.asSet();
  }


  /**
   * erzeugt neues mapping von key nach value
   */
  public void add(U key, V value) {
    if (key == null || value == null) {
      throw new IllegalArgumentException("key and value must both not be null");
    }
    InnerSet<V> setValues;
    InnerSet<U> setKeys;
    
    lock.writeLock().lock();
    try {
      while (true) {
        setValues = keyMap.get(key);
        if (setValues == null) {
          setValues = new InnerSet<V>();
          InnerSet<V> previousSet = keyMap.putIfAbsent(key, setValues);
          if (previousSet != null) {
            setValues = previousSet;
          }
        }
        if (setValues.prepareToAdd()) {
          //sichergestellt, dass man nicht zu einem set addet, welches bereits gel�scht werden soll
          break;
        }
      }

      while (true) {
        setKeys = valueMap.get(value);
        if (setKeys == null) {
          setKeys = new InnerSet<U>();
          InnerSet<U> previousSet = valueMap.putIfAbsent(value, setKeys);
          if (previousSet != null) {
            setKeys = previousSet;
          }
        }
        if (setKeys.prepareToAdd()) {
          break;
        }
      }

      setValues.add(value);
      setKeys.add(key);
    } finally {
      lock.writeLock().unlock();
    }

    setValues.addComplete();
    setKeys.addComplete();
  }


  /**
   * entfernt key und alle mappings zu ihm zugeordneten values.
   * @return true falls erfolgreich entfernt, false falls nicht gefunden
   */
  public boolean removeKey(U key) {
    lock.writeLock().lock();
    try {
      return remove(key, keyMap, valueMap);
    } finally {
      lock.writeLock().unlock();
    }
  }


  /**
   * entfernt value und alle mappings zu ihm zugeordneten keys.
   * @return true falls erfolgreich entfernt, false falls nicht gefunden
   */
  public boolean removeValue(V val) {
    lock.writeLock().lock();
    try {
      return remove(val, valueMap, keyMap);
    } finally {
      lock.writeLock().unlock();
    }
  }


  /**
   * entfernt das �bergebene mapping
   * @return true falls erfolgreich entfernt, false falls mapping nicht gefunden 
   */
  public boolean removeMapping(U key, V val) {
    lock.writeLock().lock();
    try {
      Pair<InnerSet<V>, Boolean> r1 = removeElementFromSet(keyMap, key, val);
      Pair<InnerSet<U>, Boolean> r2 = removeElementFromSet(valueMap, val, key);
      if (r2.getFirst() != null) {
        r2.getFirst().unPrepareToDelete();
      }
      if (r1.getFirst() != null) {
        r1.getFirst().unPrepareToDelete();
      }

      return r1.getSecond() || r2.getSecond();
    } finally {
      lock.writeLock().unlock();
    }
  }


  private static <R, S> Pair<InnerSet<S>, Boolean> removeElementFromSet(ConcurrentMap<R, InnerSet<S>> map, R key, S val) {
    InnerSet<S> valueSet;
    do {
      valueSet = map.get(key);
      if (valueSet == null) {
        return Pair.of(null, false);
      }
    } while (!valueSet.prepareToDelete());

    boolean setRemoved = false;
    boolean removed = false;
    if (valueSet.remove(val)) {
      removed = true;
      if (valueSet.size() == 0) {
        map.remove(key);
        setRemoved = true;
      }
    } else {
      if (valueSet.size() == 0) {
        throw new RuntimeException("unexpected state: modcnt=" + valueSet.modCnt.get());
      }
    }
    if (setRemoved) {
      return Pair.of(null, removed);
    }
    return Pair.of(valueSet, removed);
  }


  private static <R, S> boolean remove(R r, ConcurrentMap<R, InnerSet<S>> rmap, ConcurrentMap<S, InnerSet<R>> smap) {
    InnerSet<S> oldValSet;
    do {
      oldValSet = rmap.get(r);
      if (oldValSet == null) {
        return false;
      }
    } while (!oldValSet.prepareToDelete());

    InnerSet<S> removed = rmap.remove(r);
    if (removed != oldValSet) {
      throw new RuntimeException();
    }

    for (S val : removed) {
      Pair<InnerSet<R>, Boolean> r1 = removeElementFromSet(smap, val, r);
      if (r1.getFirst() != null) {
        r1.getFirst().unPrepareToDelete();
      }
    }
    return true;
  }


  /**
   * @return alle bekannten keys
   */
  public Set<U> getAllKeys() {
    lock.readLock().lock();
    try {
      return new HashSet<U>(keyMap.keySet());
    } finally {
      lock.readLock().unlock();
    }
  }


  /**
   * @return anzahl aller bekannten keys
   */
  public int keySize() {
    return keyMap.size();
  }


  /**
   * @return anzahl aller bekannten values
   */
  public int valueSize() {
    return valueMap.size();
  }


  /**
   * @return alle bekannten values
   */
  public Set<V> getAllValues() {
    lock.readLock().lock();
    try {
      return new HashSet<V>(valueMap.keySet());
    } finally {
      lock.readLock().unlock();
    }
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    boolean first = true;
    for (U k : keyMap.keySet()) {
      if (first) {
        first = false;
      } else {
        sb.append(", ");
      }
      InnerSet<V> innerSet = keyMap.get(k);
      sb.append(k).append("->").append(innerSet).append("");
    }
    sb.append("} ----- {");
    first = true;
    for (V v : valueMap.keySet()) {
      if (first) {
        first = false;
      } else {
        sb.append(", ");
      }
      InnerSet<U> innerSet = valueMap.get(v);
      sb.append(v).append("->").append(innerSet).append("");
    }
    sb.append("}");
    return sb.toString();
  }

}

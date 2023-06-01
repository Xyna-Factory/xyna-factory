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
package com.gip.xyna.utils.collections;



import java.util.concurrent.ConcurrentHashMap;



/**
 * map, aus der value objekte sicher entfernt werden können, wenn sie von keinem anderen client mehr verwendet werden.
 * es kann damit sichergestellt werden, dass jeder client immer mit dem aktuellen/gleichen objekt in der map arbeitet.<p>
 * 
 * motivation: normalerweise muss man beim entfernen von komplexen objekten aus einer map immer stark darauf aufpassen,
 * dass nicht ein anderer thread gleichzeitig das zu entfernende objekt noch als gültig sieht.<p>
 * 
 * beispiel: value der map ist eine liste, die nur dann entfernt werden soll, wenn sie leer ist.<p>
 * 
 * achtung: um synchronisierung des values an sich muss man sich selbst kümmern.<p>
 * 
 * verwendungsmuster:
 * <pre>
 * class ListWrapper extends ObjectWithRemovalSupport {
 *     private List list; 
 *     public abstract boolean shouldBeDeleted() {
 *       return list.isEmpty();
 *     }
 * }
 * 
 * ConcurrentMapWithObjectRemovalSupport&lt;String, ListWrapper&gt; map = ...
 * String key = ...
 * ListWrapper l = map.lazyCreateGet(key);
 * try {
 *   l.add(element);
 *   ....
 *   
 *   //es ist sichergestellt, dass l nun nicht aus der map entfernt wird, solange für das remove nur cleanup() verwendet wird
 *   ....
 * 
 *   l.remove(element);
 * } finally {
 *   //entfernt key aus map nur, wenn liste leer ist, und kein anderer thread das objekt per lazyCreateGet geholt hat
 *   map.cleanup(key);
 * } 
 * </pre>
 * 
 * es muss zu jedem {@link #lazyCreateGet(Object)} oder {@link #lazyCreateGet(Object, ObjectWithRemovalSupport)} auch ein
 * {@link #cleanup(Object)} aufgerufen werden!
 *
 */
public abstract class ConcurrentMapWithObjectRemovalSupport<K, V extends ObjectWithRemovalSupport> extends ConcurrentHashMap<K, V> {

  private static final long serialVersionUID = 8435074195068267807L;

  public ConcurrentMapWithObjectRemovalSupport() {
    
  }

  public ConcurrentMapWithObjectRemovalSupport(int initialCapacity,
                                               float loadFactor, int concurrencyLevel) {
    super(initialCapacity, loadFactor, concurrencyLevel);
  }


  /**
   * erstellt objekt falls nicht vorhanden mittels {@link #createValue(Object)}.
   * ansonsten wird das existierende objekt zurückgegeben.
   * markiert objekt als in benutzung, sodass es nicht aus der map entfernt werden kann,
   * solange man nicht {@link #cleanup(Object)} aufgerufen hat.
   */
  public V lazyCreateGet(K key) {
    V v;
    while (true) {
      v = get(key);
      if (v == null) {
        v = createValue(key);
        V previous = putIfAbsent(key, v);
        if (previous != null) {
          v = previous;
        }
      }
      if (v.markForUsage()) {
        break;
      }
      //object wurde gerade wieder gelöscht
    }
    return v;
  }


  /**
   * trägt übergebenes objekt falls nicht vorhanden in map ein.
   * ansonsten wird das existierende objekt zurückgegeben.
   * markiert objekt als in benutzung, sodass es nicht aus der map entfernt werden kann,
   * solange man nicht {@link #cleanup(Object)} aufgerufen hat.
   */
  public V lazyCreateGet(K key, V newValue) {
    V v;
    while (true) {
      v = get(key);
      if (v == null) {
        v = newValue;
        V previous = putIfAbsent(key, v);
        if (previous != null) {
          v = previous;
        }
      }
      if (v.markForUsage()) {
        break;
      }
      //object wurde gerade wieder gelöscht
    }
    return v;
  }


  /**
   * falls objekt von niemandem benutzt wird und {@link ObjectWithRemovalSupport#shouldBeDeleted()} zutrifft,
   * wird es aus der map entfernt.
   * ansonsten wird objekt als nicht mehr in benutzung markiert. es sollte vorher {@link #lazyCreateGet(Object)} oder
   * {@link #lazyCreateGet(Object, ObjectWithRemovalSupport)} aufgerufen worden sein.
   */
  public void cleanup(K key) {
    V v = get(key);
    v.unmarkForUsage();
    if (v.shouldBeDeleted()) {
      if (v.markForDeletion()) {
        //kein anderer thread hat das objekt markiert
        if (v.shouldBeDeleted()) {
          remove(key);
          v.onDeletion();
        } else {
          //anderer thread hatte es nach unserer ersten überprüfung kurz gehabt und geändert
          v.unmarkForDeletion();
        }
      }
    }
  }
  
  /**
   * verarbeitet einen value
   * @param <V> typ des values in der map
   * @param <R> resulttype
   */
  public interface ValueProcessor<V, R> {
    public R exec(V v);
  }

  /**
   * verarbeitung des values zum key ohne sich um das create und cleanup kümmern zu müssen.
   */
  public <R> R process(final K key, ValueProcessor<V, R> processor) {
    V v = lazyCreateGet(key);
    try {
      return processor.exec(v);
    } finally {
      cleanup(key);
    }
  }

  /**
   * erstellt neues objekt
   */
  public abstract V createValue(K key);


  //wenn man alles löschen würde, kann es zu NPEs kommen, weil der obige code davon ausgeht, sich auf die markierungen verlassen zu können
  //TODO eigtl benötigt man entsprechende behandlung auch für iterator-remove, normales remove, etc
  /**
   * löscht jedes key value paar, dessen value nicht in benutzung ist ohne überprüfung von {@link ObjectWithRemovalSupport#shouldBeDeleted()}
   */
  @Override
  public void clear() {
    for (K key : keySet()) {
      V v = get(key);
      if (v != null) {
        if (v.markForDeletion()) {
          remove(key);
          v.onDeletion();
        }
      }
    }
  }
  

}

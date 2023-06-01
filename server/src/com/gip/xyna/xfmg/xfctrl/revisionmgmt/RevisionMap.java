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
package com.gip.xyna.xfmg.xfctrl.revisionmgmt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.gip.xyna.utils.collections.WrappedMap;


/**
 * RevisionMap ist eine Map vond Revision auf eine &lt;V&gt;-Instanz.
 * Sie hat nicht nur die üblichen Map-Zugriffe, sondern vor allem eine getOrCreate-Methode,
 * mit der neue &lt;V&gt;-Instanzen erstellt werden.
 * Dazu dient die Implementierung des ValueCreator-Interfaces, die im Konstruktor übergeben werden muss.
 * 
 * TODO Es fehlt eine automatische Benachrichtigung, wenn die Revision aus dem RevisionManagement entfernt wird.
 * 
 *
 */
public class RevisionMap<V> extends WrappedMap<Long, V> {

  public interface ValueCreator<V> {
    public V construct(long revision);
    public void destruct(long revision, V value);
  }

  
  private ValueCreator<V> valueCreator;
  
  public RevisionMap(ValueCreator<V> valueCreator) {
    super(new ConcurrentHashMap<Long, V>());
    this.valueCreator = valueCreator;
  }


  public V getOrCreate(long revision) {
    V value = wrapped.get(revision);
    if( value == null ) {
      synchronized (valueCreator) {
        value = wrapped.get(revision);
        if( value == null ) {
          value = valueCreator.construct(revision);
          wrapped.put(revision, value);
        }
      }
    }
    return value;
  }


  public static <V> RevisionMap<List<V>> createList(Class<V> valueClass) {
    return new RevisionMap<List<V>>(new ListCreator<V>());
  }
  public static class ListCreator<V>  implements ValueCreator<List<V>> {
    public List<V> construct(long revision) {
      return new ArrayList<V>();
    }
    public void destruct(long revision, List<V> value) {
      //nichts zu tun
    }
  }

}

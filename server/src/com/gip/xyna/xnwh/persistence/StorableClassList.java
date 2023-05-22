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
package com.gip.xyna.xnwh.persistence;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



/**
 *
 */
public class StorableClassList extends AbstractList<Class<? extends Storable<?> >> {
  
  List<Class<? extends Storable<?> >> storables = new ArrayList<Class<? extends Storable<?> >>();

  public StorableClassList(Class<? extends Storable<?>> class1) {
    storables.add(class1);
  }
  public StorableClassList(Class<? extends Storable<?>> class1, 
                           Class<? extends Storable<?>> class2) {
    storables.add(class1);
    storables.add(class2);
  }
  public StorableClassList(Class<? extends Storable<?>> class1,
                           Class<? extends Storable<?>> class2,
                           Class<? extends Storable<?>> class3) {
    storables.add(class1);
    storables.add(class2);
    storables.add(class3);
  }
  public StorableClassList(Class<? extends Storable<?>> class1,
                           Class<? extends Storable<?>> class2,
                           Class<? extends Storable<?>> class3,
                           Class<? extends Storable<?>> class4) {
    storables.add(class1);
    storables.add(class2);
    storables.add(class3);
    storables.add(class4);
  }

  public StorableClassList(Class<? extends Storable<?>>[] classes) {
    for( Class<? extends Storable<?>> clazz : classes ) {
      storables.add(clazz);
    }
  }  
  
  public StorableClassList(List<Class<? extends Storable<?>>> storablesList) {
    storables.addAll(storablesList);
  }
  
  
  
  public List<Class<? extends Storable<?>>> asList() {
    return Collections.unmodifiableList( storables );
  }


  @SuppressWarnings("unchecked")
  public Class<? extends Storable<?>>[] asArray() {
    return (Class<? extends Storable<?>>[]) storables.toArray(new Class[storables.size()]);
  }


  @Override
  public Class<? extends Storable<?>> get(int index) {
    return storables.get(index);
  }

  @Override
  public int size() {
    return storables.size();
  }
  
  public static StorableClassList combine(StorableClassList storables, Class<? extends Storable<?>> storable) {
    StorableClassList scl = new StorableClassList(storables.asList());
    scl.storables.add(storable);
    return scl;
  }
  public static StorableClassList combine(StorableClassList storables1, StorableClassList storables2) {
    StorableClassList scl = new StorableClassList(storables1.asList());
    scl.storables.addAll(storables2);
    return scl;
  }
  
  
}

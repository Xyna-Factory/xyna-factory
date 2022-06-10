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
package com.gip.xyna.xdev.xfractmod.xmdm;



import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;



public class XynaObjectList<I extends XynaObject> extends GeneralXynaObjectList<I> implements List<I>, Cloneable {

  private static final long serialVersionUID = 1L;

  /**
   * falls list == null, wird eine arraylist initialisiert
   */
  public XynaObjectList(List<? extends I> list, Class<I> c) {
    super(list, c);
  }


  public XynaObjectList(Class<I> c) {
    super(c);
  }


  public XynaObjectList(List<? extends I> list, Class<I> c, I... values) {
    super(list, c, values);
  }
  
  public XynaObjectList(Class<I> c, I ... values) {
    super(c, values);
  }


  public XynaObjectList(List<? extends I> list, String originalXmlName, String originalXmlPath) {
    super(list, originalXmlName, originalXmlPath);
  }
  

  public XynaObjectList(List<? extends I> list, String fqXmlName) {
    super(list, GenerationBase.getSimpleNameFromFQName(fqXmlName), GenerationBase.getPackageNameFromFQName(fqXmlName));
  }
  
  @Override
  public XynaObjectList<I> clone() {
    return (XynaObjectList<I>) super.clone();
  }

  public XynaObjectList<I> clone(boolean deep) {
    XynaObjectList<I> clone = clone();
    if (deep) {
      clone.list = listClone(list, deep);
    }
    return clone;
  }

  //für abwärtskompatibilität die alten signaturen erhalten

  @Override
  public boolean add(I el) {
    return super.add(el);
  }


  @Override
  public I get(int index) {
    return super.get(index);
  }


  @Override
  public boolean addAll(Collection<? extends I> c) {
    return super.addAll(c);
  }


  @Override
  public void remove(I el) {
    super.remove(el);
  }


  @Override
  public I remove(int i) {
    return super.remove(i);
  }


  @Override
  public void add(int index, I el) {
    super.add(index, el);
  }


  @Override
  public List<I> getList() {
    return super.getList();
  }


  @Override
  public int size() {
    return super.size();
  }


  @Override
  public boolean addAll(int index, Collection<? extends I> c) {
    return super.addAll(index, c);
  }


  @Override
  public void clear() {
    super.clear();
  }


  @Override
  public boolean contains(Object o) {
    return super.contains(o);
  }


  @Override
  public boolean containsAll(Collection<?> c) {
    return super.containsAll(c);
  }


  @Override
  public int indexOf(Object o) {
    return super.indexOf(o);
  }


  @Override
  public boolean isEmpty() {
    return super.isEmpty();
  }


  @Override
  public Iterator<I> iterator() {
    return super.iterator();
  }


  @Override
  public int lastIndexOf(Object o) {
    return super.lastIndexOf(o);
  }


  @Override
  public boolean remove(Object o) {
    return super.remove(o);
  }


  @Override
  public boolean removeAll(Collection<?> c) {
    return super.removeAll(c);
  }


  @Override
  public I set(int index, I element) {
    return super.set(index, element);
  }


  @Override
  public Object get(String name) throws InvalidObjectPathException {
    return super.get(name);
  }


  @Override
  public void set(String name, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    super.set(name, value);
  }

}

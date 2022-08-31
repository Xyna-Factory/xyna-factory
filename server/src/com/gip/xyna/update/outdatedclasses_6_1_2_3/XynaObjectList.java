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
package com.gip.xyna.update.outdatedclasses_6_1_2_3;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.utils.misc.DataRangeCollection;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xnwh.persistence.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;




public class XynaObjectList<I extends XynaObject> extends XynaObject implements List<I>, Cloneable {

  private static final long serialVersionUID = 1L;

  @LabelAnnotation(label="List")
  private List<I> list;

  @LabelAnnotation(label="Contained Class")
  private Class<XynaObject> containedClass;

  @LabelAnnotation(label="Original XML Name")
  private String originalXmlName;

  @LabelAnnotation(label="Original XML Path")
  private String originalXmlPath;

  @LabelAnnotation(label="Java Type")
  private String javaType;


  /**
   * falls list == null, wird eine arraylist initialisiert
   */
  public XynaObjectList(List<? extends I> list, Class<I> c) {
  }


  public boolean add(I el) {
    return list.add(el);
  }


  public void remove(I el) {
    list.remove(el);
  }


  public I remove(int i) {
    return list.remove(i);
  }


  public void add(int index, I el) {
    list.add(index, el);
  }


  /**
   * XynaObjectList is now a list as itself
   */
  @Deprecated
  public List<I> getList() {
    return list;
  }


  public int size() {
    return list.size();
  }


  public I get(int index) {
    return list.get(index);
  }


  public <T> T[] toArray(T[] array) {
    return list.toArray(array);
  }


  public boolean addAll(Collection<? extends I> c) {
    return list.addAll(c);
  }


  public boolean addAll(int index, Collection<? extends I> c) {
    return list.addAll(index, c);
  }


  public void clear() {
    list.clear();

  }


  public boolean contains(Object o) {
    return list.contains(o);
  }


  public boolean containsAll(Collection<?> c) {
    return list.containsAll(c);
  }


  public int indexOf(Object o) {
    return list.indexOf(o);
  }


  public boolean isEmpty() {
    return list.isEmpty();
  }


  public Iterator<I> iterator() {
    return list.iterator();
  }


  public int lastIndexOf(Object o) {
    return list.lastIndexOf(o);
  }


  public ListIterator<I> listIterator() {
    return list.listIterator();
  }


  public ListIterator<I> listIterator(int index) {
    return list.listIterator(index);
  }


  public boolean remove(Object o) {
    return list.remove(o);
  }


  public boolean removeAll(Collection<?> c) {
    return list.removeAll(c);
  }


  public boolean retainAll(Collection<?> c) {
    return list.retainAll(c);
  }


  public I set(int index, I element) {
    return list.set(index, element);
  }


  public List<I> subList(int fromIndex, int toIndex) {
    return list.subList(fromIndex, toIndex);
  }


  public Object[] toArray() {
    return list.toArray();
  }


  public XynaObjectList<I> clone() {
    return null;
  }

  public Class<?> getContainedClass() {
    return containedClass;
  }

  public String getContainedFQTypeName() {
    return originalXmlPath + "." + originalXmlName;
  }


  public String toXml(String varName, boolean onlyContent, long version, XMLReferenceCache cache) {
    return null;
  }


  public boolean supportsObjectVersioning() {
    return false;
  }


  public void collectChanges(long start, long end, IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers,
                             Set<Long> datapoints) {
  }


  public Object get(String path) throws InvalidObjectPathException {
    return null;
  }


  public void set(String name, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    
  }


  @Override
  public String toXml(String varName, boolean onlyContent) {
    return null;
  }
  
  
  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();


  public static Field getField(String target_fieldname) throws InvalidObjectPathException {
    Field foundField = null;
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = XynaObjectList.class.getDeclaredField(target_fieldname);
    } catch (NoSuchFieldException e) {
    }
    if (foundField == null) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(target_fieldname));
    } else {
      foundField.setAccessible(true);
      fieldMap.put(target_fieldname, foundField);
      return foundField;
    }
  }
  

  public Object readResolvePublic() {
    return new com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList<XynaObject>(list, originalXmlName, originalXmlPath);
  }
  /*
  private Object readResolve() throws ObjectStreamException {
    Stack<Integer> s = Container.stackSize.get();
    if (s != null && !s.isEmpty()) {
      //hässlicher stack-size vergleich.
      if (Thread.currentThread().getStackTrace().length - s.peek() < 12) {
        //liste ist teil von container -> wegen xynaobject-array-kompatibilität nicht hier ersetzen, sondern in container beim readresolve.
        //wenn man hier bereits umwandeln würde, gibt es arraystoreexception im container, falls dort ein XynaObject[] gespeichert ist
        return this;
      }
    }
    return readResolvePublic();
  }*/

}

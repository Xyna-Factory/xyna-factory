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
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.misc.DataRangeCollection;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ListWithConstantSize;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject;
import com.gip.xyna.xfmg.xfctrl.classloading.MDMClassLoaderXMLBase;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;



public class GeneralXynaObjectList<I extends GeneralXynaObject> implements GeneralXynaObject, List<I>, Cloneable {

  private static final long serialVersionUID = 1L;

  private static Logger logger = CentralFactoryLogging.getLogger(XynaObjectList.class);

  /*
   * manche Abfragen machen keinen Sinn, wenn man sich nicht im Server befindet. 
   * z.B. im RMI Client oder sowas. Dort sind andere Classloader.
   */
  private static boolean isInServer;
  static {
    try {
      isInServer = XynaFactory.isFactoryServer();
    } catch (RuntimeException e) {
      isInServer = false;
    }
  }

  protected List<I> list;
  private Class<XynaObject> containedClass;
  private String originalXmlName;
  private String originalXmlPath;
  @Deprecated
  private String javaType;
  
  private volatile VersionedObject<List<I>> oldVersions;


  /**
   * falls list == null, wird eine arraylist initialisiert
   */
  public GeneralXynaObjectList(List<? extends I> list, Class<I> c) {
    if (c == null) {
      throw new IllegalArgumentException("Contained class may not be null!");
    }

    XynaObjectAnnotation xoa = c.getAnnotation(XynaObjectAnnotation.class);
    if (xoa != null) {
      originalXmlName = GenerationBase.getSimpleNameFromFQName(xoa.fqXmlName());
      originalXmlPath = GenerationBase.getPackageNameFromFQName(xoa.fqXmlName());
    } else if (isInServer) {
      this.containedClass = (Class<XynaObject>) c;
      ClassLoader cl = this.containedClass.getClassLoader();
      if (cl instanceof MDMClassLoaderXMLBase) {
        MDMClassLoaderXMLBase mdmCl = (MDMClassLoaderXMLBase) cl;
        originalXmlName = mdmCl.getOriginalXmlName();
        originalXmlPath = mdmCl.getOriginalXmlPath();
      } else {
        //reservierte xynaobjects wie xynaexception oder schedulerbean oder sowas.
        String xmlName = GenerationBase.getXmlNameForReservedClass(containedClass);
        originalXmlName = GenerationBase.getSimpleNameFromFQName(xmlName);
        originalXmlPath = GenerationBase.getPackageNameFromFQName(xmlName);
      }
    } else {
      throw new IllegalArgumentException("Must declare XML Names of Members");
    }

    this.list = new ArrayList<I>();
    if (list != null) {
      this.list.addAll(list);
    }

  }


  public GeneralXynaObjectList(Class<I> c) {
    this(new ArrayList<I>(), c);
  }


  public GeneralXynaObjectList(List<? extends I> list, Class<I> c, I... values) {
    this(list, c);
    for (I value : values) {
      this.list.add(value);
    }
  }


  public GeneralXynaObjectList(Class<I> c, I... values) {
    this(null, c);
    for (I value : values) {
      this.list.add(value);
    }
  }


  public GeneralXynaObjectList(List<? extends I> list, String originalXmlName, String originalXmlPath) {
    this.originalXmlName = originalXmlName;
    this.originalXmlPath = originalXmlPath;
    this.list = new ArrayList<I>();
    if (list != null) {
      this.list.addAll(list);
    }
  }


  public boolean add(I el) {
    if (supportsObjectVersioning()) {
      if (oldVersions == null) {
        synchronized (this) {
          if (oldVersions == null) {
            oldVersions = new VersionedObject<List<I>>();
          }
        }
      }
      synchronized (oldVersions) {
        oldVersions.add(new ListWithConstantSize<I>(list));
        return list.add(el);
      }
    }
    return list.add(el);
  }


  public void remove(I el) {
    if (supportsObjectVersioning()) {
      if (oldVersions == null) {
        synchronized (this) {
          if (oldVersions == null) {
            oldVersions = new VersionedObject<List<I>>();
          }
        }
      }
      synchronized (oldVersions) {
        oldVersions.add(list);
        list = new ArrayList<I>(list);
        list.remove(el);
      }
      return;
    }
    list.remove(el);
  }


  private List<I> versionedGetList(long version) {
    if (oldVersions == null) {
      return list;
    }
    Version<List<I>> v = oldVersions.getVersion(version);
    if (v == null) {
      return list;
    }
    return v.object;
  }


  public I remove(int i) {
    if (supportsObjectVersioning()) {
      if (oldVersions == null) {
        synchronized (this) {
          if (oldVersions == null) {
            oldVersions = new VersionedObject<List<I>>();
          }
        }
      }
      synchronized (oldVersions) {
        oldVersions.add(list);
        list = new ArrayList<I>(list);
        return list.remove(i);
      }
    }
    return list.remove(i);
  }


  public void add(int index, I el) {
    if (supportsObjectVersioning()) {
      if (oldVersions == null) {
        synchronized (this) {
          if (oldVersions == null) {
            oldVersions = new VersionedObject<List<I>>();
          }
        }
      }
      synchronized (oldVersions) {
        oldVersions.add(list);
        list = new ArrayList<I>(list);
        list.add(index, el);
      }
      return;
    }
    list.add(index, el);
  }


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
    if (supportsObjectVersioning()) {
      if (oldVersions == null) {
        synchronized (this) {
          if (oldVersions == null) {
            oldVersions = new VersionedObject<List<I>>();
          }
        }
      }
      synchronized (oldVersions) {
        oldVersions.add(new ListWithConstantSize<I>(list));
        return list.addAll(c);
      }
    }
    return list.addAll(c);
  }


  public boolean addAll(int index, Collection<? extends I> c) {
    if (supportsObjectVersioning()) {
      if (oldVersions == null) {
        synchronized (this) {
          if (oldVersions == null) {
            oldVersions = new VersionedObject<List<I>>();
          }
        }
      }
      synchronized (oldVersions) {
        oldVersions.add(list);
        list = new ArrayList<I>(list);
        return list.addAll(index, c);
      }
    }
    return list.addAll(index, c);
  }


  public void clear() {
    if (supportsObjectVersioning()) {
      if (oldVersions == null) {
        synchronized (this) {
          if (oldVersions == null) {
            oldVersions = new VersionedObject<List<I>>();
          }
        }
      }
      synchronized (oldVersions) {
        oldVersions.add(list);
        list = new ArrayList<I>();
      }
      return;
    }
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
    if (supportsObjectVersioning()) {
      if (oldVersions == null) {
        synchronized (this) {
          if (oldVersions == null) {
            oldVersions = new VersionedObject<List<I>>();
          }
        }
      }
      synchronized (oldVersions) {
        oldVersions.add(list);
        list = new ArrayList<I>(list);
        return list.remove(o);
      }
    }
    return list.remove(o);
  }


  public boolean removeAll(Collection<?> c) {
    if (supportsObjectVersioning()) {
      if (oldVersions == null) {
        synchronized (this) {
          if (oldVersions == null) {
            oldVersions = new VersionedObject<List<I>>();
          }
        }
      }
      synchronized (oldVersions) {
        oldVersions.add(list);
        list = new ArrayList<I>(list);
        return list.removeAll(c);
      }
    }
    return list.removeAll(c);
  }


  public boolean retainAll(Collection<?> c) {
    if (supportsObjectVersioning()) {
      if (oldVersions == null) {
        synchronized (this) {
          if (oldVersions == null) {
            oldVersions = new VersionedObject<List<I>>();
          }
        }
      }
      synchronized (oldVersions) {
        oldVersions.add(list);
        list = new ArrayList<I>(list);
        return list.retainAll(c);
      }
    }
    return list.retainAll(c);
  }


  public I set(int index, I element) {
    if (supportsObjectVersioning()) {
      if (oldVersions == null) {
        synchronized (this) {
          if (oldVersions == null) {
            oldVersions = new VersionedObject<List<I>>();
          }
        }
      }
      synchronized (oldVersions) {
        oldVersions.add(list);
        list = new ArrayList<I>(list);
        return list.set(index, element);
      }
    }
    return list.set(index, element);
  }


  public List<I> subList(int fromIndex, int toIndex) {
    return list.subList(fromIndex, toIndex);
  }


  public Object[] toArray() {
    return list.toArray();
  }


  @SuppressWarnings("unchecked")
  public GeneralXynaObjectList<I> clone() {
    try {
      GeneralXynaObjectList<I> clone = (GeneralXynaObjectList<I>) super.clone();
      clone.list = listClone(list, false);
      clone.oldVersions = null; //hier wird eine neue GeneralXynaObjectList erzeugt, diese hat noch keine Geschichte
      return clone;
    } catch (CloneNotSupportedException e) { //wir sind Cloneable, deswegen kann es die Exception hier nicht geben
      throw new RuntimeException(e);
    }
  }


  public GeneralXynaObjectList<I> clone(boolean deep) {
    GeneralXynaObjectList<I> clone = clone();
    if (deep) {
      clone.list = listClone(list, deep);
    }
    return clone;
  }


  @SuppressWarnings("unchecked")
  protected static <I extends GeneralXynaObject> List<I> listClone(List<I> list, boolean deep) {
    List<I> newList = new ArrayList<I>();
    if (list != null) {
      if (deep) {
        for (int i = 0; i < list.size(); i++) {
          if (list.get(i) != null) {
            newList.add((I) list.get(i).clone(deep));
          } else {
            newList.add(null);
          }
        }
      } else {
        newList.addAll(list);
      }
    }
    return newList;
  }


  @Override
  public String toString() {
    return new StringBuilder().append(super.toString()).append(" (size: ").append((list != null ? list.size() : 0)).append(")").toString();
  }


  public String toXml(String varName, boolean onlyContent) {
    return toXml(varName, onlyContent, -1, null);
  }


  private static class ObjectVersion<I extends GeneralXynaObject> extends ObjectVersionBase {


    protected ObjectVersion(GeneralXynaObject xo, long version, IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }


    @Override
    public int calcHashOfMembers(Stack<GeneralXynaObject> stack) {
      GeneralXynaObjectList<I> xol = ((GeneralXynaObjectList<I>) xo);
      List<I> l = xol.versionedGetList(version);
      int h = xol.getContainedFQTypeName().hashCode();       
      for (int i = 0; i < l.size(); i++) {
        I lm = l.get(i);
        h = h * 31 + (lm == null ? 0 : lm.createObjectVersion(version, changeSetsOfMembers).hashCode(stack));
      }
      return h;
    }


    @Override
    protected boolean memberEquals(ObjectVersionBase other) {
      GeneralXynaObjectList<I> xol = ((GeneralXynaObjectList<I>) xo);
      List<I> l = xol.versionedGetList(version);

      GeneralXynaObjectList<?> otherList = (GeneralXynaObjectList) other.xo;
      List<? extends GeneralXynaObject> otherOldList = otherList.versionedGetList(other.version);

      if (l.size() != otherOldList.size()) {
        return false;
      }
      if (!xol.getContainedFQTypeName().equals(otherList.getContainedFQTypeName())) {
        //xml ist dann anders. und für audits muss dann der contained typ mit in die audits aufgenommen werden (insbesondere wenn liste leer ist)
        return false;
      }
      for (int i = 0; i < l.size(); i++) {
        GeneralXynaObject o1 = l.get(i);
        GeneralXynaObject o2 = otherOldList.get(i);
        if (!xoEqual(o1, o2, version, other.version, changeSetsOfMembers)) {
          return false;
        }
      }
      return true;
    }

  }


  @SuppressWarnings({"unchecked", "rawtypes"})
  public void collectChanges(long start, long end, IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers,
                             Set<Long> datapoints) {
    XOUtils.addChangesForComplexListMember(this, (VersionedObject) oldVersions, start, end, changeSetsOfMembers, datapoints);
  }


  public ObjectVersionBase createObjectVersion(long version, IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers) {
    return new ObjectVersion<I>(this, version, changeSetsOfMembers);
  }


  public String toXml(String varName, boolean onlyContent, long version, XMLReferenceCache cache) {
    final List<I> l = versionedGetList(version);
    if (l != null) {
      StringBuilder xml = new StringBuilder();
      if (onlyContent) {
        appendValues(xml, varName, version, cache, l);
      } else {
        // zusätzliches Data element drumrumwrappen, welches IsList=true enthält

        xml.append("<Data " + GenerationBase.ATT.REFERENCEPATH + "=\"").append(originalXmlPath)
            .append("\" " + GenerationBase.ATT.REFERENCENAME + "=\"").append(originalXmlName)
            .append("\" " + GenerationBase.ATT.ISLIST + "=\"true\" ");
        if (cache != null) {
          ObjectVersionBase ov = createObjectVersion(version, cache.changeSetsOfMembers);
          long id = cache.putIfAbsent(ov);
          if (id > 0) {
            xml.append(GenerationBase.ATT.OBJECT_REFERENCE_ID + "=\"").append(id).append("\" />\n");
          } else {
            xml.append(GenerationBase.ATT.OBJECT_ID + "=\"").append(-id).append("\" >\n");
            appendValues(xml, varName, version, cache, l);
            xml.append("</Data>\n");
          }
        } else {
          xml.append(">\n");
          appendValues(xml, varName, version, cache, l);
          xml.append("</Data>\n");
        }
      }
      return xml.toString();
    } else {
      return "";
    }
  }


  private void appendValues(StringBuilder xml, String varName, long version, XMLReferenceCache cache, List<I> l) {
    int s = l.size();
    for (int i = 0; i < s; i++) {
      I next = l.get(i);
      if (next == null) {
        xml.append("<" + GenerationBase.EL.VALUE + "/>\n");
      } else {
        xml.append("<" + GenerationBase.EL.VALUE + ">\n").append(next.toXml(varName, false, version, cache))
            .append("</" + GenerationBase.EL.VALUE + ">\n");
      }
    }
  }


  public boolean supportsObjectVersioning() {
    return XOUtils.supportsObjectVersioningForInternalObjects();
  }


  public Class<?> getContainedClass() {
    return containedClass;
  }


  public String getContainedFQTypeName() {
    return originalXmlPath + "." + originalXmlName;
  }


  public String getOriginalXmlName() {
    return originalXmlName;
  }


  public String getOriginalXmlPath() {
    return originalXmlPath;
  }


  public String toXml() {
    return toXml(null, false, -1, null);
  }


  public String toXml(String varName) {
    return toXml(varName, false, -1, null);
  }


  public Object get(String name) throws InvalidObjectPathException {
    String[] varNames = new String[] {"list"};
    Object[] vars = new Object[] {list};
    return XOUtils.get(varNames, vars, name, null);
  }


  public void set(String name, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("list".equals(name)) {
      list = (List<I>) value;
    } else {
      throw new XDEV_PARAMETER_NAME_NOT_FOUND(name);
    }
  }


}

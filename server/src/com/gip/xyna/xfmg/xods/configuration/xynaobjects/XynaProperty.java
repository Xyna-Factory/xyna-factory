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
package com.gip.xyna.xfmg.xods.configuration.xynaobjects;


import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBase;
import com.gip.xyna.xnwh.persistence.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;

@XynaObjectAnnotation(fqXmlName = "xfmg.xods.configuration.XynaProperty")
public abstract class XynaProperty extends XynaObject {

  private static final long serialVersionUID = -36156381023364L;
  private static final Logger logger = CentralFactoryLogging.getLogger(XynaProperty.class);


  @LabelAnnotation(label="Property Name")
  private String propertyName;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOfpropertyName;


  public String getPropertyName() {
    return propertyName;
  }

  public String versionedGetPropertyName(long _version) {
    if (oldVersionsOfpropertyName == null) {
      return propertyName;
    }
    String _local = propertyName;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOfpropertyName.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setPropertyName(String propertyName) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOfpropertyName;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfpropertyName;
          if (_vo == null) {
            oldVersionsOfpropertyName = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.propertyName);
        this.propertyName = propertyName;
      }
      return;
    }
    this.propertyName = propertyName;
  }

  public void unversionedSetPropertyName(String propertyName) {
    this.propertyName = propertyName;
  }

  @LabelAnnotation(label="Behaviour of Property Not Set")
  private BehaviourIfPropertyNotSet behaviourIfPropertyNotSet;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<BehaviourIfPropertyNotSet> oldVersionsOfbehaviourIfPropertyNotSet;


  public BehaviourIfPropertyNotSet getBehaviourIfPropertyNotSet() {
    return behaviourIfPropertyNotSet;
  }

  public BehaviourIfPropertyNotSet versionedGetBehaviourIfPropertyNotSet(long _version) {
    if (oldVersionsOfbehaviourIfPropertyNotSet == null) {
      return behaviourIfPropertyNotSet;
    }
    BehaviourIfPropertyNotSet _local = behaviourIfPropertyNotSet;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<BehaviourIfPropertyNotSet> _ret = oldVersionsOfbehaviourIfPropertyNotSet.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setBehaviourIfPropertyNotSet(BehaviourIfPropertyNotSet behaviourIfPropertyNotSet) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<BehaviourIfPropertyNotSet> _vo = oldVersionsOfbehaviourIfPropertyNotSet;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfbehaviourIfPropertyNotSet;
          if (_vo == null) {
            oldVersionsOfbehaviourIfPropertyNotSet = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<BehaviourIfPropertyNotSet>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.behaviourIfPropertyNotSet);
        this.behaviourIfPropertyNotSet = behaviourIfPropertyNotSet;
      }
      return;
    }
    this.behaviourIfPropertyNotSet = behaviourIfPropertyNotSet;
  }

  public void unversionedSetBehaviourIfPropertyNotSet(BehaviourIfPropertyNotSet behaviourIfPropertyNotSet) {
    this.behaviourIfPropertyNotSet = behaviourIfPropertyNotSet;
  }


  protected static class InternalBuilder<_GEN_DOM_TYPE extends XynaProperty, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>  {

    protected _GEN_DOM_TYPE instance;

    @SuppressWarnings("unchecked")
    protected InternalBuilder(XynaProperty instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }

    public XynaProperty instance() {
      return (XynaProperty) instance;
    }

    @SuppressWarnings("unchecked")
    public _GEN_BUILDER_TYPE propertyName(String propertyName) {
      this.instance.unversionedSetPropertyName(propertyName);
      return (_GEN_BUILDER_TYPE) this;
    }

    @SuppressWarnings("unchecked")
    public _GEN_BUILDER_TYPE behaviourIfPropertyNotSet(BehaviourIfPropertyNotSet behaviourIfPropertyNotSet) {
      this.instance.unversionedSetBehaviourIfPropertyNotSet(behaviourIfPropertyNotSet);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public XynaProperty() {
    super();
  }

  /**
  * Creates a new instance using locally defined member variables.
  */
  public XynaProperty(String propertyName, BehaviourIfPropertyNotSet behaviourIfPropertyNotSet) {
    this();
    this.propertyName = propertyName;
    this.behaviourIfPropertyNotSet = behaviourIfPropertyNotSet;
  }

  protected void fillVars(XynaProperty source, boolean deep) {
    this.propertyName = source.propertyName;
    this.behaviourIfPropertyNotSet = (BehaviourIfPropertyNotSet)XynaObject.clone(source.behaviourIfPropertyNotSet, deep);
    this.throwExceptionIfNoValueSet = source.throwExceptionIfNoValueSet;
    this.xpsWrapper = source.xpsWrapper;
  }


  public abstract XynaProperty clone();

  public abstract XynaProperty clone(boolean deep);

  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      XynaProperty xoc = (XynaProperty) xo;
      XynaProperty xoco = (XynaProperty) other.xo;
      if (!equal(xoc.versionedGetPropertyName(this.version), xoco.versionedGetPropertyName(other.version))) {
        return false;
      }
      if (!xoEqual(xoc.versionedGetBehaviourIfPropertyNotSet(this.version), xoco.versionedGetBehaviourIfPropertyNotSet(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      XynaProperty xoc = (XynaProperty) xo;
      String propertyName = xoc.versionedGetPropertyName(this.version);
      hash = hash * 31 + (propertyName == null ? 0 : propertyName.hashCode());
      BehaviourIfPropertyNotSet behaviourIfPropertyNotSet = xoc.versionedGetBehaviourIfPropertyNotSet(this.version);
      hash = hash * 31 + (behaviourIfPropertyNotSet == null ? 0 : behaviourIfPropertyNotSet.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
      return hash;
    }

  }


  public ObjectVersion createObjectVersion(long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
    return new ObjectVersion(this, version, changeSetsOfMembers);
  }


  public boolean supportsObjectVersioning() {
    return XOUtils.supportsObjectVersioningForInternalObjects();
  }


  public void collectChanges(long start, long end, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers, java.util.Set<Long> datapoints) {
    XOUtils.addChangesForSimpleMember(oldVersionsOfpropertyName, start, end, datapoints);
    XOUtils.addChangesForComplexMember(behaviourIfPropertyNotSet, oldVersionsOfbehaviourIfPropertyNotSet, start, end, changeSetsOfMembers, datapoints);
  }

  public String toXml(String varName, boolean onlyContent) {
    return toXml(varName, onlyContent, -1, null);
  }

  public String toXml(String varName, boolean onlyContent, long version, com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject.XMLReferenceCache cache) {
    StringBuilder xml = new StringBuilder();
    long objectId;
    if (!onlyContent) {
      long refId;
      if (cache != null) {
        ObjectVersion ov = new ObjectVersion(this, version, cache.changeSetsOfMembers);
        refId = cache.putIfAbsent(ov);
        if (refId > 0) {
          objectId = -2;
        } else {
          objectId = -refId;
          refId = -1;
        }
      } else {
        objectId = -1;
        refId = -1;
      }
      XMLHelper.beginType(xml, varName, "XynaProperty", "xfmg.xods.configuration", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendData(xml, "propertyName", versionedGetPropertyName(version), version, cache);
      XMLHelper.appendData(xml, "behaviourIfPropertyNotSet", versionedGetBehaviourIfPropertyNotSet(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"propertyName", "behaviourIfPropertyNotSet"})));
  /**
  * @deprecated use {@link #getVariableNames()} instead
  */
  @Deprecated
  public HashSet<String> getVarNames() {
    return new HashSet<String>(varNames);
  }


  public Set<String> getVariableNames() {
    return varNames;
  }

  /**
   * gets membervariable by name or path. e.g. get("myVar.myChild") gets
   * the child variable of the membervariable named "myVar" and is equivalent
   * to getMyVar().getMyChild()
   * @param name variable name or path separated by ".".
   */
  public Object get(String name) throws InvalidObjectPathException {
    String[] varNames = new String[]{"propertyName", "behaviourIfPropertyNotSet"};
    Object[] vars = new Object[]{this.propertyName, this.behaviourIfPropertyNotSet};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("propertyName".equals(name)) {
      XOUtils.checkCastability(o, String.class, "propertyName");
      setPropertyName((String) o);
    } else if ("behaviourIfPropertyNotSet".equals(name)) {
      XOUtils.checkCastability(o, BehaviourIfPropertyNotSet.class, "behaviourIfPropertyNotSet");
      setBehaviourIfPropertyNotSet((BehaviourIfPropertyNotSet) o);
    } else {
      throw new XDEV_PARAMETER_NAME_NOT_FOUND(name);
    }
  }

  protected List<? extends Documentation> readDocumentation_InternalSuperCallDestination(XynaProperty internalSuperCallDelegator) {
    return readDocumentation_InternalImplementation();
  }

  public List<? extends Documentation> readDocumentation() {
    return readDocumentation_InternalImplementation();
  }

  /*------- not generated -------- */
  
  protected XynaPropertySerializationWrapper<?> xpsWrapper;
  
  private boolean throwExceptionIfNoValueSet;
  
  public void setThrowExceptionIfNoValueSet(boolean throwExceptionIfNoValueSet) {
    this.throwExceptionIfNoValueSet = throwExceptionIfNoValueSet;
  }

  private List<? extends Documentation> readDocumentation_InternalImplementation() {
    Map<com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage, String> dd = getXynaPropertyImpl().getDefaultDocumentation();
    if( dd == null ) {
      return Collections.emptyList();
    }
    List<Documentation> documentation = new ArrayList<Documentation>(dd.size());
    for( Map.Entry<com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage, String> doc : dd.entrySet() ) {
      DocumentationLanguage lang = null;
      switch( doc.getKey() ) {
        case DE :
          lang = new DE();
          break;
        case EN :
          lang = new EN();
          break;
      }
      documentation.add( new Documentation( lang, doc.getValue()) );
    }
    return documentation;
  }

  protected void checkNoValueSet(Object value) {
    if (value == null && throwExceptionIfNoValueSet) {
      throw new RuntimeException("Neither Value nor Default Value set for property \"" + propertyName + "\"");
    }
  }
  
  public String toString() {
    return "XynaProperty("+
       (propertyName==null?"null":("\""+propertyName+"\""))+
       ","+behaviourIfPropertyNotSet+")";
  }

  public static class XynaPropertySerializationWrapper<T> implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private transient XynaPropertyBase<T,?> xynaPropertyImpl;

    public XynaPropertySerializationWrapper(XynaPropertyBase<T, ?> xynaPropertyImpl) {
      this.xynaPropertyImpl = xynaPropertyImpl;
    }
    @SuppressWarnings("unchecked")
    public void setXynaPropertyImpl(XynaPropertyBase<?, ?> xynaPropertyImpl) {
      this.xynaPropertyImpl = (XynaPropertyBase<T, ?>) xynaPropertyImpl;
    }
    
    public Object get() {
      return xynaPropertyImpl.get();
    }

    public XynaPropertyBase<?, ?> getXynaPropertyImpl() {
      return xynaPropertyImpl;
    }
  }
  
  private static Configuration configuration;
  
  @SuppressWarnings("unchecked")
  protected <T> T getPropertyValue() {
    T value;
    if (xpsWrapper != null && !xpsWrapper.getXynaPropertyImpl().getPropertyName().equals(propertyName)) {
      //vielleicht hat jemand den propertynamen umgesetzt
      xpsWrapper = null;
    }
    if (xpsWrapper != null) {
      value = (T) xpsWrapper.get();
    } else {
      Configuration c = configuration;
      if (c == null) { //keine notwendigkeit, das zu synchronisieren
        c = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration();
        if (XynaFactory.getInstance().finishedInitialization()) {
          configuration = c;
        }
      }
      value = (T) c.getProperty(propertyName);
    }
    checkNoValueSet(value);
    return value;
  }
  
  public <T> XynaProperty setXynaPropertyImpl(XynaPropertyBase<T, ?> xynaPropertyImpl) {
    if( xpsWrapper == null ) {
      xpsWrapper = new XynaPropertySerializationWrapper<T>(xynaPropertyImpl);
    } else {
      xpsWrapper.setXynaPropertyImpl(xynaPropertyImpl);
    }
    get_InternalImplementation();
    return this;
  }
  
  protected XynaPropertyBase<?, ?> getXynaPropertyImpl() {
    return xpsWrapper.getXynaPropertyImpl();
  }
  
  protected abstract Object get_InternalImplementation();
  
  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();
  
  public static Field getField(String target_fieldname) throws InvalidObjectPathException {
    Field foundField = null;
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = XynaProperty.class.getDeclaredField(target_fieldname);
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
  
}

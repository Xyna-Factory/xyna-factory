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
package com.gip.xyna.xfmg.xods.configuration.xynaobjects;


import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;

@XynaObjectAnnotation(fqXmlName = "xfmg.xods.configuration.XynaPropertyInteger")
public class XynaPropertyInteger extends XynaProperty {

  private static final long serialVersionUID = 4994534669L;
  private static final Logger logger = CentralFactoryLogging.getLogger(XynaPropertyInteger.class);


  @LabelAnnotation(label="Value")
  private Integer value;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> oldVersionsOfvalue;


  public Integer getValue() {
    return value;
  }

  public Integer versionedGetValue(long _version) {
    if (oldVersionsOfvalue == null) {
      return value;
    }
    Integer _local = value;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Integer> _ret = oldVersionsOfvalue.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setValue(Integer value) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> _vo = oldVersionsOfvalue;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfvalue;
          if (_vo == null) {
            oldVersionsOfvalue = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.value);
        this.value = value;
      }
      return;
    }
    this.value = value;
  }

  public void unversionedSetValue(Integer value) {
    this.value = value;
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends XynaPropertyInteger, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>> extends XynaProperty.InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>  {

    protected InternalBuilder(XynaPropertyInteger instance) {
      super(instance);
    }

    public XynaPropertyInteger instance() {
      return (XynaPropertyInteger) instance;
    }

    @SuppressWarnings("unchecked")
    public _GEN_BUILDER_TYPE value(Integer value) {
      this.instance.unversionedSetValue(value);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<XynaPropertyInteger, Builder> {
    public Builder() {
      super(new XynaPropertyInteger());
    }
    public Builder(XynaPropertyInteger instance) {
      super(instance);
    }
  }

  public Builder buildXynaPropertyInteger() {
    return new Builder(this);
  }

  public XynaPropertyInteger() {
    super();
  }

  /**
  * Creates a new instance using locally defined member variables.
  */
  public XynaPropertyInteger(Integer value) {
    this();
    this.value = value;
  }

  /**
  * Creates a new instance expecting all inherited member variables
  */
  public XynaPropertyInteger(String propertyName, com.gip.xyna.xfmg.xods.configuration.xynaobjects.BehaviourIfPropertyNotSet behaviourIfPropertyNotSet, Integer value) {
    this(value);
    unversionedSetPropertyName(propertyName);
    unversionedSetBehaviourIfPropertyNotSet(behaviourIfPropertyNotSet);
  }

  protected void fillVars(XynaPropertyInteger source, boolean deep) {
    super.fillVars(source, deep);
    this.value = source.value;
  }

  public XynaPropertyInteger clone() {
    return clone(true);
  }

  public XynaPropertyInteger clone(boolean deep) {
    XynaPropertyInteger cloned = new XynaPropertyInteger();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xfmg.xods.configuration.xynaobjects.XynaProperty.ObjectVersion {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      if (!super.memberEquals(o)) {
        return false;
      }
      ObjectVersion other = (ObjectVersion) o;
      XynaPropertyInteger xoc = (XynaPropertyInteger) xo;
      XynaPropertyInteger xoco = (XynaPropertyInteger) other.xo;
      if (!equal(xoc.versionedGetValue(this.version), xoco.versionedGetValue(other.version))) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = super.calcHashOfMembers(stack);
      XynaPropertyInteger xoc = (XynaPropertyInteger) xo;
      Integer value = xoc.versionedGetValue(this.version);
      hash = hash * 31 + (value == null ? 0 : value.hashCode());
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
    super.collectChanges(start, end, changeSetsOfMembers, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfvalue, start, end, datapoints);
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
      XMLHelper.beginType(xml, varName, "XynaPropertyInteger", "xfmg.xods.configuration", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      xml.append(super.toXml(varName, true, version, cache));
      XMLHelper.appendData(xml, "value", versionedGetValue(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"value"})));
  /**
  * @deprecated use {@link #getVariableNames()} instead
  */
  @Deprecated
  public HashSet<String> getVarNames() {
    HashSet<String> ret = new HashSet<String>(varNames);
    ret.addAll(super.getVarNames());
    return ret;
  }


  public Set<String> getVariableNames() {
    Set<String> ret = new HashSet<String>(varNames);
    ret.addAll(super.getVariableNames());
    return Collections.unmodifiableSet(ret);
  }

  /**
   * gets membervariable by name or path. e.g. get("myVar.myChild") gets
   * the child variable of the membervariable named "myVar" and is equivalent
   * to getMyVar().getMyChild()
   * @param name variable name or path separated by ".".
   */
  public Object get(String name) throws InvalidObjectPathException {
    String[] varNames = new String[]{"value"};
    Object[] vars = new Object[]{this.value};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      o = super.get(name);
      if (o == XOUtils.VARNAME_NOTFOUND) {
        throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
      }
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("value".equals(name)) {
      XOUtils.checkCastability(o, Integer.class, "value");
      setValue((Integer) o);
    } else {
      super.set(name, o);
    }
  }

  protected Integer get_InternalSuperCallDestination(XynaPropertyInteger internalSuperCallDelegator) {
    return get_InternalImplementation();
  }

  public Integer get() {
    return get_InternalImplementation();
  }

  public void onDeployment() throws XynaException {
    super.onDeployment();
  }

  public void onUndeployment() throws XynaException {
    super.onUndeployment();
  }

  /**
   * "Publishing" of super-call. May be called by reflection only.
   */
  private List<? extends Documentation> readDocumentation_InternalSuperCallProxy(XynaProperty internalSuperCallDelegator) {
    return super.readDocumentation_InternalSuperCallDestination(internalSuperCallDelegator);
  }

  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();
  
  public static Field getField(String target_fieldname) throws InvalidObjectPathException {
    Field foundField = null;
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = XynaPropertyInteger.class.getDeclaredField(target_fieldname);
    } catch (NoSuchFieldException e) {
    }
    if (foundField == null) {
      return XynaProperty.getField(target_fieldname);
    } else {
      foundField.setAccessible(true);
      fieldMap.put(target_fieldname, foundField);
      return foundField;
    }
  }
  
/*------- not generated -------- */
  
  @Override
  protected Integer get_InternalImplementation() {
    setValue(getPropertyValue());
    return value;
  }
  
  public String toString() {
    return "XynaPropertyInteger("+super.toString()+","+value+")";
  }

}

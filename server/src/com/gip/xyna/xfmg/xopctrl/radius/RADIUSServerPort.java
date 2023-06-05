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
package com.gip.xyna.xfmg.xopctrl.radius;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;


@XynaObjectAnnotation(fqXmlName = "xfmg.xopctrl.radius.RADIUSServerPort")
public class RADIUSServerPort extends XynaObject {

  private static final long serialVersionUID = 4795154016996482760L;
  private static final Logger logger = CentralFactoryLogging.getLogger(RADIUSServerPort.class);


  private int value;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> oldVersionsOfvalue;


  public int getValue() {
    return value;
  }

  public int versionedGetValue(long _version) {
    if (oldVersionsOfvalue == null) {
      return value;
    }
    int _local = value;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Integer> _ret = oldVersionsOfvalue.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setValue(int value) {
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

  public void unversionedSetValue(int value) {
    this.value = value;
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends RADIUSServerPort, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>  {

    protected _GEN_DOM_TYPE instance;

    protected InternalBuilder(RADIUSServerPort instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }

    public RADIUSServerPort instance() {
      return (RADIUSServerPort) instance;
    }

    public _GEN_BUILDER_TYPE value(int value) {
      this.instance.unversionedSetValue(value);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<RADIUSServerPort, Builder> {
    public Builder() {
      super(new RADIUSServerPort());
    }
    public Builder(RADIUSServerPort instance) {
      super(instance);
    }
  }

  public Builder buildRADIUSServerPort() {
    return new Builder(this);
  }

  public RADIUSServerPort() {
    super();
  }

  /**
  * Creates a new instance using locally defined member variables.
  */
  public RADIUSServerPort(int value) {
    this();
    this.value = value;
  }

  protected void fillVars(RADIUSServerPort source, boolean deep) {
    this.value = source.value;
  }

  public RADIUSServerPort clone() {
    return clone(true);
  }

  public RADIUSServerPort clone(boolean deep) {
    RADIUSServerPort cloned = new RADIUSServerPort();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      RADIUSServerPort xoc = (RADIUSServerPort) xo;
      RADIUSServerPort xoco = (RADIUSServerPort) other.xo;
      if (xoc.versionedGetValue(this.version) != xoco.versionedGetValue(other.version)) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      RADIUSServerPort xoc = (RADIUSServerPort) xo;
      int value = xoc.versionedGetValue(this.version);
      hash = hash * 31 + Integer.valueOf(value).hashCode();
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
      XMLHelper.beginType(xml, varName, "RADIUSServerPort", "xfmg.xopctrl.radius", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
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
    String[] varNames = new String[]{"value"};
    Object[] vars = new Object[]{this.value};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("value".equals(name)) {
      XOUtils.checkCastability(o, Integer.class, "value");
      if (o != null) {
        setValue((Integer) o);
      }
    } else {
      throw new XDEV_PARAMETER_NAME_NOT_FOUND(name);
    }
  }

  public void onDeployment() throws XynaException {
    super.onDeployment();
  }

  public void onUndeployment() throws XynaException {
    super.onUndeployment();
  }

  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();
  
  public static Field getField(String target_fieldname) throws InvalidObjectPathException {
    Field foundField = null;
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = RADIUSServerPort.class.getDeclaredField(target_fieldname);
      if (foundField == null) {
        throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(target_fieldname));
      } else {
        foundField.setAccessible(true);
        fieldMap.put(target_fieldname, foundField);
        return foundField;
      }
    } catch (NoSuchFieldException e) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(target_fieldname, e));
    }
  }
}

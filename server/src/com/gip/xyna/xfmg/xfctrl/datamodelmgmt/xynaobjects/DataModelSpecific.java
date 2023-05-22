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
package com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects;


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
import com.gip.xyna.xprc.xfractwfe.generation.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;

@XynaObjectAnnotation(fqXmlName = "xfmg.xfctrl.datamodel.DataModelSpecific")
public class DataModelSpecific extends XynaObject implements Comparable<DataModelSpecific> {

  private static final long serialVersionUID = -1624481956814927L;
  private static final Logger logger = CentralFactoryLogging.getLogger(DataModelSpecific.class);


  @LabelAnnotation(label="Key")
  private String key;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOfkey;


  public String getKey() {
    return key;
  }

  public String versionedGetKey(long _version) {
    if (oldVersionsOfkey == null) {
      return key;
    }
    String _local = key;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOfkey.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setKey(String key) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOfkey;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfkey;
          if (_vo == null) {
            oldVersionsOfkey = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.key);
        this.key = key;
      }
      return;
    }
    this.key = key;
  }

  public void unversionedSetKey(String key) {
    this.key = key;
  }

  @LabelAnnotation(label="Value")
  private String value;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOfvalue;


  public String getValue() {
    return value;
  }

  public String versionedGetValue(long _version) {
    if (oldVersionsOfvalue == null) {
      return value;
    }
    String _local = value;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOfvalue.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setValue(String value) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOfvalue;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfvalue;
          if (_vo == null) {
            oldVersionsOfvalue = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
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

  public void unversionedSetValue(String value) {
    this.value = value;
  }

  @LabelAnnotation(label="Label")
  private String label;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOflabel;


  public String getLabel() {
    return label;
  }

  public String versionedGetLabel(long _version) {
    if (oldVersionsOflabel == null) {
      return label;
    }
    String _local = label;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOflabel.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setLabel(String label) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOflabel;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOflabel;
          if (_vo == null) {
            oldVersionsOflabel = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.label);
        this.label = label;
      }
      return;
    }
    this.label = label;
  }

  public void unversionedSetLabel(String label) {
    this.label = label;
  }


  protected static class InternalBuilder<_GEN_DOM_TYPE extends DataModelSpecific, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>  {

    protected _GEN_DOM_TYPE instance;

    protected InternalBuilder(DataModelSpecific instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }

    public DataModelSpecific instance() {
      return (DataModelSpecific) instance;
    }

    public _GEN_BUILDER_TYPE key(String key) {
      this.instance.unversionedSetKey(key);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE value(String value) {
      this.instance.unversionedSetValue(value);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE label(String label) {
      this.instance.unversionedSetLabel(label);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<DataModelSpecific, Builder> {
    public Builder() {
      super(new DataModelSpecific());
    }
    public Builder(DataModelSpecific instance) {
      super(instance);
    }
  }

  public Builder buildDataModelSpecific() {
    return new Builder(this);
  }

  public DataModelSpecific() {
    super();
  }

  /**
  * Creates a new instance using locally defined member variables.
  */
  public DataModelSpecific(String key, String value, String label) {
    this();
    this.key = key;
    this.value = value;
    this.label = label;
  }

  protected void fillVars(DataModelSpecific source, boolean deep) {
    this.key = source.key;
    this.value = source.value;
    this.label = source.label;
  }

  public DataModelSpecific clone() {
    return clone(true);
  }

  public DataModelSpecific clone(boolean deep) {
    DataModelSpecific cloned = new DataModelSpecific();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      DataModelSpecific xoc = (DataModelSpecific) xo;
      DataModelSpecific xoco = (DataModelSpecific) other.xo;
      if (!equal(xoc.versionedGetKey(this.version), xoco.versionedGetKey(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetValue(this.version), xoco.versionedGetValue(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetLabel(this.version), xoco.versionedGetLabel(other.version))) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      DataModelSpecific xoc = (DataModelSpecific) xo;
      String key = xoc.versionedGetKey(this.version);
      hash = hash * 31 + (key == null ? 0 : key.hashCode());
      String value = xoc.versionedGetValue(this.version);
      hash = hash * 31 + (value == null ? 0 : value.hashCode());
      String label = xoc.versionedGetLabel(this.version);
      hash = hash * 31 + (label == null ? 0 : label.hashCode());
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
    XOUtils.addChangesForSimpleMember(oldVersionsOfkey, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfvalue, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOflabel, start, end, datapoints);
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
      XMLHelper.beginType(xml, varName, "DataModelSpecific", "xfmg.xfctrl.datamodel", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendData(xml, "key", versionedGetKey(version), version, cache);
      XMLHelper.appendData(xml, "value", versionedGetValue(version), version, cache);
      XMLHelper.appendData(xml, "label", versionedGetLabel(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"key", "value", "label"})));
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
    String[] varNames = new String[]{"key", "value", "label"};
    Object[] vars = new Object[]{this.key, this.value, this.label};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("key".equals(name)) {
      XOUtils.checkCastability(o, String.class, "key");
      setKey((String) o);
    } else if ("value".equals(name)) {
      XOUtils.checkCastability(o, String.class, "value");
      setValue((String) o);
    } else if ("label".equals(name)) {
      XOUtils.checkCastability(o, String.class, "label");
      setLabel((String) o);
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
      foundField = DataModelSpecific.class.getDeclaredField(target_fieldname);
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

  
  /*-------not generated------*/

  public int compareTo(DataModelSpecific o) {
    int c = 0;
    if (key != null) {
      c = key.compareTo(o.key);
    }
    if (c == 0 && label != null) {
      c = label.compareTo(o.label);
    }
    return c;
  }


  public String toString() {
    return "DataModelSpecific(" + (key == null ? "null" : ("\"" + key + "\"")) + "," + (value == null ? "null" : ("\"" + value + "\""))
        + "," + (label == null ? "null" : ("\"" + label + "\"")) + ")";
  }


}

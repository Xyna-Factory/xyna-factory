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
package com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects;



import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;



@XynaObjectAnnotation(fqXmlName = "xfmg.xfctrl.datamodel.XmomType")
public class XmomType extends XynaObject implements Comparable<XmomType> {

  private static final long serialVersionUID = -1507694381429171L;


  @LabelAnnotation(label="Path")
  private String path;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOfpath;


  public String getPath() {
    return path;
  }


  public String versionedGetPath(long _version) {
    if (oldVersionsOfpath == null) {
      return path;
    }
    String _local = path;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOfpath.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }


  public void setPath(String path) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOfpath;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfpath;
          if (_vo == null) {
            oldVersionsOfpath = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.path);
        this.path = path;
      }
      return;
    }
    this.path = path;
  }


  public void unversionedSetPath(String path) {
    this.path = path;
  }


  @LabelAnnotation(label="Name")
  private String name;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOfname;


  public String getName() {
    return name;
  }


  public String versionedGetName(long _version) {
    if (oldVersionsOfname == null) {
      return name;
    }
    String _local = name;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOfname.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }


  public void setName(String name) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOfname;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfname;
          if (_vo == null) {
            oldVersionsOfname = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.name);
        this.name = name;
      }
      return;
    }
    this.name = name;
  }


  public void unversionedSetName(String name) {
    this.name = name;
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


  protected static class InternalBuilder<_GEN_DOM_TYPE extends XmomType, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>> {

    protected _GEN_DOM_TYPE instance;


    protected InternalBuilder(XmomType instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }


    public XmomType instance() {
      return (XmomType) instance;
    }


    public _GEN_BUILDER_TYPE path(String path) {
      this.instance.unversionedSetPath(path);
      return (_GEN_BUILDER_TYPE) this;
    }


    public _GEN_BUILDER_TYPE name(String name) {
      this.instance.unversionedSetName(name);
      return (_GEN_BUILDER_TYPE) this;
    }


    public _GEN_BUILDER_TYPE label(String label) {
      this.instance.unversionedSetLabel(label);
      return (_GEN_BUILDER_TYPE) this;
    }
    
    
    /*-------not generated------*/


    @SuppressWarnings("unchecked")
    public _GEN_BUILDER_TYPE fqTypeName(String fqTypeName) {
      int idx = fqTypeName.lastIndexOf('.');
      if (idx == -1) {
        this.instance.setPath("");
        this.instance.setName(fqTypeName);
      } else {
        this.instance.setPath(fqTypeName.substring(0, idx));
        this.instance.setName(fqTypeName.substring(idx + 1));
      }
      return (_GEN_BUILDER_TYPE) this;
    }


  }

  public static class Builder extends InternalBuilder<XmomType, Builder> {

    public Builder() {
      super(new XmomType());
    }


    public Builder(XmomType instance) {
      super(instance);
    }
  }


  public Builder buildXmomType() {
    return new Builder(this);
  }


  public XmomType() {
    super();
  }


  /**
  * Creates a new instance using locally defined member variables.
  */
  public XmomType(String path, String name, String label) {
    this();
    this.path = path;
    this.name = name;
    this.label = label;
  }


  protected void fillVars(XmomType source, boolean deep) {
    this.path = source.path;
    this.name = source.name;
    this.label = source.label;
  }


  public XmomType clone() {
    return clone(true);
  }


  public XmomType clone(boolean deep) {
    XmomType cloned = new XmomType();
    cloned.fillVars(this, deep);
    return cloned;
  }


  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version,
                         java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }


    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      XmomType xoc = (XmomType) xo;
      XmomType xoco = (XmomType) other.xo;
      if (!equal(xoc.versionedGetPath(this.version), xoco.versionedGetPath(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetName(this.version), xoco.versionedGetName(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetLabel(this.version), xoco.versionedGetLabel(other.version))) {
        return false;
      }
      return true;
    }


    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      XmomType xoc = (XmomType) xo;
      String path = xoc.versionedGetPath(this.version);
      hash = hash * 31 + (path == null ? 0 : path.hashCode());
      String name = xoc.versionedGetName(this.version);
      hash = hash * 31 + (name == null ? 0 : name.hashCode());
      String label = xoc.versionedGetLabel(this.version);
      hash = hash * 31 + (label == null ? 0 : label.hashCode());
      return hash;
    }

  }


  public ObjectVersion createObjectVersion(long version,
                                           java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
    return new ObjectVersion(this, version, changeSetsOfMembers);
  }


  public boolean supportsObjectVersioning() {
    return XOUtils.supportsObjectVersioningForInternalObjects();
  }


  public void collectChanges(long start, long end,
                             java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers,
                             java.util.Set<Long> datapoints) {
    XOUtils.addChangesForSimpleMember(oldVersionsOfpath, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfname, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOflabel, start, end, datapoints);
  }


  public String toXml(String varName, boolean onlyContent) {
    return toXml(varName, onlyContent, -1, null);
  }


  public String toXml(String varName, boolean onlyContent, long version,
                      com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject.XMLReferenceCache cache) {
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
      XMLHelper.beginType(xml, varName, "XmomType", "xfmg.xfctrl.datamodel", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendData(xml, "path", versionedGetPath(version), version, cache);
      XMLHelper.appendData(xml, "name", versionedGetName(version), version, cache);
      XMLHelper.appendData(xml, "label", versionedGetLabel(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }


  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays
      .asList(new String[] {"path", "name", "label"})));


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
    String[] varNames = new String[] {"path", "name", "label"};
    Object[] vars = new Object[] {this.path, this.name, this.label};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }


  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("path".equals(name)) {
      XOUtils.checkCastability(o, String.class, "path");
      setPath((String) o);
    } else if ("name".equals(name)) {
      XOUtils.checkCastability(o, String.class, "name");
      setName((String) o);
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
      foundField = XmomType.class.getDeclaredField(target_fieldname);
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

  public int compareTo(XmomType o) {
    int c = 0;
    if (path != null) {
      c = path.compareTo(o.path);
    }
    if (c == 0 && name != null) {
      c = name.compareTo(o.name);
    }
    return c;
  }


  public XmomType(com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType type) {
    this();
    this.path = type.getPath();
    this.name = type.getName();
    this.label = type.getLabel();
  }


  public String toString() {
    return "XmomType(" + (path == null ? "null" : ("\"" + path + "\"")) + "," + (name == null ? "null" : ("\"" + name + "\"")) + ","
        + (label == null ? "null" : ("\"" + label + "\"")) + ")";
  }


  public String getFqName() {
    if (path == null || path.length() == 0) {
      return name;
    }
    return path + "." + name;
  }

}

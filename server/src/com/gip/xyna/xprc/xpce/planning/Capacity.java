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
package com.gip.xyna.xprc.xpce.planning;

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
import com.gip.xyna.xprc.xsched.SchedulerBean;

@XynaObjectAnnotation(fqXmlName = "xprc.Capacity")
public class Capacity extends XynaObject {

  private static final long serialVersionUID = 1L;


  @LabelAnnotation(label="Cap Name")
  private String capName;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOfcapName;


  public String getCapName() {
    return capName;
  }

  public String versionedGetCapName(long _version) {
    if (oldVersionsOfcapName == null) {
      return capName;
    }
    String _local = capName;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOfcapName.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setCapName(String capName) {
    validateCapacityName(capName);
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOfcapName;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfcapName;
          if (_vo == null) {
            oldVersionsOfcapName = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.capName);
        this.capName = capName;
      }
      return;
    }
    this.capName = capName;
  }

  public void unversionedSetCapName(String capName) {
    validateCapacityName(capName);
    this.capName = capName;
  }

  @LabelAnnotation(label="Cardinality")
  private int cardinality;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> oldVersionsOfcardinality;


  public int getCardinality() {
    return cardinality;
  }

  public int versionedGetCardinality(long _version) {
    if (oldVersionsOfcardinality == null) {
      return cardinality;
    }
    int _local = cardinality;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Integer> _ret = oldVersionsOfcardinality.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setCardinality(int cardinality) {
    validateCardinality(cardinality);
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> _vo = oldVersionsOfcardinality;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfcardinality;
          if (_vo == null) {
            oldVersionsOfcardinality = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.cardinality);
        this.cardinality = cardinality;
      }
      return;
    }
    this.cardinality = cardinality;
  }

  public void unversionedSetCardinality(int cardinality) {
    validateCardinality(cardinality);
    this.cardinality = cardinality;
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends Capacity, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>  {

    protected _GEN_DOM_TYPE instance;

    protected InternalBuilder(Capacity instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }

    public Capacity instance() {
      return (Capacity) instance;
    }

    public _GEN_BUILDER_TYPE capName(String capName) {
      this.instance.unversionedSetCapName(capName);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE cardinality(int cardinality) {
      this.instance.unversionedSetCardinality(cardinality);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<Capacity, Builder> {
    public Builder() {
      super(new Capacity());
    }
    public Builder(Capacity instance) {
      super(instance);
    }
  }

  public Builder buildCapacity() {
    return new Builder(this);
  }

  public Capacity() {
    super();
  }

  /**
  * Creates a new instance using locally defined member variables.
  */
  public Capacity(String capName, int cardinality) {
    this();
    unversionedSetCapName(capName);
    unversionedSetCardinality(cardinality);
  }

  protected void fillVars(Capacity source, boolean deep) {
    this.capName = source.capName;
    this.cardinality = source.cardinality;
  }

  public Capacity clone() {
    return clone(true);
  }

  public Capacity clone(boolean deep) {
    Capacity cloned = new Capacity();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      Capacity xoc = (Capacity) xo;
      Capacity xoco = (Capacity) other.xo;
      if (!equal(xoc.versionedGetCapName(this.version), xoco.versionedGetCapName(other.version))) {
        return false;
      }
      if (xoc.versionedGetCardinality(this.version) != xoco.versionedGetCardinality(other.version)) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      Capacity xoc = (Capacity) xo;
      String capName = xoc.versionedGetCapName(this.version);
      hash = hash * 31 + (capName == null ? 0 : capName.hashCode());
      int cardinality = xoc.versionedGetCardinality(this.version);
      hash = hash * 31 + Integer.valueOf(cardinality).hashCode();
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
    XOUtils.addChangesForSimpleMember(oldVersionsOfcapName, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfcardinality, start, end, datapoints);
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
      XMLHelper.beginType(xml, varName, "Capacity", "xprc", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendData(xml, "capName", versionedGetCapName(version), version, cache);
      XMLHelper.appendData(xml, "cardinality", versionedGetCardinality(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"capName", "cardinality"})));
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
    String[] varNames = new String[]{"capName", "cardinality"};
    Object[] vars = new Object[]{this.capName, this.cardinality};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("capName".equals(name)) {
      XOUtils.checkCastability(o, String.class, "capName");
      setCapName((String) o);
    } else if ("cardinality".equals(name)) {
      XOUtils.checkCastability(o, Integer.class, "cardinality");
      if (o != null) {
        setCardinality((Integer) o);
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
      foundField = Capacity.class.getDeclaredField(target_fieldname);
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


/*------- not generated -------- */

  public String toString() {
    return "Capacity("+
       (capName==null?"null":("\""+capName+"\""))+
       ","+cardinality+")";
  }

  private static void validateCapacityName(String capName) {
    if (capName == null) {
      throw new IllegalArgumentException("capacity name may not be null");
    }
    if (capName.length() == 0) {
      throw new IllegalArgumentException("capacity name may not be empty");
    }
  }

  private static void validateCardinality(int cardinality) {
    if (cardinality < 0) {
      throw new IllegalArgumentException("capacity cardinality may not be negative");
    }
  }

}



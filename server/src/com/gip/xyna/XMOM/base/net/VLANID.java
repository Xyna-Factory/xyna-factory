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
package com.gip.xyna.XMOM.base.net;


import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XMOM.base.net.internal.VlanIdData;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;

@XynaObjectAnnotation(fqXmlName = "base.net.VLANID")
public class VLANID extends XynaObject {

  private static final long serialVersionUID = 93629074L;
  private static final Logger logger = CentralFactoryLogging.getLogger(VLANID.class);

  private VlanIdData _idData;
  /**
   * required for getField
   * when using VLANID in a mapping and assigning a value to "iD", the type adjustment mechanism
   * calls getField("iD") to determine which type it should adjust to. It should adjust to int
   * therefore a Member of type int is required.
   * Can be removed once XBE-462 is implemented.
   */
  @SuppressWarnings("unused")
  private volatile int _idDataInt; 

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<VlanIdData> oldVersionsOfiD;


  public int getID() {
    return transformVlanIdToInt(_idData);
  }

  public int versionedGetID(long _version) {
    if (oldVersionsOfiD == null) {
      return transformVlanIdToInt(_idData);
    }
    int _local = transformVlanIdToInt(_idData);
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<VlanIdData> _ret = oldVersionsOfiD.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return transformVlanIdToInt(_ret.object);
  }

  public void setID(int iD) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<VlanIdData> _vo = oldVersionsOfiD;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfiD;
          if (_vo == null) {
            oldVersionsOfiD = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<VlanIdData>();
          }
        }
      }
      VlanIdData local = transformIntToVlanId(iD);
      synchronized (_vo) {
        _vo.add(this._idData);
        this._idData = local;
      }
      return;
    }
    unversionedSetID(iD);
  }

  public void unversionedSetID(int iD) {
    this._idData = transformIntToVlanId(iD);
  }


  private static int transformVlanIdToInt(VlanIdData vd) {
    if (vd == null) {
      return -1;
    }
    return vd.getId();
  }

  private static VlanIdData transformIntToVlanId(int id) {
    try {
      return new VlanIdData(id);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends VLANID, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>  {

    protected _GEN_DOM_TYPE instance;

    protected InternalBuilder(VLANID instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }

    public VLANID instance() {
      return (VLANID)instance;
    }

    public _GEN_BUILDER_TYPE iD(int iD) {
      this.instance.unversionedSetID(iD);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<VLANID, Builder> {
    public Builder() {
      super(new VLANID());
    }
    public Builder(VLANID instance) {
      super(instance);
    }
  }

  public Builder buildVLANID() {
    return new Builder(this);
  }

  public VLANID() {
    super();
  }

  /**
  * Creates a new instance ignoring inherited member variables. Inherited member variables may
  * have been overwritten.
  */
  public VLANID(int iD) {
    this();
    this.unversionedSetID(iD);
  }

  protected void fillVars(VLANID source, boolean deep) {
    this.setID(source.getID());
  }

  public VLANID clone() {
    return clone(true);
  }

  public VLANID clone(boolean deep) {
    VLANID cloned = new VLANID();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      VLANID xoc = (VLANID) xo;
      VLANID xoco = (VLANID) other.xo;
      if (xoc.versionedGetID(this.version) != xoco.versionedGetID(other.version)) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      VLANID xoc = (VLANID) xo;
      int iD = xoc.versionedGetID(this.version);
      hash = hash * 31 + Integer.valueOf(iD).hashCode();
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
    XOUtils.addChangesForSimpleMember(oldVersionsOfiD, start, end, datapoints);
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
      XMLHelper.beginType(xml, varName, "VLANID", "base.net", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendData(xml, "iD", versionedGetID(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"iD"})));
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
    String[] varNames = new String[]{"iD"};
    Object[] vars = new Object[]{this.getID()};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("iD".equals(name)) {
      XOUtils.checkCastability(o, Integer.class, "iD");
      if(o != null) {
        this.setID((Integer) o);
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
    
    if("iD".equals(target_fieldname)){
    target_fieldname = "_idDataInt";
  }
    
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = VLANID.class.getDeclaredField(target_fieldname);
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

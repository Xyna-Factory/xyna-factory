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
import com.gip.xyna.XMOM.base.net.internal.MACAddressData;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;

@XynaObjectAnnotation(fqXmlName = "base.net.MACAddress")
public class MACAddress extends XynaObject {

  private static final long serialVersionUID = 16682745373L;
  private static final Logger logger = CentralFactoryLogging.getLogger(MACAddress.class);

  private MACAddressData _addressData;
  /**
   * required for getField
   * when using MACAddress in a mapping and assigning a value to "address", the type adjustment mechanism
   * calls getField("address") to determine which type it should adjust to. It should adjust to String
   * therefore a Member of type String is required.
   * Can be removed once XBE-462 is implemented.
   */
  @SuppressWarnings("unused")
  private volatile String _addressString; 
  
  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<MACAddressData> oldVersionsOfaddress;


  public String getAddress() {
    return transformMacToString(_addressData);
  }

  public String versionedGetAddress(long _version) {
    if (oldVersionsOfaddress == null) {
      return getAddress();
    }
    String _local = getAddress();
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<MACAddressData> _ret = oldVersionsOfaddress.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return transformMacToString(_ret.object);
  }

  public void setAddress(String address) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<MACAddressData> _vo = oldVersionsOfaddress;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfaddress;
          if (_vo == null) {
            oldVersionsOfaddress = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<MACAddressData>();
          }
        }
      }
      
      MACAddressData local = transformStringToMac(address);
      synchronized (_vo) {
        _vo.add(this._addressData);
        this._addressData = local;
      }
      return;
    }
    unversionedSetAddress(address);
  }

  public void unversionedSetAddress(String address) {
    this._addressData = transformStringToMac(address);
  }

  private static String transformMacToString(MACAddressData mac) {
    if (mac == null) {
      return null;
    }
    return mac.getColonSeparated();
  }

  private static MACAddressData transformStringToMac(String address) {
    try {
      return new MACAddressData(address);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  
  protected static class InternalBuilder<_GEN_DOM_TYPE extends MACAddress, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>  {

    protected _GEN_DOM_TYPE instance;

    protected InternalBuilder(MACAddress instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }

    public MACAddress instance() {
      return (MACAddress)instance;
    }

    public _GEN_BUILDER_TYPE address(String address) {
      this.instance.unversionedSetAddress(address);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<MACAddress, Builder> {
    public Builder() {
      super(new MACAddress());
    }
    public Builder(MACAddress instance) {
      super(instance);
    }
  }

  public Builder buildMACAddress() {
    return new Builder(this);
  }

  public MACAddress() {
    super();
  }

  /**
  * Creates a new instance ignoring inherited member variables. Inherited member variables may
  * have been overwritten.
  */
  public MACAddress(String address) {
    this();
    this.unversionedSetAddress(address);
  }

  protected void fillVars(MACAddress source, boolean deep) {
    this.setAddress(source.getAddress());
  }

  public MACAddress clone() {
    return clone(true);
  }

  public MACAddress clone(boolean deep) {
    MACAddress cloned = new MACAddress();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      MACAddress xoc = (MACAddress) xo;
      MACAddress xoco = (MACAddress) other.xo;
      if (!equal(xoc.versionedGetAddress(this.version), xoco.versionedGetAddress(other.version))) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      MACAddress xoc = (MACAddress) xo;
      String address = xoc.versionedGetAddress(this.version);
      hash = hash * 31 + (address == null ? 0 : address.hashCode());
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
    XOUtils.addChangesForSimpleMember(oldVersionsOfaddress, start, end, datapoints);
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
      XMLHelper.beginType(xml, varName, "MACAddress", "base.net", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendData(xml, "address", versionedGetAddress(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }


  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"address"})));
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
    String[] varNames = new String[]{"address"};
    Object[] vars = new Object[]{this.getAddress()};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("address".equals(name)) {
      XOUtils.checkCastability(o, String.class, "address");
      if(o != null) {
        this.setAddress((String) o);
      }
    } else {
      throw new XDEV_PARAMETER_NAME_NOT_FOUND(name);
    }
  }


  public String dotSeparated() {
    if (this._addressData == null) {
      return null;
    }
    return this._addressData.getDotSeparated();
  }


  public String hyphenSeparated() {
    if (this._addressData == null) {
      return null;
    }
    return this._addressData.getHyphenSeparated();
  }


  public void onDeployment() throws XynaException {
    super.onDeployment();
  }

  public void onUndeployment() throws XynaException {
    super.onUndeployment();
  }

  protected void initImplementationOfInstanceMethods() {
    throw new java.lang.RuntimeException("Unexpected call of initImplementationOfInstanceMethods. Non abstract implementation of library is needed.");
  }
  
  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();
  
  public static Field getField(String target_fieldname) throws InvalidObjectPathException {
    Field foundField = null;


    if ("address".equals(target_fieldname)) {
      target_fieldname = "_addressString";
    }

    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = MACAddress.class.getDeclaredField(target_fieldname);
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

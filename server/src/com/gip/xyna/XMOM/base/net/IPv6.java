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
import com.gip.xyna.XMOM.base.net.exception.FormatException;
import com.gip.xyna.XMOM.base.net.exception.IPv6FormatException;
import com.gip.xyna.XMOM.base.net.exception.ValidationException;
import com.gip.xyna.XMOM.base.net.internal.IPv6Address;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;


@XynaObjectAnnotation(fqXmlName = "base.net.IPv6")
public class IPv6 extends XynaObject {

  private static final long serialVersionUID = 16682745373L;
  private static final Logger _logger = CentralFactoryLogging.getLogger(IPv6.class);


  private IPv6Address _address;

  // fake-variable that can be found by XOUtils.getLabelFor(...) when determining labels for fields in getVariableNames()
  @LabelAnnotation(label="Address")
  private transient IPv6Address address;
  
  /**
   * required for getField
   * when using IPv6 in a mapping and assigning a value to "address", the type adjustment mechanism
   * calls getField("address") to determine which type it should adjust to. It should adjust to String
   * therefore a Member of type String is required.
   */
  @SuppressWarnings("unused")
  private volatile String _addressString; 

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IPv6Address> oldVersionsOfaddress;


  public String getAddress() {
    return transformAddressToString(_address);
  }

  private static String transformAddressToString(IPv6Address a) {
    if (a == null) {
      return null;
    }
    return a.toShortHexRepresentation();
  }

  public void setAddress(String address) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IPv6Address> _vo = oldVersionsOfaddress;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfaddress;
          if (_vo == null) {
            oldVersionsOfaddress = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IPv6Address>();
          }
        }
      }
      IPv6Address local = transformStringToAddress(address);
      synchronized (_vo) {
        _vo.add(this._address);
        _address = local;
      }
      return;
    }
    _address = transformStringToAddress(address);
  }

  private static IPv6Address transformStringToAddress(String address) {
    if (address == null) {
      return null;
    }
    try {
      return new IPv6Address(address);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public String versionedGetAddress(long _version) {
    if (oldVersionsOfaddress == null) {
      return getAddress();
    }
    IPv6Address _local = _address;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<IPv6Address> _ret = oldVersionsOfaddress.getVersion(_version);
    if (_ret == null) {
      return transformAddressToString(_local);
    }
    return transformAddressToString(_ret.object);
  }


  public void unversionedSetAddress(String address) {
    _address = transformStringToAddress(address);
  }


  protected IPv6Address getIPv6Address() {
    return _address;
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends IPv6, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>  {

    protected _GEN_DOM_TYPE instance;

    protected InternalBuilder(IPv6 instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }

    public IPv6 instance() {
      return (IPv6)instance;
    }

    public _GEN_BUILDER_TYPE address(String address) {
      this.instance.unversionedSetAddress(address);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<IPv6, Builder> {
    public Builder() {
      super(new IPv6());
    }
    public Builder(IPv6 instance) {
      super(instance);
    }
  }

  public Builder buildIPv6() {
    return new Builder(this);
  }

  public IPv6() {
    super();
  }

  /**
  * Creates a new instance ignoring inherited member variables. Inherited member variables may
  * have been overwritten.
  */
  public IPv6(String address) {
    this();
    this.unversionedSetAddress(address);
  }

  protected IPv6(IPv6Address address) {
    this();
    this._address = address;
  }

  protected void fillVars(IPv6 source, boolean deep) {
    this.setAddress(source.getAddress());
  }

  public IPv6 clone() {
    return clone(true);
  }

  public IPv6 clone(boolean deep) {
    IPv6 cloned = new IPv6();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      IPv6 xoc = (IPv6) xo;
      IPv6 xoco = (IPv6) other.xo;
      if (!equal(xoc.versionedGetAddress(this.version), xoco.versionedGetAddress(other.version))) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      IPv6 xoc = (IPv6) xo;
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
      XMLHelper.beginType(xml, varName, "IPv6", "base.net", objectId, refId);
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

  public void onDeployment() throws XynaException {
    super.onDeployment();
  }

  public void onUndeployment() throws XynaException {
    super.onUndeployment();
  }
  
  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();
  
  public static Field getField(String target_fieldname) throws InvalidObjectPathException {
    Field foundField = null;
    
    
    if("address".equals(target_fieldname)){
      target_fieldname = "_addressString";
    }
    
    
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = IPv6.class.getDeclaredField(target_fieldname);
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


  public String toFullHexRepresentation() {
    if (this._address == null) {
      return null;
    }
    return _address.toFullHexRepresentation();
  }


  public String toShortHexRepresentation() {
    if (this._address == null) {
      return null;
    }
    return _address.toShortHexRepresentation();
  }


  public boolean isLinkLocal() throws ValidationException, FormatException {
    if (this._address == null) {
      return false;
    }
    return _address.isLinkLocal();
  }


  public boolean isMulticast() throws ValidationException, FormatException {
    if (this._address == null) {
      return false;
    }
    return _address.isMulticast();
  }


  public boolean isUniqueLocalUnicast() throws ValidationException, FormatException {
    if (this._address == null) {
      return false;
    }
    return _address.isUniqueLocalUnicast();
  }


  public boolean isUniqueLocalAddress() throws ValidationException, FormatException {
    if (this._address == null) {
      return false;
    }
    return _address.isUniqueLocalAddress();
  }


  public boolean isLocalLoopback() throws ValidationException, FormatException {
    if (this._address == null) {
      return false;
    }
    return _address.isLocalLoopback();
  }


  public boolean isUnspecifiedAddress() throws ValidationException, FormatException {
    if (this._address == null) {
      return false;
    }
    return _address.isUnspecifiedAddress();
  }


  public boolean is6To4Address() throws ValidationException, FormatException {
    if (this._address == null) {
      return false;
    }
    return _address.is6To4Address();
  }


  public IPv4 convertToIPv4Address() throws ValidationException, FormatException {
    if (this._address == null) {
      return null;
    }
    return new IPv4(_address.convertToV4Address());
  }

  public boolean isBroadcastAddress(IPv6Netmask mask) throws FormatException, ValidationException {
    if (_address == null) {
      return false;
    }
    return _address.isBroadcastAddress(mask.getIPv6NetmaskData());
  }

  public boolean isNetworkAddress(IPv6Netmask mask) throws FormatException, ValidationException {
    if (_address == null) {
      return false;
    }
    return _address.isNetworkAddress(mask.getIPv6NetmaskData());
  }

  public boolean isGatewayAddress(IPv6Netmask mask) throws FormatException, ValidationException {
    if (_address == null) {
      return false;
    }
    return _address.isGatewayAddress(mask.getIPv6NetmaskData());
  }


  public IPv6 getSubsequentIP() throws ValidationException, FormatException {
    if (this._address == null) {
      return null;
    }
    return new IPv6(_address.inc());
  }

  public IPv6 getWithOffset(IPv6 ip) throws ValidationException, FormatException {
    if (this._address == null) {
      return null;
    }
    return new IPv6(this._address.getWithOffset(ip.getIPv6Address()));
  }

  protected static IPv6 fromIPv4(IPv4 input) throws IPv6FormatException {
    IPv6Address addr = IPv6Address.fromV4Address(input.getIPv4AddressData());
    return new IPv6(addr);
  }

  public IPv6Subnet subnet(IPv6Netmask mask) throws FormatException, ValidationException {
    if (this._address == null) {
      return null;
    }
    IPv6Address networkAddr = _address.toNetworkAddressOfNetmask(mask.getIPv6NetmaskData());
    IPv6 networkAddr2 = new IPv6(networkAddr);
    return new IPv6Subnet(mask, networkAddr2);
  }

}

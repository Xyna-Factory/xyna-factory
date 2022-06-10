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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XMOM.base.net.exception.FormatException;
import com.gip.xyna.XMOM.base.net.exception.ValidationException;
import com.gip.xyna.XMOM.base.net.internal.IPv6Address;
import com.gip.xyna.XMOM.base.net.internal.IPv6NetmaskData;
import com.gip.xyna.XMOM.base.net.internal.IPv6SubnetData;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;


@XynaObjectAnnotation(fqXmlName = "base.net.IPv6Subnet")
public class IPv6Subnet extends XynaObject {

  private static final long serialVersionUID = 1577421322158L;
  private static final Logger _logger = CentralFactoryLogging.getLogger(IPv6Subnet.class);

  private IPv6 _network;
  private IPv6Netmask _mask;
  
  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IPv6> oldVersionsOfnetwork;


  public IPv6 getNetwork() {
    return _network;
  }

  public IPv6 versionedGetNetwork(long _version) {
    if (oldVersionsOfnetwork == null) {
      return _network;
    }
    IPv6 _local = _network;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<IPv6> _ret = oldVersionsOfnetwork.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setNetwork(IPv6 network) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IPv6> _vo = oldVersionsOfnetwork;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfnetwork;
          if (_vo == null) {
            oldVersionsOfnetwork = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IPv6>();
          }
        }
      }
      
      synchronized (_vo) {
        _vo.add(this._network);
        this._network = network;
      }
      return;
    }
    unversionedSetNetwork(network);
  }
  

  public void unversionedSetNetwork(IPv6 network) {
    this._network = network;
  }

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IPv6Netmask> oldVersionsOfmask;


  public IPv6Netmask getMask() {
    return _mask;
  }

  public IPv6Netmask versionedGetMask(long _version) {
    if (oldVersionsOfmask == null) {
      return _mask;
    }
    IPv6Netmask _local = _mask;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<IPv6Netmask> _ret = oldVersionsOfmask.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setMask(IPv6Netmask mask) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IPv6Netmask> _vo = oldVersionsOfmask;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfmask;
          if (_vo == null) {
            oldVersionsOfmask = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IPv6Netmask>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this._mask);
        this._mask = mask;
      }
      return;
    }
    unversionedSetMask(mask);
  }

  public void unversionedSetMask(IPv6Netmask mask) {
    this._mask = mask;
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends IPv6Subnet, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>  {

    protected _GEN_DOM_TYPE instance;

    protected InternalBuilder(IPv6Subnet instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }

    public IPv6Subnet instance() {
      return (IPv6Subnet)instance;
    }

    public _GEN_BUILDER_TYPE mask(IPv6Netmask mask) {
      this.instance.unversionedSetMask(mask);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE network(IPv6 network) {
      this.instance.unversionedSetNetwork(network);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<IPv6Subnet, Builder> {
    public Builder() {
      super(new IPv6Subnet());
    }
    public Builder(IPv6Subnet instance) {
      super(instance);
    }
  }

  public Builder buildIPv6Subnet() {
    return new Builder(this);
  }

  public IPv6Subnet() {
    super();
  }

  /**
  * Creates a new instance ignoring inherited member variables. Inherited member variables may
  * have been overwritten.
  */
  public IPv6Subnet(IPv6Netmask mask, IPv6 network) {
    this();
    this.unversionedSetMask(mask);
    this.unversionedSetNetwork(network);
  }

  protected void fillVars(IPv6Subnet source, boolean deep) {
    this.setMask((IPv6Netmask)XynaObject.clone(source.getMask(), deep));
    this.setNetwork((IPv6)XynaObject.clone(source.getNetwork(), deep));
  }

  public IPv6Subnet clone() {
    return clone(true);
  }

  public IPv6Subnet clone(boolean deep) {
    IPv6Subnet cloned = new IPv6Subnet();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      IPv6Subnet xoc = (IPv6Subnet) xo;
      IPv6Subnet xoco = (IPv6Subnet) other.xo;
      if (!xoEqual(xoc.versionedGetMask(this.version), xoco.versionedGetMask(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      if (!xoEqual(xoc.versionedGetNetwork(this.version), xoco.versionedGetNetwork(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      IPv6Subnet xoc = (IPv6Subnet) xo;
      IPv6Netmask mask = xoc.versionedGetMask(this.version);
      hash = hash * 31 + (mask == null ? 0 : mask.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
      IPv6 network = xoc.versionedGetNetwork(this.version);
      hash = hash * 31 + (network == null ? 0 : network.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
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
    XOUtils.addChangesForComplexMember(_mask, oldVersionsOfmask, start, end, changeSetsOfMembers, datapoints);
    XOUtils.addChangesForComplexMember(_network, oldVersionsOfnetwork, start, end, changeSetsOfMembers, datapoints);
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
      XMLHelper.beginType(xml, varName, "IPv6Subnet", "base.net", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendData(xml, "mask", versionedGetMask(version), version, cache);
      XMLHelper.appendData(xml, "network", versionedGetNetwork(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"mask", "network"})));
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
    String[] varNames = new String[]{"mask", "network"};
    Object[] vars = new Object[]{this.getMask(), this.getNetwork()};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }

  
  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("mask".equals(name)) {
      XOUtils.checkCastability(o, IPv6Netmask.class, "mask");
      this.setMask((IPv6Netmask) o);
    } else if ("network".equals(name)) {
      XOUtils.checkCastability(o, IPv6.class, "network");
      this.setNetwork((IPv6) o);
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
    
    if ("network".equals(target_fieldname)) {
      target_fieldname = "_network";
    }
    if ("mask".equals(target_fieldname)) {
      target_fieldname = "_mask";
    }
    
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = IPv6Subnet.class.getDeclaredField(target_fieldname);
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

  private IPv6SubnetData getIPv6SubnetData() throws ValidationException, FormatException {
    if (_mask == null || _network == null) {
      return null;
    }
    IPv6Address network = new IPv6Address(_network.getAddress());
    IPv6NetmaskData mask = new IPv6NetmaskData(_mask.prefixLength());
    return new IPv6SubnetData(network, mask);
  }
  
  public boolean ipWithinSubnet(IPv6 addr) throws ValidationException, FormatException {
    IPv6SubnetData _data = getIPv6SubnetData();
    if (_data == null) {
      return false;
    }
    return _data.ipWithinSubnet(addr.getIPv6Address());
  }
  
  
  public IPv6 broadcastAddress() throws FormatException, ValidationException {
    IPv6SubnetData _data = getIPv6SubnetData();
    if (_data == null) {
      return null;
    }
    return new IPv6(_data.getBroadcastAddress());
  }
  
  
  public IPv6 gatewayAddress() throws FormatException, ValidationException {
    IPv6SubnetData _data = getIPv6SubnetData();
    if (_data == null) {
      return null;
    }
    return new IPv6(_data.getGatewayAddress());
  }
  
  
  public IPv6 nextFreeIP(List<IPv6> list) throws FormatException, ValidationException {
    IPv6SubnetData _data = getIPv6SubnetData();
    if (_data == null) {
      return null;
    }
    List<IPv6Address> adapted = new ArrayList<IPv6Address>();
    for (IPv6 ip : list) {
      adapted.add(ip.getIPv6Address());
    }
    return new IPv6(_data.getNextFreeIP(adapted));
  }
  
  
  public List<IPv6> allIPsInSubnet() throws FormatException, ValidationException {
    IPv6SubnetData _data = getIPv6SubnetData();
    if (_data == null) {
      return null;
    }
    List<IPv6> ret = new ArrayList<IPv6>();
    List<IPv6Address> tmp = _data.getAllIPsInSubnet();
    for (IPv6Address ip : tmp) {
      ret.add(new IPv6(ip));
    }
    return ret;
  }
  
}

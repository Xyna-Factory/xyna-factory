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
import com.gip.xyna.XMOM.base.net.internal.IPv4Address;
import com.gip.xyna.XMOM.base.net.internal.IPv4NetmaskData;
import com.gip.xyna.XMOM.base.net.internal.IPv4SubnetData;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xnwh.persistence.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;


@XynaObjectAnnotation(fqXmlName = "base.net.IPv4Subnet")
public class IPv4Subnet extends XynaObject {

  private static final long serialVersionUID = 0L;
  private static final Logger logger = CentralFactoryLogging.getLogger(IPv4Netmask.class);

  private IPv4 _network;
  private IPv4Netmask _mask;

  // fake-variables that can be found by XOUtils.getLabelFor(...) when determining labels for fields in getVariableNames()
  @LabelAnnotation(label="Network")
  private transient IPv6 network;
  @LabelAnnotation(label="Mask")
  private transient IPv6Netmask mask;


  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IPv4> oldVersionsOfnetwork;


  public IPv4 getNetwork() {
    return _network;
  }

  public IPv4 versionedGetNetwork(long _version) {
    if (oldVersionsOfnetwork == null) {
      return _network;
    }
    IPv4 _local = _network;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<IPv4> _ret = oldVersionsOfnetwork.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setNetwork(IPv4 network) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IPv4> _vo = oldVersionsOfnetwork;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfnetwork;
          if (_vo == null) {
            oldVersionsOfnetwork = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IPv4>();
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

  public void unversionedSetNetwork(IPv4 network) {
    this._network = network;
  }

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IPv4Netmask> oldVersionsOfmask;


  public IPv4Netmask getMask() {
    return _mask;
  }

  public IPv4Netmask versionedGetMask(long _version) {
    if (oldVersionsOfmask == null) {
      return _mask;
    }
    IPv4Netmask _local = _mask;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<IPv4Netmask> _ret = oldVersionsOfmask.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setMask(IPv4Netmask mask) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IPv4Netmask> _vo = oldVersionsOfmask;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfmask;
          if (_vo == null) {
            oldVersionsOfmask = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IPv4Netmask>();
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

  public void unversionedSetMask(IPv4Netmask mask) {
    this._mask = mask;
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends IPv4Subnet, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>  {

    protected _GEN_DOM_TYPE instance;

    protected InternalBuilder(IPv4Subnet instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }

    public IPv4Subnet instance() {
      return (IPv4Subnet)instance;
    }

    public _GEN_BUILDER_TYPE mask(IPv4Netmask mask) {
      this.instance.unversionedSetMask(mask);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE network(IPv4 network) {
      this.instance.unversionedSetNetwork(network);
      return (_GEN_BUILDER_TYPE) this;
    }
  }

  public static class Builder extends InternalBuilder<IPv4Subnet, Builder> {
    public Builder() {
      super(new IPv4Subnet());
    }
    public Builder(IPv4Subnet instance) {
      super(instance);
    }
  }

  public Builder buildIPv4Subnet() {
    return new Builder(this);
  }

  public IPv4Subnet() {
    super();
  }

  /**
  * Creates a new instance ignoring inherited member variables. Inherited member variables may
  * have been overwritten.
  */
  public IPv4Subnet(IPv4 network, IPv4Netmask mask) {
    this();
    unversionedSetMask(mask);
    unversionedSetNetwork(network);
  }

  protected void fillVars(IPv4Subnet source, boolean deep) {
    unversionedSetMask(source.getMask());
    unversionedSetNetwork(source.getNetwork());
  }

  public IPv4Subnet clone() {
    return clone(true);
  }

  public IPv4Subnet clone(boolean deep) {
    IPv4Subnet cloned = new IPv4Subnet();
    cloned.fillVars(this, deep);
    return cloned;
  }  

  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      IPv4Subnet xoc = (IPv4Subnet) xo;
      IPv4Subnet xoco = (IPv4Subnet) other.xo;
      if (!xoEqual(xoc.versionedGetNetwork(this.version), xoco.versionedGetNetwork(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      if (!xoEqual(xoc.versionedGetMask(this.version), xoco.versionedGetMask(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      IPv4Subnet xoc = (IPv4Subnet) xo;
      IPv4 network = xoc.versionedGetNetwork(this.version);
      hash = hash * 31 + (network == null ? 0 : network.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
      IPv4Netmask mask = xoc.versionedGetMask(this.version);
      hash = hash * 31 + (mask == null ? 0 : mask.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
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
    XOUtils.addChangesForComplexMember(_network, oldVersionsOfnetwork, start, end, changeSetsOfMembers, datapoints);
    XOUtils.addChangesForComplexMember(_mask, oldVersionsOfmask, start, end, changeSetsOfMembers, datapoints);
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
      XMLHelper.beginType(xml, varName, "IPv4Subnet", "base.net", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendData(xml, "network", versionedGetNetwork(version), version, cache);
      XMLHelper.appendData(xml, "mask", versionedGetMask(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }
  
  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"network", "mask"})));
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
    String[] varNames = new String[]{"network", "mask"};
    Object[] vars = new Object[]{this.getNetwork(), this.getMask()};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("mask".equals(name)) {
      XOUtils.checkCastability(o, IPv4Netmask.class, "mask");
      if(o != null) {
        this.setMask((IPv4Netmask) o);
      }
    } else if ("network".equals(name)) {
      XOUtils.checkCastability(o, IPv4.class, "network");
      if(o != null) {
        this.setNetwork((IPv4) o);
      }
    }
    else {
      throw new XDEV_PARAMETER_NAME_NOT_FOUND(name);
    }
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
      foundField = IPv4Subnet.class.getDeclaredField(target_fieldname);
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
  

  protected IPv4SubnetData getIPv4SubnetData() throws FormatException, ValidationException {
    if (_network == null || _mask == null) {
      return null;
    }
    IPv4Address network = new IPv4Address(_network.getAsLong());
    IPv4NetmaskData mask = new IPv4NetmaskData(_mask.prefixLength());
    return new IPv4SubnetData(network, mask);
  }
  
  
  public IPv4 broadcastAddress() throws FormatException, ValidationException {
    IPv4SubnetData _data = getIPv4SubnetData();
    if (_data == null) {
      return null;
    }
    return new IPv4(_data.getBroadcastAddress());
  }

  
  public IPv4 gatewayAddress() throws FormatException, ValidationException {
    IPv4SubnetData _data = getIPv4SubnetData();
    if (_data == null) {
      return null;
    }
    return new IPv4(_data.getGatewayAddress());
  }
  
  
  public boolean ipWithinSubnet(IPv4 ip) throws FormatException, ValidationException {
    IPv4SubnetData _data = getIPv4SubnetData();
    if (_data == null) {
      return false;
    }
    return _data.ipWithinSubnet(ip.getIPv4AddressData());
  }
  
  
  public IPv4 nextFreeIP(List<IPv4> list) throws FormatException, ValidationException {
    IPv4SubnetData _data = getIPv4SubnetData();
    if (_data == null) {
      return null;
    }
    List<IPv4Address> adapted = new ArrayList<IPv4Address>();
    for (IPv4 ip : list) {
      adapted.add(ip.getIPv4AddressData());
    }
    return new IPv4(_data.getNextFreeIP(adapted));
  }
  
  
  public List<IPv4> allIPsInSubnet() throws FormatException, ValidationException {
    IPv4SubnetData _data = getIPv4SubnetData();
    if (_data == null) {
      return null;
    }
    List<IPv4> ret = new ArrayList<IPv4>();
    List<IPv4Address> tmp = _data.getAllIPsInSubnet();
    for (IPv4Address ip : tmp) {
      ret.add(new IPv4(ip));
    }
    return ret;
  }
  
  public IPv4 toWildcardMask() throws FormatException, ValidationException {
    IPv4SubnetData _data = getIPv4SubnetData();
    if (_data == null) {
      return null;
    }
    return new IPv4(_data.getNetmask().getIPInvertedAsLong());
  }
  
}

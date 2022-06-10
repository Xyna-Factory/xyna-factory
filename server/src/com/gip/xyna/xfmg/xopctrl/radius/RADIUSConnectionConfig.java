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
import com.gip.xyna.XMOM.base.IP;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;


@XynaObjectAnnotation(fqXmlName = "xfmg.xopctrl.radius.RADIUSConnectionConfig")
public class RADIUSConnectionConfig extends XynaObject {

  private static final long serialVersionUID = -2739407417543253221L;
  private static final Logger logger = CentralFactoryLogging.getLogger(RADIUSConnectionConfig.class);


  private IP ip;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IP> oldVersionsOfip;


  public IP getIp() {
    return ip;
  }

  public IP versionedGetIp(long _version) {
    if (oldVersionsOfip == null) {
      return ip;
    }
    IP _local = ip;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<IP> _ret = oldVersionsOfip.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setIp(IP ip) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IP> _vo = oldVersionsOfip;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfip;
          if (_vo == null) {
            oldVersionsOfip = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<IP>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.ip);
        this.ip = ip;
      }
      return;
    }
    this.ip = ip;
  }

  public void unversionedSetIp(IP ip) {
    this.ip = ip;
  }

  private RADIUSServerPort port;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<RADIUSServerPort> oldVersionsOfport;


  public RADIUSServerPort getPort() {
    return port;
  }

  public RADIUSServerPort versionedGetPort(long _version) {
    if (oldVersionsOfport == null) {
      return port;
    }
    RADIUSServerPort _local = port;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<RADIUSServerPort> _ret = oldVersionsOfport.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setPort(RADIUSServerPort port) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<RADIUSServerPort> _vo = oldVersionsOfport;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfport;
          if (_vo == null) {
            oldVersionsOfport = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<RADIUSServerPort>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.port);
        this.port = port;
      }
      return;
    }
    this.port = port;
  }

  public void unversionedSetPort(RADIUSServerPort port) {
    this.port = port;
  }

  private PresharedKey presharedKey;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<PresharedKey> oldVersionsOfpresharedKey;


  public PresharedKey getPresharedKey() {
    return presharedKey;
  }

  public PresharedKey versionedGetPresharedKey(long _version) {
    if (oldVersionsOfpresharedKey == null) {
      return presharedKey;
    }
    PresharedKey _local = presharedKey;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<PresharedKey> _ret = oldVersionsOfpresharedKey.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setPresharedKey(PresharedKey presharedKey) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<PresharedKey> _vo = oldVersionsOfpresharedKey;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfpresharedKey;
          if (_vo == null) {
            oldVersionsOfpresharedKey = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<PresharedKey>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.presharedKey);
        this.presharedKey = presharedKey;
      }
      return;
    }
    this.presharedKey = presharedKey;
  }

  public void unversionedSetPresharedKey(PresharedKey presharedKey) {
    this.presharedKey = presharedKey;
  }

  private Integer maxRetries;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> oldVersionsOfmaxRetries;


  public Integer getMaxRetries() {
    return maxRetries;
  }

  public Integer versionedGetMaxRetries(long _version) {
    if (oldVersionsOfmaxRetries == null) {
      return maxRetries;
    }
    Integer _local = maxRetries;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Integer> _ret = oldVersionsOfmaxRetries.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setMaxRetries(Integer maxRetries) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> _vo = oldVersionsOfmaxRetries;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfmaxRetries;
          if (_vo == null) {
            oldVersionsOfmaxRetries = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.maxRetries);
        this.maxRetries = maxRetries;
      }
      return;
    }
    this.maxRetries = maxRetries;
  }

  public void unversionedSetMaxRetries(Integer maxRetries) {
    this.maxRetries = maxRetries;
  }

  private Integer connectionTimeout;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> oldVersionsOfconnectionTimeout;


  public Integer getConnectionTimeout() {
    return connectionTimeout;
  }

  public Integer versionedGetConnectionTimeout(long _version) {
    if (oldVersionsOfconnectionTimeout == null) {
      return connectionTimeout;
    }
    Integer _local = connectionTimeout;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Integer> _ret = oldVersionsOfconnectionTimeout.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setConnectionTimeout(Integer connectionTimeout) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> _vo = oldVersionsOfconnectionTimeout;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfconnectionTimeout;
          if (_vo == null) {
            oldVersionsOfconnectionTimeout = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.connectionTimeout);
        this.connectionTimeout = connectionTimeout;
      }
      return;
    }
    this.connectionTimeout = connectionTimeout;
  }

  public void unversionedSetConnectionTimeout(Integer connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends RADIUSConnectionConfig, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>  {

    protected _GEN_DOM_TYPE instance;

    protected InternalBuilder(RADIUSConnectionConfig instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }

    public RADIUSConnectionConfig instance() {
      return (RADIUSConnectionConfig) instance;
    }

    public _GEN_BUILDER_TYPE ip(IP ip) {
      this.instance.unversionedSetIp(ip);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE port(RADIUSServerPort port) {
      this.instance.unversionedSetPort(port);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE presharedKey(PresharedKey presharedKey) {
      this.instance.unversionedSetPresharedKey(presharedKey);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE maxRetries(Integer maxRetries) {
      this.instance.unversionedSetMaxRetries(maxRetries);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE connectionTimeout(Integer connectionTimeout) {
      this.instance.unversionedSetConnectionTimeout(connectionTimeout);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<RADIUSConnectionConfig, Builder> {
    public Builder() {
      super(new RADIUSConnectionConfig());
    }
    public Builder(RADIUSConnectionConfig instance) {
      super(instance);
    }
  }

  public Builder buildRADIUSConnectionConfig() {
    return new Builder(this);
  }

  public RADIUSConnectionConfig() {
    super();
  }


  /**
  * Creates a new instance using locally defined member variables.
  */
  public RADIUSConnectionConfig(IP ip, RADIUSServerPort port, PresharedKey presharedKey, Integer maxRetries, Integer connectionTimeout) {
    this();
    this.ip = ip;
    this.port = port;
    this.presharedKey = presharedKey;
    this.maxRetries = maxRetries;
    this.connectionTimeout = connectionTimeout;
  }

  protected void fillVars(RADIUSConnectionConfig source, boolean deep) {
    this.ip = (IP)XynaObject.clone(source.ip, deep);
    this.port = (RADIUSServerPort)XynaObject.clone(source.port, deep);
    this.presharedKey = (PresharedKey)XynaObject.clone(source.presharedKey, deep);
    this.maxRetries = source.maxRetries;
    this.connectionTimeout = source.connectionTimeout;
  }

  public RADIUSConnectionConfig clone() {
    return clone(true);
  }

  public RADIUSConnectionConfig clone(boolean deep) {
    RADIUSConnectionConfig cloned = new RADIUSConnectionConfig();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      RADIUSConnectionConfig xoc = (RADIUSConnectionConfig) xo;
      RADIUSConnectionConfig xoco = (RADIUSConnectionConfig) other.xo;
      if (!xoEqual(xoc.versionedGetIp(this.version), xoco.versionedGetIp(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      if (!xoEqual(xoc.versionedGetPort(this.version), xoco.versionedGetPort(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      if (!xoEqual(xoc.versionedGetPresharedKey(this.version), xoco.versionedGetPresharedKey(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      if (!equal(xoc.versionedGetMaxRetries(this.version), xoco.versionedGetMaxRetries(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetConnectionTimeout(this.version), xoco.versionedGetConnectionTimeout(other.version))) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      RADIUSConnectionConfig xoc = (RADIUSConnectionConfig) xo;
      IP ip = xoc.versionedGetIp(this.version);
      hash = hash * 31 + (ip == null ? 0 : ip.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
      RADIUSServerPort port = xoc.versionedGetPort(this.version);
      hash = hash * 31 + (port == null ? 0 : port.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
      PresharedKey presharedKey = xoc.versionedGetPresharedKey(this.version);
      hash = hash * 31 + (presharedKey == null ? 0 : presharedKey.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
      Integer maxRetries = xoc.versionedGetMaxRetries(this.version);
      hash = hash * 31 + (maxRetries == null ? 0 : maxRetries.hashCode());
      Integer connectionTimeout = xoc.versionedGetConnectionTimeout(this.version);
      hash = hash * 31 + (connectionTimeout == null ? 0 : connectionTimeout.hashCode());
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
    XOUtils.addChangesForComplexMember(ip, oldVersionsOfip, start, end, changeSetsOfMembers, datapoints);
    XOUtils.addChangesForComplexMember(port, oldVersionsOfport, start, end, changeSetsOfMembers, datapoints);
    XOUtils.addChangesForComplexMember(presharedKey, oldVersionsOfpresharedKey, start, end, changeSetsOfMembers, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfmaxRetries, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfconnectionTimeout, start, end, datapoints);
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
      XMLHelper.beginType(xml, varName, "RADIUSConnectionConfig", "xfmg.xopctrl.radius", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendData(xml, "ip", versionedGetIp(version), version, cache);
      XMLHelper.appendData(xml, "port", versionedGetPort(version), version, cache);
      XMLHelper.appendData(xml, "presharedKey", versionedGetPresharedKey(version), version, cache);
      XMLHelper.appendData(xml, "maxRetries", versionedGetMaxRetries(version), version, cache);
      XMLHelper.appendData(xml, "connectionTimeout", versionedGetConnectionTimeout(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"ip", "port", "presharedKey", "maxRetries", "connectionTimeout"})));
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
    String[] varNames = new String[]{"ip", "port", "presharedKey", "maxRetries", "connectionTimeout"};
    Object[] vars = new Object[]{this.ip, this.port, this.presharedKey, this.maxRetries, this.connectionTimeout};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("ip".equals(name)) {
      XOUtils.checkCastability(o, IP.class, "ip");
      setIp((IP) o);
    } else if ("port".equals(name)) {
      XOUtils.checkCastability(o, RADIUSServerPort.class, "port");
      setPort((RADIUSServerPort) o);
    } else if ("presharedKey".equals(name)) {
      XOUtils.checkCastability(o, PresharedKey.class, "presharedKey");
      setPresharedKey((PresharedKey) o);
    } else if ("maxRetries".equals(name)) {
      XOUtils.checkCastability(o, Integer.class, "maxRetries");
      setMaxRetries((Integer) o);
    } else if ("connectionTimeout".equals(name)) {
      XOUtils.checkCastability(o, Integer.class, "connectionTimeout");
      setConnectionTimeout((Integer) o);
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
      foundField = RADIUSConnectionConfig.class.getDeclaredField(target_fieldname);
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


  /*------- not generated -------- */
  
  public RADIUSConnectionConfig(RADIUSServer server, int maxRetries, int connectionTimeout) {
    this(server.getIp(), server.getPort(), server.getPresharedKey(), maxRetries, connectionTimeout);
  }

  
}

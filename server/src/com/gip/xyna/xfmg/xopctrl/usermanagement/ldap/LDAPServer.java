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
package com.gip.xyna.xfmg.xopctrl.usermanagement.ldap;


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

@XynaObjectAnnotation(fqXmlName = "xact.ldap.LDAPServer")
public class LDAPServer extends XynaObject {

  private static final long serialVersionUID = 1;
  private static final Logger logger = CentralFactoryLogging.getLogger(LDAPServer.class);


  private String host;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOfhost;


  public String getHost() {
    return host;
  }

  public String versionedGetHost(long _version) {
    if (oldVersionsOfhost == null) {
      return host;
    }
    String _local = host;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOfhost.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setHost(String host) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOfhost;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfhost;
          if (_vo == null) {
            oldVersionsOfhost = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.host);
        this.host = host;
      }
      return;
    }
    this.host = host;
  }

  public void unversionedSetHost(String host) {
    this.host = host;
  }

  private int port;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> oldVersionsOfport;


  public int getPort() {
    return port;
  }

  public int versionedGetPort(long _version) {
    if (oldVersionsOfport == null) {
      return port;
    }
    int _local = port;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Integer> _ret = oldVersionsOfport.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setPort(int port) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> _vo = oldVersionsOfport;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfport;
          if (_vo == null) {
            oldVersionsOfport = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer>();
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

  public void unversionedSetPort(int port) {
    this.port = port;
  }

  private SSLParameter sSLParameter;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<SSLParameter> oldVersionsOfsSLParameter;


  public SSLParameter getSSLParameter() {
    return sSLParameter;
  }

  public SSLParameter versionedGetSSLParameter(long _version) {
    if (oldVersionsOfsSLParameter == null) {
      return sSLParameter;
    }
    SSLParameter _local = sSLParameter;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<SSLParameter> _ret = oldVersionsOfsSLParameter.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setSSLParameter(SSLParameter sSLParameter) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<SSLParameter> _vo = oldVersionsOfsSLParameter;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfsSLParameter;
          if (_vo == null) {
            oldVersionsOfsSLParameter = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<SSLParameter>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.sSLParameter);
        this.sSLParameter = sSLParameter;
      }
      return;
    }
    this.sSLParameter = sSLParameter;
  }

  public void unversionedSetSSLParameter(SSLParameter sSLParameter) {
    this.sSLParameter = sSLParameter;
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends LDAPServer, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>  {

    protected _GEN_DOM_TYPE instance;

    protected InternalBuilder(LDAPServer instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }

    public LDAPServer instance() {
      return (LDAPServer) instance;
    }

    public _GEN_BUILDER_TYPE host(String host) {
      this.instance.unversionedSetHost(host);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE port(int port) {
      this.instance.unversionedSetPort(port);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE sSLParameter(SSLParameter sSLParameter) {
      this.instance.unversionedSetSSLParameter(sSLParameter);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<LDAPServer, Builder> {
    public Builder() {
      super(new LDAPServer());
    }
    public Builder(LDAPServer instance) {
      super(instance);
    }
  }

  public Builder buildLDAPServer() {
    return new Builder(this);
  }

  public LDAPServer() {
    super();
  }

  /**
  * Creates a new instance using locally defined member variables.
  */
  public LDAPServer(String host, int port, SSLParameter sSLParameter) {
    this();
    this.host = host;
    this.port = port;
    this.sSLParameter = sSLParameter;
  }

  protected void fillVars(LDAPServer source, boolean deep) {
    this.host = source.host;
    this.port = source.port;
    this.sSLParameter = (SSLParameter)XynaObject.clone(source.sSLParameter, deep);
  }

  public LDAPServer clone() {
    return clone(true);
  }

  public LDAPServer clone(boolean deep) {
    LDAPServer cloned = new LDAPServer();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      LDAPServer xoc = (LDAPServer) xo;
      LDAPServer xoco = (LDAPServer) other.xo;
      if (!equal(xoc.versionedGetHost(this.version), xoco.versionedGetHost(other.version))) {
        return false;
      }
      if (xoc.versionedGetPort(this.version) != xoco.versionedGetPort(other.version)) {
        return false;
      }
      if (!xoEqual(xoc.versionedGetSSLParameter(this.version), xoco.versionedGetSSLParameter(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      LDAPServer xoc = (LDAPServer) xo;
      String host = xoc.versionedGetHost(this.version);
      hash = hash * 31 + (host == null ? 0 : host.hashCode());
      int port = xoc.versionedGetPort(this.version);
      hash = hash * 31 + Integer.valueOf(port).hashCode();
      SSLParameter sSLParameter = xoc.versionedGetSSLParameter(this.version);
      hash = hash * 31 + (sSLParameter == null ? 0 : sSLParameter.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
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
    XOUtils.addChangesForSimpleMember(oldVersionsOfhost, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfport, start, end, datapoints);
    XOUtils.addChangesForComplexMember(sSLParameter, oldVersionsOfsSLParameter, start, end, changeSetsOfMembers, datapoints);
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
      XMLHelper.beginType(xml, varName, "LDAPServer", "xact.ldap", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendData(xml, "host", versionedGetHost(version), version, cache);
      XMLHelper.appendData(xml, "port", versionedGetPort(version), version, cache);
      XMLHelper.appendData(xml, "sSLParameter", versionedGetSSLParameter(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"host", "port", "sSLParameter"})));
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
    String[] varNames = new String[]{"host", "port", "sSLParameter"};
    Object[] vars = new Object[]{this.host, this.port, this.sSLParameter};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("host".equals(name)) {
      XOUtils.checkCastability(o, String.class, "host");
      setHost((String) o);
    } else if ("port".equals(name)) {
      XOUtils.checkCastability(o, Integer.class, "port");
      if (o != null) {
        setPort((Integer) o);
      }
    } else if ("sSLParameter".equals(name)) {
      XOUtils.checkCastability(o, SSLParameter.class, "sSLParameter");
      setSSLParameter((SSLParameter) o);
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
      foundField = LDAPServer.class.getDeclaredField(target_fieldname);
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

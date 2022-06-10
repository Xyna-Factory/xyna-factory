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
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;

@XynaObjectAnnotation(fqXmlName = "xact.ldap.SSLKeystoreParameter")
public class SSLKeystoreParameter extends SSLParameter {

  private static final long serialVersionUID = 1;
  private static final Logger logger = CentralFactoryLogging.getLogger(SSLKeystoreParameter.class);


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

  private String type;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOftype;


  public String getType() {
    return type;
  }

  public String versionedGetType(long _version) {
    if (oldVersionsOftype == null) {
      return type;
    }
    String _local = type;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOftype.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setType(String type) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOftype;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOftype;
          if (_vo == null) {
            oldVersionsOftype = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.type);
        this.type = type;
      }
      return;
    }
    this.type = type;
  }

  public void unversionedSetType(String type) {
    this.type = type;
  }

  private String passphrase;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOfpassphrase;


  public String getPassphrase() {
    return passphrase;
  }

  public String versionedGetPassphrase(long _version) {
    if (oldVersionsOfpassphrase == null) {
      return passphrase;
    }
    String _local = passphrase;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOfpassphrase.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setPassphrase(String passphrase) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOfpassphrase;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfpassphrase;
          if (_vo == null) {
            oldVersionsOfpassphrase = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.passphrase);
        this.passphrase = passphrase;
      }
      return;
    }
    this.passphrase = passphrase;
  }

  public void unversionedSetPassphrase(String passphrase) {
    this.passphrase = passphrase;
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends SSLKeystoreParameter, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>> extends SSLParameter.InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>  {

    protected InternalBuilder(SSLKeystoreParameter instance) {
      super(instance);
    }

    public SSLKeystoreParameter instance() {
      return (SSLKeystoreParameter) instance;
    }

    public _GEN_BUILDER_TYPE path(String path) {
      this.instance.unversionedSetPath(path);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE type(String type) {
      this.instance.unversionedSetType(type);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE passphrase(String passphrase) {
      this.instance.unversionedSetPassphrase(passphrase);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<SSLKeystoreParameter, Builder> {
    public Builder() {
      super(new SSLKeystoreParameter());
    }
    public Builder(SSLKeystoreParameter instance) {
      super(instance);
    }
  }

  public Builder buildSSLKeystoreParameter() {
    return new Builder(this);
  }

  public SSLKeystoreParameter() {
    super();
  }

  /**
  * Creates a new instance using locally defined member variables.
  */
  public SSLKeystoreParameter(String path, String type, String passphrase) {
    this();
    this.path = path;
    this.type = type;
    this.passphrase = passphrase;
  }

  protected void fillVars(SSLKeystoreParameter source, boolean deep) {
    super.fillVars(source, deep);
    this.path = source.path;
    this.type = source.type;
    this.passphrase = source.passphrase;
  }

  public SSLKeystoreParameter clone() {
    return clone(true);
  }

  public SSLKeystoreParameter clone(boolean deep) {
    SSLKeystoreParameter cloned = new SSLKeystoreParameter();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.SSLParameter.ObjectVersion {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      if (!super.memberEquals(o)) {
        return false;
      }
      ObjectVersion other = (ObjectVersion) o;
      SSLKeystoreParameter xoc = (SSLKeystoreParameter) xo;
      SSLKeystoreParameter xoco = (SSLKeystoreParameter) other.xo;
      if (!equal(xoc.versionedGetPath(this.version), xoco.versionedGetPath(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetType(this.version), xoco.versionedGetType(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetPassphrase(this.version), xoco.versionedGetPassphrase(other.version))) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = super.calcHashOfMembers(stack);
      SSLKeystoreParameter xoc = (SSLKeystoreParameter) xo;
      String path = xoc.versionedGetPath(this.version);
      hash = hash * 31 + (path == null ? 0 : path.hashCode());
      String type = xoc.versionedGetType(this.version);
      hash = hash * 31 + (type == null ? 0 : type.hashCode());
      String passphrase = xoc.versionedGetPassphrase(this.version);
      hash = hash * 31 + (passphrase == null ? 0 : passphrase.hashCode());
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
    super.collectChanges(start, end, changeSetsOfMembers, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfpath, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOftype, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfpassphrase, start, end, datapoints);
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
      XMLHelper.beginType(xml, varName, "SSLKeystoreParameter", "xact.ldap", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      xml.append(super.toXml(varName, true, version, cache));
      XMLHelper.appendData(xml, "path", versionedGetPath(version), version, cache);
      XMLHelper.appendData(xml, "type", versionedGetType(version), version, cache);
      XMLHelper.appendData(xml, "passphrase", versionedGetPassphrase(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"path", "type", "passphrase"})));
  /**
  * @deprecated use {@link #getVariableNames()} instead
  */
  @Deprecated
  public HashSet<String> getVarNames() {
    HashSet<String> ret = new HashSet<String>(varNames);
    ret.addAll(super.getVarNames());
    return ret;
  }


  public Set<String> getVariableNames() {
    Set<String> ret = new HashSet<String>(varNames);
    ret.addAll(super.getVariableNames());
    return Collections.unmodifiableSet(ret);
  }

  /**
   * gets membervariable by name or path. e.g. get("myVar.myChild") gets
   * the child variable of the membervariable named "myVar" and is equivalent
   * to getMyVar().getMyChild()
   * @param name variable name or path separated by ".".
   */
  public Object get(String name) throws InvalidObjectPathException {
    String[] varNames = new String[]{"path", "type", "passphrase"};
    Object[] vars = new Object[]{this.path, this.type, this.passphrase};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      o = super.get(name);
      if (o == XOUtils.VARNAME_NOTFOUND) {
        throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
      }
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("path".equals(name)) {
      XOUtils.checkCastability(o, String.class, "path");
      setPath((String) o);
    } else if ("type".equals(name)) {
      XOUtils.checkCastability(o, String.class, "type");
      setType((String) o);
    } else if ("passphrase".equals(name)) {
      XOUtils.checkCastability(o, String.class, "passphrase");
      setPassphrase((String) o);
    } else {
      super.set(name, o);
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
      foundField = SSLParameter.class.getDeclaredField(target_fieldname);
    } catch (NoSuchFieldException e) {
    }
    if (foundField == null) {
      return SSLParameter.getField(target_fieldname);
    } else {
      foundField.setAccessible(true);
      fieldMap.put(target_fieldname, foundField);
      return foundField;
    }
  }

}

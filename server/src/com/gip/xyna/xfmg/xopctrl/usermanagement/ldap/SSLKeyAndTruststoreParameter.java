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
import com.gip.xyna.xprc.xfractwfe.generation.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;

@XynaObjectAnnotation(fqXmlName = "xact.ldap.SSLKeyAndTruststoreParameter")
public class SSLKeyAndTruststoreParameter extends SSLParameter {

  private static final long serialVersionUID = 34812612580074L;
  private static final Logger logger = CentralFactoryLogging.getLogger(SSLKeyAndTruststoreParameter.class);


  @LabelAnnotation(label="SSL Keystore")
  private SSLKeystoreParameter sSLKeystore;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<SSLKeystoreParameter> oldVersionsOfsSLKeystore;


  public SSLKeystoreParameter getSSLKeystore() {
    return sSLKeystore;
  }

  public SSLKeystoreParameter versionedGetSSLKeystore(long _version) {
    if (oldVersionsOfsSLKeystore == null) {
      return sSLKeystore;
    }
    SSLKeystoreParameter _local = sSLKeystore;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<SSLKeystoreParameter> _ret = oldVersionsOfsSLKeystore.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setSSLKeystore(SSLKeystoreParameter sSLKeystore) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<SSLKeystoreParameter> _vo = oldVersionsOfsSLKeystore;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfsSLKeystore;
          if (_vo == null) {
            oldVersionsOfsSLKeystore = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<SSLKeystoreParameter>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.sSLKeystore);
        this.sSLKeystore = sSLKeystore;
      }
      return;
    }
    this.sSLKeystore = sSLKeystore;
  }

  public void unversionedSetSSLKeystore(SSLKeystoreParameter sSLKeystore) {
    this.sSLKeystore = sSLKeystore;
  }

  @LabelAnnotation(label="SSL Truststore")
  private SSLKeystoreParameter sSLTruststore;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<SSLKeystoreParameter> oldVersionsOfsSLTruststore;


  public SSLKeystoreParameter getSSLTruststore() {
    return sSLTruststore;
  }

  public SSLKeystoreParameter versionedGetSSLTruststore(long _version) {
    if (oldVersionsOfsSLTruststore == null) {
      return sSLTruststore;
    }
    SSLKeystoreParameter _local = sSLTruststore;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<SSLKeystoreParameter> _ret = oldVersionsOfsSLTruststore.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setSSLTruststore(SSLKeystoreParameter sSLTruststore) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<SSLKeystoreParameter> _vo = oldVersionsOfsSLTruststore;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfsSLTruststore;
          if (_vo == null) {
            oldVersionsOfsSLTruststore = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<SSLKeystoreParameter>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.sSLTruststore);
        this.sSLTruststore = sSLTruststore;
      }
      return;
    }
    this.sSLTruststore = sSLTruststore;
  }

  public void unversionedSetSSLTruststore(SSLKeystoreParameter sSLTruststore) {
    this.sSLTruststore = sSLTruststore;
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends SSLKeyAndTruststoreParameter, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>> extends SSLParameter.InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>  {

    protected InternalBuilder(SSLKeyAndTruststoreParameter instance) {
      super(instance);
    }

    public SSLKeyAndTruststoreParameter instance() {
      return (SSLKeyAndTruststoreParameter) instance;
    }

    public _GEN_BUILDER_TYPE sSLKeystore(SSLKeystoreParameter sSLKeystore) {
      this.instance.unversionedSetSSLKeystore(sSLKeystore);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE sSLTruststore(SSLKeystoreParameter sSLTruststore) {
      this.instance.unversionedSetSSLTruststore(sSLTruststore);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<SSLKeyAndTruststoreParameter, Builder> {
    public Builder() {
      super(new SSLKeyAndTruststoreParameter());
    }
    public Builder(SSLKeyAndTruststoreParameter instance) {
      super(instance);
    }
  }

  public Builder buildSSLKeyAndTruststoreParameter() {
    return new Builder(this);
  }

  public SSLKeyAndTruststoreParameter() {
    super();
  }

  /**
  * Creates a new instance using locally defined member variables.
  */
  public SSLKeyAndTruststoreParameter(SSLKeystoreParameter sSLKeystore, SSLKeystoreParameter sSLTruststore) {
    this();
    this.sSLKeystore = sSLKeystore;
    this.sSLTruststore = sSLTruststore;
  }

  protected void fillVars(SSLKeyAndTruststoreParameter source, boolean deep) {
    super.fillVars(source, deep);
    this.sSLKeystore = (SSLKeystoreParameter)XynaObject.clone(source.sSLKeystore, deep);
    this.sSLTruststore = (SSLKeystoreParameter)XynaObject.clone(source.sSLTruststore, deep);
  }

  public SSLKeyAndTruststoreParameter clone() {
    return clone(true);
  }

  public SSLKeyAndTruststoreParameter clone(boolean deep) {
    SSLKeyAndTruststoreParameter cloned = new SSLKeyAndTruststoreParameter();
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
      SSLKeyAndTruststoreParameter xoc = (SSLKeyAndTruststoreParameter) xo;
      SSLKeyAndTruststoreParameter xoco = (SSLKeyAndTruststoreParameter) other.xo;
      if (!xoEqual(xoc.versionedGetSSLKeystore(this.version), xoco.versionedGetSSLKeystore(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      if (!xoEqual(xoc.versionedGetSSLTruststore(this.version), xoco.versionedGetSSLTruststore(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = super.calcHashOfMembers(stack);
      SSLKeyAndTruststoreParameter xoc = (SSLKeyAndTruststoreParameter) xo;
      SSLKeystoreParameter sSLKeystore = xoc.versionedGetSSLKeystore(this.version);
      hash = hash * 31 + (sSLKeystore == null ? 0 : sSLKeystore.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
      SSLKeystoreParameter sSLTruststore = xoc.versionedGetSSLTruststore(this.version);
      hash = hash * 31 + (sSLTruststore == null ? 0 : sSLTruststore.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
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
    XOUtils.addChangesForComplexMember(sSLKeystore, oldVersionsOfsSLKeystore, start, end, changeSetsOfMembers, datapoints);
    XOUtils.addChangesForComplexMember(sSLTruststore, oldVersionsOfsSLTruststore, start, end, changeSetsOfMembers, datapoints);
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
      XMLHelper.beginType(xml, varName, "SSLKeyAndTruststoreParameter", "xact.ldap", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      xml.append(super.toXml(varName, true, version, cache));
      XMLHelper.appendData(xml, "sSLKeystore", versionedGetSSLKeystore(version), version, cache);
      XMLHelper.appendData(xml, "sSLTruststore", versionedGetSSLTruststore(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"sSLKeystore", "sSLTruststore"})));
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
    String[] varNames = new String[]{"sSLKeystore", "sSLTruststore"};
    Object[] vars = new Object[]{this.sSLKeystore, this.sSLTruststore};
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
    if ("sSLKeystore".equals(name)) {
      XOUtils.checkCastability(o, SSLKeystoreParameter.class, "sSLKeystore");
      setSSLKeystore((SSLKeystoreParameter) o);
    } else if ("sSLTruststore".equals(name)) {
      XOUtils.checkCastability(o, SSLKeystoreParameter.class, "sSLTruststore");
      setSSLTruststore((SSLKeystoreParameter) o);
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
      foundField = SSLKeyAndTruststoreParameter.class.getDeclaredField(target_fieldname);
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

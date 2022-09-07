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
package com.gip.xyna.xfmg.xopctrl.usermanagement;

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
import com.gip.xyna.xnwh.persistence.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;

@XynaObjectAnnotation(fqXmlName = "xfmg.xopctrl.AuthenticationResult")
public class AuthenticationResult extends XynaObject {
  
  private static final long serialVersionUID = 6209625865071726385L;
  private static final Logger logger = CentralFactoryLogging.getLogger(AuthenticationResult.class);

  @LabelAnnotation(label="Success")
  private Boolean success;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Boolean> oldVersionsOfsuccess;


  public Boolean getSuccess() {
    return success;
  }

  public Boolean versionedGetSuccess(long _version) {
    if (oldVersionsOfsuccess == null) {
      return success;
    }
    Boolean _local = success;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Boolean> _ret = oldVersionsOfsuccess.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setSuccess(Boolean success) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Boolean> _vo = oldVersionsOfsuccess;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfsuccess;
          if (_vo == null) {
            oldVersionsOfsuccess = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Boolean>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.success);
        this.success = success;
      }
      return;
    }
    this.success = success;
  }

  public void unversionedSetSuccess(Boolean success) {
    this.success = success;
  }

  @LabelAnnotation(label="Role")
  private String role;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOfrole;


  public String getRole() {
    return role;
  }

  public String versionedGetRole(long _version) {
    if (oldVersionsOfrole == null) {
      return role;
    }
    String _local = role;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOfrole.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setRole(String role) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOfrole;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfrole;
          if (_vo == null) {
            oldVersionsOfrole = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.role);
        this.role = role;
      }
      return;
    }
    this.role = role;
  }

  public void unversionedSetRole(String role) {
    this.role = role;
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends AuthenticationResult, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>  {

    protected _GEN_DOM_TYPE instance;

    protected InternalBuilder(AuthenticationResult instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }

    public AuthenticationResult instance() {
      return (AuthenticationResult) instance;
    }

    public _GEN_BUILDER_TYPE success(Boolean success) {
      this.instance.unversionedSetSuccess(success);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE role(String role) {
      this.instance.unversionedSetRole(role);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<AuthenticationResult, Builder> {
    public Builder() {
      super(new AuthenticationResult());
    }
    public Builder(AuthenticationResult instance) {
      super(instance);
    }
  }

  public Builder buildAuthenticationResult() {
    return new Builder(this);
  }

  public AuthenticationResult() {
    super();
  }

  /**
  * Creates a new instance using locally defined member variables.
  */
  public AuthenticationResult(Boolean success, String role) {
    this();
    this.success = success;
    this.role = role;
  }

  protected void fillVars(AuthenticationResult source, boolean deep) {
    this.success = source.success;
    this.role = source.role;
  }

  public AuthenticationResult clone() {
    return clone(true);
  }

  public AuthenticationResult clone(boolean deep) {
    AuthenticationResult cloned = new AuthenticationResult();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      AuthenticationResult xoc = (AuthenticationResult) xo;
      AuthenticationResult xoco = (AuthenticationResult) other.xo;
      if (!equal(xoc.versionedGetSuccess(this.version), xoco.versionedGetSuccess(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetRole(this.version), xoco.versionedGetRole(other.version))) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      AuthenticationResult xoc = (AuthenticationResult) xo;
      Boolean success = xoc.versionedGetSuccess(this.version);
      hash = hash * 31 + (success == null ? 0 : success.hashCode());
      String role = xoc.versionedGetRole(this.version);
      hash = hash * 31 + (role == null ? 0 : role.hashCode());
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
    XOUtils.addChangesForSimpleMember(oldVersionsOfsuccess, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfrole, start, end, datapoints);
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
      XMLHelper.beginType(xml, varName, "AuthenticationResult", "xfmg.xopctrl", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendData(xml, "success", versionedGetSuccess(version), version, cache);
      XMLHelper.appendData(xml, "role", versionedGetRole(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"success", "role"})));
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
    String[] varNames = new String[]{"success", "role"};
    Object[] vars = new Object[]{this.success, this.role};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("success".equals(name)) {
      XOUtils.checkCastability(o, Boolean.class, "success");
      setSuccess((Boolean) o);
    } else if ("role".equals(name)) {
      XOUtils.checkCastability(o, String.class, "role");
      setRole((String) o);
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
      foundField = AuthenticationResult.class.getDeclaredField(target_fieldname);
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
  
  public boolean wasSuccesfull() {
    return success;
  }

}

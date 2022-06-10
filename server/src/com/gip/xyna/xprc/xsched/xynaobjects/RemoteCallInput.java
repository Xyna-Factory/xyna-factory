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
package com.gip.xyna.xprc.xsched.xynaobjects;

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
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import com.gip.xyna.xprc.xsched.SchedulerBean;


@XynaObjectAnnotation(fqXmlName = "xprc.xpce.RemoteCallInput")
public class RemoteCallInput extends XynaObject {

  private static final long serialVersionUID = -19278118407554L;
  private static final Logger logger = CentralFactoryLogging.getLogger(RemoteCallInput.class);


  private String orderType;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOforderType;

  private com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> lazyInit_oldVersionsOforderType() {
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOforderType;
    if (_vo == null) {
      synchronized (this) {
        _vo = oldVersionsOforderType;
        if (_vo == null) {
          oldVersionsOforderType = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
        }
      }
    }
    return _vo;
  }


  public String getOrderType() {
    return orderType;
  }

  public String versionedGetOrderType(long _version) {
    if (oldVersionsOforderType == null) {
      return orderType;
    }
    String _local = orderType;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOforderType.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setOrderType(String orderType) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = lazyInit_oldVersionsOforderType();
      synchronized (_vo) {
        _vo.add(this.orderType);
        this.orderType = orderType;
      }
      return;
    }
    this.orderType = orderType;
  }

  public void unversionedSetOrderType(String orderType) {
    this.orderType = orderType;
  }

  private String remoteDestinationInstance;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOfremoteDestinationInstance;

  private com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> lazyInit_oldVersionsOfremoteDestinationInstance() {
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOfremoteDestinationInstance;
    if (_vo == null) {
      synchronized (this) {
        _vo = oldVersionsOfremoteDestinationInstance;
        if (_vo == null) {
          oldVersionsOfremoteDestinationInstance = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
        }
      }
    }
    return _vo;
  }


  public String getRemoteDestinationInstance() {
    return remoteDestinationInstance;
  }

  public String versionedGetRemoteDestinationInstance(long _version) {
    if (oldVersionsOfremoteDestinationInstance == null) {
      return remoteDestinationInstance;
    }
    String _local = remoteDestinationInstance;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOfremoteDestinationInstance.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setRemoteDestinationInstance(String remoteDestinationInstance) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = lazyInit_oldVersionsOfremoteDestinationInstance();
      synchronized (_vo) {
        _vo.add(this.remoteDestinationInstance);
        this.remoteDestinationInstance = remoteDestinationInstance;
      }
      return;
    }
    this.remoteDestinationInstance = remoteDestinationInstance;
  }

  public void unversionedSetRemoteDestinationInstance(String remoteDestinationInstance) {
    this.remoteDestinationInstance = remoteDestinationInstance;
  }
  
  public boolean supportsObjectVersioning() {
    if (!com.gip.xyna.XynaFactory.isFactoryServer()) {
      return false;
    }
    if (com.gip.xyna.xfmg.xods.configuration.XynaProperty.useVersioningConfig.get() == 4) {
      return true;
    } else {
      return false;
    }
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends RemoteCallInput, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>  {

    protected _GEN_DOM_TYPE instance;

    protected InternalBuilder(RemoteCallInput instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }

    public RemoteCallInput instance() {
      return (RemoteCallInput) instance;
    }

    public _GEN_BUILDER_TYPE orderType(String orderType) {
      this.instance.unversionedSetOrderType(orderType);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE remoteDestinationInstance(String remoteDestinationInstance) {
      this.instance.unversionedSetRemoteDestinationInstance(remoteDestinationInstance);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<RemoteCallInput, Builder> {
    public Builder() {
      super(new RemoteCallInput());
    }
    public Builder(RemoteCallInput instance) {
      super(instance);
    }
  }

  public Builder buildRemoteCallInput() {
    return new Builder(this);
  }

  public RemoteCallInput() {
    super();
  }

  /**
  * Creates a new instance using locally defined member variables.
  */
  public RemoteCallInput(String orderType, String remoteDestinationInstance) {
    this();
    this.orderType = orderType;
    this.remoteDestinationInstance = remoteDestinationInstance;
  }

  protected void fillVars(RemoteCallInput source, boolean deep) {
    this.orderType = source.orderType;
    this.remoteDestinationInstance = source.remoteDestinationInstance;
  }

  public RemoteCallInput clone() {
    return clone(true);
  }

  public RemoteCallInput clone(boolean deep) {
    RemoteCallInput cloned = new RemoteCallInput();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      RemoteCallInput xoc = (RemoteCallInput) xo;
      RemoteCallInput xoco = (RemoteCallInput) other.xo;
      if (!equal(xoc.versionedGetOrderType(this.version), xoco.versionedGetOrderType(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetRemoteDestinationInstance(this.version), xoco.versionedGetRemoteDestinationInstance(other.version))) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      RemoteCallInput xoc = (RemoteCallInput) xo;
      String orderType = xoc.versionedGetOrderType(this.version);
      hash = hash * 31 + (orderType == null ? 0 : orderType.hashCode());
      String remoteDestinationInstance = xoc.versionedGetRemoteDestinationInstance(this.version);
      hash = hash * 31 + (remoteDestinationInstance == null ? 0 : remoteDestinationInstance.hashCode());
      return hash;
    }

  }


  public ObjectVersion createObjectVersion(long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
    return new ObjectVersion(this, version, changeSetsOfMembers);
  }


  public void collectChanges(long start, long end, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers, java.util.Set<Long> datapoints) {
    XOUtils.addChangesForSimpleMember(oldVersionsOforderType, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfremoteDestinationInstance, start, end, datapoints);
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
      XMLHelper.beginType(xml, varName, "RemoteCallInput", "xprc.xpce", objectId, refId, RevisionManagement.getRevisionByClass(RemoteCallInput.class), cache);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendData(xml, "orderType", versionedGetOrderType(version), version, cache);
      XMLHelper.appendData(xml, "remoteDestinationInstance", versionedGetRemoteDestinationInstance(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"orderType", "remoteDestinationInstance"})));
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
    String[] varNames = new String[]{"orderType", "remoteDestinationInstance"};
    Object[] vars = new Object[]{this.orderType, this.remoteDestinationInstance};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("orderType".equals(name)) {
      XOUtils.checkCastability(o, String.class, "orderType");
      setOrderType((String) o);
    } else if ("remoteDestinationInstance".equals(name)) {
      XOUtils.checkCastability(o, String.class, "remoteDestinationInstance");
      setRemoteDestinationInstance((String) o);
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
      foundField = RemoteCallInput.class.getDeclaredField(target_fieldname);
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
  
  //=============================================== Eigener Code =======================================
  
  private RuntimeContext runtimeContext;
  
  public RemoteCallInput(String orderType, RuntimeContext runtimeContext, String remoteDestinationInstance) {
    this(orderType, remoteDestinationInstance);
    this.runtimeContext = runtimeContext;
  }
  
  public RuntimeContext getRuntimeContext() {
    return runtimeContext;
  }

}

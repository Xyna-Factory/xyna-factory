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
import com.gip.xyna.xnwh.persistence.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import com.gip.xyna.xprc.xsched.SchedulerBean;

@XynaObjectAnnotation(fqXmlName = "xprc.xsched.Priority")
public class Priority extends XynaObject {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = CentralFactoryLogging.getLogger(Priority.class);


  @LabelAnnotation(label="Priority")
  private Integer priority;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> oldVersionsOfpriority;


  public Integer getPriority() {
    return priority;
  }

  public Integer versionedGetPriority(long _version) {
    if (oldVersionsOfpriority == null) {
      return priority;
    }
    Integer _local = priority;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Integer> _ret = oldVersionsOfpriority.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setPriority(Integer priority) {
    if (useVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> _vo = oldVersionsOfpriority;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfpriority;
          if (_vo == null) {
            oldVersionsOfpriority = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.priority);
        this.priority = priority;
      }
      return;
    }
    this.priority = priority;
  }

  public void unversionedSetPriority(Integer priority) {
    this.priority = priority;
  }

  private boolean useVersioning() {
    return true;
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends Priority, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>  {

    protected _GEN_DOM_TYPE instance;

    protected InternalBuilder(Priority instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }

    public Priority instance() {
      return (Priority) instance;
    }

    public _GEN_BUILDER_TYPE priority(Integer priority) {
      this.instance.unversionedSetPriority(priority);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<Priority, Builder> {
    public Builder() {
      super(new Priority());
    }
    public Builder(Priority instance) {
      super(instance);
    }
  }

  public Builder buildPriority() {
    return new Builder(this);
  }

  public Priority() {
    super();
  }

  /**
  * Creates a new instance using locally defined member variables.
  */
  public Priority(Integer priority) {
    this();
    this.priority = priority;
  }

  protected void fillVars(Priority source, boolean deep) {
    this.priority = source.priority;
  }

  public Priority clone() {
    return clone(true);
  }

  public Priority clone(boolean deep) {
    Priority cloned = new Priority();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      Priority xoc = (Priority) xo;
      Priority xoco = (Priority) other.xo;
      if (!equal(xoc.versionedGetPriority(this.version), xoco.versionedGetPriority(other.version))) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      Priority xoc = (Priority) xo;
      Integer priority = xoc.versionedGetPriority(this.version);
      hash = hash * 31 + (priority == null ? 0 : priority.hashCode());
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
    XOUtils.addChangesForSimpleMember(oldVersionsOfpriority, start, end, datapoints);
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
      XMLHelper.beginType(xml, varName, "Priority", "xprc.xsched", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendData(xml, "priority", versionedGetPriority(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"priority"})));
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
    String[] varNames = new String[]{"priority"};
    Object[] vars = new Object[]{this.priority};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("priority".equals(name)) {
      XOUtils.checkCastability(o, Integer.class, "priority");
      setPriority((Integer) o);
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
      foundField = Priority.class.getDeclaredField(target_fieldname);
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

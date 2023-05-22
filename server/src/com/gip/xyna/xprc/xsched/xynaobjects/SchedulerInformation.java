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
package com.gip.xyna.xprc.xsched.xynaobjects;


import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import com.gip.xyna.xprc.xsched.SchedulerBean;


@XynaObjectAnnotation(fqXmlName = "xprc.xsched.SchedulerInformation")
public class SchedulerInformation extends SchedulerBean {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = CentralFactoryLogging.getLogger(SchedulerInformation.class);


  @LabelAnnotation(label="Time Constraint")
  private TimeConstraint timeConstraint;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<TimeConstraint> oldVersionsOftimeConstraint;


  public TimeConstraint getTimeConstraint() {
    return timeConstraint;
  }

  public TimeConstraint versionedGetTimeConstraint(long _version) {
    if (oldVersionsOftimeConstraint == null) {
      return timeConstraint;
    }
    TimeConstraint _local = timeConstraint;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<TimeConstraint> _ret = oldVersionsOftimeConstraint.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setTimeConstraint(TimeConstraint timeConstraint) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<TimeConstraint> _vo = oldVersionsOftimeConstraint;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOftimeConstraint;
          if (_vo == null) {
            oldVersionsOftimeConstraint = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<TimeConstraint>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.timeConstraint);
        this.timeConstraint = timeConstraint;
      }
      return;
    }
    this.timeConstraint = timeConstraint;
  }

  public void unversionedSetTimeConstraint(TimeConstraint timeConstraint) {
    this.timeConstraint = timeConstraint;
  }

  @LabelAnnotation(label="Priority")
  private Priority priority;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Priority> oldVersionsOfpriority;


  public Priority getPriority() {
    return priority;
  }

  public Priority versionedGetPriority(long _version) {
    if (oldVersionsOfpriority == null) {
      return priority;
    }
    Priority _local = priority;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Priority> _ret = oldVersionsOfpriority.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setPriority(Priority priority) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Priority> _vo = oldVersionsOfpriority;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfpriority;
          if (_vo == null) {
            oldVersionsOfpriority = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Priority>();
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

  public void unversionedSetPriority(Priority priority) {
    this.priority = priority;
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends SchedulerInformation, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>> extends SchedulerBean.InternalBuilder<SchedulerBean, InternalBuilder<_GEN_DOM_TYPE,_GEN_BUILDER_TYPE>> {


    protected InternalBuilder(SchedulerInformation instance) {
      super(instance);
    }

    public SchedulerInformation instance() {
      return (SchedulerInformation) instance;
    }

    public _GEN_BUILDER_TYPE timeConstraint(TimeConstraint timeConstraint) {
      this.instance().unversionedSetTimeConstraint(timeConstraint);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE priority(Priority priority) {
      this.instance().unversionedSetPriority(priority);
      return (_GEN_BUILDER_TYPE) this;
    }
    
    public _GEN_BUILDER_TYPE capacities(List<com.gip.xyna.xprc.xpce.planning.Capacity> capacities) {
      return (_GEN_BUILDER_TYPE) super.capacities(capacities);
    }
    
    @Override
    public _GEN_BUILDER_TYPE vetos(List<com.gip.xyna.xprc.xpce.planning.Veto> vetos) {
      return (_GEN_BUILDER_TYPE) super.vetos(vetos);
    }

  }

  public static class Builder extends InternalBuilder<SchedulerInformation, Builder> {
    public Builder() {
      super(new SchedulerInformation());
    }
    public Builder(SchedulerInformation instance) {
      super(instance);
    }
  }

  public Builder buildSchedulerInformation() {
    return new Builder(this);
  }

  public SchedulerInformation() {
    super();
  }


  protected void fillVars(SchedulerInformation source, boolean deep) {
    super.fillVars(source, deep);
    this.timeConstraint = (TimeConstraint)XynaObject.clone(source.timeConstraint, deep);
    this.priority = (Priority)XynaObject.clone(source.priority, deep);
  }

  public SchedulerInformation clone() {
    return clone(true);
  }

  public SchedulerInformation clone(boolean deep) {
    SchedulerInformation cloned = new SchedulerInformation();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends SchedulerBean.ObjectVersion {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      if (!super.memberEquals(o)) {
        return false;
      }
      ObjectVersion other = (ObjectVersion) o;
      SchedulerInformation xoc = (SchedulerInformation) xo;
      SchedulerInformation xoco = (SchedulerInformation) other.xo;
      if (!xoEqual(xoc.versionedGetTimeConstraint(this.version), xoco.versionedGetTimeConstraint(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      if (!xoEqual(xoc.versionedGetPriority(this.version), xoco.versionedGetPriority(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = super.calcHashOfMembers(stack);
      SchedulerInformation xoc = (SchedulerInformation) xo;
      TimeConstraint timeConstraint = xoc.versionedGetTimeConstraint(this.version);
      hash = hash * 31 + (timeConstraint == null ? 0 : timeConstraint.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
      Priority priority = xoc.versionedGetPriority(this.version);
      hash = hash * 31 + (priority == null ? 0 : priority.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
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
    XOUtils.addChangesForComplexMember(timeConstraint, oldVersionsOftimeConstraint, start, end, changeSetsOfMembers, datapoints);
    XOUtils.addChangesForComplexMember(priority, oldVersionsOfpriority, start, end, changeSetsOfMembers, datapoints);
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
      XMLHelper.beginType(xml, varName, "SchedulerInformation", "xprc.xsched", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      XMLHelper.appendDataList(xml, "capacities", "Capacity", "xprc.xsched", versionedGetCapacities(version), version, cache);
      XMLHelper.appendDataList(xml, "vetos", "Veto", "xprc.xsched", versionedGetVetos(version), version, cache);
      XMLHelper.appendData(xml, "timeConstraint", versionedGetTimeConstraint(version), version, cache);
      XMLHelper.appendData(xml, "priority", versionedGetPriority(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"capacities", "vetos", "timeConstraint", "priority"})));
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
    String[] varNames = new String[]{"capacities", "vetos", "timeConstraint", "priority"};
    Object[] vars = new Object[]{this.capacities, this.vetos, this.timeConstraint, this.priority};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }

  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("timeConstraint".equals(name)) {
      XOUtils.checkCastability(o, TimeConstraint.class, "timeConstraint");
      setTimeConstraint((TimeConstraint) o);
    } else if ("priority".equals(name)) {
      XOUtils.checkCastability(o, Priority.class, "priority");
      setPriority((Priority) o);
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

  /*------- not generated -------- */
  
  //ACHTUNG: Erbt von SchedulerBean, aber nicht im XML! Vetos und Capacities werden von SchedulerBean geerbt, aber haben hier abgeleitete Capacity/Veto Klassen

  /**
  * Creates a new instance ignoring inherited member variables. Inherited member variables may
  * have been overwritten.
  */
  public SchedulerInformation(List<Capacity> capacities, List<Veto> vetos, TimeConstraint timeConstraint, Priority priority) {
    super(capacities, vetos);
    this.timeConstraint = timeConstraint;
    this.priority = priority;
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public List<? extends Capacity> getCapacities() {
    return (List<? extends Capacity>) super.getCapacities();
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public List<? extends Veto> getVetos() {
    return (List<? extends Veto>) super.getVetos();
  }
  
  public String toString() {
    return "SchedulerInformation("+
       (capacities==null?"":"capacities="+capacities)+
       (vetos==null?"":",vetos="+vetos)+
       (timeConstraint==null?"":",timeConstraint="+timeConstraint)+
       (priority==null?"":",priority="+priority)+")";
  }

  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();
  
  public static Field getField(String target_fieldname) throws InvalidObjectPathException {
    Field foundField = null;
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = SchedulerInformation.class.getDeclaredField(target_fieldname);
    } catch (NoSuchFieldException e) {
    }
    if (foundField == null) {
      return SchedulerBean.getField(target_fieldname);
    } else {
      foundField.setAccessible(true);
      fieldMap.put(target_fieldname, foundField);
      return foundField;
    }
  }
  
}

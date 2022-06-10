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
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import com.gip.xyna.xprc.xsched.SchedulerBean;
import com.gip.xyna.xprc.xsched.timeconstraint.AbsRelTime;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint.TimeConstraint_Start;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint.TimeConstraint_Start_Timeout;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint.TimeConstraint_Window;

@XynaObjectAnnotation(fqXmlName = "xprc.xsched.WindowTimeConstraint")
public class WindowTimeConstraint extends com.gip.xyna.xprc.xsched.xynaobjects.TimeConstraint {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = CentralFactoryLogging.getLogger(WindowTimeConstraint.class);


  private TimeConfiguration startTime;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<TimeConfiguration> oldVersionsOfstartTime;


  public TimeConfiguration getStartTime() {
    return startTime;
  }

  public TimeConfiguration versionedGetStartTime(long _version) {
    if (oldVersionsOfstartTime == null) {
      return startTime;
    }
    TimeConfiguration _local = startTime;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<TimeConfiguration> _ret = oldVersionsOfstartTime.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setStartTime(TimeConfiguration startTime) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<TimeConfiguration> _vo = oldVersionsOfstartTime;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfstartTime;
          if (_vo == null) {
            oldVersionsOfstartTime = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<TimeConfiguration>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.startTime);
        this.startTime = startTime;
      }
      return;
    }
    this.startTime = startTime;
  }

  public void unversionedSetStartTime(TimeConfiguration startTime) {
    this.startTime = startTime;
  }

  private TimeConfiguration schedulingTimeout;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<TimeConfiguration> oldVersionsOfschedulingTimeout;


  public TimeConfiguration getSchedulingTimeout() {
    return schedulingTimeout;
  }

  public TimeConfiguration versionedGetSchedulingTimeout(long _version) {
    if (oldVersionsOfschedulingTimeout == null) {
      return schedulingTimeout;
    }
    TimeConfiguration _local = schedulingTimeout;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<TimeConfiguration> _ret = oldVersionsOfschedulingTimeout.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setSchedulingTimeout(TimeConfiguration schedulingTimeout) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<TimeConfiguration> _vo = oldVersionsOfschedulingTimeout;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfschedulingTimeout;
          if (_vo == null) {
            oldVersionsOfschedulingTimeout = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<TimeConfiguration>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.schedulingTimeout);
        this.schedulingTimeout = schedulingTimeout;
      }
      return;
    }
    this.schedulingTimeout = schedulingTimeout;
  }

  public void unversionedSetSchedulingTimeout(TimeConfiguration schedulingTimeout) {
    this.schedulingTimeout = schedulingTimeout;
  }

  private String windowName;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOfwindowName;


  public String getWindowName() {
    return windowName;
  }

  public String versionedGetWindowName(long _version) {
    if (oldVersionsOfwindowName == null) {
      return windowName;
    }
    String _local = windowName;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOfwindowName.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setWindowName(String windowName) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOfwindowName;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfwindowName;
          if (_vo == null) {
            oldVersionsOfwindowName = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.windowName);
        this.windowName = windowName;
      }
      return;
    }
    this.windowName = windowName;
  }

  public void unversionedSetWindowName(String windowName) {
    this.windowName = windowName;
  }

  private RelativeTimeConfiguration startTimeInWindow;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<RelativeTimeConfiguration> oldVersionsOfstartTimeInWindow;


  public RelativeTimeConfiguration getStartTimeInWindow() {
    return startTimeInWindow;
  }

  public RelativeTimeConfiguration versionedGetStartTimeInWindow(long _version) {
    if (oldVersionsOfstartTimeInWindow == null) {
      return startTimeInWindow;
    }
    RelativeTimeConfiguration _local = startTimeInWindow;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<RelativeTimeConfiguration> _ret = oldVersionsOfstartTimeInWindow.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setStartTimeInWindow(RelativeTimeConfiguration startTimeInWindow) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<RelativeTimeConfiguration> _vo = oldVersionsOfstartTimeInWindow;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfstartTimeInWindow;
          if (_vo == null) {
            oldVersionsOfstartTimeInWindow = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<RelativeTimeConfiguration>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.startTimeInWindow);
        this.startTimeInWindow = startTimeInWindow;
      }
      return;
    }
    this.startTimeInWindow = startTimeInWindow;
  }

  public void unversionedSetStartTimeInWindow(RelativeTimeConfiguration startTimeInWindow) {
    this.startTimeInWindow = startTimeInWindow;
  }

  private RelativeTimeConfiguration schedulingTimeoutInWindow;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<RelativeTimeConfiguration> oldVersionsOfschedulingTimeoutInWindow;


  public RelativeTimeConfiguration getSchedulingTimeoutInWindow() {
    return schedulingTimeoutInWindow;
  }

  public RelativeTimeConfiguration versionedGetSchedulingTimeoutInWindow(long _version) {
    if (oldVersionsOfschedulingTimeoutInWindow == null) {
      return schedulingTimeoutInWindow;
    }
    RelativeTimeConfiguration _local = schedulingTimeoutInWindow;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<RelativeTimeConfiguration> _ret = oldVersionsOfschedulingTimeoutInWindow.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setSchedulingTimeoutInWindow(RelativeTimeConfiguration schedulingTimeoutInWindow) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<RelativeTimeConfiguration> _vo = oldVersionsOfschedulingTimeoutInWindow;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfschedulingTimeoutInWindow;
          if (_vo == null) {
            oldVersionsOfschedulingTimeoutInWindow = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<RelativeTimeConfiguration>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.schedulingTimeoutInWindow);
        this.schedulingTimeoutInWindow = schedulingTimeoutInWindow;
      }
      return;
    }
    this.schedulingTimeoutInWindow = schedulingTimeoutInWindow;
  }

  public void unversionedSetSchedulingTimeoutInWindow(RelativeTimeConfiguration schedulingTimeoutInWindow) {
    this.schedulingTimeoutInWindow = schedulingTimeoutInWindow;
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends WindowTimeConstraint, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>> extends com.gip.xyna.xprc.xsched.xynaobjects.TimeConstraint.InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>  {

    protected InternalBuilder(WindowTimeConstraint instance) {
      super(instance);
    }

    public WindowTimeConstraint instance() {
      return (WindowTimeConstraint) instance;
    }

    public _GEN_BUILDER_TYPE startTime(TimeConfiguration startTime) {
      this.instance.unversionedSetStartTime(startTime);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE schedulingTimeout(TimeConfiguration schedulingTimeout) {
      this.instance.unversionedSetSchedulingTimeout(schedulingTimeout);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE windowName(String windowName) {
      this.instance.unversionedSetWindowName(windowName);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE startTimeInWindow(RelativeTimeConfiguration startTimeInWindow) {
      this.instance.unversionedSetStartTimeInWindow(startTimeInWindow);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE schedulingTimeoutInWindow(RelativeTimeConfiguration schedulingTimeoutInWindow) {
      this.instance.unversionedSetSchedulingTimeoutInWindow(schedulingTimeoutInWindow);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<WindowTimeConstraint, Builder> {
    public Builder() {
      super(new WindowTimeConstraint());
    }
    public Builder(WindowTimeConstraint instance) {
      super(instance);
    }
  }

  public Builder buildWindowTimeConstraint() {
    return new Builder(this);
  }

  public WindowTimeConstraint() {
    super();
  }

  /**
  * Creates a new instance using locally defined member variables.
  */
  public WindowTimeConstraint(TimeConfiguration startTime, TimeConfiguration schedulingTimeout, String windowName, RelativeTimeConfiguration startTimeInWindow, RelativeTimeConfiguration schedulingTimeoutInWindow) {
    this();
    this.startTime = startTime;
    this.schedulingTimeout = schedulingTimeout;
    this.windowName = windowName;
    this.startTimeInWindow = startTimeInWindow;
    this.schedulingTimeoutInWindow = schedulingTimeoutInWindow;
  }

  protected void fillVars(WindowTimeConstraint source, boolean deep) {
    super.fillVars(source, deep);
    this.startTime = (TimeConfiguration)XynaObject.clone(source.startTime, deep);
    this.schedulingTimeout = (TimeConfiguration)XynaObject.clone(source.schedulingTimeout, deep);
    this.windowName = source.windowName;
    this.startTimeInWindow = (RelativeTimeConfiguration)XynaObject.clone(source.startTimeInWindow, deep);
    this.schedulingTimeoutInWindow = (RelativeTimeConfiguration)XynaObject.clone(source.schedulingTimeoutInWindow, deep);
  }

  public WindowTimeConstraint clone() {
    return clone(true);
  }

  public WindowTimeConstraint clone(boolean deep) {
    WindowTimeConstraint cloned = new WindowTimeConstraint();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xprc.xsched.xynaobjects.TimeConstraint.ObjectVersion {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      if (!super.memberEquals(o)) {
        return false;
      }
      ObjectVersion other = (ObjectVersion) o;
      WindowTimeConstraint xoc = (WindowTimeConstraint) xo;
      WindowTimeConstraint xoco = (WindowTimeConstraint) other.xo;
      if (!xoEqual(xoc.versionedGetStartTime(this.version), xoco.versionedGetStartTime(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      if (!xoEqual(xoc.versionedGetSchedulingTimeout(this.version), xoco.versionedGetSchedulingTimeout(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      if (!equal(xoc.versionedGetWindowName(this.version), xoco.versionedGetWindowName(other.version))) {
        return false;
      }
      if (!xoEqual(xoc.versionedGetStartTimeInWindow(this.version), xoco.versionedGetStartTimeInWindow(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      if (!xoEqual(xoc.versionedGetSchedulingTimeoutInWindow(this.version), xoco.versionedGetSchedulingTimeoutInWindow(other.version), this.version, other.version, changeSetsOfMembers)) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = super.calcHashOfMembers(stack);
      WindowTimeConstraint xoc = (WindowTimeConstraint) xo;
      TimeConfiguration startTime = xoc.versionedGetStartTime(this.version);
      hash = hash * 31 + (startTime == null ? 0 : startTime.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
      TimeConfiguration schedulingTimeout = xoc.versionedGetSchedulingTimeout(this.version);
      hash = hash * 31 + (schedulingTimeout == null ? 0 : schedulingTimeout.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
      String windowName = xoc.versionedGetWindowName(this.version);
      hash = hash * 31 + (windowName == null ? 0 : windowName.hashCode());
      RelativeTimeConfiguration startTimeInWindow = xoc.versionedGetStartTimeInWindow(this.version);
      hash = hash * 31 + (startTimeInWindow == null ? 0 : startTimeInWindow.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
      RelativeTimeConfiguration schedulingTimeoutInWindow = xoc.versionedGetSchedulingTimeoutInWindow(this.version);
      hash = hash * 31 + (schedulingTimeoutInWindow == null ? 0 : schedulingTimeoutInWindow.createObjectVersion(this.version, changeSetsOfMembers).hashCode(stack));
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
    XOUtils.addChangesForComplexMember(startTime, oldVersionsOfstartTime, start, end, changeSetsOfMembers, datapoints);
    XOUtils.addChangesForComplexMember(schedulingTimeout, oldVersionsOfschedulingTimeout, start, end, changeSetsOfMembers, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfwindowName, start, end, datapoints);
    XOUtils.addChangesForComplexMember(startTimeInWindow, oldVersionsOfstartTimeInWindow, start, end, changeSetsOfMembers, datapoints);
    XOUtils.addChangesForComplexMember(schedulingTimeoutInWindow, oldVersionsOfschedulingTimeoutInWindow, start, end, changeSetsOfMembers, datapoints);
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
      XMLHelper.beginType(xml, varName, "WindowTimeConstraint", "xprc.xsched", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      xml.append(super.toXml(varName, true, version, cache));
      XMLHelper.appendData(xml, "startTime", versionedGetStartTime(version), version, cache);
      XMLHelper.appendData(xml, "schedulingTimeout", versionedGetSchedulingTimeout(version), version, cache);
      XMLHelper.appendData(xml, "windowName", versionedGetWindowName(version), version, cache);
      XMLHelper.appendData(xml, "startTimeInWindow", versionedGetStartTimeInWindow(version), version, cache);
      XMLHelper.appendData(xml, "schedulingTimeoutInWindow", versionedGetSchedulingTimeoutInWindow(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"startTime", "schedulingTimeout", "windowName", "startTimeInWindow", "schedulingTimeoutInWindow"})));
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
    String[] varNames = new String[]{"startTime", "schedulingTimeout", "windowName", "startTimeInWindow", "schedulingTimeoutInWindow"};
    Object[] vars = new Object[]{this.startTime, this.schedulingTimeout, this.windowName, this.startTimeInWindow, this.schedulingTimeoutInWindow};
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
    if ("startTime".equals(name)) {
      XOUtils.checkCastability(o, TimeConfiguration.class, "startTime");
      setStartTime((TimeConfiguration) o);
    } else if ("schedulingTimeout".equals(name)) {
      XOUtils.checkCastability(o, TimeConfiguration.class, "schedulingTimeout");
      setSchedulingTimeout((TimeConfiguration) o);
    } else if ("windowName".equals(name)) {
      XOUtils.checkCastability(o, String.class, "windowName");
      setWindowName((String) o);
    } else if ("startTimeInWindow".equals(name)) {
      XOUtils.checkCastability(o, RelativeTimeConfiguration.class, "startTimeInWindow");
      setStartTimeInWindow((RelativeTimeConfiguration) o);
    } else if ("schedulingTimeoutInWindow".equals(name)) {
      XOUtils.checkCastability(o, RelativeTimeConfiguration.class, "schedulingTimeoutInWindow");
      setSchedulingTimeoutInWindow((RelativeTimeConfiguration) o);
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
      foundField = WindowTimeConstraint.class.getDeclaredField(target_fieldname);
    } catch (NoSuchFieldException e) {
    }
    if (foundField == null) {
      return TimeConstraint.getField(target_fieldname);
    } else {
      foundField.setAccessible(true);
      fieldMap.put(target_fieldname, foundField);
      return foundField;
    }
  }

  /*------- not generated -------- */

  public String toString() {
    return "WindowTimeConstraint("+super.toString()+
       (startTime==null?"":",startTime="+startTime)+
       (schedulingTimeout==null?"":",schedulingTimeout="+schedulingTimeout)+
       (windowName==null?"":",windowName="+("\""+windowName+"\""))+
       (startTimeInWindow==null?"":",startTimeInWindow="+startTimeInWindow)+
       (schedulingTimeoutInWindow==null?"":",schedulingTimeoutInWindow="+schedulingTimeoutInWindow)+")";
  }

  
  @Override
  public com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint toDefinition() {
    return new TimeConstraint_Window( simpleTimeConstraintFrom(startTime,schedulingTimeout),
                                      windowName,
                                      simpleTimeConstraintFrom(startTimeInWindow,schedulingTimeoutInWindow)

        );
  }

  private TimeConstraint_Start simpleTimeConstraintFrom(TimeConfiguration start, TimeConfiguration timeout) {
    AbsRelTime st = start != null ? start.toAbsRelTime() : AbsRelTime.relative(0);
    if( timeout == null ) {
      return new TimeConstraint_Start(st);
    } else {
      return new TimeConstraint_Start_Timeout(st, timeout.toAbsRelTime() );
    }
  }
  
  public static WindowTimeConstraint fromDefinition(TimeConstraint_Window timeConstraint) {
    WindowTimeConstraint wtc = new WindowTimeConstraint();
    wtc.setStartTime(getStartTime(timeConstraint.getBeforeTimeConstraint())); // TimeConfiguration.fromAbsRelTime(timeConstraint.getBeforeTimeConstraint().getStart()));
    wtc.setSchedulingTimeout(getSchedulingTimeout( timeConstraint.getBeforeTimeConstraint() ));
    wtc.setWindowName(timeConstraint.getWindowName());
    wtc.setStartTimeInWindow((RelativeTimeConfiguration)getStartTime(timeConstraint.getInnerTimeConstraint()));
    wtc.setSchedulingTimeoutInWindow((RelativeTimeConfiguration)getSchedulingTimeout(timeConstraint.getInnerTimeConstraint()));
    return wtc;
  }

  private static TimeConfiguration getStartTime(TimeConstraint_Start tc) {
    return TimeConfiguration.fromAbsRelTime(tc.getStart());
  }
  private static TimeConfiguration getSchedulingTimeout(TimeConstraint_Start tc) {
   if( tc instanceof TimeConstraint_Start_Timeout ) {
     TimeConstraint_Start_Timeout tcst = (TimeConstraint_Start_Timeout)tc;
     return TimeConfiguration.fromAbsRelTime( tcst.getSchedulingTimeout() );
   } else {
     return null;
   }
  }

  

}

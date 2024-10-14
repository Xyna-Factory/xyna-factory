/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import com.gip.xyna.xprc.xsched.SchedulerBean;
import com.gip.xyna.xprc.xsched.timeconstraint.AbsRelTime;

@XynaObjectAnnotation(fqXmlName = "xprc.xsched.RelativeTimeConfiguration")
public class RelativeTimeConfiguration extends com.gip.xyna.xprc.xsched.xynaobjects.TimeConfiguration {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = CentralFactoryLogging.getLogger(RelativeTimeConfiguration.class);


  @LabelAnnotation(label="Milliseconds")
  private Long milliSeconds;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Long> oldVersionsOfmilliSeconds;


  public Long getMilliSeconds() {
    return milliSeconds;
  }

  public Long versionedGetMilliSeconds(long _version) {
    if (oldVersionsOfmilliSeconds == null) {
      return milliSeconds;
    }
    Long _local = milliSeconds;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Long> _ret = oldVersionsOfmilliSeconds.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setMilliSeconds(Long milliSeconds) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Long> _vo = oldVersionsOfmilliSeconds;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfmilliSeconds;
          if (_vo == null) {
            oldVersionsOfmilliSeconds = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Long>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.milliSeconds);
        this.milliSeconds = milliSeconds;
      }
      return;
    }
    this.milliSeconds = milliSeconds;
  }

  public void unversionedSetMilliSeconds(Long milliSeconds) {
    this.milliSeconds = milliSeconds;
  }

  @LabelAnnotation(label="Seconds")
  private Long seconds;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Long> oldVersionsOfseconds;


  public Long getSeconds() {
    return seconds;
  }

  public Long versionedGetSeconds(long _version) {
    if (oldVersionsOfseconds == null) {
      return seconds;
    }
    Long _local = seconds;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Long> _ret = oldVersionsOfseconds.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setSeconds(Long seconds) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Long> _vo = oldVersionsOfseconds;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfseconds;
          if (_vo == null) {
            oldVersionsOfseconds = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Long>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.seconds);
        this.seconds = seconds;
      }
      return;
    }
    this.seconds = seconds;
  }

  public void unversionedSetSeconds(Long seconds) {
    this.seconds = seconds;
  }

  @LabelAnnotation(label="Minutes")
  private Long minutes;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Long> oldVersionsOfminutes;


  public Long getMinutes() {
    return minutes;
  }

  public Long versionedGetMinutes(long _version) {
    if (oldVersionsOfminutes == null) {
      return minutes;
    }
    Long _local = minutes;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Long> _ret = oldVersionsOfminutes.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setMinutes(Long minutes) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Long> _vo = oldVersionsOfminutes;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfminutes;
          if (_vo == null) {
            oldVersionsOfminutes = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Long>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.minutes);
        this.minutes = minutes;
      }
      return;
    }
    this.minutes = minutes;
  }

  public void unversionedSetMinutes(Long minutes) {
    this.minutes = minutes;
  }

  @LabelAnnotation(label="Hours")
  private Long hours;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Long> oldVersionsOfhours;


  public Long getHours() {
    return hours;
  }

  public Long versionedGetHours(long _version) {
    if (oldVersionsOfhours == null) {
      return hours;
    }
    Long _local = hours;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Long> _ret = oldVersionsOfhours.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setHours(Long hours) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Long> _vo = oldVersionsOfhours;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfhours;
          if (_vo == null) {
            oldVersionsOfhours = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Long>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.hours);
        this.hours = hours;
      }
      return;
    }
    this.hours = hours;
  }

  public void unversionedSetHours(Long hours) {
    this.hours = hours;
  }

  @LabelAnnotation(label="Days")
  private Long days;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Long> oldVersionsOfdays;


  public Long getDays() {
    return days;
  }

  public Long versionedGetDays(long _version) {
    if (oldVersionsOfdays == null) {
      return days;
    }
    Long _local = days;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Long> _ret = oldVersionsOfdays.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setDays(Long days) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Long> _vo = oldVersionsOfdays;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfdays;
          if (_vo == null) {
            oldVersionsOfdays = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Long>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.days);
        this.days = days;
      }
      return;
    }
    this.days = days;
  }

  public void unversionedSetDays(Long days) {
    this.days = days;
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends RelativeTimeConfiguration, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>> extends com.gip.xyna.xprc.xsched.xynaobjects.TimeConfiguration.InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>  {

    protected InternalBuilder(RelativeTimeConfiguration instance) {
      super(instance);
    }

    public RelativeTimeConfiguration instance() {
      return (RelativeTimeConfiguration) instance;
    }

    public _GEN_BUILDER_TYPE milliSeconds(Long milliSeconds) {
      this.instance.unversionedSetMilliSeconds(milliSeconds);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE seconds(Long seconds) {
      this.instance.unversionedSetSeconds(seconds);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE minutes(Long minutes) {
      this.instance.unversionedSetMinutes(minutes);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE hours(Long hours) {
      this.instance.unversionedSetHours(hours);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE days(Long days) {
      this.instance.unversionedSetDays(days);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<RelativeTimeConfiguration, Builder> {
    public Builder() {
      super(new RelativeTimeConfiguration());
    }
    public Builder(RelativeTimeConfiguration instance) {
      super(instance);
    }
  }

  public Builder buildRelativeTimeConfiguration() {
    return new Builder(this);
  }

  public RelativeTimeConfiguration() {
    super();
  }

  /**
  * Creates a new instance using locally defined member variables.
  */
  public RelativeTimeConfiguration(Long milliSeconds, Long seconds, Long minutes, Long hours, Long days) {
    this();
    this.milliSeconds = milliSeconds;
    this.seconds = seconds;
    this.minutes = minutes;
    this.hours = hours;
    this.days = days;
  }

  protected void fillVars(RelativeTimeConfiguration source, boolean deep) {
    super.fillVars(source, deep);
    this.milliSeconds = source.milliSeconds;
    this.seconds = source.seconds;
    this.minutes = source.minutes;
    this.hours = source.hours;
    this.days = source.days;
  }

  public RelativeTimeConfiguration clone() {
    return clone(true);
  }

  public RelativeTimeConfiguration clone(boolean deep) {
    RelativeTimeConfiguration cloned = new RelativeTimeConfiguration();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xprc.xsched.xynaobjects.TimeConfiguration.ObjectVersion {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      if (!super.memberEquals(o)) {
        return false;
      }
      ObjectVersion other = (ObjectVersion) o;
      RelativeTimeConfiguration xoc = (RelativeTimeConfiguration) xo;
      RelativeTimeConfiguration xoco = (RelativeTimeConfiguration) other.xo;
      if (!equal(xoc.versionedGetMilliSeconds(this.version), xoco.versionedGetMilliSeconds(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetSeconds(this.version), xoco.versionedGetSeconds(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetMinutes(this.version), xoco.versionedGetMinutes(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetHours(this.version), xoco.versionedGetHours(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetDays(this.version), xoco.versionedGetDays(other.version))) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = super.calcHashOfMembers(stack);
      RelativeTimeConfiguration xoc = (RelativeTimeConfiguration) xo;
      Long milliSeconds = xoc.versionedGetMilliSeconds(this.version);
      hash = hash * 31 + (milliSeconds == null ? 0 : milliSeconds.hashCode());
      Long seconds = xoc.versionedGetSeconds(this.version);
      hash = hash * 31 + (seconds == null ? 0 : seconds.hashCode());
      Long minutes = xoc.versionedGetMinutes(this.version);
      hash = hash * 31 + (minutes == null ? 0 : minutes.hashCode());
      Long hours = xoc.versionedGetHours(this.version);
      hash = hash * 31 + (hours == null ? 0 : hours.hashCode());
      Long days = xoc.versionedGetDays(this.version);
      hash = hash * 31 + (days == null ? 0 : days.hashCode());
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
    XOUtils.addChangesForSimpleMember(oldVersionsOfmilliSeconds, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfseconds, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfminutes, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfhours, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfdays, start, end, datapoints);
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
      XMLHelper.beginType(xml, varName, "RelativeTimeConfiguration", "xprc.xsched", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      xml.append(super.toXml(varName, true, version, cache));
      XMLHelper.appendData(xml, "milliSeconds", versionedGetMilliSeconds(version), version, cache);
      XMLHelper.appendData(xml, "seconds", versionedGetSeconds(version), version, cache);
      XMLHelper.appendData(xml, "minutes", versionedGetMinutes(version), version, cache);
      XMLHelper.appendData(xml, "hours", versionedGetHours(version), version, cache);
      XMLHelper.appendData(xml, "days", versionedGetDays(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"milliSeconds", "seconds", "minutes", "hours", "days"})));
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
    String[] varNames = new String[]{"milliSeconds", "seconds", "minutes", "hours", "days"};
    Object[] vars = new Object[]{this.milliSeconds, this.seconds, this.minutes, this.hours, this.days};
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
    if ("milliSeconds".equals(name)) {
      XOUtils.checkCastability(o, Long.class, "milliSeconds");
      setMilliSeconds((Long) o);
    } else if ("seconds".equals(name)) {
      XOUtils.checkCastability(o, Long.class, "seconds");
      setSeconds((Long) o);
    } else if ("minutes".equals(name)) {
      XOUtils.checkCastability(o, Long.class, "minutes");
      setMinutes((Long) o);
    } else if ("hours".equals(name)) {
      XOUtils.checkCastability(o, Long.class, "hours");
      setHours((Long) o);
    } else if ("days".equals(name)) {
      XOUtils.checkCastability(o, Long.class, "days");
      setDays((Long) o);
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
      foundField = RelativeTimeConfiguration.class.getDeclaredField(target_fieldname);
    } catch (NoSuchFieldException e) {
    }
    if (foundField == null) {
      return TimeConfiguration.getField(target_fieldname);
    } else {
      foundField.setAccessible(true);
      fieldMap.put(target_fieldname, foundField);
      return foundField;
    }
  }


  /*------- not generated -------- */
  
  public AbsRelTime toAbsRelTime() {
    long millis = 0;
    millis += getMillis( milliSeconds, 1);
    millis += getMillis( seconds,      1000L);
    millis += getMillis( minutes,      1000L*60);
    millis += getMillis( hours,        1000L*60*60);
    millis += getMillis( days,         1000L*60*60*24);
    return AbsRelTime.relative(millis);
  }

  private long getMillis(Long value, long multiplier) {
    if( value == null ) {
      return 0;
    } else {
      return multiplier*value.longValue();
    }
  }

  public static RelativeTimeConfiguration fromMillis(long time, boolean onlyOneField) {
    RelativeTimeConfiguration ret = new RelativeTimeConfiguration();
    if( time == 0 ) {
      ret.milliSeconds = 0L;
      return ret;
    }
    long millis = time;
    long seconds = millis/1000;
    long minutes = seconds/60;
    long hours = minutes/60;
    long days = hours/24;
    if( onlyOneField ) {
      if( millis%1000 != 0 ) {
        ret.milliSeconds = millis;
        return ret;
      }
      if( seconds%60 != 0 ) {
        ret.seconds = seconds;
        return ret;
      }
      if( minutes%60 != 0 ) {
        ret.minutes = minutes;
        return ret;
      }
      if( hours%24 != 0 ) {
        ret.hours = hours;
        return ret;
      }
      ret.days = days;
      return ret;
    } else {
      ret.milliSeconds = millis%1000;
      ret.seconds = seconds%60;
      ret.minutes = minutes%60;
      ret.hours = hours%24;
      ret.days = days;
      return ret;
    }
  }
}

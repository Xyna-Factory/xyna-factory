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
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
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


@XynaObjectAnnotation(fqXmlName = "xprc.xsched.AbsoluteTimeConfiguration")
public class AbsoluteTimeConfiguration extends TimeConfiguration {

  private static final long serialVersionUID = 1L;
  private static final Logger logger = CentralFactoryLogging.getLogger(AbsoluteTimeConfiguration.class);


  @LabelAnnotation(label="Year")
  private Integer year;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> oldVersionsOfyear;


  public Integer getYear() {
    return year;
  }

  public Integer versionedGetYear(long _version) {
    if (oldVersionsOfyear == null) {
      return year;
    }
    Integer _local = year;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Integer> _ret = oldVersionsOfyear.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setYear(Integer year) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> _vo = oldVersionsOfyear;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfyear;
          if (_vo == null) {
            oldVersionsOfyear = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.year);
        this.year = year;
      }
      return;
    }
    this.year = year;
  }

  public void unversionedSetYear(Integer year) {
    this.year = year;
  }

  @LabelAnnotation(label="Month")
  private Integer month;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> oldVersionsOfmonth;


  public Integer getMonth() {
    return month;
  }

  public Integer versionedGetMonth(long _version) {
    if (oldVersionsOfmonth == null) {
      return month;
    }
    Integer _local = month;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Integer> _ret = oldVersionsOfmonth.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setMonth(Integer month) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> _vo = oldVersionsOfmonth;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfmonth;
          if (_vo == null) {
            oldVersionsOfmonth = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.month);
        this.month = month;
      }
      return;
    }
    this.month = month;
  }

  public void unversionedSetMonth(Integer month) {
    this.month = month;
  }

  @LabelAnnotation(label="Day")
  private Integer day;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> oldVersionsOfday;


  public Integer getDay() {
    return day;
  }

  public Integer versionedGetDay(long _version) {
    if (oldVersionsOfday == null) {
      return day;
    }
    Integer _local = day;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Integer> _ret = oldVersionsOfday.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setDay(Integer day) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> _vo = oldVersionsOfday;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfday;
          if (_vo == null) {
            oldVersionsOfday = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.day);
        this.day = day;
      }
      return;
    }
    this.day = day;
  }

  public void unversionedSetDay(Integer day) {
    this.day = day;
  }

  @LabelAnnotation(label="Hour")
  private Integer hour;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> oldVersionsOfhour;


  public Integer getHour() {
    return hour;
  }

  public Integer versionedGetHour(long _version) {
    if (oldVersionsOfhour == null) {
      return hour;
    }
    Integer _local = hour;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Integer> _ret = oldVersionsOfhour.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setHour(Integer hour) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> _vo = oldVersionsOfhour;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfhour;
          if (_vo == null) {
            oldVersionsOfhour = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.hour);
        this.hour = hour;
      }
      return;
    }
    this.hour = hour;
  }

  public void unversionedSetHour(Integer hour) {
    this.hour = hour;
  }

  @LabelAnnotation(label="Minute")
  private Integer minute;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> oldVersionsOfminute;


  public Integer getMinute() {
    return minute;
  }

  public Integer versionedGetMinute(long _version) {
    if (oldVersionsOfminute == null) {
      return minute;
    }
    Integer _local = minute;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Integer> _ret = oldVersionsOfminute.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setMinute(Integer minute) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> _vo = oldVersionsOfminute;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfminute;
          if (_vo == null) {
            oldVersionsOfminute = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.minute);
        this.minute = minute;
      }
      return;
    }
    this.minute = minute;
  }

  public void unversionedSetMinute(Integer minute) {
    this.minute = minute;
  }

  @LabelAnnotation(label="Second")
  private Integer second;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> oldVersionsOfsecond;


  public Integer getSecond() {
    return second;
  }

  public Integer versionedGetSecond(long _version) {
    if (oldVersionsOfsecond == null) {
      return second;
    }
    Integer _local = second;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Integer> _ret = oldVersionsOfsecond.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setSecond(Integer second) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> _vo = oldVersionsOfsecond;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfsecond;
          if (_vo == null) {
            oldVersionsOfsecond = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.second);
        this.second = second;
      }
      return;
    }
    this.second = second;
  }

  public void unversionedSetSecond(Integer second) {
    this.second = second;
  }

  @LabelAnnotation(label="Millisecond")
  private Integer milliSecond;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> oldVersionsOfmilliSecond;


  public Integer getMilliSecond() {
    return milliSecond;
  }

  public Integer versionedGetMilliSecond(long _version) {
    if (oldVersionsOfmilliSecond == null) {
      return milliSecond;
    }
    Integer _local = milliSecond;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Integer> _ret = oldVersionsOfmilliSecond.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setMilliSecond(Integer milliSecond) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> _vo = oldVersionsOfmilliSecond;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfmilliSecond;
          if (_vo == null) {
            oldVersionsOfmilliSecond = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.milliSecond);
        this.milliSecond = milliSecond;
      }
      return;
    }
    this.milliSecond = milliSecond;
  }

  public void unversionedSetMilliSecond(Integer milliSecond) {
    this.milliSecond = milliSecond;
  }

  @LabelAnnotation(label="Timezone Offset")
  private Integer timezoneOffset;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> oldVersionsOftimezoneOffset;


  public Integer getTimezoneOffset() {
    return timezoneOffset;
  }

  public Integer versionedGetTimezoneOffset(long _version) {
    if (oldVersionsOftimezoneOffset == null) {
      return timezoneOffset;
    }
    Integer _local = timezoneOffset;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<Integer> _ret = oldVersionsOftimezoneOffset.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setTimezoneOffset(Integer timezoneOffset) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer> _vo = oldVersionsOftimezoneOffset;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOftimezoneOffset;
          if (_vo == null) {
            oldVersionsOftimezoneOffset = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<Integer>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.timezoneOffset);
        this.timezoneOffset = timezoneOffset;
      }
      return;
    }
    this.timezoneOffset = timezoneOffset;
  }

  public void unversionedSetTimezoneOffset(Integer timezoneOffset) {
    this.timezoneOffset = timezoneOffset;
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends AbsoluteTimeConfiguration, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>> extends TimeConfiguration.InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>  {

    protected InternalBuilder(AbsoluteTimeConfiguration instance) {
      super(instance);
    }

    public AbsoluteTimeConfiguration instance() {
      return (AbsoluteTimeConfiguration) instance;
    }

    public _GEN_BUILDER_TYPE year(Integer year) {
      this.instance.unversionedSetYear(year);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE month(Integer month) {
      this.instance.unversionedSetMonth(month);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE day(Integer day) {
      this.instance.unversionedSetDay(day);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE hour(Integer hour) {
      this.instance.unversionedSetHour(hour);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE minute(Integer minute) {
      this.instance.unversionedSetMinute(minute);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE second(Integer second) {
      this.instance.unversionedSetSecond(second);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE milliSecond(Integer milliSecond) {
      this.instance.unversionedSetMilliSecond(milliSecond);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE timezoneOffset(Integer timezoneOffset) {
      this.instance.unversionedSetTimezoneOffset(timezoneOffset);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<AbsoluteTimeConfiguration, Builder> {
    public Builder() {
      super(new AbsoluteTimeConfiguration());
    }
    public Builder(AbsoluteTimeConfiguration instance) {
      super(instance);
    }
  }

  public Builder buildAbsoluteTimeConfiguration() {
    return new Builder(this);
  }

  public AbsoluteTimeConfiguration() {
    super();
  }

  /**
  * Creates a new instance using locally defined member variables.
  */
  public AbsoluteTimeConfiguration(Integer year, Integer month, Integer day, Integer hour, Integer minute, Integer second, Integer milliSecond, Integer timezoneOffset) {
    this();
    this.year = year;
    this.month = month;
    this.day = day;
    this.hour = hour;
    this.minute = minute;
    this.second = second;
    this.milliSecond = milliSecond;
    this.timezoneOffset = timezoneOffset;
  }

  protected void fillVars(AbsoluteTimeConfiguration source, boolean deep) {
    super.fillVars(source, deep);
    this.year = source.year;
    this.month = source.month;
    this.day = source.day;
    this.hour = source.hour;
    this.minute = source.minute;
    this.second = source.second;
    this.milliSecond = source.milliSecond;
    this.timezoneOffset = source.timezoneOffset;
  }

  public AbsoluteTimeConfiguration clone() {
    return clone(true);
  }

  public AbsoluteTimeConfiguration clone(boolean deep) {
    AbsoluteTimeConfiguration cloned = new AbsoluteTimeConfiguration();
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
      AbsoluteTimeConfiguration xoc = (AbsoluteTimeConfiguration) xo;
      AbsoluteTimeConfiguration xoco = (AbsoluteTimeConfiguration) other.xo;
      if (!equal(xoc.versionedGetYear(this.version), xoco.versionedGetYear(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetMonth(this.version), xoco.versionedGetMonth(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetDay(this.version), xoco.versionedGetDay(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetHour(this.version), xoco.versionedGetHour(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetMinute(this.version), xoco.versionedGetMinute(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetSecond(this.version), xoco.versionedGetSecond(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetMilliSecond(this.version), xoco.versionedGetMilliSecond(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetTimezoneOffset(this.version), xoco.versionedGetTimezoneOffset(other.version))) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = super.calcHashOfMembers(stack);
      AbsoluteTimeConfiguration xoc = (AbsoluteTimeConfiguration) xo;
      Integer year = xoc.versionedGetYear(this.version);
      hash = hash * 31 + (year == null ? 0 : year.hashCode());
      Integer month = xoc.versionedGetMonth(this.version);
      hash = hash * 31 + (month == null ? 0 : month.hashCode());
      Integer day = xoc.versionedGetDay(this.version);
      hash = hash * 31 + (day == null ? 0 : day.hashCode());
      Integer hour = xoc.versionedGetHour(this.version);
      hash = hash * 31 + (hour == null ? 0 : hour.hashCode());
      Integer minute = xoc.versionedGetMinute(this.version);
      hash = hash * 31 + (minute == null ? 0 : minute.hashCode());
      Integer second = xoc.versionedGetSecond(this.version);
      hash = hash * 31 + (second == null ? 0 : second.hashCode());
      Integer milliSecond = xoc.versionedGetMilliSecond(this.version);
      hash = hash * 31 + (milliSecond == null ? 0 : milliSecond.hashCode());
      Integer timezoneOffset = xoc.versionedGetTimezoneOffset(this.version);
      hash = hash * 31 + (timezoneOffset == null ? 0 : timezoneOffset.hashCode());
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
    XOUtils.addChangesForSimpleMember(oldVersionsOfyear, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfmonth, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfday, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfhour, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfminute, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfsecond, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfmilliSecond, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOftimezoneOffset, start, end, datapoints);
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
      XMLHelper.beginType(xml, varName, "AbsoluteTimeConfiguration", "xprc.xsched", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      xml.append(super.toXml(varName, true, version, cache));
      XMLHelper.appendData(xml, "year", versionedGetYear(version), version, cache);
      XMLHelper.appendData(xml, "month", versionedGetMonth(version), version, cache);
      XMLHelper.appendData(xml, "day", versionedGetDay(version), version, cache);
      XMLHelper.appendData(xml, "hour", versionedGetHour(version), version, cache);
      XMLHelper.appendData(xml, "minute", versionedGetMinute(version), version, cache);
      XMLHelper.appendData(xml, "second", versionedGetSecond(version), version, cache);
      XMLHelper.appendData(xml, "milliSecond", versionedGetMilliSecond(version), version, cache);
      XMLHelper.appendData(xml, "timezoneOffset", versionedGetTimezoneOffset(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"year", "month", "day", "hour", "minute", "second", "milliSecond", "timezoneOffset"})));
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
    String[] varNames = new String[]{"year", "month", "day", "hour", "minute", "second", "milliSecond", "timezoneOffset"};
    Object[] vars = new Object[]{this.year, this.month, this.day, this.hour, this.minute, this.second, this.milliSecond, this.timezoneOffset};
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
    if ("year".equals(name)) {
      XOUtils.checkCastability(o, Integer.class, "year");
      setYear((Integer) o);
    } else if ("month".equals(name)) {
      XOUtils.checkCastability(o, Integer.class, "month");
      setMonth((Integer) o);
    } else if ("day".equals(name)) {
      XOUtils.checkCastability(o, Integer.class, "day");
      setDay((Integer) o);
    } else if ("hour".equals(name)) {
      XOUtils.checkCastability(o, Integer.class, "hour");
      setHour((Integer) o);
    } else if ("minute".equals(name)) {
      XOUtils.checkCastability(o, Integer.class, "minute");
      setMinute((Integer) o);
    } else if ("second".equals(name)) {
      XOUtils.checkCastability(o, Integer.class, "second");
      setSecond((Integer) o);
    } else if ("milliSecond".equals(name)) {
      XOUtils.checkCastability(o, Integer.class, "milliSecond");
      setMilliSecond((Integer) o);
    } else if ("timezoneOffset".equals(name)) {
      XOUtils.checkCastability(o, Integer.class, "timezoneOffset");
      setTimezoneOffset((Integer) o);
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
  
  public AbsRelTime toAbsRelTime() {
    TimeZone tz = TimeZone.getTimeZone("GMT"); // ignore DST
    int timeOffsetInMilliSeconds = 0;
    if( timezoneOffset != null ) {
      timeOffsetInMilliSeconds = timezoneOffset.intValue() * 60 * 60 * 1000;
    }
    tz.setRawOffset(timeOffsetInMilliSeconds);
    Calendar tempCalendar = Calendar.getInstance(tz);
    setToCalendar( tempCalendar, Calendar.YEAR, year, 0, tempCalendar.get(Calendar.YEAR) );
    setToCalendar( tempCalendar, Calendar.MONTH, month, -1, Calendar.JANUARY);
    setToCalendar( tempCalendar, Calendar.DAY_OF_MONTH, day, 0, 1);
    setToCalendar( tempCalendar, Calendar.HOUR_OF_DAY, hour, 0, 0);
    setToCalendar( tempCalendar, Calendar.MINUTE, minute, 0, 0);
    setToCalendar( tempCalendar, Calendar.SECOND, second, 0, 0);
    setToCalendar( tempCalendar, Calendar.MILLISECOND, milliSecond, 0, 0);
    return AbsRelTime.absolute(tempCalendar.getTimeInMillis());
  }

  private void setToCalendar(Calendar calendar, int field, Integer value, int offset, int defaultValue) {
    if( value == null ) {
      calendar.set(field, defaultValue);
    } else {
      calendar.set(field, value.intValue() + offset);
    }
  }

  public static AbsoluteTimeConfiguration fromMillis(long time) {
    AbsoluteTimeConfiguration ret = new AbsoluteTimeConfiguration();
    Calendar tempCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    tempCalendar.setTimeInMillis(time);
    ret.year = tempCalendar.get(Calendar.YEAR);
    ret.month = tempCalendar.get(Calendar.MONTH)+1;
    ret.day = tempCalendar.get(Calendar.DAY_OF_MONTH);
    ret.hour = tempCalendar.get(Calendar.HOUR_OF_DAY);
    ret.minute = tempCalendar.get(Calendar.MINUTE);
    ret.second = tempCalendar.get(Calendar.SECOND);
    ret.milliSecond = tempCalendar.get(Calendar.MILLISECOND);
    ret.timezoneOffset = 0;
    return ret;
  }
  
  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();
  
  public static Field getField(String target_fieldname) throws InvalidObjectPathException {
    Field foundField = null;
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = AbsoluteTimeConfiguration.class.getDeclaredField(target_fieldname);
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

}

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
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import com.gip.xyna.xprc.xsched.SchedulerBean;
import com.gip.xyna.xprc.xsched.timeconstraint.AbsRelTime;

@XynaObjectAnnotation(fqXmlName = "base.date.RelativeDate")
public class RelativeDate extends com.gip.xyna.xprc.xsched.xynaobjects.Date {

  private static final long serialVersionUID = 13594591899L;
  private static final Logger logger = CentralFactoryLogging.getLogger(RelativeDate.class);

  @LabelAnnotation(label="Duration")
  private String duration;

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOfduration;


  public String getDuration() {
    return duration;
  }

  public String versionedGetDuration(long _version) {
    if (oldVersionsOfduration == null) {
      return duration;
    }
    String _local = duration;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOfduration.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void setDuration(String duration) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOfduration;
      if (_vo == null) {
        synchronized (this) {
          _vo = oldVersionsOfduration;
          if (_vo == null) {
            oldVersionsOfduration = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
          }
        }
      }
      synchronized (_vo) {
        _vo.add(this.duration);
        this.duration = duration;
      }
      return;
    }
    this.duration = duration;
  }

  public void unversionedSetDuration(String duration) {
    this.duration = duration;
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends RelativeDate, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>> extends com.gip.xyna.xprc.xsched.xynaobjects.Date.InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>  {

    protected InternalBuilder(RelativeDate instance) {
      super(instance);
    }

    public RelativeDate instance() {
      return (RelativeDate) instance;
    }

    public _GEN_BUILDER_TYPE duration(String duration) {
      this.instance.unversionedSetDuration(duration);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<RelativeDate, Builder> {
    public Builder() {
      super(new RelativeDate());
    }
    public Builder(RelativeDate instance) {
      super(instance);
    }
  }

  public Builder buildRelativeDate() {
    return new Builder(this);
  }

  public RelativeDate() {
    super();
  }

  /**
  * Creates a new instance using locally defined member variables.
  */
  public RelativeDate(String duration) {
    this();
    this.duration = duration;
  }

  protected void fillVars(RelativeDate source, boolean deep) {
    super.fillVars(source, deep);
    this.duration = source.duration;
    this.durationAsObject = source.durationAsObject;
    this.lastDuration = source.lastDuration;
  }

  public RelativeDate clone() {
    return clone(true);
  }

  public RelativeDate clone(boolean deep) {
    RelativeDate cloned = new RelativeDate();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends com.gip.xyna.xprc.xsched.xynaobjects.Date.ObjectVersion {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      if (!super.memberEquals(o)) {
        return false;
      }
      ObjectVersion other = (ObjectVersion) o;
      RelativeDate xoc = (RelativeDate) xo;
      RelativeDate xoco = (RelativeDate) other.xo;
      if (!equal(xoc.versionedGetDuration(this.version), xoco.versionedGetDuration(other.version))) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = super.calcHashOfMembers(stack);
      RelativeDate xoc = (RelativeDate) xo;
      String duration = xoc.versionedGetDuration(this.version);
      hash = hash * 31 + (duration == null ? 0 : duration.hashCode());
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
    XOUtils.addChangesForSimpleMember(oldVersionsOfduration, start, end, datapoints);
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
      XMLHelper.beginType(xml, varName, "RelativeDate", "base.date", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      xml.append(super.toXml(varName, true, version, cache));
      XMLHelper.appendData(xml, "duration", versionedGetDuration(version), version, cache);
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{"duration"})));
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
    String[] varNames = new String[]{"duration"};
    Object[] vars = new Object[]{this.duration};
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
    if ("duration".equals(name)) {
      XOUtils.checkCastability(o, String.class, "duration");
      setDuration((String) o);
    } else {
      super.set(name, o);
    }
  }

  protected String asString_InternalSuperCallDestination(RelativeDate internalSuperCallDelegator) {
    return asString_InternalImplementation();
  }

  public String asString() {
    return asString_InternalImplementation();
  }

  protected void fromMillis_InternalSuperCallDestination(RelativeDate internalSuperCallDelegator, long millis) {
    fromMillis_InternalImplementation(millis);
  }

  public void fromMillis(long millis) {
    fromMillis_InternalImplementation(millis);
  }

  protected long toMillis_InternalSuperCallDestination(RelativeDate internalSuperCallDelegator) {
    return toMillis_InternalImplementation();
  }

  public long toMillis() {
    return toMillis_InternalImplementation();
  }

  protected void validate_InternalSuperCallDestination(RelativeDate internalSuperCallDelegator) {
    validate_InternalImplementation();
  }

  public void validate() {
    validate_InternalImplementation();
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
      foundField = RelativeDate.class.getDeclaredField(target_fieldname);
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
  

  /*------- not generated -------- */
  
  private Duration durationAsObject;
  private String lastDuration;

  private String asString_InternalImplementation() {
    return duration;
  }

  private void fromMillis_InternalImplementation(long millis) {
    durationAsObject = new Duration(millis).convertToLargestUnitWithoutPrecisionLost();
    duration = durationAsObject.toSumString();
    lastDuration = duration;
  }

  private long toMillis_InternalImplementation() {
    if (duration == null) {
      throw new NullPointerException("RelativeDate: duration member is null");
    }
    if( ! duration.equals(lastDuration) ) {
      durationAsObject = Duration.valueOfSum(duration);
      lastDuration = duration;
    }
    return durationAsObject.getDurationInMillis();
  }

  public AbsRelTime toAbsRelTime() {
    return new AbsRelTime(toMillis(), true);
  }

  private void validate_InternalImplementation() {
    if (duration == null) {
      throw new NullPointerException("RelativeDate: duration member is null");
    }
    if( ! duration.equals(lastDuration) ) {
      durationAsObject = Duration.valueOfSum(duration);
      lastDuration = duration;
    }
  }
 

}

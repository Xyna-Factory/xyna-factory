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
import com.gip.xyna.xprc.xsched.SchedulerBean;

@XynaObjectAnnotation(fqXmlName = "base.date.Now")
public class Now extends AbsoluteDate {

  private static final long serialVersionUID = -1L;
  private static final Logger logger = CentralFactoryLogging.getLogger(Now.class);


  public static class Builder extends InternalBuilder<Now, Builder> {
    public Builder() {
      super(new Now());
    }
    public Builder(Now instance) {
      super(instance);
    }
  }

  public Builder buildNow() {
    return new Builder(this);
  }


  protected void fillVars(Now source, boolean deep) {
    super.fillVars(source, deep);
  }

  public Now clone() {
    return clone(true);
  }

  public Now clone(boolean deep) {
    Now cloned = new Now();
    cloned.fillVars(this, deep);
    return cloned;
  }

  public static class ObjectVersion extends AbsoluteDate.ObjectVersion {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      if (!super.memberEquals(o)) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = super.calcHashOfMembers(stack);
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
      XMLHelper.beginType(xml, varName, "Now", "base.date", objectId, refId);
    } else {
      objectId = -1;
    }
    if (objectId != -2) {
      xml.append(super.toXml(varName, true, version, cache));
    }
    if (!onlyContent) {
      XMLHelper.endType(xml);
    }
    return xml.toString();
  }

  private static Set<String> varNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[]{})));
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
    String[] varNames = new String[]{};
    Object[] vars = new Object[]{};
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
    super.set(name, o);
  }

  public void onDeployment() throws XynaException {
    super.onDeployment();
  }

  public void onUndeployment() throws XynaException {
    super.onUndeployment();
  }

  /**
   * "Publishing" of super-call. May be called by reflection only.
   */
  private String asString_InternalSuperCallProxy(AbsoluteDate internalSuperCallDelegator) {
    return super.asString_InternalSuperCallDestination(internalSuperCallDelegator);
  }

  /**
   * "Publishing" of super-call. May be called by reflection only.
   */
  private void fromMillis_InternalSuperCallProxy(AbsoluteDate internalSuperCallDelegator, long millis) {
    super.fromMillis_InternalSuperCallDestination(internalSuperCallDelegator, millis);
  }

  /**
   * "Publishing" of super-call. May be called by reflection only.
   */
  private AbsoluteDate toAbsoluteDate_InternalSuperCallProxy(AbsoluteDate internalSuperCallDelegator, DateFormat dateFormat) {
    return super.toAbsoluteDate_InternalSuperCallDestination(internalSuperCallDelegator, dateFormat);
  }

  /**
   * "Publishing" of super-call. May be called by reflection only.
   */
  private long toMillis_InternalSuperCallProxy(AbsoluteDate internalSuperCallDelegator) {
    return super.toMillis_InternalSuperCallDestination(internalSuperCallDelegator);
  }

  /**
   * "Publishing" of super-call. May be called by reflection only.
   */
  private void validate_InternalSuperCallProxy(AbsoluteDate internalSuperCallDelegator) {
    super.validate_InternalSuperCallDestination(internalSuperCallDelegator);
  }
  
  
  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();
  
  public static Field getField(String target_fieldname) throws InvalidObjectPathException {
    Field foundField = null;
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = Now.class.getDeclaredField(target_fieldname);
    } catch (NoSuchFieldException e) {
    }
    if (foundField == null) {
      return AbsoluteDate.getField(target_fieldname);
    } else {
      foundField.setAccessible(true);
      fieldMap.put(target_fieldname, foundField);
      return foundField;
    }
  }
  
  
  /*-------------------- not generated -------------------- */

  public Now() {
    super();
    init(this);
  }

  /**
  * Creates a new instance expecting all inherited member variables
  */
  public Now(String date, DateFormat format) {
    this();
    getOrCreateLazyDateFormat().validate(date, format.getFormat());
    unversionedSetDate(date);
    unversionedSetFormat(format);
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends Now, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>> extends AbsoluteDate.InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>  {

    protected InternalBuilder(Now instance) {
      super(instance);
    }

    public Now instance() {
      init(instance);
      return (Now) instance;
    }

  }

  private static void init(Now instance) {
    long now = System.currentTimeMillis();
    String format = instance.getFormatInternally();
    instance.unversionedSetDate(instance.getOrCreateLazyDateFormat().format(now, format));
  }

  protected String getFormatInternally() {
    if (getFormat() == null || getFormat().getFormat() == null) {
      return "yyyy-MM-dd HH:mm:ss";
    } else {
      return super.getFormatInternally();
    }
  }
}

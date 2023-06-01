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
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import com.gip.xyna.xprc.xsched.SchedulerBean;
import com.gip.xyna.xprc.xsched.timeconstraint.AbsRelTime;



@XynaObjectAnnotation(fqXmlName = "base.date.Forever")
public class Forever extends com.gip.xyna.xprc.xsched.xynaobjects.Date {

  private static final long serialVersionUID = -1L;
  private static final Logger logger = CentralFactoryLogging.getLogger(Forever.class);


  protected static class InternalBuilder<_GEN_DOM_TYPE extends Forever, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>> extends com.gip.xyna.xprc.xsched.xynaobjects.Date.InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>  {

    protected InternalBuilder(Forever instance) {
      super(instance);
    }

    public Forever instance() {
      return (Forever) instance;
    }

  }

  public static class Builder extends InternalBuilder<Forever, Builder> {
    public Builder() {
      super(new Forever());
    }
    public Builder(Forever instance) {
      super(instance);
    }
  }

  public Builder buildForever() {
    return new Builder(this);
  }

  public Forever() {
    super();
  }

  protected void fillVars(Forever source, boolean deep) {
    super.fillVars(source, deep);
  }

  public Forever clone() {
    return clone(true);
  }

  public Forever clone(boolean deep) {
    Forever cloned = new Forever();
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
      XMLHelper.beginType(xml, varName, "Forever", "base.date", objectId, refId);
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

  protected String asString_InternalSuperCallDestination(Forever internalSuperCallDelegator) {
    return asString_InternalImplementation();
  }

  public String asString() {
    return asString_InternalImplementation();
  }

  protected void fromMillis_InternalSuperCallDestination(Forever internalSuperCallDelegator, long millis) {
    fromMillis_InternalImplementation(millis);
  }

  public void fromMillis(long millis) {
    fromMillis_InternalImplementation(millis);
  }


  protected long toMillis_InternalSuperCallDestination(Forever internalSuperCallDelegator) {
    return toMillis_InternalImplementation();
  }

  public long toMillis() {
    return toMillis_InternalImplementation();
  }


  protected void validate_InternalSuperCallDestination(Forever internalSuperCallDelegator) {
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
    throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(target_fieldname));
  }

  /*------- not generated -------- */
  
  private String asString_InternalImplementation() {
    return "forever";
  }

  private void fromMillis_InternalImplementation(long millis) {
  }

  private long toMillis_InternalImplementation() {
    return Long.MAX_VALUE / 2;
  }

  private void validate_InternalImplementation() {
  }

  public AbsRelTime toAbsRelTime() {
    return new AbsRelTime(toMillis(), false);
  }

 
}

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
package com.gip.xyna.XMOM.base.net.exception;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.XMLHelper;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;


public class VLANIDValidationException extends ValidationException implements GeneralXynaObject {

  private static final long serialVersionUID = -1L;

  private VLANIDValidationException() {
    super(new String[] {"XYNA-BASE-00017", null});
  }


  public VLANIDValidationException(String parameter1) {
    super(new String[]{"XYNA-BASE-00017", parameter1 + ""});
    setParameter1(parameter1);
  }

  public VLANIDValidationException(String parameter1, Throwable cause) {
    super(new String[]{"XYNA-BASE-00017", parameter1 + ""}, cause);
    setParameter1(parameter1);
  }

  protected VLANIDValidationException(String[] args) {
    super(args);
  }

  protected VLANIDValidationException(String[] args, Throwable cause) {
    super(args, cause);
  }

  public VLANIDValidationException initCause(Throwable t) {
    return (VLANIDValidationException) super.initCause(t);
  }


  public static class ObjectVersion extends com.gip.xyna.XMOM.base.net.exception.ValidationException.ObjectVersion {

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


  public String toXml() {
    return toXml(null);
  }

  public String toXml(String varName) {
    return toXml(varName, false, -1, null);
  }

  public String toXml(String varName, boolean onlyContent) {
    return toXml(varName, onlyContent, -1, null);
  }


  public String toXml(String varName, boolean onlyContent, long version, com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject.XMLReferenceCache cache) {
    long objectId;
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
    StringBuilder xml = new StringBuilder();
    com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.XMLHelper.beginExceptionType(xml, varName, "VLANIDValidationException", "base.net.exception", objectId, refId);
    if (objectId != -2) {
      XMLHelper.appendData(xml, "parameter1", versionedGetParameter1(version), version, cache);
    }
    xml.append("</Exception>");
    return xml.toString();
  }


  protected void fillVars(VLANIDValidationException source, boolean deep) {
    super.fillVars(source, deep);
  }

  public VLANIDValidationException cloneWithoutCause() {
    return cloneWithoutCause(true);
  }

  public VLANIDValidationException cloneWithoutCause(boolean deep) {
    VLANIDValidationException cloned = new VLANIDValidationException();
    cloned.fillVars(this, deep);
    return cloned;
  }


  public Object get(String path) throws InvalidObjectPathException {
    return super.get(path);
  }

  public void set(String name, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    super.set(name, value);
  }
  
  
  protected static class InternalBuilder<_GEN_DOM_TYPE extends VLANIDValidationException, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>> extends ValidationException.InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>  {

    protected InternalBuilder(VLANIDValidationException instance) {
      super(instance);
    }

    public VLANIDValidationException instance() {
      return (VLANIDValidationException) instance;
    }

  }

  public static class Builder extends InternalBuilder<VLANIDValidationException, Builder> {
    public Builder() {
      super(new VLANIDValidationException());
    }
    public Builder(VLANIDValidationException instance) {
      super(instance);
    }
  }

  public Builder buildVLANIDValidationException() {
    return new Builder(this);
  }

  
  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();
  
  public static Field getField(String target_fieldname) {
    Field foundField = null;
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = VLANIDValidationException.class.getDeclaredField(target_fieldname);
    } catch (NoSuchFieldException e) {
    }
    if (foundField == null) {
      return ValidationException.getField(target_fieldname);
    } else {
      foundField.setAccessible(true);
      fieldMap.put(target_fieldname, foundField);
      return foundField;
    }
  }

}

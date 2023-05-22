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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.XMLHelper;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;

@XynaObjectAnnotation(fqXmlName = "base.date.InvalidDateStringException")
public class InvalidDateStringException extends XynaExceptionBase implements GeneralXynaObject {

  private static final long serialVersionUID = 1983019862663L;

  private String date1;
  private String format;

  public InvalidDateStringException() {
    super(new String[] {"XYNA-BASE-00000", null, null});
  }

  public InvalidDateStringException(String date1, String format) {
    super(new String[]{"XYNA-BASE-00000", date1 + "", format + ""});
    setDate1(date1);
    setFormat(format);
  }

  public InvalidDateStringException(String date1, String format, Throwable cause) {
    super(new String[]{"XYNA-BASE-00000", date1 + "", format + ""}, cause);
    setDate1(date1);
    setFormat(format);
  }

  protected InvalidDateStringException(String[] args) {
    super(args);
  }

  protected InvalidDateStringException(String[] args, Throwable cause) {
    super(args, cause);
  }

  protected void refreshArgs() {
    super.refreshArgs();
    String[] args = getArgs();
    args[0] = date1 + "";
    args[1] = format + "";
  }

  public InvalidDateStringException initCause(Throwable t) {
    return (InvalidDateStringException) super.initCause(t);
  }

  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOfdate1;


  private com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> lazyInit_oldVersionsOfdate1() {
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOfdate1;
    if (_vo == null) {
      synchronized (this) {
        _vo = oldVersionsOfdate1;
        if (_vo == null) {
          oldVersionsOfdate1 = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
        }
      }
    }
    return _vo;
  }



  public String getDate1() {
    return date1;
  }


  public void setDate1(String date1) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = lazyInit_oldVersionsOfdate1();
      synchronized (_vo) {
        _vo.add(this.date1);
        this.date1 = date1;
      }
      return;
    }
    this.date1 = date1;
  }


  public String versionedGetDate1(long _version) {
    if (oldVersionsOfdate1 == null) {
      return date1;
    }
    String _local = date1;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOfdate1.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  
  public void unversionedSetDate1(String date1) {
    this.date1 = date1;
  }


  private volatile com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> oldVersionsOfformat;


  private com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> lazyInit_oldVersionsOfformat() {
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = oldVersionsOfformat;
    if (_vo == null) {
      synchronized (this) {
        _vo = oldVersionsOfformat;
        if (_vo == null) {
          oldVersionsOfformat = _vo = new com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String>();
        }
      }
    }
    return _vo;
  }



  public String getFormat() {
    return format;
  }


  public void setFormat(String format) {
    if (supportsObjectVersioning()) {
      com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.VersionedObject<String> _vo = lazyInit_oldVersionsOfformat();
      synchronized (_vo) {
        _vo.add(this.format);
        this.format = format;
      }
      return;
    }
    this.format = format;
  }


  public String versionedGetFormat(long _version) {
    if (oldVersionsOfformat == null) {
      return format;
    }
    String _local = format;
    com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.Version<String> _ret = oldVersionsOfformat.getVersion(_version);
    if (_ret == null) {
      return _local;
    }
    return _ret.object;
  }

  public void unversionedSetFormat(String format) {
    this.format = format;
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


  public static class ObjectVersion extends com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase {

    public ObjectVersion(GeneralXynaObject xo, long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
      super(xo, version, changeSetsOfMembers);
    }

    protected boolean memberEquals(com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase o) {
      ObjectVersion other = (ObjectVersion) o;
      InvalidDateStringException xoc = (InvalidDateStringException) xo;
      InvalidDateStringException xoco = (InvalidDateStringException) other.xo;
      if (!equal(xoc.versionedGetDate1(this.version), xoco.versionedGetDate1(other.version))) {
        return false;
      }
      if (!equal(xoc.versionedGetFormat(this.version), xoco.versionedGetFormat(other.version))) {
        return false;
      }
      return true;
    }

    public int calcHashOfMembers(java.util.Stack<GeneralXynaObject> stack) {
      int hash = 1;
      InvalidDateStringException xoc = (InvalidDateStringException) xo;
      String date1 = xoc.versionedGetDate1(this.version);
      hash = hash * 31 + (date1 == null ? 0 : date1.hashCode());
      String format = xoc.versionedGetFormat(this.version);
      hash = hash * 31 + (format == null ? 0 : format.hashCode());
      return hash;
    }

  }


  public ObjectVersion createObjectVersion(long version, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers) {
    return new ObjectVersion(this, version, changeSetsOfMembers);
  }



  public void collectChanges(long start, long end, java.util.IdentityHashMap<GeneralXynaObject, com.gip.xyna.utils.misc.DataRangeCollection> changeSetsOfMembers, java.util.Set<Long> datapoints) {
    XOUtils.addChangesForSimpleMember(oldVersionsOfdate1, start, end, datapoints);
    XOUtils.addChangesForSimpleMember(oldVersionsOfformat, start, end, datapoints);
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
    com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.XMLHelper.beginExceptionType(xml, varName, "InvalidDateStringException", "base.date", objectId, refId, RevisionManagement.getRevisionByClass(getClass()), cache);
    if (objectId != -2) {
      XMLHelper.appendData(xml, "date1", versionedGetDate1(version), version, cache);
      XMLHelper.appendData(xml, "format", versionedGetFormat(version), version, cache);
    }
    com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.XMLHelper.endExceptionType(xml);
    return xml.toString();
  }


  protected void fillVars(InvalidDateStringException source, boolean deep) {
    this.date1 = source.date1;
    this.format = source.format;
  }

  public InvalidDateStringException cloneWithoutCause() {
    return cloneWithoutCause(true);
  }

  public InvalidDateStringException cloneWithoutCause(boolean deep) {
    InvalidDateStringException cloned = new InvalidDateStringException();
    cloned.fillVars(this, deep);
    return cloned;
  }


  public void set(String name, Object o) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    if ("date1".equals(name)) {
      XOUtils.checkCastability(o, String.class, "date1");
      setDate1((String) o);
    } else if ("format".equals(name)) {
      XOUtils.checkCastability(o, String.class, "format");
      setFormat((String) o);
    } else {
      throw new XDEV_PARAMETER_NAME_NOT_FOUND(name);
    }
  }


  /**
   * gets membervariable by name or path. e.g. get("myVar.myChild") gets
   * the child variable of the membervariable named "myVar" and is equivalent
   * to getMyVar().getMyChild()
   * @param name variable name or path separated by ".".
   */
  public Object get(String name) throws InvalidObjectPathException {
    String[] varNames = new String[]{"date1", "format"};
    Object[] vars = new Object[]{this.date1, this.format};
    Object o = XOUtils.getIfNameIsInVarNames(varNames, vars, name);
    if (o == XOUtils.VARNAME_NOTFOUND) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(name));
    }
    return o;
  }

  protected static class InternalBuilder<_GEN_DOM_TYPE extends InvalidDateStringException, _GEN_BUILDER_TYPE extends InternalBuilder<_GEN_DOM_TYPE, _GEN_BUILDER_TYPE>>  {

    protected _GEN_DOM_TYPE instance;

    protected InternalBuilder(InvalidDateStringException instance) {
      this.instance = (_GEN_DOM_TYPE) instance;
    }

    public InvalidDateStringException instance() {
      return (InvalidDateStringException) instance;
    }

    public _GEN_BUILDER_TYPE date1(String date1) {
      this.instance.unversionedSetDate1(date1);
      return (_GEN_BUILDER_TYPE) this;
    }

    public _GEN_BUILDER_TYPE format(String format) {
      this.instance.unversionedSetFormat(format);
      return (_GEN_BUILDER_TYPE) this;
    }

  }

  public static class Builder extends InternalBuilder<InvalidDateStringException, Builder> {
    public Builder() {
      super(new InvalidDateStringException());
    }
    public Builder(InvalidDateStringException instance) {
      super(instance);
    }
  }

  public Builder buildInvalidDateStringException() {
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
      foundField = InvalidDateStringException.class.getDeclaredField(target_fieldname);
    } catch (NoSuchFieldException e) {
    }
    if (foundField == null) {
      return XynaException.getField(target_fieldname);
    } else {
      foundField.setAccessible(true);
      fieldMap.put(target_fieldname, foundField);
      return foundField;
    }
  }

}
